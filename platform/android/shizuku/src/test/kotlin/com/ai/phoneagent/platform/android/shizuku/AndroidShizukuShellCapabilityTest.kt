package com.ai.phoneagent.platform.android.shizuku

import com.ai.phoneagent.core.capability.CapabilityState
import com.ai.phoneagent.core.capability.ShellExecRequest
import com.ai.phoneagent.core.capability.ShellExecResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AndroidShizukuShellCapabilityTest {
    @Test
    fun `binder unavailable maps to unavailable health`() {
        val capability = AndroidShizukuShellCapability(FakeShizukuEnvironment(binderAlive = false))

        val health = capability.health.value

        assertFalse(health.available)
        assertEquals(CapabilityState.Unavailable, health.state)
        assertEquals("shizuku.binder_unavailable", health.lastError?.code)
    }

    @Test
    fun `missing permission maps to permission required health`() {
        val capability = AndroidShizukuShellCapability(
            FakeShizukuEnvironment(binderAlive = true, permissionGranted = false),
        )

        val health = capability.health.value

        assertFalse(health.available)
        assertEquals(CapabilityState.PermissionRequired, health.state)
        assertEquals(listOf("shizuku.permission"), health.missingRequirements.map { it.id })
    }

    @Test
    fun `ready environment executes command through backend`() = runBlocking {
        val capability = AndroidShizukuShellCapability(
            FakeShizukuEnvironment(
                binderAlive = true,
                permissionGranted = true,
                result = ShellExecResult(exitCode = 0, stdout = "ok", stderr = ""),
            ),
        )

        val result = capability.exec(ShellExecRequest(listOf("echo", "ok")))

        assertTrue(capability.health.value.available)
        assertTrue(result.success)
        assertEquals("ok", result.stdout)
    }
}

private class FakeShizukuEnvironment(
    private val binderAlive: Boolean,
    private val permissionGranted: Boolean = false,
    private val result: ShellExecResult = ShellExecResult(exitCode = 0, stdout = "", stderr = ""),
) : ShizukuEnvironment {
    override fun isBinderAlive(): Boolean = binderAlive
    override fun hasPermission(): Boolean = permissionGranted
    override fun exec(request: ShellExecRequest): ShellExecResult = result
}
