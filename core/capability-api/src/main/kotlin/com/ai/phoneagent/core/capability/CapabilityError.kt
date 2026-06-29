package com.ai.phoneagent.core.capability

data class CapabilityError(
    val code: String,
    val message: String,
    val causeClass: String? = null,
    val recoverable: Boolean = true,
    val suggestedAction: String? = null,
)
