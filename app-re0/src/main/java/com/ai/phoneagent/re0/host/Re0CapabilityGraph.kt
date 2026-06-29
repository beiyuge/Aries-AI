package com.ai.phoneagent.re0.host

import android.content.Context
import com.ai.phoneagent.platform.android.capability.AndroidCapabilityRegistry
import com.ai.phoneagent.platform.android.accessibility.AndroidAccessibilityCapability
import com.ai.phoneagent.platform.android.accessibility.AndroidAccessibilityServiceStatus
import com.ai.phoneagent.platform.android.accessibility.AndroidUiTreeCapability
import com.ai.phoneagent.platform.android.background.AndroidBackgroundTasksCapability
import com.ai.phoneagent.platform.android.floating.AndroidFloatingWindowCapability
import com.ai.phoneagent.platform.android.input.AndroidInputInjectionCapability
import com.ai.phoneagent.platform.android.nativeruntime.AndroidNativeRuntimeCapability
import com.ai.phoneagent.platform.android.permissions.AndroidPermissionsCapability
import com.ai.phoneagent.platform.android.screen.AndroidScreenCaptureCapability
import com.ai.phoneagent.platform.android.shizuku.AndroidShizukuShellCapability
import com.ai.phoneagent.platform.android.virtualdisplay.AndroidVirtualDisplayCapability

object Re0CapabilityGraph {
    fun createRegistry(context: Context): AndroidCapabilityRegistry {
        val accessibilityStatus = AndroidAccessibilityServiceStatus(context.applicationContext)
        return AndroidCapabilityRegistry(
            listOf(
                AndroidPermissionsCapability(),
                AndroidShizukuShellCapability(),
                AndroidAccessibilityCapability(accessibilityStatus),
                AndroidScreenCaptureCapability(),
                AndroidUiTreeCapability(accessibilityStatus),
                AndroidInputInjectionCapability(accessibilityStatus),
                AndroidVirtualDisplayCapability(),
                AndroidFloatingWindowCapability(context.applicationContext),
                AndroidBackgroundTasksCapability(),
                AndroidNativeRuntimeCapability(),
            ),
        )
    }
}
