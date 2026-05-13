package com.niki914.nexus.agentic.mod.feat.hyper

import com.niki914.nexus.agentic.mod.feat.BaseConfigProvider
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

// TODO 在架构没有扩张以前，争取把 hook 点都独立出来：如 xx_action:{class:x.x.x,method:x,sig:[xx.xx,xx.xx],business:{xxindex:0,...}}
object XiaoaiConfigProvider : BaseConfigProvider() {
    val operationManagerClass: String?
        get() = getString("classes.operation_manager")

    val setQueryInfoMethodName: String?
        get() = getString("accessors.operation_manager.set_query_info.method_name")

    val setQueryInfoMethodParams: List<String>?
        get() = getList("accessors.operation_manager.set_query_info.param_types")
            ?.mapNotNull { it.jsonPrimitive.contentOrNull }

    val setQueryInfoDialogIdArgIndex: Int?
        get() = getInt("accessors.operation_manager.set_query_info.dialog_id_arg_index")

    val setQueryInfoQueryArgIndex: Int?
        get() = getInt("accessors.operation_manager.set_query_info.query_arg_index")
}