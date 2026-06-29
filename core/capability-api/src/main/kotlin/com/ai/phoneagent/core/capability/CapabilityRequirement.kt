package com.ai.phoneagent.core.capability

data class CapabilityRequirement(
    val id: String,
    val title: String,
    val description: String,
    val action: CapabilityAction = CapabilityAction.None,
)
