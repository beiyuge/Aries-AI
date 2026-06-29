package com.ai.phoneagent.platform.android.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Bundle
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.ai.phoneagent.core.capability.CapabilityError
import com.ai.phoneagent.core.capability.InputResult
import com.ai.phoneagent.core.capability.SwipeRequest
import com.ai.phoneagent.core.capability.TapRequest
import com.ai.phoneagent.core.capability.TypeTextRequest
import com.ai.phoneagent.core.capability.KeyRequest
import com.ai.phoneagent.core.capability.UiTreeDetail
import com.ai.phoneagent.core.capability.UiTreeDumpRequest
import com.ai.phoneagent.core.capability.UiTreeDumpResult
import java.lang.ref.WeakReference
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

class Re0AccessibilityService : AccessibilityService() {
    override fun onServiceConnected() {
        Re0AccessibilityBridge.attach(this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        Re0AccessibilityBridge.detach(this)
        super.onDestroy()
    }
}

interface AccessibilityInputBackend {
    suspend fun tap(request: TapRequest): InputResult
    suspend fun swipe(request: SwipeRequest): InputResult
    suspend fun typeText(request: TypeTextRequest): InputResult
    suspend fun key(request: KeyRequest): InputResult
}

object Re0AccessibilityBridge : AccessibilityInputBackend {
    private var serviceRef: WeakReference<Re0AccessibilityService>? = null

    fun attach(service: Re0AccessibilityService) {
        serviceRef = WeakReference(service)
    }

    fun detach(service: Re0AccessibilityService) {
        if (serviceRef?.get() == service) {
            serviceRef = null
        }
    }

    fun isConnected(): Boolean = serviceRef?.get() != null

    fun dumpUiTree(request: UiTreeDumpRequest): UiTreeDumpResult {
        val service = serviceRef?.get()
            ?: return UiTreeDumpResult(
                json = "{}",
                nodeCount = 0,
                source = "accessibility",
                error = CapabilityError(
                    code = "accessibility.service_disconnected",
                    message = "Accessibility service is not connected.",
                    recoverable = true,
                ),
            )
        val root = service.rootInActiveWindow
            ?: return UiTreeDumpResult(
                json = "{}",
                nodeCount = 0,
                source = "accessibility",
                error = CapabilityError(
                    code = "ui_tree.root_missing",
                    message = "Accessibility rootInActiveWindow is null.",
                    recoverable = true,
                ),
            )
        val builder = StringBuilder()
        val nodeCount = appendNodeJson(root, request.detail, builder, 0)
        return UiTreeDumpResult(
            json = builder.toString(),
            nodeCount = nodeCount,
            source = "accessibility",
        )
    }

    override suspend fun tap(request: TapRequest): InputResult {
        val service = serviceRef?.get()
            ?: return InputResult(
                backend = "accessibility",
                durationMs = 0,
                error = CapabilityError(
                    code = "accessibility.service_disconnected",
                    message = "Accessibility service is not connected.",
                    recoverable = true,
                ),
            )
        return service.dispatchGesturePath(
            path = Path().apply {
                moveTo(request.point.x.toFloat(), request.point.y.toFloat())
            },
            startTimeMs = 0,
            durationMs = 1,
        )
    }

    override suspend fun swipe(request: SwipeRequest): InputResult {
        val service = serviceRef?.get()
            ?: return disconnectedInputResult()
        val path = Path().apply {
            moveTo(request.from.x.toFloat(), request.from.y.toFloat())
            lineTo(request.to.x.toFloat(), request.to.y.toFloat())
        }
        return service.dispatchGesturePath(
            path = path,
            startTimeMs = 0,
            durationMs = request.durationMs,
        )
    }

    override suspend fun typeText(request: TypeTextRequest): InputResult {
        val service = serviceRef?.get()
            ?: return disconnectedInputResult()
        val focusedNode = service.rootInActiveWindow?.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
            ?: return InputResult(
                backend = "accessibility",
                durationMs = 0,
                error = CapabilityError(
                    code = "input.focus_missing",
                    message = "No focused editable node is available for text input.",
                    recoverable = true,
                ),
            )
        val args = Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, request.text)
        }
        val startedAt = System.currentTimeMillis()
        val performed = focusedNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
        focusedNode.recycle()
        return if (performed) {
            InputResult(
                backend = "accessibility",
                durationMs = System.currentTimeMillis() - startedAt,
            )
        } else {
            InputResult(
                backend = "accessibility",
                durationMs = System.currentTimeMillis() - startedAt,
                error = CapabilityError(
                    code = "input.type_text_rejected",
                    message = "Focused node rejected ACTION_SET_TEXT.",
                    recoverable = true,
                ),
            )
        }
    }

    override suspend fun key(request: KeyRequest): InputResult {
        val service = serviceRef?.get()
            ?: return disconnectedInputResult()
        val action = when (request.keyCode) {
            KeyEvent.KEYCODE_BACK -> AccessibilityService.GLOBAL_ACTION_BACK
            KeyEvent.KEYCODE_HOME -> AccessibilityService.GLOBAL_ACTION_HOME
            KeyEvent.KEYCODE_APP_SWITCH -> AccessibilityService.GLOBAL_ACTION_RECENTS
            KeyEvent.KEYCODE_NOTIFICATION -> AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS
            KeyEvent.KEYCODE_SETTINGS -> AccessibilityService.GLOBAL_ACTION_QUICK_SETTINGS
            else -> null
        } ?: return InputResult(
            backend = "accessibility",
            durationMs = 0,
            error = CapabilityError(
                code = "input.key_unsupported",
                message = "Accessibility backend supports only global navigation key actions.",
                recoverable = true,
            ),
        )
        val startedAt = System.currentTimeMillis()
        val performed = service.performGlobalAction(action)
        return if (performed) {
            InputResult(
                backend = "accessibility",
                durationMs = System.currentTimeMillis() - startedAt,
            )
        } else {
            InputResult(
                backend = "accessibility",
                durationMs = System.currentTimeMillis() - startedAt,
                error = CapabilityError(
                    code = "input.global_action_rejected",
                    message = "Accessibility rejected the requested global key action.",
                    recoverable = true,
                ),
            )
        }
    }

    private suspend fun Re0AccessibilityService.dispatchGesturePath(
        path: Path,
        startTimeMs: Long,
        durationMs: Long,
    ): InputResult =
        suspendCancellableCoroutine { continuation ->
            val startedAt = System.currentTimeMillis()
            val gesture = GestureDescription.Builder()
                .addStroke(GestureDescription.StrokeDescription(path, startTimeMs, durationMs))
                .build()
            val dispatched = dispatchGesture(
                gesture,
                object : AccessibilityService.GestureResultCallback() {
                    override fun onCompleted(gestureDescription: GestureDescription?) {
                        if (continuation.isActive) {
                            continuation.resume(
                                InputResult(
                                    backend = "accessibility",
                                    durationMs = System.currentTimeMillis() - startedAt,
                                ),
                            )
                        }
                    }

                    override fun onCancelled(gestureDescription: GestureDescription?) {
                        if (continuation.isActive) {
                            continuation.resume(
                                InputResult(
                                    backend = "accessibility",
                                    durationMs = 1,
                                    error = CapabilityError(
                                        code = "input.gesture_cancelled",
                                        message = "Accessibility gesture was cancelled.",
                                        recoverable = true,
                                    ),
                                ),
                            )
                        }
                    }
                },
                null,
            )
            if (!dispatched && continuation.isActive) {
                continuation.resume(
                    InputResult(
                        backend = "accessibility",
                        durationMs = 0,
                        error = CapabilityError(
                            code = "input.gesture_rejected",
                            message = "Accessibility rejected the gesture request.",
                            recoverable = true,
                        ),
                    ),
                )
            }
        }

    private fun disconnectedInputResult(): InputResult = InputResult(
        backend = "accessibility",
        durationMs = 0,
        error = CapabilityError(
            code = "accessibility.service_disconnected",
            message = "Accessibility service is not connected.",
            recoverable = true,
        ),
    )
}

private fun appendNodeJson(
    node: AccessibilityNodeInfo,
    detail: UiTreeDetail,
    builder: StringBuilder,
    depth: Int,
): Int {
    builder.append('{')
    appendJsonField(builder, "className", node.className?.toString().orEmpty())
    builder.append(',')
    appendJsonField(builder, "viewId", node.viewIdResourceName.orEmpty())
    if (detail != UiTreeDetail.Minimal) {
        builder.append(',')
        appendJsonField(builder, "text", node.text?.toString().orEmpty())
        builder.append(',')
        appendJsonField(builder, "contentDescription", node.contentDescription?.toString().orEmpty())
    }
    if (detail == UiTreeDetail.Full) {
        builder.append(',')
        builder.append("\"clickable\":").append(node.isClickable)
        builder.append(',')
        builder.append("\"enabled\":").append(node.isEnabled)
        builder.append(',')
        builder.append("\"depth\":").append(depth)
    }
    var count = 1
    builder.append(",\"children\":[")
    var childWritten = false
    for (index in 0 until node.childCount) {
        val child = node.getChild(index) ?: continue
        if (childWritten) builder.append(',')
        count += appendNodeJson(child, detail, builder, depth + 1)
        childWritten = true
        child.recycle()
    }
    builder.append("]}")
    return count
}

private fun appendJsonField(builder: StringBuilder, name: String, value: String) {
    builder.append('"').append(name).append("\":\"").append(escapeJson(value)).append('"')
}

private fun escapeJson(value: String): String =
    value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")
