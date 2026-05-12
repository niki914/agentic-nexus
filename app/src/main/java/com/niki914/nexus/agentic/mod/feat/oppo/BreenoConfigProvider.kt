package com.niki914.nexus.agentic.mod.feat.oppo

import com.niki914.nexus.agentic.mod.feat.BaseConfigProvider
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

object BreenoConfigProvider : BaseConfigProvider() {
    val roomIdManagerClass: String?
        get() = getString("classes.room_id_manager")

    val roomIdManagerCreateRoomMethod: String?
        get() = getString("accessors.room_id_manager.create_room.method_name")

    val roomIdManagerCreateRoomMethodParams: List<String>?
        get() {
            val list = getList("accessors.room_id_manager.create_room.param_types")
            return list?.mapNotNull { it.jsonPrimitive.contentOrNull }
        }

    val viewBeanClass: String?
        get() = getString("classes.view_bean")

    val dataCenterClass: String?
        get() = getString("classes.data_center")

    val operationFactoryClass: String?
        get() = getString("classes.operation_factory")

    val directiveClass: String?
        get() = getString("classes.directive")

    val cleanOperationClass: String?
        get() = getString("classes.clean_operation")

    val doNothingOperationClass: String?
        get() = getString("classes.do_nothing_operation")

    val feedbackInfoClass: String?
        get() = getString("classes.feedback_info")

    val footerInfoClass: String?
        get() = getString("classes.footer_info")

    val dataCenterInsertMessageMethod: String?
        get() = getString("accessors.data_center.insert_message.method_name")

    val dataCenterInsertMessageMethodParams: List<String>?
        get() {
            val list = getList("accessors.data_center.insert_message.param_types")
            return list?.mapNotNull { it.jsonPrimitive.contentOrNull }
        }

    val dataCenterUpdateMessageMethod: String?
        get() = getString("accessors.data_center.update_message")

    val operationFactoryCreateMethod: String?
        get() = getString("accessors.operation_factory.create.method_name")

    val operationFactoryCreateMethodParams: List<String>?
        get() {
            val list = getList("accessors.operation_factory.create.param_types")
            return list?.mapNotNull { it.jsonPrimitive.contentOrNull }
        }

    val beanGetChatTypeMethod: String?
        get() = getString("accessors.bean.get_chat_type")

    val beanSetChatTypeMethod: String?
        get() = getString("accessors.bean.set_chat_type")

    val beanGetRoomIdMethod: String?
        get() = getString("accessors.bean.get_room_id")

    val beanSetRoomIdMethod: String?
        get() = getString("accessors.bean.set_room_id")

    val beanGetContentMethod: String?
        get() = getString("accessors.bean.get_content")

    val beanSetContentMethod: String?
        get() = getString("accessors.bean.set_content")

    val beanSetRecordIdMethod: String?
        get() = getString("accessors.bean.set_record_id")

    val beanSetFinalMethod: String?
        get() = getString("accessors.bean.set_final")

    val beanSetFirstSliceMethod: String?
        get() = getString("accessors.bean.set_first_slice")

    val beanAddClientLocalDataMethod: String?
        get() = getString("accessors.bean.add_client_local_data")

    val beanGetClientLocalDataMethod: String?
        get() = getString("accessors.bean.get_client_local_data")

    val beanSetFeedbackInfoMethod: String?
        get() = getString("accessors.bean.set_feedback_info")

    val feedbackSetFooterInfoMethod: String?
        get() = getString("accessors.feedback.set_footer_info")

    val footerInfoSetCopyFlagMethod: String?
        get() = getString("accessors.footer_info.set_copy_flag")

    val footerInfoSetUpvoteFlagMethod: String?
        get() = getString("accessors.footer_info.set_upvote_flag")

    val typeQuery: Int?
        get() = getInt("schema.chat_type.query")

    val typeAnswer: Int?
        get() = getInt("schema.chat_type.answer")

    val selfInjectedMockFlagKey: String?
        get() = getString("schema.mock_flags.self_injected")

    val hideFeedbackViewLocalDataKey: String?
        get() = getString("schema.mock_flags.hide_feedback_view")

    val feedbackCopyFlagEnabled: Boolean
        get() = getBoolean("runtime.feedback_defaults.copy_flag") ?: true

    val feedbackUpvoteFlagEnabled: Boolean
        get() = getBoolean("runtime.feedback_defaults.upvote_flag") ?: true

    val mockBeanLocalDataUnit: List<Pair<String, Any>>
        get() = readConfigPairs(
            objectPath = "runtime.mock_defaults.local_data",
            legacyListPath = "mock_bean_local_data_unit"
        )

    val mockBeanMethodsUnit: List<Pair<String, Any>>
        get() = readConfigPairs(
            objectPath = "runtime.mock_defaults.bean_methods",
            legacyListPath = "mock_bean_methods_unit"
        )

    private fun readConfigPairs(
        objectPath: String,
        legacyListPath: String
    ): List<Pair<String, Any>> {
        getObject(objectPath)?.mapNotNull { (key, value) ->
            value.toConfigValue()?.let { key to it }
        }?.takeIf { it.isNotEmpty() }?.let { return it }

        val list = getList(legacyListPath) ?: return emptyList()
        return list.mapNotNull { element ->
            if (element is JsonArray && element.size == 2) {
                val key = element[0].jsonPrimitive.contentOrNull ?: return@mapNotNull null
                element[1].toConfigValue()?.let { key to it }
            } else {
                null
            }
        }
    }

    private fun JsonElement.toConfigValue(): Any? {
        val primitive = jsonPrimitive
        return primitive.intOrNull ?: primitive.booleanOrNull ?: primitive.contentOrNull
    }
}
