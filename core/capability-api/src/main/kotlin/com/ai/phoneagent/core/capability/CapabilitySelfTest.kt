package com.ai.phoneagent.core.capability

interface CapabilitySelfTest {
    fun runSelfTest(): CapabilityResult<String>
}
