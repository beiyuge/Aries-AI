package com.ai.phoneagent.platform.android.virtualdisplay

import android.app.ActivityOptions
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import com.ai.phoneagent.core.capability.CapabilityResult
import com.ai.phoneagent.core.capability.VirtualDisplayLaunchRequest

internal interface VirtualDisplayContentLauncher {
    fun launch(displayId: Int, request: VirtualDisplayLaunchRequest): CapabilityResult<Unit>
}

internal class ActivityOptionsVirtualDisplayContentLauncher(
    private val context: Context,
) : VirtualDisplayContentLauncher {
    override fun launch(
        displayId: Int,
        request: VirtualDisplayLaunchRequest,
    ): CapabilityResult<Unit> {
        if (request.applicationId.isBlank()) {
            return CapabilityResult.failure(
                VirtualDisplayErrors.invalidRequest("Application id is required."),
            )
        }
        val launchIntent = context.packageManager.getLaunchIntentForPackage(request.applicationId)
            ?: return CapabilityResult.failure(
                VirtualDisplayErrors.applicationNotFound(request.applicationId),
            )
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val options = ActivityOptions.makeBasic()
            .setLaunchDisplayId(displayId)
            .toBundle()

        return try {
            context.startActivity(launchIntent, options)
            CapabilityResult.success(Unit)
        } catch (error: ActivityNotFoundException) {
            CapabilityResult.failure(
                VirtualDisplayErrors.applicationNotFound(request.applicationId),
            )
        } catch (error: SecurityException) {
            CapabilityResult.failure(
                VirtualDisplayErrors.launchDenied(request.applicationId, error::class.qualifiedName),
            )
        } catch (error: IllegalArgumentException) {
            CapabilityResult.failure(
                VirtualDisplayErrors.launchDenied(request.applicationId, error::class.qualifiedName),
            )
        } catch (error: IllegalStateException) {
            CapabilityResult.failure(
                VirtualDisplayErrors.launchDenied(request.applicationId, error::class.qualifiedName),
            )
        }
    }
}
