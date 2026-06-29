package com.ai.phoneagent.platform.android.nativeruntime

import com.ai.phoneagent.core.capability.CapabilityState
import com.ai.phoneagent.core.capability.SpeechRecognitionEvent
import com.ai.phoneagent.core.capability.SpeechRecognitionRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AndroidSpeechRecognitionCapabilityTest {
    @Test
    fun `reports permission required when microphone is missing`() {
        val capability = AndroidSpeechRecognitionCapability(
            permissionStatus = FakeMicrophonePermissionStatus(hasPermission = false),
            availability = FakeSpeechRecognitionAvailability(available = true),
            backend = FakeSpeechBackend(),
        )

        val health = capability.health.value

        assertFalse(health.available)
        assertEquals(CapabilityState.PermissionRequired, health.state)
        assertEquals("microphone", health.missingRequirements.single().id)
    }

    @Test
    fun `reports unavailable when Android recognizer is missing`() {
        val capability = AndroidSpeechRecognitionCapability(
            permissionStatus = FakeMicrophonePermissionStatus(hasPermission = true),
            availability = FakeSpeechRecognitionAvailability(available = false),
            backend = FakeSpeechBackend(),
        )

        val health = capability.health.value

        assertFalse(health.available)
        assertEquals(CapabilityState.Unavailable, health.state)
        assertEquals("speech_recognition.unavailable", health.lastError?.code)
    }

    @Test
    fun `routes recognition events through backend`() = runBlocking {
        val capability = AndroidSpeechRecognitionCapability(
            permissionStatus = FakeMicrophonePermissionStatus(hasPermission = true),
            availability = FakeSpeechRecognitionAvailability(available = true),
            backend = FakeSpeechBackend(listOf(SpeechRecognitionEvent.Partial("hel"), SpeechRecognitionEvent.Final("hello"))),
        )

        val events = capability.recognize(SpeechRecognitionRequest(locale = "en-US")).toList()

        assertTrue(capability.health.value.available)
        assertEquals(listOf(SpeechRecognitionEvent.Partial("hel"), SpeechRecognitionEvent.Final("hello")), events)
    }

    private class FakeMicrophonePermissionStatus(
        private val hasPermission: Boolean,
    ) : MicrophonePermissionStatus {
        override fun hasRecordAudioPermission(): Boolean = hasPermission
    }

    private class FakeSpeechRecognitionAvailability(
        private val available: Boolean,
    ) : SpeechRecognitionAvailability {
        override fun isAvailable(): Boolean = available
    }

    private class FakeSpeechBackend(
        private val events: List<SpeechRecognitionEvent> = listOf(SpeechRecognitionEvent.Final("ready")),
    ) : SpeechRecognitionBackend {
        override val diagnostics: Map<String, String> = mapOf("backend" to "fake-speech")

        override fun recognize(request: SpeechRecognitionRequest): Flow<SpeechRecognitionEvent> = flowOf(*events.toTypedArray())
    }
}
