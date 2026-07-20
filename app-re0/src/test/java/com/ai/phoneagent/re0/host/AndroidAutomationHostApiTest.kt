package com.ai.phoneagent.re0.host

import com.ai.phoneagent.core.capability.CapabilityHealth
import com.ai.phoneagent.core.capability.CapabilityIds
import com.ai.phoneagent.core.capability.CapabilityResult
import com.ai.phoneagent.core.capability.CaptureRequest
import com.ai.phoneagent.core.capability.CaptureResult
import com.ai.phoneagent.core.capability.InputInjectionCapability
import com.ai.phoneagent.core.capability.InputResult
import com.ai.phoneagent.core.capability.KeyRequest
import com.ai.phoneagent.core.capability.ScreenCaptureCapability
import com.ai.phoneagent.core.capability.SwipeRequest
import com.ai.phoneagent.core.capability.TapRequest
import com.ai.phoneagent.core.capability.TypeTextRequest
import com.ai.phoneagent.core.capability.UiTreeCapability
import com.ai.phoneagent.core.capability.UiTreeDetail
import com.ai.phoneagent.core.capability.UiTreeDumpRequest
import com.ai.phoneagent.core.capability.UiTreeDumpResult
import com.ai.phoneagent.core.capability.VirtualDisplayCapability
import com.ai.phoneagent.core.capability.VirtualDisplayLaunchRequest
import com.ai.phoneagent.core.capability.VirtualDisplayLifecycle
import com.ai.phoneagent.core.capability.VirtualDisplayResult
import com.ai.phoneagent.core.capability.VirtualDisplayStartRequest
import com.ai.phoneagent.core.capability.VirtualDisplayState
import com.ai.phoneagent.platform.android.capability.AndroidCapabilityRegistry
import com.ai.phoneagent.re0.generated.AutomationResultDto
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AndroidAutomationHostApiTest {
    @Test
    fun `dispatches artifact and input operations to registered capabilities`() = runBlocking {
        val uiTree = FakeUiTreeCapability()
        val capture = FakeScreenCaptureCapability()
        val input = FakeInputInjectionCapability()
        val host = AndroidAutomationHostApi(
            registry = AndroidCapabilityRegistry(listOf(uiTree, capture, input)),
            scope = this,
            workerDispatcher = Dispatchers.Unconfined,
        )

        val readiness = awaitResult(host::checkReadiness)
        val tree = awaitResult { callback -> host.dumpUiTree("full", callback) }
        val frame = awaitResult(host::captureScreen)
        val tap = awaitResult { callback -> host.tap(12, 34, callback) }

        assertEquals("3/3 capabilities ready", readiness.summary)
        assertEquals(UiTreeDetail.Full, uiTree.lastDetail)
        assertEquals("{\"nodes\":1}", tree.text)
        assertEquals("application/json", tree.mimeType)
        assertArrayEquals(byteArrayOf(1, 2, 3), frame.bytes)
        assertEquals("image/png", frame.mimeType)
        assertTrue(tap.success)
        assertEquals(12, input.lastTap?.point?.x)
        assertEquals(34, input.lastTap?.point?.y)
    }

    @Test
    fun `returns typed validation and registration failures`() = runBlocking {
        val host = AndroidAutomationHostApi(
            registry = AndroidCapabilityRegistry(emptyList()),
            scope = this,
            workerDispatcher = Dispatchers.Unconfined,
        )

        val invalidPoint = awaitResult { callback -> host.tap(-1, 2, callback) }
        val missingCapture = awaitResult(host::captureScreen)

        assertFalse(invalidPoint.success)
        assertEquals("automation.invalid_point", invalidPoint.errorCode)
        assertFalse(missingCapture.success)
        assertEquals("capability_not_registered", missingCapture.errorCode)
    }

    @Test
    fun `delegates screen capture session lifecycle to the activity control`() = runBlocking {
        val control = FakeScreenCaptureSessionControl()
        val host = AndroidAutomationHostApi(
            registry = AndroidCapabilityRegistry(emptyList()),
            scope = this,
            workerDispatcher = Dispatchers.Unconfined,
            screenCaptureSessionControl = control,
        )

        val started = awaitResult(host::requestScreenCaptureConsent)
        val stopped = awaitResult(host::stopScreenCaptureSession)

        assertEquals("consent ready", started.summary)
        assertEquals("session stopped", stopped.summary)
        assertEquals(1, control.requestCount)
        assertEquals(1, control.stopCount)
    }

    @Test
    fun `dispatches the active virtual display lifecycle`() = runBlocking {
        val virtualDisplay = FakeVirtualDisplayCapability()
        val host = AndroidAutomationHostApi(
            registry = AndroidCapabilityRegistry(listOf(virtualDisplay)),
            scope = this,
            workerDispatcher = Dispatchers.Unconfined,
        )

        val started = awaitResult { callback ->
            host.startVirtualDisplay(720, 1280, 320, callback)
        }
        val launched = awaitResult { callback ->
            host.launchOnVirtualDisplay("com.android.settings", callback)
        }
        val captured = awaitResult(host::captureVirtualDisplay)
        val stopped = awaitResult(host::stopVirtualDisplay)

        assertTrue(started.success)
        assertEquals("sessionId=virtual-1\ndisplayId=42", started.text)
        assertTrue(launched.success)
        assertArrayEquals(byteArrayOf(7, 8, 9), captured.bytes)
        assertEquals("image/png", captured.mimeType)
        assertTrue(stopped.success)
        assertEquals("com.android.settings", virtualDisplay.lastApplicationId)
        assertEquals(VirtualDisplayLifecycle.Idle, virtualDisplay.state.value.lifecycle)
    }

    @Test
    fun `virtual display operations require an active session`() = runBlocking {
        val host = AndroidAutomationHostApi(
            registry = AndroidCapabilityRegistry(listOf(FakeVirtualDisplayCapability())),
            scope = this,
            workerDispatcher = Dispatchers.Unconfined,
        )

        val result = awaitResult(host::captureVirtualDisplay)

        assertFalse(result.success)
        assertEquals("virtual_display.session_not_found", result.errorCode)
    }

    private suspend fun awaitResult(
        block: ((Result<AutomationResultDto>) -> Unit) -> Unit,
    ): AutomationResultDto {
        val result = CompletableDeferred<Result<AutomationResultDto>>()
        block(result::complete)
        return result.await().getOrThrow()
    }
}

private class FakeScreenCaptureSessionControl : ScreenCaptureSessionControl {
    var requestCount = 0
    var stopCount = 0

    override fun requestConsent(callback: (Result<AutomationResultDto>) -> Unit) {
        requestCount += 1
        callback(
            Result.success(
                AutomationResultDto(
                    success = true,
                    summary = "consent ready",
                    recoverable = false,
                ),
            ),
        )
    }

    override fun stopSession(): AutomationResultDto {
        stopCount += 1
        return AutomationResultDto(
            success = true,
            summary = "session stopped",
            recoverable = false,
        )
    }
}

private class FakeUiTreeCapability : UiTreeCapability {
    override val id = CapabilityIds.UiTree
    override val health = MutableStateFlow(CapabilityHealth.ready(id))
    var lastDetail: UiTreeDetail? = null

    override suspend fun dump(request: UiTreeDumpRequest): UiTreeDumpResult {
        lastDetail = request.detail
        return UiTreeDumpResult(
            json = "{\"nodes\":1}",
            nodeCount = 1,
            source = "fake-ui-tree",
        )
    }
}

private class FakeScreenCaptureCapability : ScreenCaptureCapability {
    override val id = CapabilityIds.ScreenCapture
    override val health = MutableStateFlow(CapabilityHealth.ready(id))

    override suspend fun capture(request: CaptureRequest): CaptureResult = CaptureResult(
        bytes = byteArrayOf(1, 2, 3),
        width = 1080,
        height = 2400,
        source = "fake-capture",
    )
}

private class FakeInputInjectionCapability : InputInjectionCapability {
    override val id = CapabilityIds.InputInjection
    override val health = MutableStateFlow(CapabilityHealth.ready(id))
    var lastTap: TapRequest? = null

    override suspend fun tap(request: TapRequest): InputResult {
        lastTap = request
        return success()
    }

    override suspend fun swipe(request: SwipeRequest): InputResult = success()

    override suspend fun typeText(request: TypeTextRequest): InputResult = success()

    override suspend fun key(request: KeyRequest): InputResult = success()

    private fun success() = InputResult(backend = "fake-input", durationMs = 1)
}

private class FakeVirtualDisplayCapability : VirtualDisplayCapability {
    override val id = CapabilityIds.VirtualDisplay
    override val health = MutableStateFlow(CapabilityHealth.ready(id))
    override val state = MutableStateFlow(
        VirtualDisplayState(lifecycle = VirtualDisplayLifecycle.Idle),
    )
    var lastApplicationId: String? = null

    override suspend fun start(request: VirtualDisplayStartRequest): VirtualDisplayResult {
        state.value = VirtualDisplayState(
            sessionId = "virtual-1",
            lifecycle = VirtualDisplayLifecycle.Running,
            displayId = 42,
        )
        return VirtualDisplayResult(sessionId = "virtual-1", displayId = 42)
    }

    override suspend fun launch(
        sessionId: String,
        request: VirtualDisplayLaunchRequest,
    ): CapabilityResult<Unit> {
        lastApplicationId = request.applicationId
        return CapabilityResult.success(Unit)
    }

    override suspend fun stop(sessionId: String): CapabilityResult<Unit> {
        state.value = VirtualDisplayState(lifecycle = VirtualDisplayLifecycle.Idle)
        return CapabilityResult.success(Unit)
    }

    override suspend fun capture(
        sessionId: String,
        request: CaptureRequest,
    ): CaptureResult = CaptureResult(
        bytes = byteArrayOf(7, 8, 9),
        width = 720,
        height = 1280,
        source = "fake-virtual-display",
    )
}
