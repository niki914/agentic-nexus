package com.niki914.breeno.a.mod

import androidx.annotation.Keep
import com.niki914.breeno.h.IXposed
import com.niki914.breeno.h.core.runtime.Hook
import com.niki914.breeno.h.core.runtime.Runtime
import com.niki914.breeno.h.core.runtime.RuntimeBootstrap
import com.niki914.breeno.h.util.ContextHook
import com.niki914.breeno.h.util.ContextProvider
import com.niki914.breeno.h.util.HookSideLoader
import com.niki914.breeno.h.util.KVProvider
import com.niki914.breeno.h.util.xlog
import de.robv.android.xposed.callbacks.XC_LoadPackage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@Keep
class Entrance : IXposed() {
    companion object {
        const val SETTINGS_URL = "http://127.0.0.1:8787/settings"
        private val scope by lazy { CoroutineScope(Dispatchers.Default + SupervisorJob()) }
    }

    override fun getTarget() =
        Target.filter("com.heytap.speechassist")

    override fun onLoad(params: XC_LoadPackage.LoadPackageParam) {
        HookSideLoader.load(scope, ContextHook(), params)
        scope.launch(Dispatchers.IO) {
            val ctx = ContextProvider.await()
            xlog("Entrance: context initialized: $ctx")

            // 模拟从服务端获取带有路由包名和 Config 的 JSON
            // 实际上此处你可以传入 ctx 获取到的 versionCode，这里伪造为 127400
            val mockJsonStr = getConfig(params.packageName, 127400)

            try {
                val jsonObj = Json.decodeFromString<JsonObject>(mockJsonStr)
                val targetPkg = jsonObj["package_name"]?.jsonPrimitive?.contentOrNull
                val configObj = jsonObj["config"]?.jsonObject

                if (configObj != null) {
                    KVProvider.provide(configObj)
                    onSettingsFetched(params, targetPkg)
                } else {
                    xlog("No config found for package: ${params.packageName}")
                }
            } catch (e: Throwable) {
                xlog("Failed to parse config: ${e.message}")
            }
        }
    }

    private fun onSettingsFetched(params: XC_LoadPackage.LoadPackageParam, targetPkg: String?) {
        // 根据 targetPkg 进行映射和 Hook 路由
        val hookInstance: Hook? = when (targetPkg) {
            "com.heytap.speechassist" -> BreenoChatHook(scope)
            // "com.miui.voiceassist" -> XiaoAiChatHook(scope) // 未来扩展
            else -> null
        }

        if (hookInstance == null) {
            xlog("No Hook implementation found for package: $targetPkg")
            return
        }

        RuntimeBootstrap.installIfNeeded(
            params,
            create = {
                Runtime(
                    scope = scope,
                    hooks = listOf(
                        hookInstance
                    )
                )
            }
        )
    }
}
