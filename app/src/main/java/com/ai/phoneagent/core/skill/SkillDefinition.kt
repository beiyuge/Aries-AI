package com.ai.phoneagent.core.skill

data class SkillDefinition(
    val id: String = "",
    val version: String = "1.0.0",
    val title: String = "",
    val description: String = "",
    val enabled: Boolean = true,
    val priority: Int = 0,
    val triggers: List<String> = emptyList(),
    val systemPromptPatch: String = "",
    val allowedActions: List<String> = emptyList(),
    val maxSteps: Int? = null,
)

