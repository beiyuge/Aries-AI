package com.ai.phoneagent.core.capability

@JvmInline
value class CapabilityId(val value: String) {
    init {
        require(value.isNotBlank()) { "CapabilityId cannot be blank" }
    }

    override fun toString(): String = value
}
