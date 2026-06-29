package com.ai.phoneagent.platform.android.floating

import android.content.Context
import android.provider.Settings
import com.ai.phoneagent.core.capability.CapabilityAction
import com.ai.phoneagent.core.capability.CapabilityError
import com.ai.phoneagent.core.capability.CapabilityHealth
import com.ai.phoneagent.core.capability.CapabilityId
import com.ai.phoneagent.core.capability.CapabilityIds
import com.ai.phoneagent.core.capability.CapabilityRequirement
import com.ai.phoneagent.core.capability.CapabilityResult
import com.ai.phoneagent.core.capability.CapabilitySelfTest
import com.ai.phoneagent.core.capability.FloatingWindowCapability
import com.ai.phoneagent.core.capability.FloatingWindowRequest
import com.ai.phoneagent.core.capability.FloatingWindowSession
import com.ai.phoneagent.core.capability.FloatingWindowState
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow

class AndroidFloatingWindowCapability(
    private val permission: OverlayPermissionStatus,
    private val controller: FloatingWindowController,
) : FloatingWindowCapability, CapabilitySelfTest {
    constructor(context: Context) : this(
        permission = AndroidOverlayPermissionStatus(context.applicationContext),
        controller = ServiceFloatingWindowController(context.applicationContext),
    )

    override val id: CapabilityId = CapabilityIds.FloatingWindow
    override val state = MutableStateFlow(controller.currentState())
    override val health = MutableStateFlow(readHealth())

    override suspend fun show(request: FloatingWindowRequest): CapabilityResult<FloatingWindowSession> {
        refreshHealth()
        if (!permission.canDrawOverlays()) {
            return CapabilityResult.failure(overlayPermissionError())
        }
        val session = FloatingWindowSession(
            sessionId = UUID.randomUUID().toString(),
            mode = request.mode,
        )
        val result = controller.show(session, request)
        state.value = controller.currentState()
        refreshHealth()
        return result
    }

    override suspend fun hide(sessionId: String): CapabilityResult<Unit> {
        val result = controller.hide(sessionId)
        state.value = controller.currentState()
        refreshHealth()
        return result
    }

    override fun runSelfTest(): CapabilityResult<String> {
        refreshHealth()
        return if (health.value.available) {
            CapabilityResult.success("${id.value}: overlay permission ready, visible=${state.value.visible}")
        } else {
            CapabilityResult.failure(health.value.lastError ?: overlayPermissionError())
        }
    }

    fun refreshHealth() {
        health.value = readHealth()
    }

    private fun readHealth(): CapabilityHealth {
        val currentState = controller.currentState()
        state.value = currentState
        val diagnostics = mapOf(
            "platform" to "android",
            "backend" to "window-manager-service",
            "visible" to currentState.visible.toString(),
            "mode" to currentState.mode.orEmpty(),
        )
        return if (permission.canDrawOverlays()) {
            CapabilityHealth.ready(id = id, diagnostics = diagnostics)
        } else {
            CapabilityHealth.permissionRequired(
                id = id,
                missingRequirements = listOf(overlayRequirement()),
                diagnostics = diagnostics,
            )
        }
    }

    private fun overlayRequirement(): CapabilityRequirement = CapabilityRequirement(
        id = "overlay.window",
        title = "Floating Window",
        description = "Allow Aries re0 to show overlay controls.",
        action = CapabilityAction.OpenSettings(Settings.ACTION_MANAGE_OVERLAY_PERMISSION),
    )

    private fun overlayPermissionError(): CapabilityError = CapabilityError(
        code = "floating_window.overlay_permission_required",
        message = "Android overlay permission is required before showing the floating window.",
        recoverable = true,
        suggestedAction = "Grant display-over-other-apps permission.",
    )
}
