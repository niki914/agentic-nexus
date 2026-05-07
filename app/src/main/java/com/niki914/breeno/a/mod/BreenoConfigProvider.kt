package com.niki914.breeno.a.mod

import com.niki914.breeno.h.util.KVProvider
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.booleanOrNull

object BreenoConfigProvider {
    suspend fun getRoomIdManagerClass(): String? = KVProvider.getString("room_id_manager_class")
    suspend fun getRoomIdManagerMethodP(): String? = KVProvider.getString("room_id_manager_method_p")
    
    suspend fun getViewBeanClass(): String? = KVProvider.getString("view_bean_class")
    suspend fun getTypeQueryValue(): Int? = KVProvider.getInt("type_query_value")
    suspend fun getTypeAnswerValue(): Int? = KVProvider.getInt("type_answer_value")
    
    suspend fun getDataCenterClass(): String? = KVProvider.getString("data_center_class")
    suspend fun getDataCenterMethodR(): String? = KVProvider.getString("data_center_method_r")
    suspend fun getDataCenterMethodG1(): String? = KVProvider.getString("data_center_method_g1")
    
    suspend fun getMockBeanMethodsUnit(): List<Pair<String, Any>> {
        val list = KVProvider.getList("mock_bean_methods_unit") ?: return emptyList()
        return list.mapNotNull { element ->
            if (element is JsonArray && element.size == 2) {
                val methodName = element[0].jsonPrimitive.contentOrNull ?: return@mapNotNull null
                val primitive = element[1].jsonPrimitive
                val value: Any = primitive.intOrNull ?: primitive.booleanOrNull ?: primitive.contentOrNull ?: return@mapNotNull null
                methodName to value
            } else {
                null
            }
        }
    }
    
    suspend fun getMockBeanLocalDataUnit(): List<Pair<String, Any>> {
        val list = KVProvider.getList("mock_bean_local_data_unit") ?: return emptyList()
        return list.mapNotNull { element ->
            if (element is JsonArray && element.size == 2) {
                val key = element[0].jsonPrimitive.contentOrNull ?: return@mapNotNull null
                val primitive = element[1].jsonPrimitive
                val value: Any = primitive.intOrNull ?: primitive.booleanOrNull ?: primitive.contentOrNull ?: return@mapNotNull null
                key to value
            } else {
                null
            }
        }
    }
    
    suspend fun getBlockedSkillTypes(): List<String> {
        val list = KVProvider.getList("blocked_skill_types") ?: return emptyList()
        return list.mapNotNull { it.jsonPrimitive.contentOrNull }
    }
}