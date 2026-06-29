package com.ai.phoneagent.platform.android.shizuku

import android.content.pm.PackageManager
import com.ai.phoneagent.core.capability.CapabilityError
import com.ai.phoneagent.core.capability.CapabilityHealth
import com.ai.phoneagent.core.capability.CapabilityId
import com.ai.phoneagent.core.capability.CapabilityIds
import com.ai.phoneagent.core.capability.CapabilityRequirement
import com.ai.phoneagent.core.capability.CapabilityResult
import com.ai.phoneagent.core.capability.CapabilitySelfTest
import com.ai.phoneagent.core.capability.ShellExecRequest
import com.ai.phoneagent.core.capability.ShellExecResult
import com.ai.phoneagent.core.capability.ShellExecutionCapability
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.MutableStateFlow

class AndroidShizukuShellCapability(
    private val environment: ShizukuEnvironment = ReflectiveShizukuEnvironment(),
) : ShellExecutionCapability, CapabilitySelfTest {
    override val id: CapabilityId = CapabilityIds.ShizukuShell
    override val health = MutableStateFlow(readHealth())

    override suspend fun exec(request: ShellExecRequest): ShellExecResult {
        refreshHealth()
        val currentHealth = health.value
        if (!currentHealth.available) {
            return ShellExecResult(
                exitCode = -1,
                stdout = "",
                stderr = "",
                error = currentHealth.lastError,
            )
        }
        return environment.exec(request)
    }

    override fun runSelfTest(): CapabilityResult<String> {
        refreshHealth()
        val currentHealth = health.value
        return if (currentHealth.available) {
            CapabilityResult.success("${id.value}: Shizuku binder and permission are ready")
        } else {
            CapabilityResult.failure(
                currentHealth.lastError ?: CapabilityError(
                    code = "shizuku.not_ready",
                    message = "Shizuku shell is not ready.",
                    recoverable = true,
                ),
            )
        }
    }

    fun refreshHealth() {
        health.value = readHealth()
    }

    private fun readHealth(): CapabilityHealth {
        val diagnostics = mapOf(
            "platform" to "android",
            "backend" to "shizuku",
        )
        if (!environment.isBinderAlive()) {
            return CapabilityHealth.unavailable(
                id = id,
                error = CapabilityError(
                    code = "shizuku.binder_unavailable",
                    message = "Shizuku binder is not running.",
                    recoverable = true,
                    suggestedAction = "Start Shizuku and grant Aries permission.",
                ),
                diagnostics = diagnostics,
            )
        }
        if (!environment.hasPermission()) {
            return CapabilityHealth.permissionRequired(
                id = id,
                missingRequirements = listOf(shizukuPermissionRequirement()),
                diagnostics = diagnostics,
            )
        }
        return CapabilityHealth.ready(id = id, diagnostics = diagnostics)
    }

    private fun shizukuPermissionRequirement(): CapabilityRequirement =
        CapabilityRequirement(
            id = "shizuku.permission",
            title = "Shizuku",
            description = "Grant Shizuku permission for shell-backed automation.",
        )
}

interface ShizukuEnvironment {
    fun isBinderAlive(): Boolean
    fun hasPermission(): Boolean
    fun exec(request: ShellExecRequest): ShellExecResult
}

class ReflectiveShizukuEnvironment : ShizukuEnvironment {
    override fun isBinderAlive(): Boolean = runCatching {
        shizukuClass().getMethod("pingBinder").invoke(null) as Boolean
    }.getOrDefault(false)

    override fun hasPermission(): Boolean = runCatching {
        val result = shizukuClass().getMethod("checkSelfPermission").invoke(null) as Int
        result == PackageManager.PERMISSION_GRANTED
    }.getOrDefault(false)

    override fun exec(request: ShellExecRequest): ShellExecResult {
        if (request.argv.isEmpty()) {
            return ShellExecResult(
                exitCode = -1,
                stdout = "",
                stderr = "",
                error = CapabilityError(
                    code = "shizuku.empty_command",
                    message = "Shell command argv must not be empty.",
                    recoverable = false,
                ),
            )
        }

        return runCatching {
            val process = shizukuClass()
                .getMethod("newProcess", Array<String>::class.java, Array<String>::class.java, String::class.java)
                .invoke(null, request.argv.toTypedArray(), emptyArray<String>(), null) as Process
            val completed = process.waitFor(request.timeoutMs, TimeUnit.MILLISECONDS)
            if (!completed) {
                process.destroyForcibly()
                ShellExecResult(
                    exitCode = -1,
                    stdout = process.inputStream.bufferedReader().readText(),
                    stderr = process.errorStream.bufferedReader().readText(),
                    error = CapabilityError(
                        code = "shizuku.command_timeout",
                        message = "Shell command timed out after ${request.timeoutMs} ms.",
                        recoverable = true,
                    ),
                )
            } else {
                ShellExecResult(
                    exitCode = process.exitValue(),
                    stdout = process.inputStream.bufferedReader().readText(),
                    stderr = process.errorStream.bufferedReader().readText(),
                )
            }
        }.getOrElse { error ->
            ShellExecResult(
                exitCode = -1,
                stdout = "",
                stderr = "",
                error = CapabilityError(
                    code = "shizuku.exec_failed",
                    message = error.message ?: "Shizuku command execution failed.",
                    recoverable = true,
                ),
            )
        }
    }

    private fun shizukuClass(): Class<*> = Class.forName("rikka.shizuku.Shizuku")
}
