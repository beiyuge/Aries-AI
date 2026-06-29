package com.ai.phoneagent.platform.android.accessibility

import com.ai.phoneagent.core.capability.CapabilityState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AccessibilityCapabilityTest {
    @Test
    fun `disabled service requires accessibility permission`() {
        val capability = AndroidAccessibilityCapability(FakeAccessibilityStatus(enabled = false, connected = false))

        val health = capability.health.value

        assertFalse(health.available)
        assertEquals(CapabilityState.PermissionRequired, health.state)
        assertEquals(listOf("accessibility.service"), health.missingRequirements.map { it.id })
    }

    @Test
    fun `enabled but disconnected service is degraded`() {
        val capability = AndroidAccessibilityCapability(FakeAccessibilityStatus(enabled = true, connected = false))

        val health = capability.health.value

        assertTrue(health.available)
        assertEquals(CapabilityState.Degraded, health.state)
        assertEquals("accessibility.service_disconnected", health.lastError?.code)
    }

    @Test
    fun `enabled and connected service is ready`() {
        val capability = AndroidAccessibilityCapability(FakeAccessibilityStatus(enabled = true, connected = true))

        assertTrue(capability.health.value.available)
        assertEquals(CapabilityState.Ready, capability.health.value.state)
    }

    @Test
    fun `settings parser matches flattened component name`() {
        val enabled = "other/.Service:com.ai.phoneagent.re0/com.ai.phoneagent.platform.android.accessibility.Re0AccessibilityService"

        val contains = AccessibilityServiceSettingsParser.containsService(
            enabledServices = enabled,
            packageName = "com.ai.phoneagent.re0",
            serviceClassName = "com.ai.phoneagent.platform.android.accessibility.Re0AccessibilityService",
        )

        assertTrue(contains)
    }
}

class FakeAccessibilityStatus(
    private val enabled: Boolean,
    private val connected: Boolean,
) : AccessibilityServiceStatus {
    override fun isEnabled(): Boolean = enabled
    override fun isConnected(): Boolean = connected
}
