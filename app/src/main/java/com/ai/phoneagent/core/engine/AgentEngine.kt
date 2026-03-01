package com.ai.phoneagent.core.engine

import com.ai.phoneagent.PhoneAgentAccessibilityService

data class AgentExecutionRequest(
    val apiKey: String,
    val baseUrl: String,
    val model: String,
    val useThirdPartyApi: Boolean,
    val task: String,
    val service: PhoneAgentAccessibilityService?,
    val control: AgentControl,
    val onLog: (String) -> Unit,
)

data class AgentExecutionResult(
    val success: Boolean,
    val message: String,
    val steps: Int,
)

interface AgentControl {
    fun isPaused(): Boolean
    suspend fun confirm(message: String): Boolean
}

interface AgentEngine {
    suspend fun run(request: AgentExecutionRequest): AgentExecutionResult
}

