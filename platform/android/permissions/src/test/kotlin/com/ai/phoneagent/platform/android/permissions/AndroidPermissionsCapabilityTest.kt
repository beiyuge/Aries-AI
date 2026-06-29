package com.ai.phoneagent.platform.android.permissions

import com.ai.phoneagent.core.capability.CapabilityIds
import com.ai.phoneagent.core.capability.CapabilityState
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AndroidPermissionsCapabilityTest {
    @Test
    fun `health reports registered Android permission requirements`() {
        val capability = AndroidPermissionsCapability()
        val health = capability.health.value

        assertEquals(CapabilityIds.Permissions, health.id)
        assertEquals(CapabilityState.PermissionRequired, health.state)
        assertFalse(health.available)
        assertEquals("android", health.diagnostics["platform"])
        assertEquals(PermissionRequirementCatalog.defaultRequirements().size, health.missingRequirements.size)
    }

    @Test
    fun `settings lookup succeeds only for known requirement ids`() = runBlocking {
        val capability = AndroidPermissionsCapability()

        assertTrue(capability.openRequirementSettings("overlay.window").isSuccess)
        assertFalse(capability.openRequirementSettings("missing").isSuccess)
    }
}
