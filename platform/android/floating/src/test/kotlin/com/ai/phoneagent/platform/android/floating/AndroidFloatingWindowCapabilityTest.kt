package com.ai.phoneagent.platform.android.floating

import com.ai.phoneagent.core.capability.CapabilityIds
import com.ai.phoneagent.core.capability.CapabilityState
import com.ai.phoneagent.core.capability.CapabilityResult
import com.ai.phoneagent.core.capability.FloatingWindowRequest
import com.ai.phoneagent.core.capability.FloatingWindowSession
import com.ai.phoneagent.core.capability.FloatingWindowState
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AndroidFloatingWindowCapabilityTest {
    @Test
    fun `missing overlay permission reports permission required`() {
        val capability = AndroidFloatingWindowCapability(
            permission = FakeOverlayPermissionStatus(granted = false),
            controller = FakeFloatingWindowController(),
        )

        val health = capability.health.value

        assertEquals(CapabilityIds.FloatingWindow, capability.id)
        assertFalse(health.available)
        assertEquals(CapabilityState.PermissionRequired, health.state)
        assertEquals(listOf("overlay.window"), health.missingRequirements.map { it.id })
    }

    @Test
    fun `granted overlay permission reports ready`() {
        val capability = AndroidFloatingWindowCapability(
            permission = FakeOverlayPermissionStatus(granted = true),
            controller = FakeFloatingWindowController(),
        )

        assertTrue(capability.health.value.available)
        assertEquals(CapabilityState.Ready, capability.health.value.state)
    }

    @Test
    fun `show starts controller session and updates state`() = runBlocking {
        val controller = FakeFloatingWindowController()
        val capability = AndroidFloatingWindowCapability(
            permission = FakeOverlayPermissionStatus(granted = true),
            controller = controller,
        )

        val result = capability.show(FloatingWindowRequest(mode = "compact", widthDp = 240, heightDp = 120))

        assertTrue(result.isSuccess)
        assertTrue(capability.state.value.visible)
        assertEquals("compact", capability.state.value.mode)
        assertNotNull(result.getOrNull()?.sessionId)
    }

    @Test
    fun `show fails before controller when overlay permission is missing`() = runBlocking {
        val controller = FakeFloatingWindowController()
        val capability = AndroidFloatingWindowCapability(
            permission = FakeOverlayPermissionStatus(granted = false),
            controller = controller,
        )

        val result = capability.show(FloatingWindowRequest(mode = "compact"))

        assertFalse(result.isSuccess)
        assertEquals("floating_window.overlay_permission_required", result.errorOrNull()?.code)
        assertFalse(controller.showCalled)
    }

    @Test
    fun `hide delegates to controller and clears state`() = runBlocking {
        val controller = FakeFloatingWindowController()
        val capability = AndroidFloatingWindowCapability(
            permission = FakeOverlayPermissionStatus(granted = true),
            controller = controller,
        )
        val session = capability.show(FloatingWindowRequest(mode = "compact")).getOrNull()!!

        val result = capability.hide(session.sessionId)

        assertTrue(result.isSuccess)
        assertFalse(capability.state.value.visible)
    }
}

private class FakeOverlayPermissionStatus(
    private val granted: Boolean,
) : OverlayPermissionStatus {
    override fun canDrawOverlays(): Boolean = granted
}

private class FakeFloatingWindowController : FloatingWindowController {
    var showCalled = false
    private var state = FloatingWindowState()

    override fun currentState(): FloatingWindowState = state

    override fun show(
        session: FloatingWindowSession,
        request: FloatingWindowRequest,
    ): CapabilityResult<FloatingWindowSession> {
        showCalled = true
        state = FloatingWindowState(
            sessionId = session.sessionId,
            visible = true,
            mode = session.mode,
        )
        return CapabilityResult.success(session)
    }

    override fun hide(sessionId: String): CapabilityResult<Unit> {
        state = FloatingWindowState()
        return CapabilityResult.success(Unit)
    }
}
