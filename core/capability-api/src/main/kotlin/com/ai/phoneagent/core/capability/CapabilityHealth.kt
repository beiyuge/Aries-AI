package com.ai.phoneagent.core.capability

data class CapabilityHealth(
    val id: CapabilityId,
    val available: Boolean,
    val state: CapabilityState,
    val missingRequirements: List<CapabilityRequirement> = emptyList(),
    val lastError: CapabilityError? = null,
    val diagnostics: Map<String, String> = emptyMap(),
) {
    companion object {
        fun ready(
            id: CapabilityId,
            diagnostics: Map<String, String> = emptyMap(),
        ): CapabilityHealth = CapabilityHealth(
            id = id,
            available = true,
            state = CapabilityState.Ready,
            diagnostics = diagnostics,
        )

        fun permissionRequired(
            id: CapabilityId,
            missingRequirements: List<CapabilityRequirement>,
            diagnostics: Map<String, String> = emptyMap(),
        ): CapabilityHealth = CapabilityHealth(
            id = id,
            available = false,
            state = CapabilityState.PermissionRequired,
            missingRequirements = missingRequirements,
            diagnostics = diagnostics,
        )

        fun failed(
            id: CapabilityId,
            error: CapabilityError,
            diagnostics: Map<String, String> = emptyMap(),
        ): CapabilityHealth = CapabilityHealth(
            id = id,
            available = false,
            state = CapabilityState.Failed,
            lastError = error,
            diagnostics = diagnostics,
        )
    }
}
