package com.ai.phoneagent.core.capability.test

import com.ai.phoneagent.core.capability.Capability
import com.ai.phoneagent.core.capability.CapabilityHealth
import com.ai.phoneagent.core.capability.CapabilityId
import kotlinx.coroutines.flow.MutableStateFlow

class FakeCapability(
    override val id: CapabilityId,
    initialHealth: CapabilityHealth = CapabilityHealth.ready(id),
) : Capability {
    override val health = MutableStateFlow(initialHealth)
}
