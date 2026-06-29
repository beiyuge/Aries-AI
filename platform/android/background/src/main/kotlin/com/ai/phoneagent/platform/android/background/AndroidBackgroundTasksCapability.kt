package com.ai.phoneagent.platform.android.background

import android.content.Context
import com.ai.phoneagent.core.capability.BackgroundTaskRequest
import com.ai.phoneagent.core.capability.BackgroundTaskStatus
import com.ai.phoneagent.core.capability.BackgroundTasksCapability
import com.ai.phoneagent.core.capability.CapabilityHealth
import com.ai.phoneagent.core.capability.CapabilityId
import com.ai.phoneagent.core.capability.CapabilityIds
import com.ai.phoneagent.core.capability.CapabilityResult
import com.ai.phoneagent.core.capability.CapabilitySelfTest
import kotlinx.coroutines.flow.MutableStateFlow

class AndroidBackgroundTasksCapability(
    scheduler: AndroidBackgroundTaskScheduler,
) : BackgroundTasksCapability, CapabilitySelfTest {
    override val id: CapabilityId = CapabilityIds.BackgroundTasks
    override val health = MutableStateFlow(CapabilityHealth.ready(id, scheduler.diagnostics))

    constructor(context: Context) : this(WorkManagerBackgroundTaskScheduler(context.applicationContext))

    private val taskScheduler = scheduler

    override suspend fun enqueue(request: BackgroundTaskRequest): CapabilityResult<BackgroundTaskStatus> =
        taskScheduler.enqueue(request)

    override suspend fun status(taskId: String): CapabilityResult<BackgroundTaskStatus> =
        taskScheduler.status(taskId)

    override suspend fun cancel(taskId: String): CapabilityResult<Unit> =
        taskScheduler.cancel(taskId)

    override fun runSelfTest(): CapabilityResult<String> =
        CapabilityResult.success("${id.value}: ${health.value.state.name} backend=${health.value.diagnostics["backend"]}")
}
