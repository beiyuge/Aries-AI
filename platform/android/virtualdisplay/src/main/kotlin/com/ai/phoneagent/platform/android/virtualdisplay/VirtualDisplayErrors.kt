package com.ai.phoneagent.platform.android.virtualdisplay

import com.ai.phoneagent.core.capability.CapabilityError

object VirtualDisplayErrors {
    fun invalidRequest(message: String): CapabilityError = CapabilityError(
        code = "virtual_display.invalid_request",
        message = message,
        recoverable = false,
    )

    fun operationFailed(message: String): CapabilityError = CapabilityError(
        code = "virtual_display.operation_failed",
        message = message,
        recoverable = true,
    )

    fun securityDenied(message: String?): CapabilityError = CapabilityError(
        code = "virtual_display.security_denied",
        message = message ?: "Android denied virtual display creation.",
        recoverable = true,
    )

    fun sessionNotFound(sessionId: String): CapabilityError = CapabilityError(
        code = "virtual_display.session_not_found",
        message = "Virtual display session '$sessionId' is not active.",
        recoverable = true,
    )

    fun frameUnavailable(): CapabilityError = CapabilityError(
        code = "virtual_display.frame_unavailable",
        message = "No frame is available for this virtual display yet.",
        recoverable = true,
    )
}
