package com.ai.phoneagent.platform.android.background

import com.ai.phoneagent.core.capability.BackgroundTaskRequest
import com.ai.phoneagent.core.capability.BackgroundTaskState
import com.ai.phoneagent.core.capability.BackgroundTaskStatus
import com.ai.phoneagent.core.capability.CapabilityIds
import com.ai.phoneagent.core.capability.CapabilityState
import com.ai.phoneagent.core.capability.CapabilityResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AndroidBackgroundTasksCapabilityTest {
    @Test
    fun `reports ready WorkManager backend`() {
        val capability = AndroidBackgroundTasksCapability(FakeScheduler())

        assertEquals(CapabilityIds.BackgroundTasks, capability.id)
        assertTrue(capability.health.value.available)
        assertEquals(CapabilityState.Ready, capability.health.value.state)
        assertEquals("fake", capability.health.value.diagnostics["backend"])
    }

    @Test
    fun `routes enqueue status and cancel through scheduler`() = runBlocking {
        val scheduler = FakeScheduler()
        val capability = AndroidBackgroundTasksCapability(scheduler)
        val request = BackgroundTaskRequest(taskId = "sync-1", kind = "sync")

        val enqueue = capability.enqueue(request)
        val status = capability.status("sync-1")
        val cancel = capability.cancel("sync-1")

        assertTrue(enqueue.isSuccess)
        assertEquals(BackgroundTaskState.Enqueued, enqueue.getOrNull()?.state)
        assertEquals(BackgroundTaskState.Enqueued, status.getOrNull()?.state)
        assertTrue(cancel.isSuccess)
        assertEquals(listOf("enqueue:sync-1", "status:sync-1", "cancel:sync-1"), scheduler.calls)
    }

    private class FakeScheduler : AndroidBackgroundTaskScheduler {
        val calls = mutableListOf<String>()
        override val diagnostics: Map<String, String> = mapOf(
            "platform" to "android",
            "backend" to "fake",
        )

        override suspend fun enqueue(request: BackgroundTaskRequest): CapabilityResult<BackgroundTaskStatus> {
            calls += "enqueue:${request.taskId}"
            return CapabilityResult.success(
                BackgroundTaskStatus(
                    taskId = request.taskId,
                    kind = request.kind,
                    state = BackgroundTaskState.Enqueued,
                ),
            )
        }

        override suspend fun status(taskId: String): CapabilityResult<BackgroundTaskStatus> {
            calls += "status:$taskId"
            return CapabilityResult.success(
                BackgroundTaskStatus(
                    taskId = taskId,
                    kind = "sync",
                    state = BackgroundTaskState.Enqueued,
                ),
            )
        }

        override suspend fun cancel(taskId: String): CapabilityResult<Unit> {
            calls += "cancel:$taskId"
            return CapabilityResult.success(Unit)
        }
    }
}
