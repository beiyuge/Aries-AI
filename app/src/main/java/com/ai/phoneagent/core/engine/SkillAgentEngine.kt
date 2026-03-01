package com.ai.phoneagent.core.engine

import android.content.Context
import com.ai.phoneagent.UiAutomationAgent
import com.ai.phoneagent.core.config.AgentConfiguration
import com.ai.phoneagent.core.skill.SkillCallRegistry
import com.ai.phoneagent.core.skill.SkillRegistry
import com.ai.phoneagent.core.skill.SkillResolver
import com.ai.phoneagent.core.skill.SkillRuntimeContext

class SkillAgentEngine(
    context: Context,
    config: AgentConfiguration,
) : AgentEngine {
    private val appContext = context.applicationContext
    private val delegate = UiAutomationAgent(appContext, config)
    private val skillRegistry = SkillRegistry(appContext)
    private val skillResolver = SkillResolver()

    override suspend fun run(request: AgentExecutionRequest): AgentExecutionResult {
        val enabledSkills = skillRegistry.loadEnabledSkills()
        val runtimeContextBase =
            enabledSkills
                .takeIf { it.isNotEmpty() }
                ?.let { skills -> skillResolver.resolve(request.task, skills) }
                ?.let { SkillRuntimeContext.from(it) }

        val callableSkillPrompt =
            SkillCallRegistry.buildPromptBlock(
                SkillCallRegistry.getDescriptors(enabledSkills.map { it.id })
            )
        val runtimeContext =
            runtimeContextBase?.let { base ->
                if (callableSkillPrompt.isBlank()) {
                    base
                } else {
                    base.copy(
                        promptPatch =
                            buildString {
                                if (base.promptPatch.isNotBlank()) {
                                    append(base.promptPatch.trim())
                                    append("\n\n")
                                }
                                append(callableSkillPrompt)
                            }
                    )
                }
            }

        if (enabledSkills.isEmpty()) {
            request.onLog("Skill engine: no skills loaded, fallback to default flow.")
        } else if (runtimeContext == null) {
            request.onLog("Skill engine: no skill matched, fallback to default flow.")
        } else {
            request.onLog("Skill engine: selected ${runtimeContext.skillId} v${runtimeContext.skillVersion}")
            if (runtimeContext.allowedActions.isNotEmpty()) {
                request.onLog(
                    "Skill engine: allowed actions ${runtimeContext.allowedActions.joinToString(", ")}"
                )
            }
            val availableDescriptors = SkillCallRegistry.getDescriptors(enabledSkills.map { it.id })
            if (availableDescriptors.isNotEmpty()) {
                request.onLog(
                    "Skill engine: callable skills ${availableDescriptors.joinToString(", ") { it.id }}"
                )
            }
        }

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
                runtimeContext = runtimeContext,
            )

        return AgentExecutionResult(
            success = result.success,
            message = result.message,
            steps = result.steps,
        )
    }

}
