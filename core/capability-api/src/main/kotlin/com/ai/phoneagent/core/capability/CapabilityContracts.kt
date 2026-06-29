package com.ai.phoneagent.core.capability

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

typealias PermissionRequirement = CapabilityRequirement

interface PermissionCapability : Capability {
    suspend fun listRequirements(): List<PermissionRequirement>
    suspend fun openRequirementSettings(requirementId: String): CapabilityResult<Unit>
}

interface ShellExecutionCapability : Capability {
    suspend fun exec(request: ShellExecRequest): ShellExecResult
}

interface ScreenCaptureCapability : Capability {
    suspend fun capture(request: CaptureRequest): CaptureResult
}

interface UiTreeCapability : Capability {
    suspend fun dump(request: UiTreeDumpRequest): UiTreeDumpResult
}

interface InputInjectionCapability : Capability {
    suspend fun tap(request: TapRequest): InputResult
    suspend fun swipe(request: SwipeRequest): InputResult
    suspend fun typeText(request: TypeTextRequest): InputResult
    suspend fun key(request: KeyRequest): InputResult
}

interface VirtualDisplayCapability : Capability {
    val state: StateFlow<VirtualDisplayState>
    suspend fun start(request: VirtualDisplayStartRequest): VirtualDisplayResult
    suspend fun stop(sessionId: String): CapabilityResult<Unit>
    suspend fun capture(sessionId: String, request: CaptureRequest): CaptureResult
}

interface FloatingWindowCapability : Capability {
    val state: StateFlow<FloatingWindowState>
    suspend fun show(request: FloatingWindowRequest): CapabilityResult<FloatingWindowSession>
    suspend fun hide(sessionId: String): CapabilityResult<Unit>
}

interface BackgroundTasksCapability : Capability {
    suspend fun enqueue(request: BackgroundTaskRequest): CapabilityResult<BackgroundTaskStatus>
    suspend fun status(taskId: String): CapabilityResult<BackgroundTaskStatus>
    suspend fun cancel(taskId: String): CapabilityResult<Unit>
}

interface NativeRuntimeCapability : Capability {
    suspend fun inspect(): CapabilityResult<NativeRuntimeInfo>
}

interface SpeechRecognitionCapability : Capability {
    fun recognize(request: SpeechRecognitionRequest): Flow<SpeechRecognitionEvent>
}

interface LocalModelCapability : Capability {
    suspend fun load(request: LocalModelLoadRequest): CapabilityResult<Unit>
    fun generate(request: LocalGenerateRequest): Flow<LocalGenerateEvent>
    suspend fun unload(modelId: String): CapabilityResult<Unit>
}
