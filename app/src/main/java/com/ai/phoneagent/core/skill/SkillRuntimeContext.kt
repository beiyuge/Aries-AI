package com.ai.phoneagent.core.skill

data class SkillRuntimeContext(
    val skillId: String,
    val skillVersion: String,
    val promptPatch: String,
    val allowedActions: Set<String>,
    val maxStepsOverride: Int?,
) {
    fun applyToSystemPrompt(basePrompt: String): String {
        if (promptPatch.isBlank()) return basePrompt
        return buildString {
            append(basePrompt.trimEnd())
            append("\n\n# Active Skill\n")
            append("skill_id: ")
            append(skillId)
            append("\n")
            append("skill_version: ")
            append(skillVersion)
            append("\n")
            append(promptPatch.trim())
            append("\n")
        }
    }

    fun isActionAllowed(actionName: String?): Boolean {
        if (allowedActions.isEmpty()) return true
        val normalized = normalizeActionName(actionName)
        if (normalized.isEmpty()) return false
        return normalized in allowedActions
    }

    fun resolveMaxSteps(defaultMaxSteps: Int): Int {
        val override = maxStepsOverride ?: return defaultMaxSteps
        return override.coerceIn(1, defaultMaxSteps)
    }

    companion object {
        fun from(skill: SkillDefinition): SkillRuntimeContext {
            val actions =
                skill.allowedActions
                    .map(::normalizeActionName)
                    .filter { it.isNotEmpty() }
                    .toSet()
            return SkillRuntimeContext(
                skillId = skill.id.trim(),
                skillVersion = skill.version.trim().ifBlank { "1.0.0" },
                promptPatch = skill.systemPromptPatch.trim(),
                allowedActions = actions,
                maxStepsOverride = skill.maxSteps?.takeIf { it > 0 },
            )
        }

        private fun normalizeActionName(raw: String?): String {
            return raw
                ?.trim()
                ?.trim('"', '\'', ' ')
                ?.lowercase()
                ?.replace(" ", "_")
                .orEmpty()
        }
    }
}

