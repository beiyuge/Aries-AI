package com.ai.phoneagent.platform.android.nativeruntime

import com.ai.phoneagent.core.capability.CapabilityIds
import com.ai.phoneagent.core.capability.CapabilityState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class AndroidNativeRuntimeCapabilityTest {
    @Test
    fun `reports explicit unavailable backend`() {
        val capability = AndroidNativeRuntimeCapability()

        assertEquals(CapabilityIds.NativeRuntime, capability.id)
        assertFalse(capability.health.value.available)
        assertEquals(CapabilityState.Unavailable, capability.health.value.state)
        assertEquals("native_runtime.not_implemented", capability.health.value.lastError?.code)
    }
}
