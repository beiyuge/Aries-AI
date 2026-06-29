package com.ai.phoneagent.platform.android.virtualdisplay

import android.content.Context
import com.ai.phoneagent.core.capability.CapabilityHealth
import com.ai.phoneagent.core.capability.CapabilityId
import com.ai.phoneagent.core.capability.CapabilityIds
import com.ai.phoneagent.core.capability.CapabilityResult
import com.ai.phoneagent.core.capability.CapabilitySelfTest
import com.ai.phoneagent.core.capability.CaptureRequest
import com.ai.phoneagent.core.capability.CaptureResult
import com.ai.phoneagent.core.capability.VirtualDisplayCapability
import com.ai.phoneagent.core.capability.VirtualDisplayLifecycle
import com.ai.phoneagent.core.capability.VirtualDisplayResult
import com.ai.phoneagent.core.capability.VirtualDisplayStartRequest
import com.ai.phoneagent.core.capability.VirtualDisplayState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking

class AndroidVirtualDisplayCapability(
    private val controller: AndroidVirtualDisplayController,
) : VirtualDisplayCapability, CapabilitySelfTest {
    override val id: CapabilityId = CapabilityIds.VirtualDisplay
    override val state = MutableStateFlow(VirtualDisplayState(lifecycle = VirtualDisplayLifecycle.Idle))
    override val health = MutableStateFlow(CapabilityHealth.ready(id, controller.diagnostics))

    constructor(context: Context) : this(DisplayManagerVirtualDisplayController(context.applicationContext))

    override suspend fun start(request: VirtualDisplayStartRequest): VirtualDisplayResult {
        state.value = state.value.copy(lifecycle = VirtualDisplayLifecycle.Preparing, diagnostics = controller.diagnostics)
        val result = controller.start(request)
        val session = result.getOrNull()
        return if (session != null) {
            state.value = VirtualDisplayState(
                sessionId = session.sessionId,
                lifecycle = VirtualDisplayLifecycle.Running,
                displayId = session.displayId,
                diagnostics = controller.diagnostics + session.diagnostics,
            )
            VirtualDisplayResult(
                sessionId = session.sessionId,
                displayId = session.displayId,
            )
        } else {
            val error = result.errorOrNull() ?: VirtualDisplayErrors.operationFailed("start returned no session")
            state.value = VirtualDisplayState(
                lifecycle = VirtualDisplayLifecycle.Failed,
                diagnostics = controller.diagnostics + ("error" to error.code),
            )
            VirtualDisplayResult(sessionId = "", displayId = -1, error = error)
        }
    }

    override suspend fun stop(sessionId: String): CapabilityResult<Unit> {
        state.value = state.value.copy(lifecycle = VirtualDisplayLifecycle.Stopping)
        val result = controller.stop(sessionId)
        state.value = if (result.isSuccess) {
            VirtualDisplayState(lifecycle = VirtualDisplayLifecycle.Idle, diagnostics = controller.diagnostics)
        } else {
            val error = result.errorOrNull()
            VirtualDisplayState(
                sessionId = sessionId,
                lifecycle = VirtualDisplayLifecycle.Failed,
                diagnostics = controller.diagnostics + ("error" to (error?.code ?: "unknown")),
            )
        }
        return result
    }

    override suspend fun capture(sessionId: String, request: CaptureRequest): CaptureResult =
        controller.capture(sessionId, request)

    override fun runSelfTest(): CapabilityResult<String> = runBlocking {
        val start = controller.start(
            VirtualDisplayStartRequest(
                width = SELF_TEST_WIDTH,
                height = SELF_TEST_HEIGHT,
                densityDpi = SELF_TEST_DENSITY_DPI,
            ),
        )
        val session = start.getOrNull()
            ?: return@runBlocking CapabilityResult.failure<String>(
                start.errorOrNull() ?: VirtualDisplayErrors.operationFailed("Self-test did not create a session."),
            )
        val stop = controller.stop(session.sessionId)
        if (!stop.isSuccess) {
            return@runBlocking CapabilityResult.failure<String>(
                stop.errorOrNull() ?: VirtualDisplayErrors.operationFailed("Self-test could not release the session."),
            )
        }
        CapabilityResult.success("${id.value}: self-test displayId=${session.displayId} ${session.width}x${session.height}")
    }

    private companion object {
        const val SELF_TEST_WIDTH = 64
        const val SELF_TEST_HEIGHT = 64
        const val SELF_TEST_DENSITY_DPI = 320
    }
}
