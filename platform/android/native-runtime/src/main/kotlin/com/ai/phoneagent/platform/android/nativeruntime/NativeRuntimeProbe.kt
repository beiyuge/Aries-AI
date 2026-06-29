package com.ai.phoneagent.platform.android.nativeruntime

import com.ai.phoneagent.core.capability.NativeRuntimeInfo

interface NativeRuntimeProbe {
    fun inspect(): NativeRuntimeInfo
}
