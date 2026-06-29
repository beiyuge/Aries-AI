package com.ai.phoneagent.platform.android.nativeruntime

import com.ai.phoneagent.core.capability.Capability
import com.ai.phoneagent.core.capability.CapabilityError
import com.ai.phoneagent.core.capability.CapabilityHealth
import com.ai.phoneagent.core.capability.CapabilityId
import com.ai.phoneagent.core.capability.CapabilityIds
import com.ai.phoneagent.core.capability.CapabilityResult
import com.ai.phoneagent.core.capability.CapabilitySelfTest
import kotlinx.coroutines.flow.MutableStateFlow

class AndroidNativeRuntimeCapability : Capability, CapabilitySelfTest {
    override val id: CapabilityId = CapabilityIds.NativeRuntime
    override val health = MutableStateFlow(
        CapabilityHealth.unavailable(
            id = id,
            error = notImplementedError(),
            diagnostics = mapOf("platform" to "android", "backend" to "native-runtime"),
        ),
    )

    override fun runSelfTest(): CapabilityResult<String> = CapabilityResult.failure(notImplementedError())

    private fun notImplementedError(): CapabilityError = CapabilityError(
        code = "native_runtime.not_implemented",
        message = "Android native runtime backend is not implemented yet.",
        recoverable = true,
    )
}
