package com.ai.phoneagent.platform.android.virtualdisplay

import com.ai.phoneagent.core.capability.CapabilityIds
import com.ai.phoneagent.core.capability.CapabilityState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class AndroidVirtualDisplayCapabilityTest {
    @Test
    fun `reports explicit unavailable backend`() {
        val capability = AndroidVirtualDisplayCapability()

        assertEquals(CapabilityIds.VirtualDisplay, capability.id)
        assertFalse(capability.health.value.available)
        assertEquals(CapabilityState.Unavailable, capability.health.value.state)
        assertEquals("virtual_display.not_implemented", capability.health.value.lastError?.code)
    }
}
