package com.ai.phoneagent.core.capability

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CapabilityApiContractTest {
    @Test
    fun `ready health marks capability available with no missing requirements`() {
        val health = CapabilityHealth.ready(CapabilityId("screen.capture"))

        assertEquals(CapabilityId("screen.capture"), health.id)
        assertTrue(health.available)
        assertEquals(CapabilityState.Ready, health.state)
        assertTrue(health.missingRequirements.isEmpty())
        assertNull(health.lastError)
    }

    @Test
    fun `permission required health is unavailable and carries missing requirements`() {
        val requirement = CapabilityRequirement(
            id = "accessibility.service",
            title = "Accessibility Service",
            description = "Enable the re0 accessibility service",
            action = CapabilityAction.OpenSettings("android.settings.ACCESSIBILITY_SETTINGS"),
        )

        val health = CapabilityHealth.permissionRequired(
            id = CapabilityId("accessibility"),
            missingRequirements = listOf(requirement),
        )

        assertFalse(health.available)
        assertEquals(CapabilityState.PermissionRequired, health.state)
        assertEquals(listOf(requirement), health.missingRequirements)
    }

    @Test
    fun `failure result exposes recoverable capability error`() {
        val error = CapabilityError(
            code = "shizuku.unavailable",
            message = "Shizuku binder is not running",
            recoverable = true,
            suggestedAction = "Start Shizuku and retry",
        )

        val result = CapabilityResult.failure<String>(error)

        assertFalse(result.isSuccess)
        assertEquals(error, result.errorOrNull())
        assertEquals("shizuku.unavailable", result.errorOrNull()?.code)
    }

    @Test
    fun `unsupported health is unavailable and not supported`() {
        val health = CapabilityHealth.unsupported(
            id = CapabilityId("desktop.virtual.display"),
            reason = "Virtual display is not available on this platform.",
        )

        assertFalse(health.available)
        assertFalse(health.supported)
        assertEquals(CapabilityState.Unsupported, health.state)
        assertEquals("capability.unsupported", health.lastError?.code)
    }

    @Test
    fun `degraded health remains available with an attached diagnostic error`() {
        val error = CapabilityError(
            code = "screen.partial",
            message = "Only the default display can be captured.",
            recoverable = true,
        )

        val health = CapabilityHealth.degraded(CapabilityId("screen.capture"), error)

        assertTrue(health.available)
        assertTrue(health.supported)
        assertEquals(CapabilityState.Degraded, health.state)
        assertEquals(error, health.lastError)
    }
}
