package com.ai.phoneagent.platform.android.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.ai.phoneagent.core.capability.CapabilityError
import com.ai.phoneagent.core.capability.InputResult
import com.ai.phoneagent.core.capability.ScreenPoint
import com.ai.phoneagent.core.capability.TapRequest
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

object Re0AccessibilityBridge {
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

    suspend fun tap(request: TapRequest): InputResult {
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
        return service.dispatchTap(request.point)
    }

    private suspend fun Re0AccessibilityService.dispatchTap(point: ScreenPoint): InputResult =
        suspendCancellableCoroutine { continuation ->
            val path = Path().apply {
                moveTo(point.x.toFloat(), point.y.toFloat())
            }
            val gesture = GestureDescription.Builder()
                .addStroke(GestureDescription.StrokeDescription(path, 0, 1))
                .build()
            val dispatched = dispatchGesture(
                gesture,
                object : AccessibilityService.GestureResultCallback() {
                    override fun onCompleted(gestureDescription: GestureDescription?) {
                        if (continuation.isActive) {
                            continuation.resume(InputResult(backend = "accessibility", durationMs = 1))
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
