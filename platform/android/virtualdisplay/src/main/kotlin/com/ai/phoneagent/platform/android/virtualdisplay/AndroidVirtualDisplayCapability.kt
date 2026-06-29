package com.ai.phoneagent.platform.android.virtualdisplay

import com.ai.phoneagent.core.capability.CapabilityError
import com.ai.phoneagent.core.capability.CapabilityHealth
import com.ai.phoneagent.core.capability.CapabilityId
import com.ai.phoneagent.core.capability.CapabilityIds
import com.ai.phoneagent.core.capability.CapabilityResult
import com.ai.phoneagent.core.capability.CapabilitySelfTest
import com.ai.phoneagent.core.capability.CaptureRequest
import com.ai.phoneagent.core.capability.CaptureResult
import com.ai.phoneagent.core.capability.VirtualDisplayCapability
import com.ai.phoneagent.core.capability.VirtualDisplayLifecycle
import com.ai.phoneagent.core.capability.VirtualDisplayResult
import com.ai.phoneagent.core.capability.VirtualDisplayStartRequest
import com.ai.phoneagent.core.capability.VirtualDisplayState
import kotlinx.coroutines.flow.MutableStateFlow

class AndroidVirtualDisplayCapability : VirtualDisplayCapability, CapabilitySelfTest {
    override val id: CapabilityId = CapabilityIds.VirtualDisplay
    override val state = MutableStateFlow(VirtualDisplayState(lifecycle = VirtualDisplayLifecycle.Idle))
    override val health = MutableStateFlow(unavailableHealth())

    override suspend fun start(request: VirtualDisplayStartRequest): VirtualDisplayResult = VirtualDisplayResult(
        sessionId = "",
        displayId = -1,
        error = notImplementedError(),
    )

    override suspend fun stop(sessionId: String): CapabilityResult<Unit> =
        CapabilityResult.failure(notImplementedError())

    override suspend fun capture(sessionId: String, request: CaptureRequest): CaptureResult = CaptureResult(
        source = "android-virtual-display",
        error = notImplementedError(),
    )

    override fun runSelfTest(): CapabilityResult<String> = CapabilityResult.failure(notImplementedError())

    private fun unavailableHealth(): CapabilityHealth = CapabilityHealth.unavailable(
        id = id,
        error = notImplementedError(),
        diagnostics = mapOf("platform" to "android", "backend" to "virtual-display"),
    )

    private fun notImplementedError(): CapabilityError = CapabilityError(
        code = "virtual_display.not_implemented",
        message = "Android virtual display backend is not implemented yet.",
        recoverable = true,
    )
}
