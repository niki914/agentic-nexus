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

    val captureResponseTargetEnabled: Boolean
        get() = getBoolean("actions.capture_response_target.enabled") ?: true

    val captureResponseTargetOwnerClass: String?
        get() = getString("actions.capture_response_target.target.owner_class")

    val captureResponseTargetMethodName: String?
        get() = getString("actions.capture_response_target.target.method_name")

    val captureResponseTargetMethodParams: List<String>?
        get() = getList("actions.capture_response_target.target.param_types")
            ?.mapNotNull { it.jsonPrimitive.contentOrNull }

    val captureResponseTargetHookTiming: String?
        get() = getString("actions.capture_response_target.target.hook_timing")

    val captureResponseTargetDialogIdGetter: String?
        get() = getString("actions.capture_response_target.business.card_dialog_id_getter")

    val blockNativeStreamAction: JsonObject?
        get() = getObject("actions.block_native_stream")

    val blockNativeStreamEnabled: Boolean
        get() = getBoolean("actions.block_native_stream.enabled") ?: true

    val blockNativeStreamOwnerClass: String?
        get() = getString("actions.block_native_stream.target.owner_class")

    val blockNativeStreamMethodName: String?
        get() = getString("actions.block_native_stream.target.method_name")

    val blockNativeStreamMethodParams: List<String>?
        get() = getList("actions.block_native_stream.target.param_types")
            ?.mapNotNull { it.jsonPrimitive.contentOrNull }

    val blockNativeStreamHookTiming: String?
        get() = getString("actions.block_native_stream.target.hook_timing")

    val blockNativeStreamDialogIdGetter: String?
        get() = getString("actions.block_native_stream.business.card_dialog_id_getter")

    val renderStreamCardAction: JsonObject?
        get() = getObject("actions.render_stream_card")

    val renderStreamCardEnabled: Boolean
        get() = getBoolean("actions.render_stream_card.enabled") ?: true

    val renderStreamCardOwnerClass: String?
        get() = getString("actions.render_stream_card.target.owner_class")

    val renderStreamCardMethodName: String?
        get() = getString("actions.render_stream_card.target.method_name")

    val renderStreamCardMethodParams: List<String>?
        get() = getList("actions.render_stream_card.target.param_types")
            ?.mapNotNull { it.jsonPrimitive.contentOrNull }

    val renderStreamCardTotalTextKey: String
        get() = getString("actions.render_stream_card.business.total_text_key") ?: "totalText"

    val renderStreamCardCompleteKey: String
        get() = getString("actions.render_stream_card.business.complete_key") ?: "isLlmContentDisplayComplete"

    val renderStreamCardIllegalKey: String
        get() = getString("actions.render_stream_card.business.illegal_key") ?: "isIllegalContent"
}
