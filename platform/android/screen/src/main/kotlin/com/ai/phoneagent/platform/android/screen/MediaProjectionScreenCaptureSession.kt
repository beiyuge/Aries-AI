package com.ai.phoneagent.platform.android.screen

import android.content.Context
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.os.Handler
import android.os.HandlerThread
import com.ai.phoneagent.core.capability.CapabilityError
import com.ai.phoneagent.core.capability.CapabilityHealth
import com.ai.phoneagent.core.capability.CapabilityIds
import com.ai.phoneagent.core.capability.CaptureFormat
import com.ai.phoneagent.core.capability.CaptureRequest
import com.ai.phoneagent.core.capability.CaptureResult
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

class MediaProjectionScreenCaptureSession : ScreenCaptureSession {
    private val lock = Any()
    private val _health = MutableStateFlow(screenCaptureMissingHealth())
    private val _latestFrame = MutableStateFlow<RawScreenFrame?>(null)
    private var projection: MediaProjection? = null
    private var projectionCallback: MediaProjection.Callback? = null
    private var imageReader: ImageReader? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var handlerThread: HandlerThread? = null

    override val health = _health.asStateFlow()

    fun markStarting() {
        _health.value = CapabilityHealth.unavailable(
            id = CapabilityIds.ScreenCapture,
            error = CapabilityError(
                code = "screen_capture.session_starting",
                message = "Android screen capture session is starting.",
                recoverable = true,
            ),
            diagnostics = screenCaptureDiagnostics("starting"),
        )
    }

    fun attach(context: Context, mediaProjection: MediaProjection) {
        synchronized(lock) {
            releaseLocked(stopProjection = true)
            _latestFrame.value = null
            val metrics = context.resources.displayMetrics
            val width = metrics.widthPixels
            val height = metrics.heightPixels
            val thread = HandlerThread("aries-re0-screen-capture").apply { start() }
            val handler = Handler(thread.looper)
            val reader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 3)
            val callback = object : MediaProjection.Callback() {
                override fun onStop() {
                    handleProjectionStopped()
                }
            }

            mediaProjection.registerCallback(callback, handler)
            reader.setOnImageAvailableListener(::onImageAvailable, handler)
            projection = mediaProjection
            projectionCallback = callback
            imageReader = reader
            handlerThread = thread
            val display = mediaProjection.createVirtualDisplay(
                "aries-re0-screen-capture",
                width,
                height,
                metrics.densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                reader.surface,
                null,
                handler,
            )

            virtualDisplay = display
            _health.value = CapabilityHealth.ready(
                id = CapabilityIds.ScreenCapture,
                diagnostics = screenCaptureDiagnostics(
                    session = "ready",
                    extras = mapOf(
                        "width" to width.toString(),
                        "height" to height.toString(),
                        "densityDpi" to metrics.densityDpi.toString(),
                    ),
                ),
            )
        }
    }

    fun fail(error: CapabilityError) {
        synchronized(lock) {
            releaseLocked(stopProjection = true)
            _health.value = CapabilityHealth.failed(
                id = CapabilityIds.ScreenCapture,
                error = error,
                diagnostics = screenCaptureDiagnostics("failed"),
            )
        }
    }

    override suspend fun capture(request: CaptureRequest): CaptureResult {
        val currentHealth = health.value
        if (!currentHealth.available) {
            return CaptureResult(
                source = SCREEN_CAPTURE_SOURCE,
                error = currentHealth.lastError ?: mediaProjectionRequiredError(),
            )
        }

        val frame = _latestFrame.value ?: withTimeoutOrNull(FRAME_TIMEOUT_MS) {
            _latestFrame.filterNotNull().first()
        }
        if (frame == null) {
            return CaptureResult(
                source = SCREEN_CAPTURE_SOURCE,
                error = CapabilityError(
                    code = "screen_capture.frame_timeout",
                    message = "No MediaProjection frame arrived within ${FRAME_TIMEOUT_MS}ms.",
                    recoverable = true,
                    suggestedAction = "Retry capture or restart the screen capture session.",
                ),
            )
        }

        return encodeFrame(frame, request)
    }

    override fun stop() {
        synchronized(lock) {
            releaseLocked(stopProjection = true)
            _latestFrame.value = null
            _health.value = screenCaptureMissingHealth()
        }
    }

    private fun onImageAvailable(reader: ImageReader) {
        val image = try {
            reader.acquireLatestImage()
        } catch (error: IllegalStateException) {
            fail(frameReadError(error::class.qualifiedName))
            null
        } ?: return

        try {
            image.toRawFrame()?.let { frame -> _latestFrame.value = frame }
        } catch (error: IllegalArgumentException) {
            fail(frameReadError(error::class.qualifiedName))
        } catch (error: IllegalStateException) {
            fail(frameReadError(error::class.qualifiedName))
        } finally {
            image.close()
        }
    }

    private fun Image.toRawFrame(): RawScreenFrame? {
        val plane = planes.firstOrNull() ?: return null
        val pixelStride = plane.pixelStride
        val rowStride = plane.rowStride
        if (pixelStride <= 0 || rowStride < width * pixelStride) {
            return null
        }
        val paddedWidth = rowStride / pixelStride
        val padded = Bitmap.createBitmap(paddedWidth, height, Bitmap.Config.ARGB_8888)
        plane.buffer.rewind()
        padded.copyPixelsFromBuffer(plane.buffer)
        val visible = if (paddedWidth == width) {
            padded
        } else {
            Bitmap.createBitmap(padded, 0, 0, width, height)
        }
        val pixels = IntArray(width * height)
        visible.getPixels(pixels, 0, width, 0, 0, width, height)
        if (visible !== padded) {
            visible.recycle()
        }
        padded.recycle()
        return RawScreenFrame(width = width, height = height, pixels = pixels)
    }

    private suspend fun encodeFrame(
        frame: RawScreenFrame,
        request: CaptureRequest,
    ): CaptureResult = withContext(Dispatchers.Default) {
        val source = Bitmap.createBitmap(
            frame.pixels,
            frame.width,
            frame.height,
            Bitmap.Config.ARGB_8888,
        )
        val targetWidth = request.maxWidth
            ?.takeIf { it > 0 && it < frame.width }
            ?: frame.width
        val targetHeight = (frame.height.toLong() * targetWidth / frame.width)
            .toInt()
            .coerceAtLeast(1)
        val output = if (targetWidth == frame.width) {
            source
        } else {
            Bitmap.createScaledBitmap(source, targetWidth, targetHeight, true)
        }
        val bytes = ByteArrayOutputStream().use { stream ->
            val format = when (request.format) {
                CaptureFormat.Png -> Bitmap.CompressFormat.PNG
                CaptureFormat.Jpeg -> Bitmap.CompressFormat.JPEG
            }
            output.compress(format, JPEG_QUALITY, stream)
            stream.toByteArray()
        }
        if (output !== source) {
            output.recycle()
        }
        source.recycle()
        CaptureResult(
            bytes = bytes,
            width = targetWidth,
            height = targetHeight,
            source = SCREEN_CAPTURE_SOURCE,
        )
    }

    private fun handleProjectionStopped() {
        synchronized(lock) {
            releaseLocked(stopProjection = false)
            _latestFrame.value = null
            _health.value = CapabilityHealth.unavailable(
                id = CapabilityIds.ScreenCapture,
                error = CapabilityError(
                    code = "screen_capture.session_stopped",
                    message = "Android stopped the MediaProjection session.",
                    recoverable = true,
                    suggestedAction = "Start screen capture consent again.",
                ),
                diagnostics = screenCaptureDiagnostics("stopped"),
            )
        }
    }

    private fun releaseLocked(stopProjection: Boolean) {
        imageReader?.setOnImageAvailableListener(null, null)
        virtualDisplay?.release()
        imageReader?.close()
        projectionCallback?.let { callback -> projection?.unregisterCallback(callback) }
        if (stopProjection) {
            projection?.stop()
        }
        handlerThread?.quitSafely()
        virtualDisplay = null
        imageReader = null
        projectionCallback = null
        projection = null
        handlerThread = null
    }

    private fun frameReadError(causeClass: String?): CapabilityError = CapabilityError(
        code = "screen_capture.frame_read_failed",
        message = "The MediaProjection frame could not be decoded.",
        recoverable = true,
        causeClass = causeClass,
    )

    private data class RawScreenFrame(
        val width: Int,
        val height: Int,
        val pixels: IntArray,
    )

    private companion object {
        const val FRAME_TIMEOUT_MS = 5_000L
        const val JPEG_QUALITY = 90
    }
}
