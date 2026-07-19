package com.ai.phoneagent.platform.android.screen

import com.ai.phoneagent.core.capability.CapabilityAction
import com.ai.phoneagent.core.capability.CapabilityError
import com.ai.phoneagent.core.capability.CapabilityHealth
import com.ai.phoneagent.core.capability.CapabilityIds
import com.ai.phoneagent.core.capability.CapabilityRequirement
import com.ai.phoneagent.core.capability.CaptureRequest
import com.ai.phoneagent.core.capability.CaptureResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

interface ScreenCaptureSession {
    val health: StateFlow<CapabilityHealth>

    suspend fun capture(request: CaptureRequest): CaptureResult

    fun stop()
}

object MissingScreenCaptureSession : ScreenCaptureSession {
    override val health = MutableStateFlow(screenCaptureMissingHealth())

    override suspend fun capture(request: CaptureRequest): CaptureResult = CaptureResult(
        source = SCREEN_CAPTURE_SOURCE,
        error = mediaProjectionRequiredError(),
    )

    override fun stop() = Unit
}

internal const val SCREEN_CAPTURE_SOURCE = "android-media-projection"

internal fun screenCaptureMissingHealth(): CapabilityHealth = CapabilityHealth.permissionRequired(
    id = CapabilityIds.ScreenCapture,
    missingRequirements = listOf(screenCaptureRequirement()),
    diagnostics = screenCaptureDiagnostics("missing"),
)

internal fun screenCaptureDiagnostics(
    session: String,
    extras: Map<String, String> = emptyMap(),
): Map<String, String> = mapOf(
    "platform" to "android",
    "backend" to "media-projection",
    "session" to session,
) + extras

internal fun mediaProjectionRequiredError(): CapabilityError = CapabilityError(
    code = "screen_capture.media_projection_required",
    message = "Android MediaProjection consent/session is required before capture.",
    recoverable = true,
    suggestedAction = "Start screen capture consent from the Android backend.",
)

fun screenCaptureRequirement(): CapabilityRequirement = CapabilityRequirement(
    id = "screen.capture.consent",
    title = "Screen Capture",
    description = "Grant Android screen capture consent for screenshots.",
    action = CapabilityAction.None,
)
