package com.ai.phoneagent.core.engine

import android.content.Context
import com.ai.phoneagent.core.config.AgentConfiguration

enum class EngineMode {
    LEGACY,
    SKILL,
}

data class EngineSelection(
    val mode: EngineMode,
    val engine: AgentEngine,
)

object AgentEngineFactory {
    private const val PREFS_NAME = "app_prefs"
    private const val KEY_AUTOMATION_ENGINE_MODE = "automation_engine_mode"
    private const val MODE_LEGACY = "legacy"
    private const val MODE_SKILL = "skill"

    fun create(context: Context, config: AgentConfiguration): EngineSelection {
        val mode = resolveMode(context)
        val appContext = context.applicationContext
        val engine =
            when (mode) {
                EngineMode.LEGACY -> LegacyAgentEngine(appContext, config)
                EngineMode.SKILL -> SkillAgentEngine(appContext, config)
            }
        return EngineSelection(mode = mode, engine = engine)
    }

    fun resolveMode(context: Context): EngineMode {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val rawMode = prefs.getString(KEY_AUTOMATION_ENGINE_MODE, MODE_SKILL)?.trim()?.lowercase()
        return when (rawMode) {
            MODE_LEGACY -> EngineMode.LEGACY
            MODE_SKILL -> EngineMode.SKILL
            else -> EngineMode.SKILL
        }
    }
}

