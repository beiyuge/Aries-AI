package com.ai.phoneagent.core.capability

data class ShellExecRequest(
    val argv: List<String>,
    val timeoutMs: Long = 10_000,
)

data class ShellExecResult(
    val exitCode: Int,
    val stdout: String,
    val stderr: String,
    val error: CapabilityError? = null,
) {
    val success: Boolean = exitCode == 0 && error == null
}

data class CaptureRequest(
    val displayId: Int? = null,
    val format: CaptureFormat = CaptureFormat.Png,
    val maxWidth: Int? = null,
)

enum class CaptureFormat { Png, Jpeg }

data class CaptureResult(
    val bytes: ByteArray = ByteArray(0),
    val width: Int = 0,
    val height: Int = 0,
    val source: String,
    val error: CapabilityError? = null,
) {
    val success: Boolean = error == null

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CaptureResult) return false
        return bytes.contentEquals(other.bytes) && width == other.width && height == other.height && source == other.source && error == other.error
    }

    override fun hashCode(): Int {
        var result = bytes.contentHashCode()
        result = 31 * result + width
        result = 31 * result + height
        result = 31 * result + source.hashCode()
        result = 31 * result + (error?.hashCode() ?: 0)
        return result
    }
}

data class UiTreeDumpRequest(
    val displayId: Int? = null,
    val detail: UiTreeDetail = UiTreeDetail.Summary,
)

enum class UiTreeDetail { Minimal, Summary, Full }

data class UiTreeDumpResult(
    val json: String,
    val nodeCount: Int,
    val source: String,
    val error: CapabilityError? = null,
) {
    val success: Boolean = error == null
}

data class ScreenPoint(val x: Int, val y: Int)

data class TapRequest(val point: ScreenPoint, val displayId: Int? = null)

data class SwipeRequest(
    val from: ScreenPoint,
    val to: ScreenPoint,
    val durationMs: Long = 300,
    val displayId: Int? = null,
)

data class TypeTextRequest(val text: String, val displayId: Int? = null)

data class KeyRequest(val keyCode: Int, val displayId: Int? = null)

data class InputResult(
    val backend: String,
    val durationMs: Long,
    val error: CapabilityError? = null,
) {
    val success: Boolean = error == null
}

data class VirtualDisplayState(
    val sessionId: String? = null,
    val lifecycle: VirtualDisplayLifecycle = VirtualDisplayLifecycle.Idle,
    val displayId: Int? = null,
    val diagnostics: Map<String, String> = emptyMap(),
)

enum class VirtualDisplayLifecycle { Idle, Preparing, Starting, Running, Recovering, Stopping, Failed }

data class VirtualDisplayStartRequest(
    val width: Int,
    val height: Int,
    val densityDpi: Int,
)

data class VirtualDisplayLaunchRequest(
    val applicationId: String,
)

data class VirtualDisplayResult(
    val sessionId: String,
    val displayId: Int,
    val error: CapabilityError? = null,
) {
    val success: Boolean = error == null
}

data class FloatingWindowState(
    val sessionId: String? = null,
    val visible: Boolean = false,
    val mode: String? = null,
)

data class FloatingWindowRequest(
    val mode: String,
    val widthDp: Int? = null,
    val heightDp: Int? = null,
)

data class FloatingWindowSession(val sessionId: String, val mode: String)

data class BackgroundTaskRequest(
    val taskId: String,
    val kind: String,
    val payload: Map<String, String> = emptyMap(),
)

data class BackgroundTaskStatus(
    val taskId: String,
    val kind: String,
    val state: BackgroundTaskState,
    val diagnostics: Map<String, String> = emptyMap(),
)

enum class BackgroundTaskState { Enqueued, Running, Succeeded, Failed, Cancelled, Unknown }

data class NativeRuntimeInfo(
    val platform: String,
    val osVersion: String,
    val sdkInt: Int?,
    val supportedAbis: List<String>,
    val diagnostics: Map<String, String> = emptyMap(),
)

data class SpeechRecognitionRequest(val locale: String? = null)

sealed interface SpeechRecognitionEvent {
    data class Partial(val text: String) : SpeechRecognitionEvent
    data class Final(val text: String) : SpeechRecognitionEvent
    data class Failed(val error: CapabilityError) : SpeechRecognitionEvent
}

data class LocalModelLoadRequest(val modelId: String, val path: String)

data class LocalGenerateRequest(val modelId: String, val prompt: String)

sealed interface LocalGenerateEvent {
    data class Token(val text: String) : LocalGenerateEvent
    data object Done : LocalGenerateEvent
    data class Failed(val error: CapabilityError) : LocalGenerateEvent
}
