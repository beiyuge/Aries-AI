package com.ai.phoneagent.platform.android.capability

import com.ai.phoneagent.core.capability.Capability
import com.ai.phoneagent.core.capability.CapabilityHealth
import com.ai.phoneagent.core.capability.CapabilityId

class AndroidCapabilityRegistry(
    capabilities: List<Capability>,
) {
    private val orderedCapabilities: List<Capability> = capabilities.toList()
    private val byId: Map<CapabilityId, Capability> = orderedCapabilities.associateBy { it.id }

    fun list(): List<Capability> = orderedCapabilities

    fun get(id: CapabilityId): Capability? = byId[id]

    fun healthSnapshot(): List<CapabilityHealth> = orderedCapabilities.map { it.health.value }
}
