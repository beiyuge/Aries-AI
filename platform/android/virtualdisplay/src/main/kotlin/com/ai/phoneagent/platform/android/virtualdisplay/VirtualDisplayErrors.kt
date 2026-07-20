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

    fun sessionAlreadyActive(sessionId: String): CapabilityError = CapabilityError(
        code = "virtual_display.session_active",
        message = "Virtual display session '$sessionId' must be stopped before starting another session.",
        recoverable = true,
        suggestedAction = "Stop the active virtual display session and retry.",
    )

    fun applicationNotFound(applicationId: String): CapabilityError = CapabilityError(
        code = "virtual_display.application_not_found",
        message = "No launchable application '$applicationId' is installed.",
        recoverable = false,
    )

    fun launchDenied(applicationId: String, causeClass: String?): CapabilityError = CapabilityError(
        code = "virtual_display.launch_denied",
        message = "Android denied launching '$applicationId' on the virtual display.",
        causeClass = causeClass,
        recoverable = true,
    )

    fun frameUnavailable(): CapabilityError = CapabilityError(
        code = "virtual_display.frame_unavailable",
        message = "No frame is available for this virtual display yet.",
        recoverable = true,
    )

    fun blackFrame(): CapabilityError = CapabilityError(
        code = "virtual_display.black_frame",
        message = "The virtual display produced only near-black frames.",
        recoverable = true,
        suggestedAction = "Launch content on the virtual display and retry capture.",
    )

    fun frameReadFailed(causeClass: String?): CapabilityError = CapabilityError(
        code = "virtual_display.frame_read_failed",
        message = "The virtual display frame could not be decoded.",
        causeClass = causeClass,
        recoverable = true,
    )
}
