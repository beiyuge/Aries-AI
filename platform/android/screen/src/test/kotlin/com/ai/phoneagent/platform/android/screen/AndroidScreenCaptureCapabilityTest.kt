package com.ai.phoneagent.platform.android.screen

import com.ai.phoneagent.core.capability.CapabilityHealth
import com.ai.phoneagent.core.capability.CapabilityIds
import com.ai.phoneagent.core.capability.CapabilityState
import com.ai.phoneagent.core.capability.CaptureRequest
import com.ai.phoneagent.core.capability.CaptureResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AndroidScreenCaptureCapabilityTest {
    @Test
    fun `missing media projection requires capture consent`() {
        val capability = AndroidScreenCaptureCapability(MissingScreenCaptureSession)

        val health = capability.health.value

        assertFalse(health.available)
        assertEquals(CapabilityState.PermissionRequired, health.state)
        assertEquals(listOf("screen.capture.consent"), health.missingRequirements.map { it.id })
    }

    @Test
    fun `ready session health is exposed without copying state`() {
        val session = FakeScreenCaptureSession()
        val capability = AndroidScreenCaptureCapability(session)

        assertTrue(capability.health.value.available)
        session.health.value = screenCaptureMissingHealth()
        assertEquals(CapabilityState.PermissionRequired, capability.health.value.state)
    }

    @Test
    fun `capture delegates to the active session`() = runBlocking {
        val session = FakeScreenCaptureSession()
        val capability = AndroidScreenCaptureCapability(session)

        val result = capability.capture(CaptureRequest(maxWidth = 720))

        assertTrue(result.success)
        assertEquals(720, session.lastRequest?.maxWidth)
        assertArrayEquals(byteArrayOf(1, 2, 3), result.bytes)
    }

    @Test
    fun `capture without session returns typed error`() = runBlocking {
        val capability = AndroidScreenCaptureCapability(MissingScreenCaptureSession)

        val result = capability.capture(CaptureRequest())

        assertFalse(result.success)
        assertEquals("screen_capture.media_projection_required", result.error?.code)
    }
}

private class FakeScreenCaptureSession : ScreenCaptureSession {
    override val health = MutableStateFlow(CapabilityHealth.ready(CapabilityIds.ScreenCapture))
    var lastRequest: CaptureRequest? = null

    override suspend fun capture(request: CaptureRequest): CaptureResult {
        lastRequest = request
        return CaptureResult(
            bytes = byteArrayOf(1, 2, 3),
            width = request.maxWidth ?: 1080,
            height = 1920,
            source = "fake-screen",
        )
    }

    override fun stop() = Unit
}
