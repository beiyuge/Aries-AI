package com.ai.phoneagent.re0.host

import com.ai.phoneagent.core.capability.CapabilityHealth
import com.ai.phoneagent.core.capability.CapabilityIds
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

    private suspend fun awaitResult(
        block: ((Result<AutomationResultDto>) -> Unit) -> Unit,
    ): AutomationResultDto {
        val result = CompletableDeferred<Result<AutomationResultDto>>()
        block(result::complete)
        return result.await().getOrThrow()
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
