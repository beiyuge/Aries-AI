package com.ai.phoneagent.core.capability

enum class CapabilityState {
    Unknown,
    Unavailable,
    PermissionRequired,
    Starting,
    Ready,
    Running,
    Degraded,
    Failed,
    Stopping,
}
