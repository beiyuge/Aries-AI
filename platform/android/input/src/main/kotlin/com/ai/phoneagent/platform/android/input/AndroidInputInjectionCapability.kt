package com.ai.phoneagent.platform.android.input

import com.ai.phoneagent.core.capability.CapabilityError
import com.ai.phoneagent.core.capability.CapabilityHealth
import com.ai.phoneagent.core.capability.CapabilityId
import com.ai.phoneagent.core.capability.CapabilityIds
import com.ai.phoneagent.core.capability.CapabilityResult
import com.ai.phoneagent.core.capability.CapabilitySelfTest
import com.ai.phoneagent.core.capability.InputInjectionCapability
import com.ai.phoneagent.core.capability.InputResult
import com.ai.phoneagent.core.capability.KeyRequest
import com.ai.phoneagent.core.capability.SwipeRequest
import com.ai.phoneagent.core.capability.TapRequest
import com.ai.phoneagent.core.capability.TypeTextRequest
import com.ai.phoneagent.platform.android.accessibility.AccessibilityServiceStatus
import com.ai.phoneagent.platform.android.accessibility.Re0AccessibilityBridge
import com.ai.phoneagent.platform.android.accessibility.accessibilityHealth
import kotlinx.coroutines.flow.MutableStateFlow

class AndroidInputInjectionCapability(
    private val status: AccessibilityServiceStatus,
) : InputInjectionCapability, CapabilitySelfTest {
    override val id: CapabilityId = CapabilityIds.InputInjection
    override val health = MutableStateFlow(readHealth())

    override suspend fun tap(request: TapRequest): InputResult {
        refreshHealth()
        if (!health.value.available) {
            return InputResult(
                backend = "accessibility",
                durationMs = 0,
                error = health.value.lastError,
            )
        }
        return Re0AccessibilityBridge.tap(request)
    }

    override suspend fun swipe(request: SwipeRequest): InputResult = unsupportedAction("input.swipe_not_implemented")

    override suspend fun typeText(request: TypeTextRequest): InputResult = unsupportedAction("input.type_text_not_implemented")

    override suspend fun key(request: KeyRequest): InputResult = unsupportedAction("input.key_not_implemented")

    override fun runSelfTest(): CapabilityResult<String> {
        refreshHealth()
        return if (health.value.available) {
            CapabilityResult.success("${id.value}: accessibility tap backend ready")
        } else {
            CapabilityResult.failure(health.value.lastError ?: notReadyError())
        }
    }

    fun refreshHealth() {
        health.value = readHealth()
    }

    private fun readHealth(): CapabilityHealth = accessibilityHealth(
        id = id,
        status = status,
        backend = "accessibility-input",
    )

    private fun unsupportedAction(code: String): InputResult = InputResult(
        backend = "accessibility",
        durationMs = 0,
        error = CapabilityError(
            code = code,
            message = "This input action is not implemented in the Android accessibility backend yet.",
            recoverable = true,
        ),
    )

    private fun notReadyError(): CapabilityError = CapabilityError(
        code = "input.backend_not_ready",
        message = "Input injection backend is not ready.",
        recoverable = true,
    )
}
