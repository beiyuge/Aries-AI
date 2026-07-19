package com.ai.phoneagent.re0.host

import com.ai.phoneagent.core.capability.test.FakeLocalModelCapability
import com.ai.phoneagent.platform.android.capability.AndroidCapabilityRegistry
import com.ai.phoneagent.re0.generated.FlutterError
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AndroidLocalModelHostApiTest {
    @Test
    fun `loads generates and unloads through the registered capability`() = runBlocking {
        val host = AndroidLocalModelHostApi(
            registry = AndroidCapabilityRegistry(listOf(FakeLocalModelCapability())),
            scope = this,
            workerDispatcher = Dispatchers.Unconfined,
        )

        assertTrue(awaitUnit { callback -> host.loadLocalModel("model", "/tmp/model", callback) }.isSuccess)
        assertEquals(
            "hello",
            awaitString { callback -> host.generateLocalModel("model", "hello", callback) }.getOrThrow(),
        )
        assertTrue(awaitUnit { callback -> host.unloadLocalModel("model", callback) }.isSuccess)
        assertTrue(
            awaitString { callback -> host.generateLocalModel("model", "hello", callback) }.isFailure,
        )
    }

    @Test
    fun `reports a typed error when the capability is absent`() = runBlocking {
        val host = AndroidLocalModelHostApi(
            registry = AndroidCapabilityRegistry(emptyList()),
            scope = this,
            workerDispatcher = Dispatchers.Unconfined,
        )

        val result = awaitString { callback ->
            host.generateLocalModel("model", "hello", callback)
        }

        assertTrue(result.isFailure)
        assertEquals(
            "local_model.not_registered",
            (result.exceptionOrNull() as FlutterError).code,
        )
    }

    private suspend fun awaitUnit(block: ((Result<Unit>) -> Unit) -> Unit): Result<Unit> {
        val result = CompletableDeferred<Result<Unit>>()
        block(result::complete)
        return result.await()
    }

    private suspend fun awaitString(block: ((Result<String>) -> Unit) -> Unit): Result<String> {
        val result = CompletableDeferred<Result<String>>()
        block(result::complete)
        return result.await()
    }
}
