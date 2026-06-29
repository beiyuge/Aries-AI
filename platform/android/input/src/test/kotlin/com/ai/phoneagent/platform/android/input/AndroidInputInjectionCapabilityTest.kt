package com.ai.phoneagent.platform.android.input

import com.ai.phoneagent.core.capability.CapabilityState
import com.ai.phoneagent.core.capability.ScreenPoint
import com.ai.phoneagent.core.capability.SwipeRequest
import com.ai.phoneagent.platform.android.accessibility.AccessibilityServiceStatus
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AndroidInputInjectionCapabilityTest {
    @Test
    fun `disabled accessibility reports permission required`() {
        val capability = AndroidInputInjectionCapability(FakeAccessibilityStatus(enabled = false, connected = false))

        assertFalse(capability.health.value.available)
        assertEquals(CapabilityState.PermissionRequired, capability.health.value.state)
    }

    @Test
    fun `connected accessibility reports ready input backend`() {
        val capability = AndroidInputInjectionCapability(FakeAccessibilityStatus(enabled = true, connected = true))

        assertTrue(capability.health.value.available)
        assertEquals(CapabilityState.Ready, capability.health.value.state)
    }

    @Test
    fun `swipe returns explicit not implemented error`() = runBlocking {
        val capability = AndroidInputInjectionCapability(FakeAccessibilityStatus(enabled = true, connected = true))

        val result = capability.swipe(SwipeRequest(ScreenPoint(0, 0), ScreenPoint(10, 10)))

        assertFalse(result.success)
        assertEquals("input.swipe_not_implemented", result.error?.code)
    }
}

private class FakeAccessibilityStatus(
    private val enabled: Boolean,
    private val connected: Boolean,
) : AccessibilityServiceStatus {
    override fun isEnabled(): Boolean = enabled
    override fun isConnected(): Boolean = connected
}
