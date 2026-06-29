package com.ai.phoneagent.platform.android.nativeruntime

import com.ai.phoneagent.core.capability.CapabilityResult
import com.ai.phoneagent.core.capability.LocalGenerateEvent
import com.ai.phoneagent.core.capability.LocalGenerateRequest
import com.ai.phoneagent.core.capability.LocalModelLoadRequest
import kotlinx.coroutines.flow.Flow

interface LocalModelBackend {
    val diagnostics: Map<String, String>
    suspend fun load(request: LocalModelLoadRequest): CapabilityResult<Unit>
    fun generate(request: LocalGenerateRequest): Flow<LocalGenerateEvent>
    suspend fun unload(modelId: String): CapabilityResult<Unit>
}
