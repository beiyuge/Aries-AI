package com.ai.phoneagent.platform.android.floating

import com.ai.phoneagent.core.capability.CapabilityError
import com.ai.phoneagent.core.capability.CapabilityHealth
import com.ai.phoneagent.core.capability.CapabilityId
import com.ai.phoneagent.core.capability.CapabilityIds
import com.ai.phoneagent.core.capability.CapabilityResult
import com.ai.phoneagent.core.capability.CapabilitySelfTest
import com.ai.phoneagent.core.capability.FloatingWindowCapability
import com.ai.phoneagent.core.capability.FloatingWindowRequest
import com.ai.phoneagent.core.capability.FloatingWindowSession
import com.ai.phoneagent.core.capability.FloatingWindowState
import kotlinx.coroutines.flow.MutableStateFlow

class AndroidFloatingWindowCapability : FloatingWindowCapability, CapabilitySelfTest {
    override val id: CapabilityId = CapabilityIds.FloatingWindow
    override val state = MutableStateFlow(FloatingWindowState())
    override val health = MutableStateFlow(unavailableHealth())

    override suspend fun show(request: FloatingWindowRequest): CapabilityResult<FloatingWindowSession> =
        CapabilityResult.failure(notImplementedError())

    override suspend fun hide(sessionId: String): CapabilityResult<Unit> =
        CapabilityResult.failure(notImplementedError())

    override fun runSelfTest(): CapabilityResult<String> = CapabilityResult.failure(notImplementedError())

    private fun unavailableHealth(): CapabilityHealth = CapabilityHealth.unavailable(
        id = id,
        error = notImplementedError(),
        diagnostics = mapOf("platform" to "android", "backend" to "floating-window"),
    )

    private fun notImplementedError(): CapabilityError = CapabilityError(
        code = "floating_window.not_implemented",
        message = "Android floating window backend is not implemented yet.",
        recoverable = true,
    )
}
