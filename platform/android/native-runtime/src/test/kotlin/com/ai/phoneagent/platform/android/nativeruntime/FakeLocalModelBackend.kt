package com.ai.phoneagent.platform.android.nativeruntime

import com.ai.phoneagent.core.capability.CapabilityResult
import com.ai.phoneagent.core.capability.LocalGenerateEvent
import com.ai.phoneagent.core.capability.LocalGenerateRequest
import com.ai.phoneagent.core.capability.LocalModelLoadRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class FakeLocalModelBackend : LocalModelBackend {
    override val diagnostics: Map<String, String> = mapOf("backend" to "fake-model")

    override suspend fun load(request: LocalModelLoadRequest): CapabilityResult<Unit> = CapabilityResult.success(Unit)

    override fun generate(request: LocalGenerateRequest): Flow<LocalGenerateEvent> =
        flowOf(LocalGenerateEvent.Token(request.prompt), LocalGenerateEvent.Done)

    override suspend fun unload(modelId: String): CapabilityResult<Unit> = CapabilityResult.success(Unit)
}
