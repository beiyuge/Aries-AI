package com.ai.phoneagent.platform.android.screen

import com.ai.phoneagent.core.capability.CapabilityResult
import com.ai.phoneagent.core.capability.CapabilitySelfTest
import com.ai.phoneagent.core.capability.CaptureRequest
import com.ai.phoneagent.core.capability.CaptureResult
import com.ai.phoneagent.core.capability.ScreenCaptureCapability

class AndroidScreenCaptureCapability(
    private val session: ScreenCaptureSession = AndroidScreenCaptureRuntime.session,
) : ScreenCaptureCapability, CapabilitySelfTest {
    override val id = session.health.value.id
    override val health = session.health

    override suspend fun capture(request: CaptureRequest): CaptureResult = session.capture(request)

    override fun runSelfTest(): CapabilityResult<String> = if (health.value.available) {
        CapabilityResult.success("${id.value}: screen capture session ready")
    } else {
        CapabilityResult.failure(health.value.lastError ?: mediaProjectionRequiredError())
    }
}
