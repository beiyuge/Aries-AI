package com.ai.phoneagent.platform.android.accessibility

import android.content.Context
import android.provider.Settings

interface AccessibilityServiceStatus {
    fun isEnabled(): Boolean
    fun isConnected(): Boolean
}

class AndroidAccessibilityServiceStatus(
    private val context: Context,
) : AccessibilityServiceStatus {
    override fun isEnabled(): Boolean {
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        )
        return AccessibilityServiceSettingsParser.containsService(
            enabledServices = enabledServices,
            packageName = context.packageName,
            serviceClassName = Re0AccessibilityService::class.java.name,
        )
    }

    override fun isConnected(): Boolean = Re0AccessibilityBridge.isConnected()
}

object AccessibilityServiceSettingsParser {
    fun containsService(
        enabledServices: String?,
        packageName: String,
        serviceClassName: String,
    ): Boolean {
        if (enabledServices.isNullOrBlank()) return false
        val relativeName = if (serviceClassName.startsWith(packageName)) {
            ".${serviceClassName.removePrefix(packageName).removePrefix(".")}"
        } else {
            serviceClassName
        }
        val accepted = setOf(
            "$packageName/$serviceClassName",
            "$packageName/$relativeName",
        )
        return enabledServices.split(':').any { entry -> entry in accepted }
    }
}
