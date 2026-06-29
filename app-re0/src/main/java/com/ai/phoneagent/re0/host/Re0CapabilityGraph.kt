package com.ai.phoneagent.re0.host

import com.ai.phoneagent.core.capability.Capability
import com.ai.phoneagent.core.capability.CapabilityError
import com.ai.phoneagent.core.capability.CapabilityHealth
import com.ai.phoneagent.core.capability.CapabilityId
import com.ai.phoneagent.core.capability.CapabilityIds
import com.ai.phoneagent.core.capability.CapabilityState
import com.ai.phoneagent.platform.android.capability.AndroidCapabilityRegistry
import com.ai.phoneagent.platform.android.permissions.PermissionRequirementCatalog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object Re0CapabilityGraph {
    fun createRegistry(): AndroidCapabilityRegistry = AndroidCapabilityRegistry(
        listOf(
            PermissionsCapability(),
            PlaceholderCapability(CapabilityIds.ShizukuShell, "Shizuku shell plugin is not implemented yet."),
            PlaceholderCapability(CapabilityIds.Accessibility, "Accessibility service plugin is not implemented yet."),
            PlaceholderCapability(CapabilityIds.ScreenCapture, "Screen capture plugin is not implemented yet."),
            PlaceholderCapability(CapabilityIds.UiTree, "UI tree plugin is not implemented yet."),
            PlaceholderCapability(CapabilityIds.InputInjection, "Input injection plugin is not implemented yet."),
            PlaceholderCapability(CapabilityIds.VirtualDisplay, "Virtual display plugin is not implemented yet."),
            PlaceholderCapability(CapabilityIds.FloatingWindow, "Floating window plugin is not implemented yet."),
            PlaceholderCapability(CapabilityIds.BackgroundTasks, "Background task plugin is not implemented yet."),
            PlaceholderCapability(CapabilityIds.NativeRuntime, "Native runtime plugin is not implemented yet."),
        ),
    )
}

private class PermissionsCapability : Capability {
    override val id: CapabilityId = CapabilityIds.Permissions

    override val health: StateFlow<CapabilityHealth> = MutableStateFlow(
        CapabilityHealth.permissionRequired(
            id = id,
            missingRequirements = PermissionRequirementCatalog.defaultRequirements(),
            diagnostics = mapOf("source" to "PermissionRequirementCatalog"),
        ),
    )
}

private class PlaceholderCapability(
    override val id: CapabilityId,
    message: String,
) : Capability {
    override val health: StateFlow<CapabilityHealth> = MutableStateFlow(
        CapabilityHealth(
            id = id,
            available = false,
            state = CapabilityState.Unavailable,
            lastError = CapabilityError(
                code = "not_implemented",
                message = message,
                recoverable = true,
            ),
        ),
    )
}
