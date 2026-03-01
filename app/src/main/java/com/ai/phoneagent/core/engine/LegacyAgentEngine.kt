package com.ai.phoneagent.core.engine

import android.content.Context
import com.ai.phoneagent.UiAutomationAgent
import com.ai.phoneagent.core.config.AgentConfiguration

class LegacyAgentEngine(
    context: Context,
    config: AgentConfiguration,
) : AgentEngine {

    private val delegate = UiAutomationAgent(context.applicationContext, config)

    override suspend fun run(request: AgentExecutionRequest): AgentExecutionResult {
        val result =
            delegate.run(
                apiKey = request.apiKey,
                baseUrl = request.baseUrl,
                model = request.model,
                useThirdPartyApi = request.useThirdPartyApi,
                task = request.task,
                service = request.service,
                control =
                    object : UiAutomationAgent.Control {
                        override fun isPaused(): Boolean = request.control.isPaused()

                        override suspend fun confirm(message: String): Boolean =
                            request.control.confirm(message)
                    },
                onLog = request.onLog,
            )
        return AgentExecutionResult(
            success = result.success,
            message = result.message,
            steps = result.steps,
        )
    }
}

