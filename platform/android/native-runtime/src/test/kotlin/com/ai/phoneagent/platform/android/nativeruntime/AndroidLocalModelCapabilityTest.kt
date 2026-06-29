package com.ai.phoneagent.platform.android.nativeruntime

import com.ai.phoneagent.core.capability.CapabilityState
import com.ai.phoneagent.core.capability.LocalGenerateEvent
import com.ai.phoneagent.core.capability.LocalGenerateRequest
import com.ai.phoneagent.core.capability.LocalModelLoadRequest
import java.io.File
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AndroidLocalModelCapabilityTest {
    @Test
    fun `reports ready wrapper health`() {
        val capability = AndroidLocalModelCapability(FakeLocalModelBackend())

        assertTrue(capability.health.value.available)
        assertEquals(CapabilityState.Ready, capability.health.value.state)
        assertEquals("fake-model", capability.health.value.diagnostics["backend"])
    }

    @Test
    fun `file backed backend loads generates and unloads model`() = runBlocking {
        val modelFile = File.createTempFile("aries-re0-model", ".bin").apply {
            writeText("model")
            deleteOnExit()
        }
        val capability = AndroidLocalModelCapability(FileBackedLocalModelBackend())

        val load = capability.load(LocalModelLoadRequest(modelId = "m1", path = modelFile.absolutePath))
        val generated = capability.generate(LocalGenerateRequest(modelId = "m1", prompt = "hello")).toList()
        val unload = capability.unload("m1")
        val afterUnload = capability.generate(LocalGenerateRequest(modelId = "m1", prompt = "hello")).toList()

        assertTrue(load.isSuccess)
        assertEquals(listOf(LocalGenerateEvent.Token("[${modelFile.name}] hello"), LocalGenerateEvent.Done), generated)
        assertTrue(unload.isSuccess)
        assertTrue(afterUnload.single() is LocalGenerateEvent.Failed)
    }

    @Test
    fun `file backed backend rejects missing model file`() = runBlocking {
        val capability = AndroidLocalModelCapability(FileBackedLocalModelBackend())

        val result = capability.load(LocalModelLoadRequest(modelId = "missing", path = "/tmp/does-not-exist-aries-re0"))

        assertEquals(false, result.isSuccess)
        assertEquals("local_model.file_unreadable", result.errorOrNull()?.code)
    }
}
