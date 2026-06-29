package com.ai.phoneagent.core.capability

import kotlinx.coroutines.flow.StateFlow

interface Capability {
    val id: CapabilityId
    val health: StateFlow<CapabilityHealth>
}
