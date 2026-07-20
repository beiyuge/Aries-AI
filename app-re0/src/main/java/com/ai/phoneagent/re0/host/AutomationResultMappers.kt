package com.ai.phoneagent.re0.host

import com.ai.phoneagent.core.capability.CapabilityError
import com.ai.phoneagent.core.capability.CapabilityResult
import com.ai.phoneagent.core.capability.CaptureFormat
import com.ai.phoneagent.core.capability.CaptureResult
import com.ai.phoneagent.core.capability.InputResult
import com.ai.phoneagent.core.capability.UiTreeDumpResult
import com.ai.phoneagent.core.capability.VirtualDisplayResult
import com.ai.phoneagent.re0.generated.AutomationResultDto

internal fun UiTreeDumpResult.toAutomationResult(): AutomationResultDto = error?.toAutomationResult()
    ?: AutomationResultDto(
        success = true,
        summary = "$nodeCount UI nodes from $source",
        recoverable = false,
        text = json,
        mimeType = "application/json",
    )

internal fun CaptureResult.toAutomationResult(format: CaptureFormat): AutomationResultDto = error?.toAutomationResult()
    ?: AutomationResultDto(
        success = true,
        summary = "Captured ${width}x$height from $source",
        recoverable = false,
        bytes = bytes,
        mimeType = when (format) {
            CaptureFormat.Png -> "image/png"
            CaptureFormat.Jpeg -> "image/jpeg"
        },
    )

internal fun InputResult.toAutomationResult(action: String): AutomationResultDto = error?.toAutomationResult()
    ?: AutomationResultDto(
        success = true,
        summary = "$action completed through $backend in ${durationMs}ms",
        recoverable = false,
    )

internal fun VirtualDisplayResult.toAutomationResult(): AutomationResultDto = error?.toAutomationResult()
    ?: AutomationResultDto(
        success = true,
        summary = "Virtual display session ready on display $displayId",
        recoverable = false,
        text = "sessionId=$sessionId\ndisplayId=$displayId",
        mimeType = "text/plain",
    )

internal fun CapabilityResult<Unit>.toUnitAutomationResult(
    successSummary: String,
): AutomationResultDto = errorOrNull()?.toAutomationResult()
    ?: AutomationResultDto(
        success = true,
        summary = successSummary,
        recoverable = false,
    )

internal fun CapabilityError.toAutomationResult(): AutomationResultDto = AutomationResultDto(
    success = false,
    summary = message,
    recoverable = recoverable,
    errorCode = code,
    errorMessage = message,
)

internal fun automationFailure(
    code: String,
    message: String,
    recoverable: Boolean,
): AutomationResultDto = AutomationResultDto(
    success = false,
    summary = message,
    recoverable = recoverable,
    errorCode = code,
    errorMessage = message,
)
