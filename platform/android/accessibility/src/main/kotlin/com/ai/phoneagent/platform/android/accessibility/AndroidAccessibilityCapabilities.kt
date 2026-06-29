package com.ai.phoneagent.platform.android.accessibility

import android.provider.Settings
import com.ai.phoneagent.core.capability.Capability
import com.ai.phoneagent.core.capability.CapabilityAction
import com.ai.phoneagent.core.capability.CapabilityError
import com.ai.phoneagent.core.capability.CapabilityHealth
import com.ai.phoneagent.core.capability.CapabilityId
import com.ai.phoneagent.core.capability.CapabilityIds
import com.ai.phoneagent.core.capability.CapabilityRequirement
import com.ai.phoneagent.core.capability.CapabilityResult
import com.ai.phoneagent.core.capability.CapabilitySelfTest
import com.ai.phoneagent.core.capability.UiTreeCapability
import com.ai.phoneagent.core.capability.UiTreeDumpRequest
import com.ai.phoneagent.core.capability.UiTreeDumpResult
import kotlinx.coroutines.flow.MutableStateFlow

class AndroidAccessibilityCapability(
    private val status: AccessibilityServiceStatus,
) : Capability, CapabilitySelfTest {
    override val id: CapabilityId = CapabilityIds.Accessibility
    override val health = MutableStateFlow(readHealth())

    override fun runSelfTest(): CapabilityResult<String> {
        refreshHealth()
        val currentHealth = health.value
        return if (currentHealth.available) {
            CapabilityResult.success("${id.value}: accessibility service connected")
        } else {
            CapabilityResult.failure(
                currentHealth.lastError ?: CapabilityError(
                    code = "accessibility.not_ready",
                    message = "Accessibility service is not ready.",
                    recoverable = true,
                ),
            )
        }
    }

    fun refreshHealth() {
        health.value = readHealth()
    }

    private fun readHealth(): CapabilityHealth = accessibilityHealth(CapabilityIds.Accessibility, status)
}

class AndroidUiTreeCapability(
    private val status: AccessibilityServiceStatus,
) : UiTreeCapability, CapabilitySelfTest {
    override val id: CapabilityId = CapabilityIds.UiTree
    override val health = MutableStateFlow(readHealth())

    override suspend fun dump(request: UiTreeDumpRequest): UiTreeDumpResult {
        refreshHealth()
        if (!health.value.available) {
            return UiTreeDumpResult(
                json = "{}",
                nodeCount = 0,
                source = "accessibility",
                error = health.value.lastError,
            )
        }
        return Re0AccessibilityBridge.dumpUiTree(request)
    }

    override fun runSelfTest(): CapabilityResult<String> {
        refreshHealth()
        return if (health.value.available) {
            val result = Re0AccessibilityBridge.dumpUiTree(UiTreeDumpRequest())
            if (result.success) {
                CapabilityResult.success("${id.value}: ${result.nodeCount} nodes visible")
            } else {
                CapabilityResult.failure(result.error ?: uiTreeUnavailableError())
            }
        } else {
            CapabilityResult.failure(health.value.lastError ?: uiTreeUnavailableError())
        }
    }

    fun refreshHealth() {
        health.value = readHealth()
    }

    private fun readHealth(): CapabilityHealth = accessibilityHealth(
        id = CapabilityIds.UiTree,
        status = status,
        backend = "accessibility-ui-tree",
    )

    private fun uiTreeUnavailableError(): CapabilityError = CapabilityError(
        code = "ui_tree.unavailable",
        message = "UI tree is unavailable because Accessibility is not connected.",
        recoverable = true,
    )
}

fun accessibilityHealth(
    id: CapabilityId,
    status: AccessibilityServiceStatus,
    backend: String = "accessibility-service",
): CapabilityHealth {
    val diagnostics = mapOf(
        "platform" to "android",
        "backend" to backend,
    )
    if (!status.isEnabled()) {
        return CapabilityHealth.permissionRequired(
            id = id,
            missingRequirements = listOf(accessibilityRequirement()),
            diagnostics = diagnostics,
        )
    }
    if (!status.isConnected()) {
        return CapabilityHealth.degraded(
            id = id,
            error = CapabilityError(
                code = "accessibility.service_disconnected",
                message = "Accessibility is enabled but the service is not connected yet.",
                recoverable = true,
            ),
            diagnostics = diagnostics,
        )
    }
    return CapabilityHealth.ready(id = id, diagnostics = diagnostics)
}

fun accessibilityRequirement(): CapabilityRequirement = CapabilityRequirement(
    id = "accessibility.service",
    title = "Accessibility Service",
    description = "Enable Aries re0 accessibility automation service.",
    action = CapabilityAction.OpenSettings(Settings.ACTION_ACCESSIBILITY_SETTINGS),
)
