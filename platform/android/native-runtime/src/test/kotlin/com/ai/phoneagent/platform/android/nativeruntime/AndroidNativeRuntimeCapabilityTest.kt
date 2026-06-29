package com.ai.phoneagent.platform.android.nativeruntime

import com.ai.phoneagent.core.capability.CapabilityIds
import com.ai.phoneagent.core.capability.CapabilityState
import com.ai.phoneagent.core.capability.NativeRuntimeInfo
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AndroidNativeRuntimeCapabilityTest {
    @Test
    fun `reports ready Android runtime backend`() {
        val capability = AndroidNativeRuntimeCapability(FakeProbe())

        assertEquals(CapabilityIds.NativeRuntime, capability.id)
        assertTrue(capability.health.value.available)
        assertEquals(CapabilityState.Ready, capability.health.value.state)
        assertEquals("fake-runtime", capability.health.value.diagnostics["backend"])
    }

    @Test
    fun `inspects runtime info through probe`() = runBlocking {
        val capability = AndroidNativeRuntimeCapability(FakeProbe())

        val result = capability.inspect()

        assertTrue(result.isSuccess)
        assertEquals("android", result.getOrNull()?.platform)
        assertEquals(listOf("arm64-v8a"), result.getOrNull()?.supportedAbis)
    }

    private class FakeProbe : NativeRuntimeProbe {
        override fun inspect(): NativeRuntimeInfo = NativeRuntimeInfo(
            platform = "android",
            osVersion = "15",
            sdkInt = 35,
            supportedAbis = listOf("arm64-v8a"),
            diagnostics = mapOf("backend" to "fake-runtime"),
        )
    }
}
