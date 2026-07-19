package com.ai.phoneagent.re0.host

import com.ai.phoneagent.core.capability.CapabilityError
import com.ai.phoneagent.core.capability.CapabilityIds
import com.ai.phoneagent.core.capability.LocalGenerateEvent
import com.ai.phoneagent.core.capability.LocalGenerateRequest
import com.ai.phoneagent.core.capability.LocalModelCapability
import com.ai.phoneagent.core.capability.LocalModelLoadRequest
import com.ai.phoneagent.platform.android.capability.AndroidCapabilityRegistry
import com.ai.phoneagent.re0.generated.FlutterError
import com.ai.phoneagent.re0.generated.LocalModelHostApi
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AndroidLocalModelHostApi(
    private val registry: AndroidCapabilityRegistry,
    private val scope: CoroutineScope,
    private val workerDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : LocalModelHostApi {
    override fun loadLocalModel(
        modelId: String,
        path: String,
        callback: (Result<Unit>) -> Unit,
    ) {
        val capability = capabilityOrReply(callback) ?: return
        scope.launch {
            val result = withContext(workerDispatcher) {
                capability.load(LocalModelLoadRequest(modelId = modelId, path = path))
            }
            callback(result.toPigeonResult())
        }
    }

    override fun generateLocalModel(
        modelId: String,
        prompt: String,
        callback: (Result<String>) -> Unit,
    ) {
        val capability = capabilityOrReply(callback) ?: return
        scope.launch {
            val generated = withContext(workerDispatcher) {
                collectGeneration(capability, modelId, prompt)
            }
            callback(generated)
        }
    }

    override fun unloadLocalModel(
        modelId: String,
        callback: (Result<Unit>) -> Unit,
    ) {
        val capability = capabilityOrReply(callback) ?: return
        scope.launch {
            val result = withContext(workerDispatcher) {
                capability.unload(modelId)
            }
            callback(result.toPigeonResult())
        }
    }

    private suspend fun collectGeneration(
        capability: LocalModelCapability,
        modelId: String,
        prompt: String,
    ): Result<String> {
        val output = StringBuilder()
        var completed = false
        var failure: CapabilityError? = null
        capability.generate(LocalGenerateRequest(modelId = modelId, prompt = prompt)).collect { event ->
            if (failure != null || completed) return@collect
            when (event) {
                is LocalGenerateEvent.Token -> output.append(event.text)
                LocalGenerateEvent.Done -> completed = true
                is LocalGenerateEvent.Failed -> failure = event.error
            }
        }
        failure?.let { return Result.failure(it.toFlutterError()) }
        if (!completed) {
            return Result.failure(
                FlutterError(
                    code = "local_model.incomplete",
                    message = "Local model generation ended without a completion event.",
                    details = mapOf("recoverable" to true),
                ),
            )
        }
        return Result.success(output.toString())
    }

    private fun <T> capabilityOrReply(callback: (Result<T>) -> Unit): LocalModelCapability? {
        val capability = registry.get(CapabilityIds.LocalModel) as? LocalModelCapability
        if (capability == null) {
            callback(
                Result.failure(
                    FlutterError(
                        code = "local_model.not_registered",
                        message = "The local model capability is not registered on this platform.",
                        details = mapOf("recoverable" to false),
                    ),
                ),
            )
        }
        return capability
    }

    private fun com.ai.phoneagent.core.capability.CapabilityResult<Unit>.toPigeonResult(): Result<Unit> {
        val error = errorOrNull()
        return if (error == null) {
            Result.success(Unit)
        } else {
            Result.failure(error.toFlutterError())
        }
    }

    private fun CapabilityError.toFlutterError(): FlutterError = FlutterError(
        code = code,
        message = message,
        details = mapOf(
            "recoverable" to recoverable,
            "suggestedAction" to suggestedAction,
        ),
    )
}
