package com.niki914.breeno.a.mod.oppo.subhooks

import com.niki914.breeno.a.mod.BreenoConfigProvider
import com.niki914.breeno.h.core.runtime.Hook
import com.niki914.breeno.h.util.hookMethod
import com.niki914.breeno.h.util.resolveParamTypes
import com.niki914.breeno.h.util.xlog
import de.robv.android.xposed.callbacks.XC_LoadPackage

class RoomIdManagerHook(
    private val onSessionReset: (String) -> Unit
) : Hook {
    override val name: String = "RoomIdManagerHook"

    override fun onHook(lpparam: XC_LoadPackage.LoadPackageParam) {
        val roomIdManagerClass = BreenoConfigProvider.roomIdManagerClass ?: return
        val createRoomMethod = BreenoConfigProvider.roomIdManagerCreateRoomMethod ?: return
        val createRoomMethodParams = BreenoConfigProvider.roomIdManagerCreateRoomMethodParams

        val params = if (createRoomMethodParams != null) {
            resolveParamTypes(createRoomMethodParams, lpparam) ?: return
        } else {
            arrayOf(String::class.java, String::class.java)
        }

        lpparam.hookMethod(
            className = roomIdManagerClass,
            methodName = createRoomMethod,
            *params,
            after = { param ->
                val newRoomId = param.result as? String ?: ""
                xlog(
                    "[$name] 新对话已创建! roomMode=${param.args[0]}, src=${param.args[1]}, roomId=$newRoomId"
                )
                onSessionReset(newRoomId)
            }
        )
    }
}
