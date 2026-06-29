package com.ai.phoneagent.core.capability.test

import com.ai.phoneagent.core.capability.LocalGenerateEvent
import com.ai.phoneagent.core.capability.LocalGenerateRequest
import com.ai.phoneagent.core.capability.LocalModelLoadRequest
import com.ai.phoneagent.core.capability.SpeechRecognitionEvent
import com.ai.phoneagent.core.capability.SpeechRecognitionRequest
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FakeCapabilityTest {
    @Test
    fun `fake speech capability emits configured events`() = runBlocking {
        val capability = FakeSpeechRecognitionCapability(listOf(SpeechRecognitionEvent.Final("hello")))

        val events = capability.recognize(SpeechRecognitionRequest()).toList()

        assertEquals(listOf(SpeechRecognitionEvent.Final("hello")), events)
    }

    @Test
    fun `fake local model requires load before generate`() = runBlocking {
        val capability = FakeLocalModelCapability()

        val missing = capability.generate(LocalGenerateRequest("m1", "hello")).toList()
        capability.load(LocalModelLoadRequest("m1", "/tmp/model"))
        val generated = capability.generate(LocalGenerateRequest("m1", "hello")).toList()

        assertTrue(missing.first() is LocalGenerateEvent.Failed)
        assertEquals(listOf(LocalGenerateEvent.Token("hello"), LocalGenerateEvent.Done), generated)
    }
}
