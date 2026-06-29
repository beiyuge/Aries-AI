package com.ai.phoneagent.core.capability

object CapabilityIds {
    val Permissions = CapabilityId("permissions")
    val ShizukuShell = CapabilityId("shizuku.shell")
    val Accessibility = CapabilityId("accessibility")
    val ScreenCapture = CapabilityId("screen.capture")
    val UiTree = CapabilityId("ui.tree")
    val InputInjection = CapabilityId("input.injection")
    val VirtualDisplay = CapabilityId("virtual.display")
    val FloatingWindow = CapabilityId("floating.window")
    val BackgroundTasks = CapabilityId("background.tasks")
    val NativeRuntime = CapabilityId("native.runtime")

    val allSystemIds: List<CapabilityId> = listOf(
        Permissions,
        ShizukuShell,
        Accessibility,
        ScreenCapture,
        UiTree,
        InputInjection,
        VirtualDisplay,
        FloatingWindow,
        BackgroundTasks,
        NativeRuntime,
    )
}
