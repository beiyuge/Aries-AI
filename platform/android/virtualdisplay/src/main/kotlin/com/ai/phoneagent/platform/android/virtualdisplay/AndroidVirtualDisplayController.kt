package com.ai.phoneagent.platform.android.virtualdisplay

import com.ai.phoneagent.core.capability.CapabilityResult
import com.ai.phoneagent.core.capability.CaptureRequest
import com.ai.phoneagent.core.capability.CaptureResult
import com.ai.phoneagent.core.capability.VirtualDisplayStartRequest

interface AndroidVirtualDisplayController {
    val diagnostics: Map<String, String>
    suspend fun start(request: VirtualDisplayStartRequest): CapabilityResult<AndroidVirtualDisplaySession>
    suspend fun stop(sessionId: String): CapabilityResult<Unit>
    suspend fun capture(sessionId: String, request: CaptureRequest): CaptureResult
}

data class AndroidVirtualDisplaySession(
    val sessionId: String,
    val displayId: Int,
    val width: Int,
    val height: Int,
    val densityDpi: Int,
    val diagnostics: Map<String, String> = emptyMap(),
)
