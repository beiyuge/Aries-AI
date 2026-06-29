package com.ai.phoneagent.platform.android.permissions

import com.ai.phoneagent.core.capability.CapabilityAction
import com.ai.phoneagent.core.capability.PermissionRequirement

object PermissionRequirementCatalog {
    fun defaultRequirements(): List<PermissionRequirement> = listOf(
        PermissionRequirement(
            id = "accessibility.service",
            title = "Accessibility Service",
            description = "Enable Aries re0 accessibility automation service.",
            action = CapabilityAction.OpenSettings("android.settings.ACCESSIBILITY_SETTINGS"),
        ),
        PermissionRequirement(
            id = "overlay.window",
            title = "Floating Window",
            description = "Allow Aries re0 to show overlay controls.",
            action = CapabilityAction.OpenSettings("android.settings.action.MANAGE_OVERLAY_PERMISSION"),
        ),
        PermissionRequirement(
            id = "notifications",
            title = "Notifications",
            description = "Allow foreground service and task status notifications.",
            action = CapabilityAction.OpenSettings("android.settings.APP_NOTIFICATION_SETTINGS"),
        ),
        PermissionRequirement(
            id = "microphone",
            title = "Microphone",
            description = "Allow speech recognition and voice input.",
            action = CapabilityAction.OpenSettings("android.settings.APPLICATION_DETAILS_SETTINGS"),
        ),
        PermissionRequirement(
            id = "shizuku.permission",
            title = "Shizuku",
            description = "Grant Shizuku permission for shell-backed automation.",
            action = CapabilityAction.OpenSettings("moe.shizuku.manager.action.MANAGE_APPLICATIONS"),
        ),
    )
}
