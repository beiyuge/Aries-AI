package com.ai.phoneagent.platform.android.nativeruntime

import com.ai.phoneagent.core.capability.CapabilityHealth
import com.ai.phoneagent.core.capability.CapabilityId
import com.ai.phoneagent.core.capability.CapabilityIds
import com.ai.phoneagent.core.capability.CapabilityResult
import com.ai.phoneagent.core.capability.CapabilitySelfTest
import com.ai.phoneagent.core.capability.LocalGenerateEvent
import com.ai.phoneagent.core.capability.LocalGenerateRequest
import com.ai.phoneagent.core.capability.LocalModelCapability
import com.ai.phoneagent.core.capability.LocalModelLoadRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking

class AndroidLocalModelCapability(
    private val backend: LocalModelBackend = FileBackedLocalModelBackend(),
) : LocalModelCapability, CapabilitySelfTest {
    override val id: CapabilityId = CapabilityIds.LocalModel
    override val health = MutableStateFlow(CapabilityHealth.ready(id, backend.diagnostics))

    override suspend fun load(request: LocalModelLoadRequest): CapabilityResult<Unit> = backend.load(request)

    override fun generate(request: LocalGenerateRequest): Flow<LocalGenerateEvent> = backend.generate(request)

    override suspend fun unload(modelId: String): CapabilityResult<Unit> = backend.unload(modelId)

    override fun runSelfTest(): CapabilityResult<String> = runBlocking {
        val event = backend.generate(LocalGenerateRequest(modelId = SELF_TEST_MODEL_ID, prompt = "health")).first()
        when (event) {
            is LocalGenerateEvent.Failed -> CapabilityResult.success("${id.value}: wrapper-ready ${event.error.code}")
            else -> CapabilityResult.success("${id.value}: ${health.value.state.name}")
        }
    }

    private companion object {
        const val SELF_TEST_MODEL_ID = "__self_test__"
    }
}
