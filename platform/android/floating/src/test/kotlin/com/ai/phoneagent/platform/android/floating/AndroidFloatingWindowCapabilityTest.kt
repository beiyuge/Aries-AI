package com.ai.phoneagent.platform.android.floating

import com.ai.phoneagent.core.capability.CapabilityIds
import com.ai.phoneagent.core.capability.CapabilityState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class AndroidFloatingWindowCapabilityTest {
    @Test
    fun `reports explicit unavailable backend`() {
        val capability = AndroidFloatingWindowCapability()

        assertEquals(CapabilityIds.FloatingWindow, capability.id)
        assertFalse(capability.health.value.available)
        assertEquals(CapabilityState.Unavailable, capability.health.value.state)
        assertEquals("floating_window.not_implemented", capability.health.value.lastError?.code)
    }
}
