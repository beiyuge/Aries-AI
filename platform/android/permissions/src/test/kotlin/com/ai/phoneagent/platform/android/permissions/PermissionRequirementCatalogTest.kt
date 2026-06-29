package com.ai.phoneagent.platform.android.permissions

import com.ai.phoneagent.core.capability.CapabilityAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PermissionRequirementCatalogTest {
    @Test
    fun `catalog contains required Android permissions for re0 system capabilities`() {
        val requirements = PermissionRequirementCatalog.defaultRequirements()
        val ids = requirements.map { it.id }

        assertTrue("accessibility.service" in ids)
        assertTrue("overlay.window" in ids)
        assertTrue("notifications" in ids)
        assertTrue("microphone" in ids)
        assertTrue("shizuku.permission" in ids)
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun `accessibility requirement opens accessibility settings`() {
        val accessibility = PermissionRequirementCatalog.defaultRequirements()
            .single { it.id == "accessibility.service" }

        assertEquals(
            CapabilityAction.OpenSettings("android.settings.ACCESSIBILITY_SETTINGS"),
            accessibility.action,
        )
    }
}
