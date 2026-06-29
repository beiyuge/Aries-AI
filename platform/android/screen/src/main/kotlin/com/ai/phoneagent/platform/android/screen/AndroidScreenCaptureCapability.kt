package com.ai.phoneagent.platform.android.screen

import com.ai.phoneagent.core.capability.CapabilityAction
import com.ai.phoneagent.core.capability.CapabilityError
import com.ai.phoneagent.core.capability.CapabilityHealth
import com.ai.phoneagent.core.capability.CapabilityId
import com.ai.phoneagent.core.capability.CapabilityIds
import com.ai.phoneagent.core.capability.CapabilityRequirement
import com.ai.phoneagent.core.capability.CapabilityResult
import com.ai.phoneagent.core.capability.CapabilitySelfTest
import com.ai.phoneagent.core.capability.CaptureRequest
import com.ai.phoneagent.core.capability.CaptureResult
import com.ai.phoneagent.core.capability.ScreenCaptureCapability
import kotlinx.coroutines.flow.MutableStateFlow

class AndroidScreenCaptureCapability(
    private val session: ScreenCaptureSession = ScreenCaptureSession.Missing,
) : ScreenCaptureCapability, CapabilitySelfTest {
    override val id: CapabilityId = CapabilityIds.ScreenCapture
    override val health = MutableStateFlow(readHealth())

    override suspend fun capture(request: CaptureRequest): CaptureResult {
        refreshHealth()
        return CaptureResult(
            source = "android-media-projection",
            error = health.value.lastError ?: mediaProjectionRequiredError(),
        )
    }

    override fun runSelfTest(): CapabilityResult<String> {
        refreshHealth()
        return if (health.value.available) {
            CapabilityResult.success("${id.value}: screen capture session ready")
        } else {
            CapabilityResult.failure(health.value.lastError ?: mediaProjectionRequiredError())
        }
    }

    fun refreshHealth() {
        health.value = readHealth()
    }

    private fun readHealth(): CapabilityHealth {
        val diagnostics = mapOf(
            "platform" to "android",
            "backend" to "media-projection",
            "session" to session.name.lowercase(),
        )
        return when (session) {
            ScreenCaptureSession.Ready -> CapabilityHealth.ready(id = id, diagnostics = diagnostics)
            ScreenCaptureSession.Missing -> CapabilityHealth.permissionRequired(
                id = id,
                missingRequirements = listOf(screenCaptureRequirement()),
                diagnostics = diagnostics,
            )
        }
    }

    private fun mediaProjectionRequiredError(): CapabilityError = CapabilityError(
        code = "screen_capture.media_projection_required",
        message = "Android MediaProjection consent/session is required before capture.",
        recoverable = true,
        suggestedAction = "Start screen capture consent from the Android backend.",
    )
}

enum class ScreenCaptureSession {
    Missing,
    Ready,
}

fun screenCaptureRequirement(): CapabilityRequirement = CapabilityRequirement(
    id = "screen.capture.consent",
    title = "Screen Capture",
    description = "Grant Android screen capture consent for screenshots.",
    action = CapabilityAction.None,
)
