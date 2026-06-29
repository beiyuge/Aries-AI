package com.ai.phoneagent.platform.android.nativeruntime

import android.content.Context
import com.ai.phoneagent.core.capability.CapabilityError
import com.ai.phoneagent.core.capability.CapabilityHealth
import com.ai.phoneagent.core.capability.CapabilityId
import com.ai.phoneagent.core.capability.CapabilityIds
import com.ai.phoneagent.core.capability.CapabilityRequirement
import com.ai.phoneagent.core.capability.CapabilitySelfTest
import com.ai.phoneagent.core.capability.CapabilityResult
import com.ai.phoneagent.core.capability.SpeechRecognitionCapability
import com.ai.phoneagent.core.capability.SpeechRecognitionEvent
import com.ai.phoneagent.core.capability.SpeechRecognitionRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class AndroidSpeechRecognitionCapability(
    private val permissionStatus: MicrophonePermissionStatus,
    private val availability: SpeechRecognitionAvailability,
    private val backend: SpeechRecognitionBackend,
) : SpeechRecognitionCapability, CapabilitySelfTest {
    override val id: CapabilityId = CapabilityIds.SpeechRecognition
    override val health = MutableStateFlow(readHealth())

    constructor(context: Context) : this(
        permissionStatus = AndroidMicrophonePermissionStatus(context.applicationContext),
        availability = AndroidSpeechRecognitionAvailability(context.applicationContext),
        backend = AndroidSpeechRecognitionBackend(context.applicationContext),
    )

    override fun recognize(request: SpeechRecognitionRequest): Flow<SpeechRecognitionEvent> = backend.recognize(request)

    override fun runSelfTest(): CapabilityResult<String> {
        val currentHealth = readHealth()
        health.value = currentHealth
        return if (currentHealth.available) {
            CapabilityResult.success("${id.value}: ${currentHealth.state.name} backend=${currentHealth.diagnostics["backend"]}")
        } else {
            CapabilityResult.failure(
                currentHealth.lastError ?: CapabilityError(
                    code = "speech_recognition.permission_required",
                    message = "Speech recognition is not ready.",
                    recoverable = true,
                ),
            )
        }
    }

    private fun readHealth(): CapabilityHealth {
        val diagnostics = backend.diagnostics
        if (!availability.isAvailable()) {
            return CapabilityHealth.unavailable(
                id = id,
                error = CapabilityError(
                    code = "speech_recognition.unavailable",
                    message = "No Android speech recognizer is available on this device.",
                    recoverable = false,
                ),
                diagnostics = diagnostics,
            )
        }
        if (!permissionStatus.hasRecordAudioPermission()) {
            return CapabilityHealth.permissionRequired(
                id = id,
                missingRequirements = listOf(microphoneRequirement()),
                diagnostics = diagnostics,
            )
        }
        return CapabilityHealth.ready(id, diagnostics)
    }

    private fun microphoneRequirement(): CapabilityRequirement = CapabilityRequirement(
        id = "microphone",
        title = "Microphone",
        description = "Allow speech recognition and voice input.",
        action = com.ai.phoneagent.core.capability.CapabilityAction.OpenSettings("android.settings.APPLICATION_DETAILS_SETTINGS"),
    )
}
