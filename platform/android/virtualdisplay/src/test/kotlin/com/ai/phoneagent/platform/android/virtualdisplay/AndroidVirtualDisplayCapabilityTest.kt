package com.ai.phoneagent.platform.android.virtualdisplay

import com.ai.phoneagent.core.capability.CapabilityResult
import com.ai.phoneagent.core.capability.CaptureRequest
import com.ai.phoneagent.core.capability.CaptureResult
import com.ai.phoneagent.core.capability.CapabilityIds
import com.ai.phoneagent.core.capability.CapabilityState
import com.ai.phoneagent.core.capability.VirtualDisplayLifecycle
import com.ai.phoneagent.core.capability.VirtualDisplayLaunchRequest
import com.ai.phoneagent.core.capability.VirtualDisplayStartRequest
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AndroidVirtualDisplayCapabilityTest {
    @Test
    fun `reports ready DisplayManager backend`() {
        val capability = AndroidVirtualDisplayCapability(FakeController())

        assertEquals(CapabilityIds.VirtualDisplay, capability.id)
        assertTrue(capability.health.value.available)
        assertEquals(CapabilityState.Ready, capability.health.value.state)
        assertEquals("fake-display", capability.health.value.diagnostics["backend"])
    }

    @Test
    fun `start and stop update lifecycle state`() = runBlocking {
        val controller = FakeController()
        val capability = AndroidVirtualDisplayCapability(controller)

        val start = capability.start(VirtualDisplayStartRequest(width = 720, height = 1280, densityDpi = 320))
        assertTrue(start.success)
        assertEquals("session-1", start.sessionId)
        assertEquals(42, start.displayId)
        assertEquals(VirtualDisplayLifecycle.Running, capability.state.value.lifecycle)

        val stop = capability.stop(start.sessionId)
        assertTrue(stop.isSuccess)
        assertEquals(VirtualDisplayLifecycle.Idle, capability.state.value.lifecycle)
        assertEquals(listOf("start:720x1280", "stop:session-1"), controller.calls)
    }

    @Test
    fun `start failure moves lifecycle to failed`() = runBlocking {
        val capability = AndroidVirtualDisplayCapability(FakeController(startError = true))

        val result = capability.start(VirtualDisplayStartRequest(width = 0, height = 1280, densityDpi = 320))

        assertEquals(false, result.success)
        assertEquals(VirtualDisplayLifecycle.Failed, capability.state.value.lifecycle)
        assertEquals("virtual_display.invalid_request", result.error?.code)
    }

    @Test
    fun `active session launches content and rejects a second start`() = runBlocking {
        val controller = FakeController()
        val capability = AndroidVirtualDisplayCapability(controller)
        val request = VirtualDisplayStartRequest(width = 720, height = 1280, densityDpi = 320)

        val start = capability.start(request)
        val launch = capability.launch(
            start.sessionId,
            VirtualDisplayLaunchRequest("com.example.target"),
        )
        val duplicate = capability.start(request)

        assertTrue(launch.isSuccess)
        assertEquals("virtual_display.session_active", duplicate.error?.code)
        assertEquals("com.example.target", capability.state.value.diagnostics["application_id"])
        assertEquals(
            listOf("start:720x1280", "launch:session-1:com.example.target"),
            controller.calls,
        )
    }

    @Test
    fun `self test creates and releases a tiny session without touching state`() {
        val controller = FakeController()
        val capability = AndroidVirtualDisplayCapability(controller)

        val result = capability.runSelfTest()

        assertTrue(result.isSuccess)
        assertEquals(VirtualDisplayLifecycle.Idle, capability.state.value.lifecycle)
        assertEquals(listOf("start:64x64", "stop:session-1"), controller.calls)
    }

    private class FakeController(
        private val startError: Boolean = false,
    ) : AndroidVirtualDisplayController {
        val calls = mutableListOf<String>()
        override val diagnostics: Map<String, String> = mapOf(
            "platform" to "android",
            "backend" to "fake-display",
        )

        override suspend fun start(request: VirtualDisplayStartRequest): CapabilityResult<AndroidVirtualDisplaySession> {
            calls += "start:${request.width}x${request.height}"
            if (startError) {
                return CapabilityResult.failure(VirtualDisplayErrors.invalidRequest("bad request"))
            }
            return CapabilityResult.success(
                AndroidVirtualDisplaySession(
                    sessionId = "session-1",
                    displayId = 42,
                    width = request.width,
                    height = request.height,
                    densityDpi = request.densityDpi,
                ),
            )
        }

        override suspend fun launch(
            sessionId: String,
            request: VirtualDisplayLaunchRequest,
        ): CapabilityResult<Unit> {
            calls += "launch:$sessionId:${request.applicationId}"
            return CapabilityResult.success(Unit)
        }

        override suspend fun stop(sessionId: String): CapabilityResult<Unit> {
            calls += "stop:$sessionId"
            return CapabilityResult.success(Unit)
        }

        override suspend fun capture(sessionId: String, request: CaptureRequest): CaptureResult =
            CaptureResult(bytes = byteArrayOf(1), width = 1, height = 1, source = "fake")
    }
}
