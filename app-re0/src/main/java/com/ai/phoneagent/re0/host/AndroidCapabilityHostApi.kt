package com.ai.phoneagent.re0.host

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import com.ai.phoneagent.core.capability.CapabilityAction
import com.ai.phoneagent.core.capability.CapabilityHealth
import com.ai.phoneagent.core.capability.CapabilityId
import com.ai.phoneagent.platform.android.capability.AndroidCapabilityRegistry
import com.ai.phoneagent.re0.generated.CapabilityHealthDto
import com.ai.phoneagent.re0.generated.CapabilityHostApi
import com.ai.phoneagent.re0.generated.FlutterError

class AndroidCapabilityHostApi(
    private val context: Context,
    private val registry: AndroidCapabilityRegistry,
    private val defaultCapabilityId: CapabilityId,
) : CapabilityHostApi {
    override fun listCapabilities(): List<String> = registry.list().map { capability ->
        capability.id.value
    }

    override fun getCapabilityHealth(id: String): CapabilityHealthDto =
        healthFor(id).toDto()

    override fun runCapabilitySelfTest(id: String): String {
        val health = healthFor(id)
        return if (health.available) {
            "${health.id.value}: ${health.state.name}"
        } else {
            val missing = health.missingRequirements.joinToString { it.id }
            "${health.id.value}: ${health.state.name}" +
                if (missing.isBlank()) "" else " missing [$missing]"
        }
    }

    override fun openCapabilitySettings(id: String) {
        val targetHealth = healthFor(id)
        val action = targetHealth.missingRequirements
            .firstOrNull()
            ?.action
            ?: if (targetHealth.id == defaultCapabilityId) CapabilityAction.OpenSettings(Settings.ACTION_APPLICATION_DETAILS_SETTINGS) else CapabilityAction.None

        when (action) {
            is CapabilityAction.OpenSettings -> openSettings(action.intentAction)
            is CapabilityAction.OpenUri -> openUri(action.uri)
            CapabilityAction.None -> Unit
        }
    }

    private fun healthFor(id: String): CapabilityHealth {
        val capabilityId = CapabilityId(id)
        return registry.get(capabilityId)?.health?.value
            ?: throw FlutterError(
                code = "capability_not_found",
                message = "Capability '$id' is not registered.",
            )
    }

    private fun CapabilityHealth.toDto(): CapabilityHealthDto = CapabilityHealthDto(
        id = id.value,
        available = available,
        state = state.name,
        missingRequirements = missingRequirements.map { requirement ->
            "${requirement.id}: ${requirement.title}"
        },
        lastErrorCode = lastError?.code,
        lastErrorMessage = lastError?.message,
    )

    private fun openSettings(action: String) {
        val intent = Intent(action).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            when (action) {
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS -> {
                    data = Uri.parse("package:${context.packageName}")
                }
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION -> {
                    data = Uri.parse("package:${context.packageName}")
                }
                Settings.ACTION_APP_NOTIFICATION_SETTINGS -> {
                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                }
            }
        }
        startIntent(intent)
    }

    private fun openUri(uri: String) {
        startIntent(
            Intent(Intent.ACTION_VIEW, Uri.parse(uri)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            },
        )
    }

    private fun startIntent(intent: Intent) {
        try {
            context.startActivity(intent)
        } catch (error: ActivityNotFoundException) {
            throw FlutterError(
                code = "settings_not_found",
                message = "No Android settings screen can handle ${intent.action}.",
                details = error.message,
            )
        } catch (error: SecurityException) {
            throw FlutterError(
                code = "settings_blocked",
                message = "Android blocked opening ${intent.action}.",
                details = error.message,
            )
        }
    }
}
