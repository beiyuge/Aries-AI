package com.ai.phoneagent.re0

import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import com.ai.phoneagent.core.capability.CapabilityIds
import com.ai.phoneagent.re0.generated.CapabilityHostApi
import com.ai.phoneagent.re0.generated.AutomationHostApi
import com.ai.phoneagent.re0.generated.LocalModelHostApi
import com.ai.phoneagent.re0.host.AndroidCapabilityHostApi
import com.ai.phoneagent.re0.host.AndroidAutomationHostApi
import com.ai.phoneagent.re0.host.AndroidLocalModelHostApi
import com.ai.phoneagent.re0.host.Re0CapabilityGraph
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

class Re0MainActivity : FlutterActivity() {
    private val capabilityRegistry by lazy {
        Re0CapabilityGraph.createRegistry(this)
    }
    private var hostScope: CoroutineScope? = null

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        hostScope = scope
        CapabilityHostApi.setUp(
            flutterEngine.dartExecutor.binaryMessenger,
            AndroidCapabilityHostApi(
                context = this,
                registry = capabilityRegistry,
                defaultCapabilityId = CapabilityIds.Permissions,
            ),
        )
        LocalModelHostApi.setUp(
            flutterEngine.dartExecutor.binaryMessenger,
            AndroidLocalModelHostApi(
                registry = capabilityRegistry,
                scope = scope,
            ),
        )
        AutomationHostApi.setUp(
            flutterEngine.dartExecutor.binaryMessenger,
            AndroidAutomationHostApi(
                registry = capabilityRegistry,
                scope = scope,
            ),
        )
    }

    override fun cleanUpFlutterEngine(flutterEngine: FlutterEngine) {
        CapabilityHostApi.setUp(flutterEngine.dartExecutor.binaryMessenger, null)
        LocalModelHostApi.setUp(flutterEngine.dartExecutor.binaryMessenger, null)
        AutomationHostApi.setUp(flutterEngine.dartExecutor.binaryMessenger, null)
        hostScope?.cancel()
        hostScope = null
        super.cleanUpFlutterEngine(flutterEngine)
    }
}
