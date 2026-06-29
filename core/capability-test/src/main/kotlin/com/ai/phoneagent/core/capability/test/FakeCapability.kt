package com.ai.phoneagent.core.capability.test

import com.ai.phoneagent.core.capability.Capability
import com.ai.phoneagent.core.capability.CapabilityError
import com.ai.phoneagent.core.capability.CapabilityHealth
import com.ai.phoneagent.core.capability.CapabilityId
import com.ai.phoneagent.core.capability.CapabilityIds
import com.ai.phoneagent.core.capability.CapabilityResult
import com.ai.phoneagent.core.capability.LocalGenerateEvent
import com.ai.phoneagent.core.capability.LocalGenerateRequest
import com.ai.phoneagent.core.capability.LocalModelCapability
import com.ai.phoneagent.core.capability.LocalModelLoadRequest
import com.ai.phoneagent.core.capability.SpeechRecognitionCapability
import com.ai.phoneagent.core.capability.SpeechRecognitionEvent
import com.ai.phoneagent.core.capability.SpeechRecognitionRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeCapability(
    override val id: CapabilityId,
    initialHealth: CapabilityHealth = CapabilityHealth.ready(id),
) : Capability {
    override val health = MutableStateFlow(initialHealth)
}

class FakeSpeechRecognitionCapability(
    events: List<SpeechRecognitionEvent> = listOf(SpeechRecognitionEvent.Final("ready")),
) : SpeechRecognitionCapability {
    override val id: CapabilityId = CapabilityIds.SpeechRecognition
    override val health = MutableStateFlow(CapabilityHealth.ready(id, mapOf("backend" to "fake")))
    private val recognitionEvents = events

    override fun recognize(request: SpeechRecognitionRequest): Flow<SpeechRecognitionEvent> = flow {
        recognitionEvents.forEach { emit(it) }
    }
}

class FakeLocalModelCapability : LocalModelCapability {
    override val id: CapabilityId = CapabilityIds.LocalModel
    override val health = MutableStateFlow(CapabilityHealth.ready(id, mapOf("backend" to "fake")))
    private val loadedModelIds = mutableSetOf<String>()

    override suspend fun load(request: LocalModelLoadRequest): CapabilityResult<Unit> {
        loadedModelIds += request.modelId
        return CapabilityResult.success(Unit)
    }

    override fun generate(request: LocalGenerateRequest): Flow<LocalGenerateEvent> = flow {
        if (request.modelId !in loadedModelIds) {
            emit(
                LocalGenerateEvent.Failed(
                    CapabilityError(
                        code = "local_model.not_loaded",
                        message = "Model '${request.modelId}' is not loaded.",
                        recoverable = true,
                    ),
                ),
            )
        } else {
            emit(LocalGenerateEvent.Token(request.prompt))
            emit(LocalGenerateEvent.Done)
        }
    }

    override suspend fun unload(modelId: String): CapabilityResult<Unit> {
        loadedModelIds -= modelId
        return CapabilityResult.success(Unit)
    }
}
