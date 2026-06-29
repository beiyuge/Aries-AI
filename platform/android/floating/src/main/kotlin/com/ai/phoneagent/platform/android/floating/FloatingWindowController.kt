package com.ai.phoneagent.platform.android.floating

import com.ai.phoneagent.core.capability.CapabilityResult
import com.ai.phoneagent.core.capability.FloatingWindowRequest
import com.ai.phoneagent.core.capability.FloatingWindowSession
import com.ai.phoneagent.core.capability.FloatingWindowState

interface OverlayPermissionStatus {
    fun canDrawOverlays(): Boolean
}

interface FloatingWindowController {
    fun currentState(): FloatingWindowState
    fun show(session: FloatingWindowSession, request: FloatingWindowRequest): CapabilityResult<FloatingWindowSession>
    fun hide(sessionId: String): CapabilityResult<Unit>
}
