package com.ai.phoneagent.platform.android.capability

import com.ai.phoneagent.core.capability.Capability
import com.ai.phoneagent.core.capability.CapabilityHealth
import com.ai.phoneagent.core.capability.CapabilityId
import com.ai.phoneagent.core.capability.CapabilityState
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AndroidCapabilityRegistryTest {
    @Test
    fun `registry returns capabilities in registration order`() {
        val first = FakeCapability("permissions")
        val second = FakeCapability("shizuku.shell")
        val registry = AndroidCapabilityRegistry(listOf(first, second))

        assertEquals(listOf(first, second), registry.list())
    }

    @Test
    fun `registry resolves capability by id`() {
        val capability = FakeCapability("input.injector")
        val registry = AndroidCapabilityRegistry(listOf(capability))

        assertEquals(capability, registry.get(CapabilityId("input.injector")))
        assertNull(registry.get(CapabilityId("missing")))
    }

    @Test
    fun `registry snapshots current health for diagnostics`() {
        val capability = FakeCapability("screen.capture")
        val registry = AndroidCapabilityRegistry(listOf(capability))

        val snapshot = registry.healthSnapshot()

        assertEquals(CapabilityState.Ready, snapshot.single().state)
        assertEquals(CapabilityId("screen.capture"), snapshot.single().id)
    }

    private class FakeCapability(id: String) : Capability {
        override val id: CapabilityId = CapabilityId(id)
        override val health = MutableStateFlow(CapabilityHealth.ready(this.id))
    }
}
