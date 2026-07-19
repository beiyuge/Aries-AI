package com.ai.phoneagent.re0.host

import com.ai.phoneagent.re0.generated.AutomationResultDto

interface ScreenCaptureSessionControl {
    fun requestConsent(callback: (Result<AutomationResultDto>) -> Unit)

    fun stopSession(): AutomationResultDto
}

object UnavailableScreenCaptureSessionControl : ScreenCaptureSessionControl {
    override fun requestConsent(callback: (Result<AutomationResultDto>) -> Unit) {
        callback(
            Result.success(
                automationFailure(
                    code = "screen_capture.consent_unavailable",
                    message = "This host cannot launch screen capture consent.",
                    recoverable = false,
                ),
            ),
        )
    }

    override fun stopSession(): AutomationResultDto = automationFailure(
        code = "screen_capture.session_control_unavailable",
        message = "This host cannot stop a screen capture session.",
        recoverable = false,
    )
}
