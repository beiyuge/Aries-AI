package com.ai.phoneagent.platform.android.nativeruntime

import android.os.Build
import com.ai.phoneagent.core.capability.CapabilityHealth
import com.ai.phoneagent.core.capability.CapabilityId
import com.ai.phoneagent.core.capability.CapabilityIds
import com.ai.phoneagent.core.capability.CapabilityResult
import com.ai.phoneagent.core.capability.CapabilitySelfTest
import com.ai.phoneagent.core.capability.NativeRuntimeCapability
import com.ai.phoneagent.core.capability.NativeRuntimeInfo
import kotlinx.coroutines.flow.MutableStateFlow

class AndroidNativeRuntimeCapability(
    private val probe: NativeRuntimeProbe = AndroidNativeRuntimeProbe,
) : NativeRuntimeCapability, CapabilitySelfTest {
    override val id: CapabilityId = CapabilityIds.NativeRuntime
    override val health = MutableStateFlow(CapabilityHealth.ready(id, probe.inspect().diagnostics))

    override suspend fun inspect(): CapabilityResult<NativeRuntimeInfo> = CapabilityResult.success(probe.inspect())

    override fun runSelfTest(): CapabilityResult<String> {
        val info = probe.inspect()
        return CapabilityResult.success("${id.value}: ${info.platform} sdk=${info.sdkInt ?: "unknown"} abi=${info.supportedAbis.joinToString()}")
    }
}

interface NativeRuntimeProbe {
    fun inspect(): NativeRuntimeInfo
}

object AndroidNativeRuntimeProbe : NativeRuntimeProbe {
    override fun inspect(): NativeRuntimeInfo = NativeRuntimeInfo(
        platform = "android",
        osVersion = Build.VERSION.RELEASE ?: "",
        sdkInt = Build.VERSION.SDK_INT,
        supportedAbis = Build.SUPPORTED_ABIS.toList(),
        diagnostics = mapOf(
            "platform" to "android",
            "backend" to "android-runtime",
            "manufacturer" to Build.MANUFACTURER.orEmpty(),
            "model" to Build.MODEL.orEmpty(),
        ),
    )
}
