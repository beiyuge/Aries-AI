package com.ai.phoneagent.core.capability

enum class CapabilityState {
    Unknown,
    Unsupported,
    Unavailable,
    PermissionRequired,
    Starting,
    Ready,
    Running,
    Degraded,
    Failed,
    Stopping,
}
