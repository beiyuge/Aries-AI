package com.ai.phoneagent.platform.android.screen

import com.ai.phoneagent.core.capability.CapabilityState
import com.ai.phoneagent.core.capability.CaptureRequest
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AndroidScreenCaptureCapabilityTest {
    @Test
    fun `missing media projection requires capture consent`() {
        val capability = AndroidScreenCaptureCapability(ScreenCaptureSession.Missing)

        val health = capability.health.value

        assertFalse(health.available)
        assertEquals(CapabilityState.PermissionRequired, health.state)
        assertEquals(listOf("screen.capture.consent"), health.missingRequirements.map { it.id })
    }

    @Test
    fun `ready media projection reports ready`() {
        val capability = AndroidScreenCaptureCapability(ScreenCaptureSession.Ready)

        assertTrue(capability.health.value.available)
        assertEquals(CapabilityState.Ready, capability.health.value.state)
    }

    @Test
    fun `capture without session returns typed error`() = runBlocking {
        val capability = AndroidScreenCaptureCapability(ScreenCaptureSession.Missing)

        val result = capability.capture(CaptureRequest())

        assertFalse(result.success)
        assertEquals("screen_capture.media_projection_required", result.error?.code)
    }
}
