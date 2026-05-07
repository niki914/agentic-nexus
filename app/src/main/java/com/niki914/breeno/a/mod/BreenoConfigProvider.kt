package com.niki914.breeno.a.mod

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

object BreenoConfigProvider : BaseConfigProvider() {
    val roomIdManagerClass: String? get() = getString("room_id_manager_class")
    val roomIdManagerMethodP: String? get() = getString("room_id_manager_method_p")

    val viewBeanClass: String? get() = getString("view_bean_class")
    val typeQuery: Int? get() = getInt("type_query_value")
    val typeAnswer: Int? get() = getInt("type_answer_value")

    val dataCenterClass: String? get() = getString("data_center_class")
    val dataCenterMethodR: String? get() = getString("data_center_method_r")
    val dataCenterMethodG1: String? get() = getString("data_center_method_g1") // TODO 命名太无意义，而且以某个特定版本为标准，预期应该猜测原方法的意义然后给它命名一个有意义的名字

    val mockBeanMethodsUnit: List<Pair<String, Any>>
        get() {
            val list = getList("mock_bean_methods_unit") ?: return emptyList()
            return list.mapNotNull { element ->
                if (element is JsonArray && element.size == 2) {
                    val methodName =
                        element[0].jsonPrimitive.contentOrNull ?: return@mapNotNull null
                    val primitive = element[1].jsonPrimitive
                    val value: Any =
                        primitive.intOrNull ?: primitive.booleanOrNull ?: primitive.contentOrNull
                        ?: return@mapNotNull null
                    methodName to value
                } else {
                    null
                }
            }
        }

    val mockBeanLocalDataUnit: List<Pair<String, Any>>
        get() {
            val list = getList("mock_bean_local_data_unit") ?: return emptyList()
            return list.mapNotNull { element ->
                if (element is JsonArray && element.size == 2) {
                    val key = element[0].jsonPrimitive.contentOrNull ?: return@mapNotNull null
                    val primitive = element[1].jsonPrimitive
                    val value: Any =
                        primitive.intOrNull ?: primitive.booleanOrNull ?: primitive.contentOrNull
                        ?: return@mapNotNull null
                    key to value
                } else {
                    null
                }
            }
        }

    val allowedSkillTypes: List<String>
        get() {
            val list = getList("allowed_skill_types") ?: return emptyList()
            return list.mapNotNull { it.jsonPrimitive.contentOrNull }
        }
}
