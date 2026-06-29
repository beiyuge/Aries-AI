package com.ai.phoneagent.platform.android.input

import com.ai.phoneagent.core.capability.CapabilityState
import com.ai.phoneagent.core.capability.CapabilityError
import com.ai.phoneagent.core.capability.InputResult
import com.ai.phoneagent.core.capability.KeyRequest
import com.ai.phoneagent.core.capability.ScreenPoint
import com.ai.phoneagent.core.capability.SwipeRequest
import com.ai.phoneagent.core.capability.TapRequest
import com.ai.phoneagent.core.capability.TypeTextRequest
import com.ai.phoneagent.platform.android.accessibility.AccessibilityInputBackend
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
    fun `ready backend routes all supported input actions`() = runBlocking {
        val backend = RecordingInputBackend()
        val capability = AndroidInputInjectionCapability(
            status = FakeAccessibilityStatus(enabled = true, connected = true),
            backend = backend,
        )

        assertTrue(capability.tap(TapRequest(ScreenPoint(1, 2))).success)
        assertTrue(capability.swipe(SwipeRequest(ScreenPoint(0, 0), ScreenPoint(10, 10))).success)
        assertTrue(capability.typeText(TypeTextRequest("hello")).success)
        assertTrue(capability.key(KeyRequest(4)).success)

        assertEquals(listOf("tap", "swipe", "typeText", "key"), backend.calls)
    }

    @Test
    fun `not ready backend blocks input before dispatch`() = runBlocking {
        val backend = RecordingInputBackend()
        val capability = AndroidInputInjectionCapability(
            status = FakeAccessibilityStatus(enabled = false, connected = false),
            backend = backend,
        )

        val result = capability.tap(TapRequest(ScreenPoint(1, 2)))

        assertFalse(result.success)
        assertEquals(CapabilityState.PermissionRequired, capability.health.value.state)
        assertTrue(backend.calls.isEmpty())
    }

    @Test
    fun `backend errors are returned unchanged`() = runBlocking {
        val capability = AndroidInputInjectionCapability(
            status = FakeAccessibilityStatus(enabled = true, connected = true),
            backend = RecordingInputBackend(
                keyResult = InputResult(
                    backend = "accessibility",
                    durationMs = 0L,
                    error = CapabilityError(
                        code = "input.key_unsupported",
                        message = "Unsupported key",
                        recoverable = true,
                    ),
                ),
            ),
        )

        val result = capability.key(KeyRequest(999))

        assertFalse(result.success)
        assertEquals("input.key_unsupported", result.error?.code)
    }
}

private class FakeAccessibilityStatus(
    private val enabled: Boolean,
    private val connected: Boolean,
) : AccessibilityServiceStatus {
    override fun isEnabled(): Boolean = enabled
    override fun isConnected(): Boolean = connected
}

private class RecordingInputBackend(
    private val tapResult: InputResult = InputResult("accessibility", 1L),
    private val swipeResult: InputResult = InputResult("accessibility", 1L),
    private val typeTextResult: InputResult = InputResult("accessibility", 1L),
    private val keyResult: InputResult = InputResult("accessibility", 1L),
) : AccessibilityInputBackend {
    val calls = mutableListOf<String>()

    override suspend fun tap(request: TapRequest): InputResult {
        calls += "tap"
        return tapResult
    }

    override suspend fun swipe(request: SwipeRequest): InputResult {
        calls += "swipe"
        return swipeResult
    }

    override suspend fun typeText(request: TypeTextRequest): InputResult {
        calls += "typeText"
        return typeTextResult
    }

    override suspend fun key(request: KeyRequest): InputResult {
        calls += "key"
        return keyResult
    }
}
