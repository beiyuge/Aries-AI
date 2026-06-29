package com.ai.phoneagent.platform.android.nativeruntime

import com.ai.phoneagent.core.capability.CapabilityError
import com.ai.phoneagent.core.capability.CapabilityResult
import com.ai.phoneagent.core.capability.LocalGenerateEvent
import com.ai.phoneagent.core.capability.LocalGenerateRequest
import com.ai.phoneagent.core.capability.LocalModelLoadRequest
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class FileBackedLocalModelBackend : LocalModelBackend {
    private val loadedModels = ConcurrentHashMap<String, File>()

    override val diagnostics: Map<String, String> = mapOf(
        "platform" to "android",
        "backend" to "file-backed-local-model",
    )

    override suspend fun load(request: LocalModelLoadRequest): CapabilityResult<Unit> {
        if (request.modelId.isBlank()) {
            return CapabilityResult.failure(invalidRequest("Model id is required."))
        }
        val modelFile = File(request.path)
        if (!modelFile.exists() || !modelFile.canRead()) {
            return CapabilityResult.failure(
                CapabilityError(
                    code = "local_model.file_unreadable",
                    message = "Model file '${request.path}' does not exist or cannot be read.",
                    recoverable = true,
                ),
            )
        }
        loadedModels[request.modelId] = modelFile
        return CapabilityResult.success(Unit)
    }

    override fun generate(request: LocalGenerateRequest): Flow<LocalGenerateEvent> = flow {
        if (request.prompt.isBlank()) {
            emit(LocalGenerateEvent.Failed(invalidRequest("Prompt is required.")))
            return@flow
        }
        val modelFile = loadedModels[request.modelId]
        if (modelFile == null) {
            emit(
                LocalGenerateEvent.Failed(
                    CapabilityError(
                        code = "local_model.not_loaded",
                        message = "Model '${request.modelId}' is not loaded.",
                        recoverable = true,
                    ),
                ),
            )
            return@flow
        }
        emit(LocalGenerateEvent.Token("[${modelFile.name}] ${request.prompt}"))
        emit(LocalGenerateEvent.Done)
    }

    override suspend fun unload(modelId: String): CapabilityResult<Unit> {
        loadedModels.remove(modelId)
        return CapabilityResult.success(Unit)
    }

    private fun invalidRequest(message: String): CapabilityError = CapabilityError(
        code = "local_model.invalid_request",
        message = message,
        recoverable = false,
    )
}
