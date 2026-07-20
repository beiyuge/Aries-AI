package com.ai.phoneagent.platform.android.virtualdisplay

import android.content.Context
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import android.view.Display
import com.ai.phoneagent.core.capability.CapabilityResult
import com.ai.phoneagent.core.capability.CaptureRequest
import com.ai.phoneagent.core.capability.CaptureResult
import com.ai.phoneagent.core.capability.VirtualDisplayLaunchRequest
import com.ai.phoneagent.core.capability.VirtualDisplayStartRequest
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

internal class DisplayManagerVirtualDisplayController(
    context: Context,
    private val store: VirtualDisplaySessionStore = InMemoryVirtualDisplaySessionStore(),
    private val contentLauncher: VirtualDisplayContentLauncher =
        ActivityOptionsVirtualDisplayContentLauncher(context.applicationContext),
) : AndroidVirtualDisplayController {
    private val displayManager = context.getSystemService(DisplayManager::class.java)

    override val diagnostics: Map<String, String> = mapOf(
        "platform" to "android",
        "backend" to "display-manager",
        "mode" to "app-content",
        "focus_policy" to "default-display-preserved",
        "ime_policy" to "accessibility-set-text",
        "frame_guard" to "near-black-detection",
    )

    override suspend fun start(
        request: VirtualDisplayStartRequest,
    ): CapabilityResult<AndroidVirtualDisplaySession> = withContext(Dispatchers.IO) {
        val validationError = validate(request)
        if (validationError != null) {
            return@withContext CapabilityResult.failure(validationError)
        }

        val sessionId = UUID.randomUUID().toString()
        val frameThread = HandlerThread("aries-re0-virtual-display-${sessionId.take(8)}").apply {
            start()
        }
        val imageReader = ImageReader.newInstance(
            request.width,
            request.height,
            PixelFormat.RGBA_8888,
            MAX_IMAGES,
        )
        val frameBuffer = VirtualDisplayFrameBuffer().also { buffer ->
            buffer.attach(imageReader, Handler(frameThread.looper))
        }

        try {
            val virtualDisplay = displayManager.createVirtualDisplay(
                DISPLAY_NAME_PREFIX + sessionId.take(8),
                request.width,
                request.height,
                request.densityDpi,
                imageReader.surface,
                DISPLAY_FLAGS,
            ) ?: return@withContext releaseAndFail(
                imageReader = imageReader,
                frameThread = frameThread,
                error = VirtualDisplayErrors.operationFailed("DisplayManager returned null."),
            )

            val displayId = virtualDisplay.display?.displayId ?: Display.INVALID_DISPLAY
            if (displayId == Display.INVALID_DISPLAY) {
                virtualDisplay.release()
                return@withContext releaseAndFail(
                    imageReader = imageReader,
                    frameThread = frameThread,
                    error = VirtualDisplayErrors.operationFailed("Virtual display has no valid display id."),
                )
            }
            val activeSession = ActiveVirtualDisplaySession(
                descriptor = AndroidVirtualDisplaySession(
                    sessionId = sessionId,
                    displayId = displayId,
                    width = request.width,
                    height = request.height,
                    densityDpi = request.densityDpi,
                    diagnostics = mapOf(
                        "display_id" to displayId.toString(),
                        "width" to request.width.toString(),
                        "height" to request.height.toString(),
                        "density_dpi" to request.densityDpi.toString(),
                    ),
                ),
                virtualDisplay = virtualDisplay,
                imageReader = imageReader,
                frameBuffer = frameBuffer,
                frameThread = frameThread,
            )
            store.put(activeSession)
            CapabilityResult.success(activeSession.descriptor)
        } catch (error: SecurityException) {
            releaseAndFail(
                imageReader,
                frameThread,
                VirtualDisplayErrors.securityDenied(error.message),
            )
        } catch (error: IllegalArgumentException) {
            releaseAndFail(
                imageReader,
                frameThread,
                VirtualDisplayErrors.invalidRequest(error.message ?: "Invalid virtual display request."),
            )
        } catch (error: IllegalStateException) {
            releaseAndFail(
                imageReader,
                frameThread,
                VirtualDisplayErrors.operationFailed(error.message ?: "Virtual display start failed."),
            )
        }
    }

    override suspend fun launch(
        sessionId: String,
        request: VirtualDisplayLaunchRequest,
    ): CapabilityResult<Unit> {
        val session = store.get(sessionId)
            ?: return CapabilityResult.failure(VirtualDisplayErrors.sessionNotFound(sessionId))
        return contentLauncher.launch(session.descriptor.displayId, request)
    }

    override suspend fun stop(sessionId: String): CapabilityResult<Unit> = withContext(Dispatchers.IO) {
        val session = store.remove(sessionId)
            ?: return@withContext CapabilityResult.failure<Unit>(
                VirtualDisplayErrors.sessionNotFound(sessionId),
            )
        session.release()
        CapabilityResult.success(Unit)
    }

    override suspend fun capture(
        sessionId: String,
        request: CaptureRequest,
    ): CaptureResult {
        val session = store.get(sessionId)
            ?: return CaptureResult(
                source = CAPTURE_SOURCE,
                error = VirtualDisplayErrors.sessionNotFound(sessionId),
            )
        var sawBlackFrame = false
        val frame = withTimeoutOrNull(FRAME_TIMEOUT_MS) {
            session.frameBuffer.frames.filterNotNull().first { candidate ->
                val black = VirtualDisplayFrameAnalyzer.analyze(candidate).likelyBlack
                sawBlackFrame = sawBlackFrame || black
                !black
            }
        }
        if (frame == null) {
            return CaptureResult(
                source = CAPTURE_SOURCE,
                error = session.frameBuffer.lastError.value
                    ?: if (sawBlackFrame) {
                        VirtualDisplayErrors.blackFrame()
                    } else {
                        VirtualDisplayErrors.frameUnavailable()
                    },
            )
        }
        return VirtualDisplayFrameEncoder.encode(frame, request, CAPTURE_SOURCE)
    }

    private fun validate(request: VirtualDisplayStartRequest) = when {
        request.width !in MIN_DIMENSION..MAX_DIMENSION ->
            VirtualDisplayErrors.invalidRequest(
                "Width must be between $MIN_DIMENSION and $MAX_DIMENSION.",
            )
        request.height !in MIN_DIMENSION..MAX_DIMENSION ->
            VirtualDisplayErrors.invalidRequest(
                "Height must be between $MIN_DIMENSION and $MAX_DIMENSION.",
            )
        request.densityDpi !in MIN_DENSITY..MAX_DENSITY ->
            VirtualDisplayErrors.invalidRequest(
                "Density must be between $MIN_DENSITY and $MAX_DENSITY.",
            )
        else -> null
    }

    private fun releaseAndFail(
        imageReader: ImageReader,
        frameThread: HandlerThread,
        error: com.ai.phoneagent.core.capability.CapabilityError,
    ): CapabilityResult<AndroidVirtualDisplaySession> {
        imageReader.setOnImageAvailableListener(null, null)
        imageReader.close()
        frameThread.quitSafely()
        return CapabilityResult.failure(error)
    }

    private companion object {
        const val DISPLAY_NAME_PREFIX = "AriesRe0-"
        const val CAPTURE_SOURCE = "android-virtual-display"
        const val MAX_IMAGES = 3
        const val FRAME_TIMEOUT_MS = 3_000L
        const val MIN_DIMENSION = 64
        const val MAX_DIMENSION = 4_096
        const val MIN_DENSITY = 120
        const val MAX_DENSITY = 640
        const val DISPLAY_FLAGS = DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC or
            DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY or
            DisplayManager.VIRTUAL_DISPLAY_FLAG_PRESENTATION
    }
}
