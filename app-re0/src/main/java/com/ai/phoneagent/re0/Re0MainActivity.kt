package com.ai.phoneagent.re0

import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import com.ai.phoneagent.core.capability.CapabilityIds
import com.ai.phoneagent.re0.generated.CapabilityHostApi
import com.ai.phoneagent.re0.host.AndroidCapabilityHostApi
import com.ai.phoneagent.re0.host.Re0CapabilityGraph

class Re0MainActivity : FlutterActivity() {
    private val capabilityRegistry by lazy {
        Re0CapabilityGraph.createRegistry(this)
    }

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        CapabilityHostApi.setUp(
            flutterEngine.dartExecutor.binaryMessenger,
            AndroidCapabilityHostApi(
                context = this,
                registry = capabilityRegistry,
                defaultCapabilityId = CapabilityIds.Permissions,
            ),
        )
    }
}
