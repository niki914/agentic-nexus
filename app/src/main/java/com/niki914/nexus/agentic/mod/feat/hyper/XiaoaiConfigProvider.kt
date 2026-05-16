package com.niki914.nexus.agentic.mod.feat.hyper

import com.niki914.nexus.agentic.mod.feat.BaseConfigProvider
import com.niki914.nexus.agentic.mod.feat.HookTarget
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

object XiaoaiConfigProvider : BaseConfigProvider() {

    object CaptureInput {
        val hookTarget: HookTarget?
            get() = parseHookTarget("actions.capture_input.target")
        val dialogIdArgIndex: Int?
            get() = getInt("actions.capture_input.business.dialog_id_arg_index")
        val queryArgIndex: Int?
            get() = getInt("actions.capture_input.business.query_arg_index")
    }

    object CaptureResponseTarget {
        val hookTarget: HookTarget?
            get() = parseHookTarget("actions.capture_response_target.target")
        val targetDialogIdGetter: String?
            get() = getString("actions.capture_response_target.business.target_dialog_id_getter")
        val instructionDialogIdGetter: String?
            get() = getString("actions.capture_response_target.business.instruction_dialog_id_getter")

        val optionalHasValueMethod: String?
            get() = getString("actions.capture_response_target.business.optional_has_value_method")
        val optionalValueGetter: String?
            get() = getString("actions.capture_response_target.business.optional_value_getter")
    }

    object ResetSession {
        val hookTarget: HookTarget?
            get() = parseHookTarget("actions.reset_session.target")
        val targetActivityClass: String?
            get() = getString("actions.reset_session.business.target_activity_class")
    }

    object BlockNativeTextStream {
        val hookTarget: HookTarget?
            get() = parseHookTarget("actions.block_native_text_stream.target")
        val targetDialogIdGetter: String?
            get() = getString("actions.block_native_text_stream.business.target_dialog_id_getter")
        val instructionFullName: String?
            get() = getString("actions.block_native_text_stream.business.instruction_full_name")
        val instructionFullNameGetter: String?
            get() = getString("actions.block_native_text_stream.business.instruction_full_name_getter")
        val instructionDialogIdGetter: String?
            get() = getString("actions.block_native_text_stream.business.instruction_dialog_id_getter")
        val optionalHasValueMethod: String?
            get() = getString("actions.block_native_text_stream.business.optional_has_value_method")
        val optionalValueGetter: String?
            get() = getString("actions.block_native_text_stream.business.optional_value_getter")
    }

    object BlockNativeTtsStream {
        val hookTarget: HookTarget?
            get() = parseHookTarget("actions.block_native_tts_stream.target")
        val targetDialogIdGetter: String?
            get() = getString("actions.block_native_tts_stream.business.target_dialog_id_getter")
        val instructionFullName: String?
            get() = getString("actions.block_native_tts_stream.business.instruction_full_name")
        val instructionFullNameGetter: String?
            get() = getString("actions.block_native_tts_stream.business.instruction_full_name_getter")
        val instructionDialogIdGetter: String?
            get() = getString("actions.block_native_tts_stream.business.instruction_dialog_id_getter")
        val optionalHasValueMethod: String?
            get() = getString("actions.block_native_tts_stream.business.optional_has_value_method")
        val optionalValueGetter: String?
            get() = getString("actions.block_native_tts_stream.business.optional_value_getter")
    }

    object BlockNativeTtsPlayback {
        val hookTarget: HookTarget?
            get() = parseHookTarget("actions.block_native_tts_playback.target")
        val targetDialogIdGetter: String?
            get() = getString("actions.block_native_tts_playback.business.target_dialog_id_getter")
    }

    object RenderTextStreamCard {
        val hookTarget: HookTarget?
            get() = parseHookTarget("actions.render_text_stream_card.target")
        val instructionNamespace: String?
            get() = getString("actions.render_text_stream_card.business.instruction_namespace")
        val instructionName: String?
            get() = getString("actions.render_text_stream_card.business.instruction_name")
        val instructionClass: String?
            get() = getString("actions.render_text_stream_card.business.instruction_class")
        val instructionConstructorParamTypes: List<String>?
            get() = getList("actions.render_text_stream_card.business.instruction_constructor_param_types")
                ?.mapNotNull { it.jsonPrimitive.contentOrNull }
        val instructionHeaderClass: String?
            get() = getString("actions.render_text_stream_card.business.instruction_header_class")
        val instructionHeaderConstructorParamTypes: List<String>?
            get() = getList("actions.render_text_stream_card.business.instruction_header_constructor_param_types")
                ?.mapNotNull { it.jsonPrimitive.contentOrNull }
        val textStreamPayloadClass: String?
            get() = getString("actions.render_text_stream_card.business.text_stream_payload_class")
        val textStreamPayloadConstructorParamTypes: List<String>?
            get() = getList("actions.render_text_stream_card.business.text_stream_payload_constructor_param_types")
                ?.mapNotNull { it.jsonPrimitive.contentOrNull }
        val instructionHeaderIdSetter: String?
            get() = getString("actions.render_text_stream_card.business.instruction_header_id_setter")
        val instructionHeaderDialogIdSetter: String?
            get() = getString("actions.render_text_stream_card.business.instruction_header_dialog_id_setter")
        val instructionHeaderSetter: String?
            get() = getString("actions.render_text_stream_card.business.instruction_header_setter")
        val instructionPayloadSetter: String?
            get() = getString("actions.render_text_stream_card.business.instruction_payload_setter")
        val instructionIdPrefix: String?
            get() = getString("actions.render_text_stream_card.business.instruction_id_prefix")
        val finalChunkText: String?
            get() = getString("actions.render_text_stream_card.business.final_chunk_text")
    }

    object RenderTts {
        val hookTarget: HookTarget?
            get() = parseHookTarget("actions.render_tts.target")
    }

    val floatWindowOwnerClass: String?
        get() = ResetSession.hookTarget?.ownerClass

    val floatWindowDetachMethodName: String?
        get() = ResetSession.hookTarget?.methodName

    val floatWindowTargetActivityClass: String?
        get() = ResetSession.targetActivityClass
}
