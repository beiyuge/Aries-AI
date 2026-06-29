package com.ai.phoneagent.platform.android.nativeruntime

import android.os.Build
import com.ai.phoneagent.core.capability.NativeRuntimeInfo

object AndroidNativeRuntimeProbe : NativeRuntimeProbe {
    override fun inspect(): NativeRuntimeInfo = NativeRuntimeInfo(
        platform = "android",
        osVersion = Build.VERSION.RELEASE ?: "",
        sdkInt = Build.VERSION.SDK_INT,
        supportedAbis = Build.SUPPORTED_ABIS.toList(),
        diagnostics = mapOf(
            "platform" to "android",
            "backend" to "android-runtime",
            "manufacturer" to Build.MANUFACTURER.orEmpty(),
            "model" to Build.MODEL.orEmpty(),
        ),
    )
}
