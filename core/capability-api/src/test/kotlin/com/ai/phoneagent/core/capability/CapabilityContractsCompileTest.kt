package com.ai.phoneagent.core.capability

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Test

class CapabilityContractsCompileTest {
    @Test
    fun `permission capability exposes requirements through typed contract`() {
        val capability = FakePermissionCapability()

        assertEquals(CapabilityIds.Permissions, capability.id)
        assertEquals(CapabilityHealth.ready(CapabilityIds.Permissions), capability.health.value)
    }

    private class FakePermissionCapability : PermissionCapability {
        override val id: CapabilityId = CapabilityIds.Permissions
        override val health = MutableStateFlow(CapabilityHealth.ready(id))
        override suspend fun listRequirements(): List<PermissionRequirement> = emptyList()
        override suspend fun openRequirementSettings(requirementId: String): CapabilityResult<Unit> = CapabilityResult.success(Unit)
    }

    @Suppress("unused")
    private class ContractSurface(
        val permission: PermissionCapability,
        val shell: ShellExecutionCapability,
        val screenCapture: ScreenCaptureCapability,
        val uiTree: UiTreeCapability,
        val inputInjection: InputInjectionCapability,
        val virtualDisplay: VirtualDisplayCapability,
        val floatingWindow: FloatingWindowCapability,
        val speechRecognition: SpeechRecognitionCapability,
        val localModel: LocalModelCapability,
    )
}
