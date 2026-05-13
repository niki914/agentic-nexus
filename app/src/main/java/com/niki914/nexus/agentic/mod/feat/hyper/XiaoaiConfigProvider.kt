package com.niki914.nexus.agentic.mod.feat.hyper

import com.niki914.nexus.agentic.mod.feat.BaseConfigProvider
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

object XiaoaiConfigProvider : BaseConfigProvider() {
    val captureInputEnabled: Boolean
        get() = getBoolean("actions.capture_input.enabled") ?: true

    val captureInputOwnerClass: String?
        get() = getString("actions.capture_input.target.owner_class")

    val captureInputMethodName: String?
        get() = getString("actions.capture_input.target.method_name")

    val captureInputMethodParams: List<String>?
        get() = getList("actions.capture_input.target.param_types")
            ?.mapNotNull { it.jsonPrimitive.contentOrNull }

    val captureInputHookTiming: String?
        get() = getString("actions.capture_input.target.hook_timing")

    val captureInputDialogIdArgIndex: Int?
        get() = getInt("actions.capture_input.business.dialog_id_arg_index")

    val captureInputQueryArgIndex: Int?
        get() = getInt("actions.capture_input.business.query_arg_index")

    val resetSessionAction: JsonObject?
        get() = getObject("actions.reset_session")

    val blockNativeStreamAction: JsonObject?
        get() = getObject("actions.block_native_stream")

    val renderStreamCardAction: JsonObject?
        get() = getObject("actions.render_stream_card")
}