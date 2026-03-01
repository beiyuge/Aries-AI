package com.ai.phoneagent.core.skill

import android.content.Context
import android.os.Build
import com.ai.phoneagent.PhoneAgentAccessibilityService

data class SkillCallDescriptor(
    val id: String,
    val description: String,
    val usage: String,
    val returns: String,
)

data class SkillCallResult(
    val success: Boolean,
    val output: String,
    val error: String = "",
)

object SkillCallRegistry {
    private val descriptors =
        mapOf(
            "test_device_info" to
                SkillCallDescriptor(
                    id = "test_device_info",
                    description = "Get current device/app/runtime information for verification.",
                    usage = """do(action="call_skill", skill="test_device_info", desc="Get device info")""",
                    returns =
                        "device_info with brand/model/android version/screen metrics/current app/package name",
                )
        )

    fun getDescriptor(skillId: String): SkillCallDescriptor? =
        descriptors[normalize(skillId)]

    fun getDescriptors(skillIds: Collection<String>): List<SkillCallDescriptor> =
        skillIds
            .asSequence()
            .mapNotNull { getDescriptor(it) }
            .distinctBy { it.id }
            .toList()

    fun buildPromptBlock(available: List<SkillCallDescriptor>): String {
        if (available.isEmpty()) return ""
        val section =
            available.joinToString("\n") { desc ->
                "- ${desc.id}: ${desc.description}\n  usage: ${desc.usage}\n  returns: ${desc.returns}"
            }
        return """
# Callable Skills
You may call the following skills when useful:
$section

Skill call format:
<answer>
	do(action="call_skill", skill="<skill_id>", desc="why you call it")
</answer>

After the skill is executed, you will receive:
SKILL_OUTPUT:<skill_id>
<output>

Use the output to continue planning or finish.
""".trimIndent()
    }

    fun execute(
        skillId: String?,
        context: Context,
        service: PhoneAgentAccessibilityService?,
    ): SkillCallResult {
        val normalizedId = normalize(skillId)
        return when (normalizedId) {
            "test_device_info" -> SkillCallResult(success = true, output = buildDeviceInfo(context, service))
            else ->
                SkillCallResult(
                    success = false,
                    output = "",
                    error = "Unsupported skill: ${skillId.orEmpty().ifBlank { "unknown" }}",
                )
        }
    }

    private fun buildDeviceInfo(context: Context, service: PhoneAgentAccessibilityService?): String {
        val appContext = context.applicationContext
        val metrics = (service?.resources ?: appContext.resources).displayMetrics
        val currentApp = service?.currentAppPackage().orEmpty().ifBlank { "unknown" }
        return listOf(
            "device_info:",
            "brand=${Build.BRAND}",
            "manufacturer=${Build.MANUFACTURER}",
            "model=${Build.MODEL}",
            "device=${Build.DEVICE}",
            "product=${Build.PRODUCT}",
            "android_release=${Build.VERSION.RELEASE}",
            "android_sdk=${Build.VERSION.SDK_INT}",
            "screen_width_px=${metrics.widthPixels}",
            "screen_height_px=${metrics.heightPixels}",
            "density_dpi=${metrics.densityDpi}",
            "current_app=$currentApp",
            "package_name=${appContext.packageName}",
        ).joinToString("\n")
    }

    private fun normalize(raw: String?): String =
        raw
            ?.trim()
            ?.trim('"', '\'', ' ')
            ?.lowercase()
            .orEmpty()
}

