package com.ai.phoneagent.platform.android.background

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters

class Re0NoOpWorker(
    context: Context,
    parameters: WorkerParameters,
) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result {
        val output = Data.Builder()
            .putString(BackgroundTaskKeys.TASK_ID, inputData.getString(BackgroundTaskKeys.TASK_ID))
            .putString(BackgroundTaskKeys.TASK_KIND, inputData.getString(BackgroundTaskKeys.TASK_KIND))
            .build()
        return Result.success(output)
    }
}
