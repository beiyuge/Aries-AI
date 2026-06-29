package com.ai.phoneagent.platform.android.background

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.ai.phoneagent.core.capability.BackgroundTaskRequest
import com.ai.phoneagent.core.capability.BackgroundTaskState
import com.ai.phoneagent.core.capability.BackgroundTaskStatus
import com.ai.phoneagent.core.capability.BackgroundTasksCapability
import com.ai.phoneagent.core.capability.CapabilityError
import com.ai.phoneagent.core.capability.CapabilityHealth
import com.ai.phoneagent.core.capability.CapabilityId
import com.ai.phoneagent.core.capability.CapabilityIds
import com.ai.phoneagent.core.capability.CapabilityResult
import com.ai.phoneagent.core.capability.CapabilitySelfTest
import java.util.concurrent.ExecutionException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext

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

interface AndroidBackgroundTaskScheduler {
    val diagnostics: Map<String, String>
    suspend fun enqueue(request: BackgroundTaskRequest): CapabilityResult<BackgroundTaskStatus>
    suspend fun status(taskId: String): CapabilityResult<BackgroundTaskStatus>
    suspend fun cancel(taskId: String): CapabilityResult<Unit>
}

class WorkManagerBackgroundTaskScheduler(context: Context) : AndroidBackgroundTaskScheduler {
    private val workManager = WorkManager.getInstance(context)

    override val diagnostics: Map<String, String> = mapOf(
        "platform" to "android",
        "backend" to "work-manager",
    )

    override suspend fun enqueue(request: BackgroundTaskRequest): CapabilityResult<BackgroundTaskStatus> =
        withContext(Dispatchers.IO) {
            if (request.taskId.isBlank() || request.kind.isBlank()) {
                return@withContext CapabilityResult.failure<BackgroundTaskStatus>(
                    CapabilityError(
                        code = "background_tasks.invalid_request",
                        message = "Task id and kind are required.",
                        recoverable = false,
                    ),
                )
            }

            val dataBuilder = Data.Builder()
                .putString(KEY_TASK_ID, request.taskId)
                .putString(KEY_TASK_KIND, request.kind)
            request.payload.forEach { (key, value) ->
                dataBuilder.putString("payload.$key", value)
            }

            val workRequest = OneTimeWorkRequestBuilder<Re0NoOpWorker>()
                .setInputData(dataBuilder.build())
                .addTag(tagFor(request.taskId))
                .addTag(kindTagFor(request.kind))
                .build()

            try {
                workManager.enqueueUniqueWork(request.taskId, ExistingWorkPolicy.REPLACE, workRequest).result.get()
                CapabilityResult.success(
                    BackgroundTaskStatus(
                        taskId = request.taskId,
                        kind = request.kind,
                        state = BackgroundTaskState.Enqueued,
                        diagnostics = diagnostics,
                    ),
                )
            } catch (error: InterruptedException) {
                Thread.currentThread().interrupt()
                CapabilityResult.failure(enqueueError(error.message))
            } catch (error: ExecutionException) {
                CapabilityResult.failure(enqueueError(error.message))
            } catch (error: IllegalStateException) {
                CapabilityResult.failure(enqueueError(error.message))
            }
        }

    override suspend fun status(taskId: String): CapabilityResult<BackgroundTaskStatus> =
        withContext(Dispatchers.IO) {
            try {
                val workInfo = workManager.getWorkInfosByTag(tagFor(taskId)).get().firstOrNull()
                    ?: return@withContext CapabilityResult.success(
                        BackgroundTaskStatus(
                            taskId = taskId,
                            kind = "",
                            state = BackgroundTaskState.Unknown,
                            diagnostics = diagnostics + ("reason" to "not_found"),
                        ),
                    )
                CapabilityResult.success(workInfo.toStatus(taskId))
            } catch (error: InterruptedException) {
                Thread.currentThread().interrupt()
                CapabilityResult.failure(statusError(error.message))
            } catch (error: ExecutionException) {
                CapabilityResult.failure(statusError(error.message))
            } catch (error: IllegalStateException) {
                CapabilityResult.failure(statusError(error.message))
            }
        }

    override suspend fun cancel(taskId: String): CapabilityResult<Unit> =
        withContext(Dispatchers.IO) {
            try {
                workManager.cancelUniqueWork(taskId).result.get()
                CapabilityResult.success(Unit)
            } catch (error: InterruptedException) {
                Thread.currentThread().interrupt()
                CapabilityResult.failure(cancelError(error.message))
            } catch (error: ExecutionException) {
                CapabilityResult.failure(cancelError(error.message))
            } catch (error: IllegalStateException) {
                CapabilityResult.failure(cancelError(error.message))
            }
        }

    private fun WorkInfo.toStatus(taskId: String): BackgroundTaskStatus = BackgroundTaskStatus(
        taskId = taskId,
        kind = outputData.getString(KEY_TASK_KIND) ?: tags.firstNotNullOfOrNull { tag ->
            tag.removePrefix(TASK_KIND_TAG_PREFIX).takeIf { tag.startsWith(TASK_KIND_TAG_PREFIX) }
        }.orEmpty(),
        state = when (state) {
            WorkInfo.State.ENQUEUED -> BackgroundTaskState.Enqueued
            WorkInfo.State.RUNNING -> BackgroundTaskState.Running
            WorkInfo.State.SUCCEEDED -> BackgroundTaskState.Succeeded
            WorkInfo.State.FAILED -> BackgroundTaskState.Failed
            WorkInfo.State.BLOCKED -> BackgroundTaskState.Enqueued
            WorkInfo.State.CANCELLED -> BackgroundTaskState.Cancelled
        },
        diagnostics = diagnostics + ("work_id" to id.toString()),
    )

    private fun enqueueError(message: String?): CapabilityError = CapabilityError(
        code = "background_tasks.enqueue_failed",
        message = message ?: "WorkManager failed to enqueue the task.",
        recoverable = true,
    )

    private fun statusError(message: String?): CapabilityError = CapabilityError(
        code = "background_tasks.status_failed",
        message = message ?: "WorkManager failed to read the task status.",
        recoverable = true,
    )

    private fun cancelError(message: String?): CapabilityError = CapabilityError(
        code = "background_tasks.cancel_failed",
        message = message ?: "WorkManager failed to cancel the task.",
        recoverable = true,
    )

    private fun tagFor(taskId: String): String = "$TASK_TAG_PREFIX$taskId"

    private fun kindTagFor(kind: String): String = "$TASK_KIND_TAG_PREFIX$kind"

    private companion object {
        const val KEY_TASK_ID = "task_id"
        const val KEY_TASK_KIND = "task_kind"
        const val TASK_TAG_PREFIX = "re0.background."
        const val TASK_KIND_TAG_PREFIX = "re0.background.kind."
    }
}

class Re0NoOpWorker(
    context: Context,
    parameters: WorkerParameters,
) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result {
        val output = Data.Builder()
            .putString("task_id", inputData.getString("task_id"))
            .putString("task_kind", inputData.getString("task_kind"))
            .build()
        return Result.success(output)
    }
}
