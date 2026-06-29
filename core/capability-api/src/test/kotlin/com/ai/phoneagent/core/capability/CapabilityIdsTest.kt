package com.ai.phoneagent.core.capability

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CapabilityIdsTest {
    @Test
    fun `core system capability ids are stable and unique`() {
        val ids = CapabilityIds.allSystemIds

        assertTrue(CapabilityIds.Permissions in ids)
        assertTrue(CapabilityIds.ShizukuShell in ids)
        assertTrue(CapabilityIds.Accessibility in ids)
        assertTrue(CapabilityIds.ScreenCapture in ids)
        assertTrue(CapabilityIds.UiTree in ids)
        assertTrue(CapabilityIds.InputInjection in ids)
        assertTrue(CapabilityIds.VirtualDisplay in ids)
        assertTrue(CapabilityIds.FloatingWindow in ids)
        assertTrue(CapabilityIds.BackgroundTasks in ids)
        assertTrue(CapabilityIds.NativeRuntime in ids)
        assertEquals(ids.size, ids.toSet().size)
    }
}
