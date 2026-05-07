package com.niki914.breeno.a.mod

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

object BreenoConfigProvider : BaseConfigProvider() {
    val roomIdManagerClass: String?
        get() = getString("classes.room_id_manager") ?: getString("room_id_manager_class")

    val roomIdManagerCreateRoomMethod: String?
        get() = getString("accessors.room_id_manager.create_room")
            ?: getString("room_id_manager_method_p")

    val viewBeanClass: String?
        get() = getString("classes.view_bean") ?: getString("view_bean_class")

    val dataCenterClass: String?
        get() = getString("classes.data_center") ?: getString("data_center_class")

    val feedbackInfoClass: String?
        get() = getString("classes.feedback_info")

    val footerInfoClass: String?
        get() = getString("classes.footer_info")

    val dataCenterInsertMessageMethod: String?
        get() = getString("accessors.data_center.insert_message")
            ?: getString("data_center_method_r")

    val dataCenterUpdateMessageMethod: String?
        get() = getString("accessors.data_center.update_message")
            ?: getString("data_center_method_g1")

    val beanGetChatTypeMethod: String
        get() = getString("accessors.bean.get_chat_type") ?: "getChatType"

    val beanSetChatTypeMethod: String
        get() = getString("accessors.bean.set_chat_type") ?: "setChatType"

    val beanGetSkillTypeMethod: String
        get() = getString("accessors.bean.get_skill_type") ?: "getSkillType"

    val beanGetRoomIdMethod: String
        get() = getString("accessors.bean.get_room_id") ?: "getRoomId"

    val beanSetRoomIdMethod: String
        get() = getString("accessors.bean.set_room_id") ?: "setRoomId"

    val beanGetContentMethod: String
        get() = getString("accessors.bean.get_content") ?: "getContent"

    val beanSetContentMethod: String
        get() = getString("accessors.bean.set_content") ?: "setContent"

    val beanSetRecordIdMethod: String
        get() = getString("accessors.bean.set_record_id") ?: "setRecordId"

    val beanSetFinalMethod: String
        get() = getString("accessors.bean.set_final") ?: "setFinal"

    val beanSetFirstSliceMethod: String
        get() = getString("accessors.bean.set_first_slice") ?: "setFirstSlice"

    val beanAddClientLocalDataMethod: String
        get() = getString("accessors.bean.add_client_local_data") ?: "addClientLocalData"

    val beanGetClientLocalDataMethod: String
        get() = getString("accessors.bean.get_client_local_data") ?: "getClientLocalData"

    val beanSetFeedbackInfoMethod: String
        get() = getString("accessors.bean.set_feedback_info") ?: "setFeedBackInfo"

    val feedbackSetFooterInfoMethod: String
        get() = getString("accessors.feedback.set_footer_info") ?: "setFooterInfo"

    val footerInfoSetCopyFlagMethod: String
        get() = getString("accessors.footer_info.set_copy_flag") ?: "setCopyFlag"

    val footerInfoSetUpvoteFlagMethod: String
        get() = getString("accessors.footer_info.set_upvote_flag") ?: "setUpvoteFlag"

    val typeQuery: Int?
        get() = getInt("schema.chat_type.query") ?: getInt("type_query_value")

    val typeAnswer: Int?
        get() = getInt("schema.chat_type.answer") ?: getInt("type_answer_value")

    val selfInjectedMockFlagKey: String
        get() = getString("schema.mock_flags.self_injected")
            ?: mockBeanLocalDataUnit.find { it.second == true }?.first
            ?: "MY_MOCK_FLAG"

    val hideFeedbackViewLocalDataKey: String
        get() = getString("schema.mock_flags.hide_feedback_view")
            ?: "bean_client_key_hide_feedback_view"

    val answerSkillPolicyMode: String
        get() = getString("runtime.answer_skill_policy.mode") ?: "whitelist"

    val answerSkillPolicyTypes: List<String>
        get() {
            val list = getList("runtime.answer_skill_policy.types")
                ?: getList("allowed_skill_types")
                ?: return emptyList()
            return list.mapNotNull { it.jsonPrimitive.contentOrNull }
        }

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
