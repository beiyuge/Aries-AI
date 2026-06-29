package com.ai.phoneagent.core.capability

sealed interface CapabilityAction {
    data class OpenSettings(val intentAction: String) : CapabilityAction
    data class OpenUri(val uri: String) : CapabilityAction
    data object None : CapabilityAction
}
