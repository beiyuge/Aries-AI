package com.ai.phoneagent.platform.android.permissions

import com.ai.phoneagent.core.capability.CapabilityError
import com.ai.phoneagent.core.capability.CapabilityHealth
import com.ai.phoneagent.core.capability.CapabilityId
import com.ai.phoneagent.core.capability.CapabilityIds
import com.ai.phoneagent.core.capability.CapabilityResult
import com.ai.phoneagent.core.capability.CapabilitySelfTest
import com.ai.phoneagent.core.capability.PermissionCapability
import com.ai.phoneagent.core.capability.PermissionRequirement
import kotlinx.coroutines.flow.MutableStateFlow

class AndroidPermissionsCapability(
    private val requirements: List<PermissionRequirement> = PermissionRequirementCatalog.defaultRequirements(),
) : PermissionCapability, CapabilitySelfTest {
    override val id: CapabilityId = CapabilityIds.Permissions

    override val health = MutableStateFlow(
        CapabilityHealth.permissionRequired(
            id = id,
            missingRequirements = requirements,
            diagnostics = mapOf(
                "platform" to "android",
                "backend" to "permission-catalog",
                "requirementCount" to requirements.size.toString(),
            ),
        ),
    )

    override suspend fun listRequirements(): List<PermissionRequirement> = requirements

    override suspend fun openRequirementSettings(requirementId: String): CapabilityResult<Unit> {
        val requirementExists = requirements.any { requirement -> requirement.id == requirementId }
        return if (requirementExists) {
            CapabilityResult.success(Unit)
        } else {
            CapabilityResult.failure(
                CapabilityError(
                    code = "permission.requirement_not_found",
                    message = "Permission requirement '$requirementId' is not registered.",
                    recoverable = false,
                ),
            )
        }
    }

    override fun runSelfTest(): CapabilityResult<String> = CapabilityResult.success(
        "${id.value}: ${requirements.size} Android permission requirements registered",
    )
}
