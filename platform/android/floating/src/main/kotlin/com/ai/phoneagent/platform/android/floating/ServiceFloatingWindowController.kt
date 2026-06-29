package com.ai.phoneagent.platform.android.floating

import android.content.Context
import android.content.Intent
import com.ai.phoneagent.core.capability.CapabilityError
import com.ai.phoneagent.core.capability.CapabilityResult
import com.ai.phoneagent.core.capability.FloatingWindowRequest
import com.ai.phoneagent.core.capability.FloatingWindowSession
import com.ai.phoneagent.core.capability.FloatingWindowState

class ServiceFloatingWindowController(
    private val context: Context,
) : FloatingWindowController {
    override fun currentState(): FloatingWindowState = Re0FloatingWindowService.currentState()

    override fun show(
        session: FloatingWindowSession,
        request: FloatingWindowRequest,
    ): CapabilityResult<FloatingWindowSession> {
        return runCatching {
            context.startService(
                Intent(context, Re0FloatingWindowService::class.java).apply {
                    action = Re0FloatingWindowService.ACTION_SHOW
                    putExtra(Re0FloatingWindowService.EXTRA_SESSION_ID, session.sessionId)
                    putExtra(Re0FloatingWindowService.EXTRA_MODE, session.mode)
                    request.widthDp?.let { putExtra(Re0FloatingWindowService.EXTRA_WIDTH_DP, it) }
                    request.heightDp?.let { putExtra(Re0FloatingWindowService.EXTRA_HEIGHT_DP, it) }
                },
            )
            CapabilityResult.success(session)
        }.getOrElse { error ->
            CapabilityResult.failure(
                CapabilityError(
                    code = "floating_window.service_start_failed",
                    message = error.message ?: "Failed to start floating window service.",
                    recoverable = true,
                ),
            )
        }
    }

    override fun hide(sessionId: String): CapabilityResult<Unit> {
        val current = Re0FloatingWindowService.currentState()
        if (current.sessionId != null && current.sessionId != sessionId) {
            return CapabilityResult.failure(
                CapabilityError(
                    code = "floating_window.session_mismatch",
                    message = "Requested session '$sessionId' does not match visible session '${current.sessionId}'.",
                    recoverable = false,
                ),
            )
        }
        return runCatching {
            context.startService(
                Intent(context, Re0FloatingWindowService::class.java).apply {
                    action = Re0FloatingWindowService.ACTION_HIDE
                    putExtra(Re0FloatingWindowService.EXTRA_SESSION_ID, sessionId)
                },
            )
            CapabilityResult.success(Unit)
        }.getOrElse { error ->
            CapabilityResult.failure(
                CapabilityError(
                    code = "floating_window.service_hide_failed",
                    message = error.message ?: "Failed to hide floating window service.",
                    recoverable = true,
                ),
            )
        }
    }
}
