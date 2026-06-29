package com.ai.phoneagent.platform.android.virtualdisplay

import android.content.Context
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import com.ai.phoneagent.core.capability.CapabilityResult
import com.ai.phoneagent.core.capability.CaptureFormat
import com.ai.phoneagent.core.capability.CaptureRequest
import com.ai.phoneagent.core.capability.CaptureResult
import com.ai.phoneagent.core.capability.VirtualDisplayStartRequest
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DisplayManagerVirtualDisplayController(
    context: Context,
    private val store: VirtualDisplaySessionStore = InMemoryVirtualDisplaySessionStore(),
) : AndroidVirtualDisplayController {
    private val displayManager = context.getSystemService(DisplayManager::class.java)

    override val diagnostics: Map<String, String> = mapOf(
        "platform" to "android",
        "backend" to "display-manager",
        "mode" to "own-content",
    )

    override suspend fun start(request: VirtualDisplayStartRequest): CapabilityResult<AndroidVirtualDisplaySession> =
        withContext(Dispatchers.IO) {
            val validationError = validate(request)
            if (validationError != null) {
                return@withContext CapabilityResult.failure(validationError)
            }

            val sessionId = UUID.randomUUID().toString()
            val imageReader = ImageReader.newInstance(
                request.width,
                request.height,
                PixelFormat.RGBA_8888,
                MAX_IMAGES,
            )

            try {
                val virtualDisplay = displayManager.createVirtualDisplay(
                    DISPLAY_NAME_PREFIX + sessionId.take(8),
                    request.width,
                    request.height,
                    request.densityDpi,
                    imageReader.surface,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY,
                ) ?: return@withContext releaseAndFail(imageReader, VirtualDisplayErrors.operationFailed("DisplayManager returned null."))

                val displayId = virtualDisplay.display?.displayId ?: -1
                val activeSession = ActiveVirtualDisplaySession(
                    descriptor = AndroidVirtualDisplaySession(
                        sessionId = sessionId,
                        displayId = displayId,
                        width = request.width,
                        height = request.height,
                        densityDpi = request.densityDpi,
                        diagnostics = mapOf("display_id" to displayId.toString()),
                    ),
                    virtualDisplay = virtualDisplay,
                    imageReader = imageReader,
                )
                store.put(activeSession)
                CapabilityResult.success(activeSession.descriptor)
            } catch (error: SecurityException) {
                releaseAndFail(imageReader, VirtualDisplayErrors.securityDenied(error.message))
            } catch (error: IllegalArgumentException) {
                releaseAndFail(imageReader, VirtualDisplayErrors.invalidRequest(error.message ?: "Invalid virtual display request."))
            } catch (error: IllegalStateException) {
                releaseAndFail(imageReader, VirtualDisplayErrors.operationFailed(error.message ?: "Virtual display start failed."))
            }
        }

    override suspend fun stop(sessionId: String): CapabilityResult<Unit> = withContext(Dispatchers.IO) {
        val session = store.remove(sessionId)
            ?: return@withContext CapabilityResult.failure<Unit>(VirtualDisplayErrors.sessionNotFound(sessionId))
        session.release()
        CapabilityResult.success(Unit)
    }

    override suspend fun capture(sessionId: String, request: CaptureRequest): CaptureResult = withContext(Dispatchers.IO) {
        val session = store.get(sessionId)
            ?: return@withContext CaptureResult(
                source = CAPTURE_SOURCE,
                error = VirtualDisplayErrors.sessionNotFound(sessionId),
            )
        val image = session.imageReader.acquireLatestImage()
            ?: return@withContext CaptureResult(
                source = CAPTURE_SOURCE,
                error = VirtualDisplayErrors.frameUnavailable(),
            )
        image.use { currentImage ->
            currentImage.toCaptureResult(request)
        }
    }

    private fun validate(request: VirtualDisplayStartRequest) = when {
        request.width !in MIN_DIMENSION..MAX_DIMENSION -> VirtualDisplayErrors.invalidRequest("Width must be between $MIN_DIMENSION and $MAX_DIMENSION.")
        request.height !in MIN_DIMENSION..MAX_DIMENSION -> VirtualDisplayErrors.invalidRequest("Height must be between $MIN_DIMENSION and $MAX_DIMENSION.")
        request.densityDpi !in MIN_DENSITY..MAX_DENSITY -> VirtualDisplayErrors.invalidRequest("Density must be between $MIN_DENSITY and $MAX_DENSITY.")
        else -> null
    }

    private fun Image.toCaptureResult(request: CaptureRequest): CaptureResult {
        val plane = planes.firstOrNull()
            ?: return CaptureResult(source = CAPTURE_SOURCE, error = VirtualDisplayErrors.frameUnavailable())
        val bitmap = plane.toBitmap(width, height)
        val croppedBitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height)
        if (croppedBitmap !== bitmap) {
            bitmap.recycle()
        }
        val bytes = ByteArrayOutputStream().use { output ->
            val format = when (request.format) {
                CaptureFormat.Png -> Bitmap.CompressFormat.PNG
                CaptureFormat.Jpeg -> Bitmap.CompressFormat.JPEG
            }
            croppedBitmap.compress(format, JPEG_QUALITY, output)
            output.toByteArray()
        }
        croppedBitmap.recycle()
        return CaptureResult(
            bytes = bytes,
            width = width,
            height = height,
            source = CAPTURE_SOURCE,
        )
    }

    private fun Image.Plane.toBitmap(width: Int, height: Int): Bitmap {
        val buffer: ByteBuffer = buffer
        val rowPadding = rowStride - pixelStride * width
        return Bitmap.createBitmap(width + rowPadding / pixelStride, height, Bitmap.Config.ARGB_8888).apply {
            copyPixelsFromBuffer(buffer)
        }
    }

    private fun releaseAndFail(
        imageReader: ImageReader,
        error: com.ai.phoneagent.core.capability.CapabilityError,
    ): CapabilityResult<AndroidVirtualDisplaySession> {
        imageReader.close()
        return CapabilityResult.failure(error)
    }

    private companion object {
        const val DISPLAY_NAME_PREFIX = "AriesRe0-"
        const val CAPTURE_SOURCE = "android-virtual-display"
        const val JPEG_QUALITY = 92
        const val MAX_IMAGES = 2
        const val MIN_DIMENSION = 1
        const val MAX_DIMENSION = 4096
        const val MIN_DENSITY = 120
        const val MAX_DENSITY = 640
    }
}

private fun Image.use(block: (Image) -> CaptureResult): CaptureResult {
    try {
        return block(this)
    } finally {
        close()
    }
}
