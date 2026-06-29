package com.ai.phoneagent.platform.android.background

import com.ai.phoneagent.core.capability.Capability
import com.ai.phoneagent.core.capability.CapabilityError
import com.ai.phoneagent.core.capability.CapabilityHealth
import com.ai.phoneagent.core.capability.CapabilityId
import com.ai.phoneagent.core.capability.CapabilityIds
import com.ai.phoneagent.core.capability.CapabilityResult
import com.ai.phoneagent.core.capability.CapabilitySelfTest
import kotlinx.coroutines.flow.MutableStateFlow

class AndroidBackgroundTasksCapability : Capability, CapabilitySelfTest {
    override val id: CapabilityId = CapabilityIds.BackgroundTasks
    override val health = MutableStateFlow(
        CapabilityHealth.unavailable(
            id = id,
            error = notImplementedError(),
            diagnostics = mapOf("platform" to "android", "backend" to "work-manager"),
        ),
    )

    override fun runSelfTest(): CapabilityResult<String> = CapabilityResult.failure(notImplementedError())

    private fun notImplementedError(): CapabilityError = CapabilityError(
        code = "background_tasks.not_implemented",
        message = "Android background task backend is not implemented yet.",
        recoverable = true,
    )
}
