package com.ai.phoneagent.re0.host

import com.ai.phoneagent.core.capability.Capability
import com.ai.phoneagent.core.capability.CapabilityId
import com.ai.phoneagent.core.capability.CapabilityIds
import com.ai.phoneagent.core.capability.CaptureFormat
import com.ai.phoneagent.core.capability.CaptureRequest
import com.ai.phoneagent.core.capability.InputInjectionCapability
import com.ai.phoneagent.core.capability.KeyRequest
import com.ai.phoneagent.core.capability.ScreenCaptureCapability
import com.ai.phoneagent.core.capability.ScreenPoint
import com.ai.phoneagent.core.capability.SwipeRequest
import com.ai.phoneagent.core.capability.TapRequest
import com.ai.phoneagent.core.capability.TypeTextRequest
import com.ai.phoneagent.core.capability.UiTreeCapability
import com.ai.phoneagent.core.capability.UiTreeDetail
import com.ai.phoneagent.core.capability.UiTreeDumpRequest
import com.ai.phoneagent.platform.android.capability.AndroidCapabilityRegistry
import com.ai.phoneagent.re0.generated.AutomationHostApi
import com.ai.phoneagent.re0.generated.AutomationResultDto
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AndroidAutomationHostApi(
    private val registry: AndroidCapabilityRegistry,
    private val scope: CoroutineScope,
    private val workerDispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val screenCaptureSessionControl: ScreenCaptureSessionControl =
        UnavailableScreenCaptureSessionControl,
) : AutomationHostApi {
    override fun checkReadiness(callback: (Result<AutomationResultDto>) -> Unit) {
        launchResult(callback) {
            val health = registry.healthSnapshot()
            val available = health.count { it.available }
            AutomationResultDto(
                success = true,
                summary = "$available/${health.size} capabilities ready",
                recoverable = false,
                text = health.joinToString(separator = "\n") { item ->
                    "${item.id.value}: ${item.state.name}"
                },
                mimeType = "text/plain",
            )
        }
    }

    override fun requestScreenCaptureConsent(callback: (Result<AutomationResultDto>) -> Unit) {
        screenCaptureSessionControl.requestConsent(callback)
    }

    override fun stopScreenCaptureSession(callback: (Result<AutomationResultDto>) -> Unit) {
        callback.success(screenCaptureSessionControl.stopSession())
    }

    override fun dumpUiTree(
        detail: String,
        callback: (Result<AutomationResultDto>) -> Unit,
    ) {
        val parsedDetail = parseDetail(detail)
            ?: return callback.success(
                automationFailure(
                    code = "automation.invalid_ui_tree_detail",
                    message = "Unknown UI tree detail '$detail'.",
                    recoverable = false,
                ),
            )
        withCapability<UiTreeCapability>(CapabilityIds.UiTree, callback) { capability ->
            capability.dump(UiTreeDumpRequest(detail = parsedDetail)).toAutomationResult()
        }
    }

    override fun captureScreen(callback: (Result<AutomationResultDto>) -> Unit) {
        withCapability<ScreenCaptureCapability>(CapabilityIds.ScreenCapture, callback) { capability ->
            val format = CaptureFormat.Png
            capability.capture(CaptureRequest(format = format)).toAutomationResult(format)
        }
    }

    override fun tap(
        x: Long,
        y: Long,
        callback: (Result<AutomationResultDto>) -> Unit,
    ) {
        val point = pointOrReply(x, y, callback) ?: return
        withCapability<InputInjectionCapability>(CapabilityIds.InputInjection, callback) { capability ->
            capability.tap(TapRequest(point)).toAutomationResult("Tap")
        }
    }

    override fun swipe(
        fromX: Long,
        fromY: Long,
        toX: Long,
        toY: Long,
        durationMs: Long,
        callback: (Result<AutomationResultDto>) -> Unit,
    ) {
        val from = pointOrReply(fromX, fromY, callback) ?: return
        val to = pointOrReply(toX, toY, callback) ?: return
        val duration = durationMs.toIntOrNull()
        if (duration == null || duration !in 1..60_000) {
            callback.success(
                automationFailure(
                    code = "automation.invalid_duration",
                    message = "Swipe duration must be between 1 and 60000ms.",
                    recoverable = false,
                ),
            )
            return
        }
        withCapability<InputInjectionCapability>(CapabilityIds.InputInjection, callback) { capability ->
            capability.swipe(
                SwipeRequest(from = from, to = to, durationMs = duration.toLong()),
            ).toAutomationResult("Swipe")
        }
    }

    override fun typeText(
        text: String,
        callback: (Result<AutomationResultDto>) -> Unit,
    ) {
        if (text.isBlank()) {
            callback.success(
                automationFailure(
                    code = "automation.empty_text",
                    message = "Text input cannot be blank.",
                    recoverable = false,
                ),
            )
            return
        }
        withCapability<InputInjectionCapability>(CapabilityIds.InputInjection, callback) { capability ->
            capability.typeText(TypeTextRequest(text)).toAutomationResult("Text input")
        }
    }

    override fun pressKey(
        keyCode: Long,
        callback: (Result<AutomationResultDto>) -> Unit,
    ) {
        val parsedKeyCode = keyCode.toIntOrNull()
        if (parsedKeyCode == null || parsedKeyCode < 0) {
            callback.success(
                automationFailure(
                    code = "automation.invalid_key_code",
                    message = "Key code must be a non-negative 32-bit integer.",
                    recoverable = false,
                ),
            )
            return
        }
        withCapability<InputInjectionCapability>(CapabilityIds.InputInjection, callback) { capability ->
            capability.key(KeyRequest(parsedKeyCode)).toAutomationResult("Key input")
        }
    }

    private fun parseDetail(detail: String): UiTreeDetail? = when (detail.lowercase()) {
        "minimal" -> UiTreeDetail.Minimal
        "summary" -> UiTreeDetail.Summary
        "full" -> UiTreeDetail.Full
        else -> null
    }

    private fun pointOrReply(
        x: Long,
        y: Long,
        callback: (Result<AutomationResultDto>) -> Unit,
    ): ScreenPoint? {
        val parsedX = x.toIntOrNull()
        val parsedY = y.toIntOrNull()
        if (parsedX == null || parsedY == null || parsedX < 0 || parsedY < 0) {
            callback.success(
                automationFailure(
                    code = "automation.invalid_point",
                    message = "Screen coordinates must be non-negative 32-bit integers.",
                    recoverable = false,
                ),
            )
            return null
        }
        return ScreenPoint(parsedX, parsedY)
    }

    private inline fun <reified T : Capability> withCapability(
        id: CapabilityId,
        noinline callback: (Result<AutomationResultDto>) -> Unit,
        crossinline operation: suspend (T) -> AutomationResultDto,
    ) {
        val capability = registry.get(id) as? T
        if (capability == null) {
            callback.success(
                automationFailure(
                    code = "capability_not_registered",
                    message = "Capability '${id.value}' is not registered on this platform.",
                    recoverable = false,
                ),
            )
            return
        }
        launchResult(callback) { operation(capability) }
    }

    private fun launchResult(
        callback: (Result<AutomationResultDto>) -> Unit,
        operation: suspend () -> AutomationResultDto,
    ) {
        scope.launch {
            callback(Result.success(withContext(workerDispatcher) { operation() }))
        }
    }

    private fun Long.toIntOrNull(): Int? = if (
        this >= Int.MIN_VALUE.toLong() && this <= Int.MAX_VALUE.toLong()
    ) {
        toInt()
    } else {
        null
    }

    private fun <T> ((Result<T>) -> Unit).success(value: T) {
        this(Result.success(value))
    }
}
