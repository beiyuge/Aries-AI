package com.ai.phoneagent.core.skill

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import java.io.File
import java.util.LinkedHashMap

class SkillRegistry(private val context: Context) {
    companion object {
        private const val TAG = "SkillRegistry"
        private const val ASSET_SKILL_DIR = "skills"
        private const val FILE_SKILL_DIR = "skills"
    }

    private val gson = Gson()

    fun loadEnabledSkills(): List<SkillDefinition> {
        val merged = LinkedHashMap<String, SkillDefinition>()
        defaultBuiltInSkills().forEach { skill ->
            merged[skill.id.lowercase()] = skill
        }
        loadSkillsFromAssets().forEach { skill ->
            merged[skill.id.lowercase()] = skill
        }
        loadSkillsFromFiles().forEach { skill ->
            // File-based definitions override assets/built-ins with same id.
            merged[skill.id.lowercase()] = skill
        }
        return merged.values.filter { it.enabled }
    }

    private fun defaultBuiltInSkills(): List<SkillDefinition> {
        val generalSkill =
            SkillDefinition(
                id = "general_mobile_assistant",
                version = "1.0.0",
                title = "General Mobile Assistant",
                description = "General-purpose mobile automation skill.",
                enabled = true,
                priority = 0,
                triggers = emptyList(),
                systemPromptPatch =
                    "Prefer verifiable low-risk actions. Avoid repeated ineffective taps.",
                allowedActions =
                    listOf(
                        "call_skill",
                        "launch",
                        "open_app",
                        "start_app",
                        "tap",
                        "click",
                        "press",
                        "type",
                        "input",
                        "text",
                        "swipe",
                        "scroll",
                        "wait",
                        "sleep",
                        "back",
                        "home",
                        "finish",
                        "take_over",
                        "takeover",
                    ),
            )

        val shoppingSkill =
            SkillDefinition(
                id = "shopping_checkout_cn",
                version = "1.0.0",
                title = "Shopping Checkout CN",
                description = "Shopping and checkout workflow skill.",
                enabled = true,
                priority = 20,
                triggers = listOf("下单", "购物", "购买", "结算", "淘宝", "京东", "拼多多"),
                systemPromptPatch =
                    "For shopping tasks, verify sku/address/delivery first. Always require take_over before final payment.",
                allowedActions =
                    listOf(
                        "call_skill",
                        "launch",
                        "open_app",
                        "start_app",
                        "tap",
                        "click",
                        "press",
                        "type",
                        "input",
                        "text",
                        "swipe",
                        "scroll",
                        "wait",
                        "sleep",
                        "back",
                        "home",
                        "take_over",
                        "takeover",
                        "finish",
                    ),
                maxSteps = 45,
            )

        val testDeviceInfoSkill =
            SkillDefinition(
                id = "test_device_info",
                version = "1.0.0",
                title = "Test Device Info",
                description = "Return current device information for skill wiring verification.",
                enabled = true,
                priority = 1000,
                triggers = listOf("test", "device info", "设备信息测试", "测试设备信息"),
                systemPromptPatch =
                    """
When this skill is selected, do not chat.
Step 1: output do(action="call_skill", skill="test_device_info", desc="Read current device info").
Step 2: after receiving SKILL_OUTPUT:test_device_info, output finish(message="...") with key fields.
                    """.trimIndent(),
                allowedActions = listOf("call_skill", "finish"),
                maxSteps = 6,
            )

        return listOf(testDeviceInfoSkill, generalSkill, shoppingSkill)
    }

    private fun loadSkillsFromAssets(): List<SkillDefinition> {
        val names =
            runCatching { context.assets.list(ASSET_SKILL_DIR) ?: emptyArray() }
                .getOrElse {
                    Log.w(TAG, "List assets/$ASSET_SKILL_DIR failed: ${it.message}")
                    emptyArray()
                }
        return names
            .asSequence()
            .filter { it.endsWith(".json", ignoreCase = true) }
            .mapNotNull { filename ->
                val path = "$ASSET_SKILL_DIR/$filename"
                val raw =
                    runCatching {
                            context.assets.open(path).bufferedReader().use { reader ->
                                reader.readText()
                            }
                        }
                        .getOrElse {
                            Log.w(TAG, "Read asset skill failed: $path, err=${it.message}")
                            return@mapNotNull null
                        }
                parseAndNormalize(raw, source = "asset:$path")
            }
            .toList()
    }

    private fun loadSkillsFromFiles(): List<SkillDefinition> {
        val root = File(context.filesDir, FILE_SKILL_DIR)
        if (!root.exists() || !root.isDirectory) return emptyList()
        val files =
            root.listFiles { file -> file.isFile && file.name.endsWith(".json", ignoreCase = true) }
                ?: return emptyList()
        return files
            .asSequence()
            .mapNotNull { file ->
                val raw =
                    runCatching { file.readText() }
                        .getOrElse {
                            Log.w(TAG, "Read file skill failed: ${file.absolutePath}, err=${it.message}")
                            return@mapNotNull null
                        }
                parseAndNormalize(raw, source = file.absolutePath)
            }
            .toList()
    }

    private fun parseAndNormalize(raw: String, source: String): SkillDefinition? {
        val parsed =
            runCatching { gson.fromJson(raw, SkillDefinition::class.java) }
                .getOrElse {
                    Log.w(TAG, "Parse skill failed: $source, err=${it.message}")
                    return null
                }
        return normalize(parsed)
    }

    private fun normalize(skill: SkillDefinition): SkillDefinition? {
        val cleanId = skill.id.trim()
        if (cleanId.isBlank()) return null
        val cleanVersion = skill.version.trim().ifBlank { "1.0.0" }
        val cleanTitle = skill.title.trim().ifBlank { cleanId }
        val cleanDescription = skill.description.trim()
        val cleanTriggers =
            skill.triggers
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .distinct()
        val cleanPromptPatch = skill.systemPromptPatch.trim()
        val cleanAllowedActions =
            skill.allowedActions
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .distinct()
        val cleanMaxSteps = skill.maxSteps?.takeIf { it > 0 }
        return skill.copy(
            id = cleanId,
            version = cleanVersion,
            title = cleanTitle,
            description = cleanDescription,
            triggers = cleanTriggers,
            systemPromptPatch = cleanPromptPatch,
            allowedActions = cleanAllowedActions,
            maxSteps = cleanMaxSteps,
        )
    }
}
