package com.ai.phoneagent.core.skill

class SkillResolver {
    fun resolve(task: String, skills: List<SkillDefinition>): SkillDefinition? {
        if (skills.isEmpty()) return null
        val normalizedTask = task.trim().lowercase()

        val scored =
            skills
                .asSequence()
                .map { skill ->
                    val triggerScore =
                        skill.triggers.maxOfOrNull { trigger ->
                            val key = trigger.trim().lowercase()
                            if (key.isNotEmpty() && normalizedTask.contains(key)) key.length else 0
                        } ?: 0
                    ScoredSkill(
                        skill = skill,
                        triggerScore = triggerScore,
                        priority = skill.priority,
                    )
                }
                .toList()

        val matched = scored.filter { it.triggerScore > 0 }
        if (matched.isNotEmpty()) {
            return matched.maxWithOrNull(
                compareBy<ScoredSkill> { it.triggerScore }
                    .thenBy { it.priority }
            )?.skill
        }

        return skills.firstOrNull { it.id.equals("general_mobile_assistant", ignoreCase = true) }
            ?: scored.maxWithOrNull(compareBy<ScoredSkill> { it.priority })?.skill
    }

    private data class ScoredSkill(
        val skill: SkillDefinition,
        val triggerScore: Int,
        val priority: Int,
    )
}

