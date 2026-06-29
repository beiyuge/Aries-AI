package com.ai.phoneagent.platform.android.background

import com.ai.phoneagent.core.capability.BackgroundTaskRequest
import com.ai.phoneagent.core.capability.BackgroundTaskStatus
import com.ai.phoneagent.core.capability.CapabilityResult

interface AndroidBackgroundTaskScheduler {
    val diagnostics: Map<String, String>
    suspend fun enqueue(request: BackgroundTaskRequest): CapabilityResult<BackgroundTaskStatus>
    suspend fun status(taskId: String): CapabilityResult<BackgroundTaskStatus>
    suspend fun cancel(taskId: String): CapabilityResult<Unit>
}
