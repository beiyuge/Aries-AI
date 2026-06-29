package com.ai.phoneagent.platform.android.background

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.ai.phoneagent.core.capability.BackgroundTaskRequest
import com.ai.phoneagent.core.capability.BackgroundTaskState
import com.ai.phoneagent.core.capability.BackgroundTaskStatus
import com.ai.phoneagent.core.capability.CapabilityError
import com.ai.phoneagent.core.capability.CapabilityResult
import java.util.concurrent.ExecutionException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
                .putString(BackgroundTaskKeys.TASK_ID, request.taskId)
                .putString(BackgroundTaskKeys.TASK_KIND, request.kind)
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
        kind = outputData.getString(BackgroundTaskKeys.TASK_KIND) ?: tags.firstNotNullOfOrNull { tag ->
            tag.removePrefix(BackgroundTaskKeys.TASK_KIND_TAG_PREFIX)
                .takeIf { tag.startsWith(BackgroundTaskKeys.TASK_KIND_TAG_PREFIX) }
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

    private fun tagFor(taskId: String): String = "${BackgroundTaskKeys.TASK_TAG_PREFIX}$taskId"

    private fun kindTagFor(kind: String): String = "${BackgroundTaskKeys.TASK_KIND_TAG_PREFIX}$kind"
}
