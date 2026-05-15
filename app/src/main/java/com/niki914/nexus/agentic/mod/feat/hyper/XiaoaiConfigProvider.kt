package com.niki914.nexus.agentic.mod.feat.hyper

import com.niki914.nexus.agentic.mod.feat.BaseConfigProvider
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

object XiaoaiConfigProvider : BaseConfigProvider() {
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

    val resetSessionOwnerClass: String?
        get() = getString("actions.reset_session.target.owner_class")

    val resetSessionMethodName: String?
        get() = getString("actions.reset_session.target.method_name")

    val resetSessionMethodParams: List<String>?
        get() = getList("actions.reset_session.target.param_types")
            ?.mapNotNull { it.jsonPrimitive.contentOrNull }

    val resetSessionHookTiming: String?
        get() = getString("actions.reset_session.target.hook_timing")

    val captureResponseTargetOwnerClass: String?
        get() = getString("actions.capture_response_target.target.owner_class")

    val captureResponseTargetMethodName: String?
        get() = getString("actions.capture_response_target.target.method_name")

    val captureResponseTargetMethodParams: List<String>?
        get() = getList("actions.capture_response_target.target.param_types")
            ?.mapNotNull { it.jsonPrimitive.contentOrNull }

    val captureResponseTargetHookTiming: String?
        get() = getString("actions.capture_response_target.target.hook_timing")

    val captureResponseTargetTargetDialogIdGetter: String?
        get() = getString("actions.capture_response_target.business.target_dialog_id_getter")

    val captureResponseTargetInstructionDialogIdGetter: String
        get() = getString("actions.capture_response_target.business.instruction_dialog_id_getter")
            ?: "getDialogId"

    val captureResponseTargetOptionalHasValueMethod: String
        get() = getString("actions.capture_response_target.business.optional_has_value_method")
            ?: "isPresent"

    val captureResponseTargetOptionalValueGetter: String
        get() = getString("actions.capture_response_target.business.optional_value_getter")
            ?: "get"

    val blockNativeTextStreamOwnerClass: String?
        get() = getString("actions.block_native_text_stream.target.owner_class")

    val blockNativeTextStreamMethodName: String?
        get() = getString("actions.block_native_text_stream.target.method_name")

    val blockNativeTextStreamMethodParams: List<String>?
        get() = getList("actions.block_native_text_stream.target.param_types")
            ?.mapNotNull { it.jsonPrimitive.contentOrNull }

    val blockNativeTextStreamHookTiming: String?
        get() = getString("actions.block_native_text_stream.target.hook_timing")

    val blockNativeTextStreamTargetDialogIdGetter: String?
        get() = getString("actions.block_native_text_stream.business.target_dialog_id_getter")

    val blockNativeTextStreamInstructionFullName: String?
        get() = getString("actions.block_native_text_stream.business.instruction_full_name")

    val blockNativeTextStreamInstructionFullNameGetter: String
        get() = getString("actions.block_native_text_stream.business.instruction_full_name_getter")
            ?: "getFullName"

    val blockNativeTextStreamInstructionDialogIdGetter: String
        get() = getString("actions.block_native_text_stream.business.instruction_dialog_id_getter")
            ?: "getDialogId"

    val blockNativeTextStreamOptionalHasValueMethod: String
        get() = getString("actions.block_native_text_stream.business.optional_has_value_method")
            ?: "isPresent"

    val blockNativeTextStreamOptionalValueGetter: String
        get() = getString("actions.block_native_text_stream.business.optional_value_getter")
            ?: "get"

    val blockNativeTtsStreamOwnerClass: String?
        get() = getString("actions.block_native_tts_stream.target.owner_class")

    val blockNativeTtsStreamMethodName: String?
        get() = getString("actions.block_native_tts_stream.target.method_name")

    val blockNativeTtsStreamMethodParams: List<String>?
        get() = getList("actions.block_native_tts_stream.target.param_types")
            ?.mapNotNull { it.jsonPrimitive.contentOrNull }

    val blockNativeTtsStreamHookTiming: String?
        get() = getString("actions.block_native_tts_stream.target.hook_timing")

    val blockNativeTtsStreamTargetDialogIdGetter: String?
        get() = getString("actions.block_native_tts_stream.business.target_dialog_id_getter")

    val blockNativeTtsStreamInstructionFullName: String?
        get() = getString("actions.block_native_tts_stream.business.instruction_full_name")

    val blockNativeTtsStreamInstructionFullNameGetter: String
        get() = getString("actions.block_native_tts_stream.business.instruction_full_name_getter")
            ?: "getFullName"

    val blockNativeTtsStreamInstructionDialogIdGetter: String
        get() = getString("actions.block_native_tts_stream.business.instruction_dialog_id_getter")
            ?: "getDialogId"

    val blockNativeTtsStreamOptionalHasValueMethod: String
        get() = getString("actions.block_native_tts_stream.business.optional_has_value_method")
            ?: "isPresent"

    val blockNativeTtsStreamOptionalValueGetter: String
        get() = getString("actions.block_native_tts_stream.business.optional_value_getter")
            ?: "get"

    val blockNativeTtsPlaybackOwnerClass: String?
        get() = getString("actions.block_native_tts_playback.target.owner_class")

    val blockNativeTtsPlaybackMethodName: String?
        get() = getString("actions.block_native_tts_playback.target.method_name")

    val blockNativeTtsPlaybackMethodParams: List<String>?
        get() = getList("actions.block_native_tts_playback.target.param_types")
            ?.mapNotNull { it.jsonPrimitive.contentOrNull }

    val blockNativeTtsPlaybackHookTiming: String?
        get() = getString("actions.block_native_tts_playback.target.hook_timing")

    val blockNativeTtsPlaybackTargetDialogIdGetter: String?
        get() = getString("actions.block_native_tts_playback.business.target_dialog_id_getter")

    val renderTextStreamCardOwnerClass: String?
        get() = getString("actions.render_text_stream_card.target.owner_class")

    val renderTextStreamCardMethodName: String?
        get() = getString("actions.render_text_stream_card.target.method_name")

    val renderTextStreamCardMethodParams: List<String>?
        get() = getList("actions.render_text_stream_card.target.param_types")
            ?.mapNotNull { it.jsonPrimitive.contentOrNull }

    val renderTextStreamCardHookTiming: String?
        get() = getString("actions.render_text_stream_card.target.hook_timing")

    val renderTextStreamCardInstructionNamespace: String
        get() = getString("actions.render_text_stream_card.business.instruction_namespace") ?: "Template"

    val renderTextStreamCardInstructionName: String
        get() = getString("actions.render_text_stream_card.business.instruction_name") ?: "ToastStream"

    val renderTextStreamCardInstructionClass: String
        get() = getString("actions.render_text_stream_card.business.instruction_class")
            ?: "com.xiaomi.ai.api.common.Instruction"

    val renderTextStreamCardInstructionConstructorParamTypes: List<String>
        get() = getList("actions.render_text_stream_card.business.instruction_constructor_param_types")
            ?.mapNotNull { it.jsonPrimitive.contentOrNull }
            ?: emptyList()

    val renderTextStreamCardInstructionHeaderClass: String
        get() = getString("actions.render_text_stream_card.business.instruction_header_class")
            ?: "com.xiaomi.ai.api.common.InstructionHeader"

    val renderTextStreamCardInstructionHeaderConstructorParamTypes: List<String>
        get() = getList("actions.render_text_stream_card.business.instruction_header_constructor_param_types")
            ?.mapNotNull { it.jsonPrimitive.contentOrNull }
            ?: listOf("java.lang.String", "java.lang.String")

    val renderTextStreamCardTextStreamPayloadClass: String
        get() = getString("actions.render_text_stream_card.business.text_stream_payload_class")
            ?: "com.xiaomi.ai.api.Template\$ToastStream"

    val renderTextStreamCardTextStreamPayloadConstructorParamTypes: List<String>
        get() = getList("actions.render_text_stream_card.business.text_stream_payload_constructor_param_types")
            ?.mapNotNull { it.jsonPrimitive.contentOrNull }
            ?: listOf("java.lang.String")

    val renderTextStreamCardInstructionHeaderIdSetter: String
        get() = getString("actions.render_text_stream_card.business.instruction_header_id_setter")
            ?: "setId"

    val renderTextStreamCardInstructionHeaderDialogIdSetter: String
        get() = getString("actions.render_text_stream_card.business.instruction_header_dialog_id_setter")
            ?: "setDialogId"

    val renderTextStreamCardInstructionHeaderSetter: String
        get() = getString("actions.render_text_stream_card.business.instruction_header_setter")
            ?: "setHeader"

    val renderTextStreamCardInstructionPayloadSetter: String
        get() = getString("actions.render_text_stream_card.business.instruction_payload_setter")
            ?: "setPayload"

    val renderTextStreamCardInstructionIdPrefix: String
        get() = getString("actions.render_text_stream_card.business.instruction_id_prefix")
            ?: "nexus_xiaoai_text"

    val renderTextStreamCardFinalChunkText: String
        get() = getString("actions.render_text_stream_card.business.final_chunk_text") ?: "<FINAL>"
}
