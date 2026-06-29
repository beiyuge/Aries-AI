package com.ai.phoneagent.platform.android.background

import com.ai.phoneagent.core.capability.CapabilityIds
import com.ai.phoneagent.core.capability.CapabilityState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class AndroidBackgroundTasksCapabilityTest {
    @Test
    fun `reports explicit unavailable backend`() {
        val capability = AndroidBackgroundTasksCapability()

        assertEquals(CapabilityIds.BackgroundTasks, capability.id)
        assertFalse(capability.health.value.available)
        assertEquals(CapabilityState.Unavailable, capability.health.value.state)
        assertEquals("background_tasks.not_implemented", capability.health.value.lastError?.code)
    }
}
