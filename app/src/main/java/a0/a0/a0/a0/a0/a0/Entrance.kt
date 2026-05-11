package a0.a0.a0.a0.a0.a0

import androidx.annotation.Keep
import com.niki914.nexus.agentic.mod.getCachedSettings
import com.niki914.nexus.agentic.mod.oppo.BreenoChatHook
import com.niki914.nexus.h.IXposed
import com.niki914.nexus.h.core.runtime.Hook
import com.niki914.nexus.h.core.runtime.Runtime
import com.niki914.nexus.h.core.runtime.RuntimeBootstrap
import com.niki914.nexus.h.util.ContextHook
import com.niki914.nexus.h.util.ContextProvider
import com.niki914.nexus.h.util.HookSideLoader
import com.niki914.nexus.h.util.KVProvider
import com.niki914.nexus.h.util.xlog
import de.robv.android.xposed.callbacks.XC_LoadPackage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@Keep
class Entrance : IXposed() {
    companion object {
        private val scope by lazy { CoroutineScope(Dispatchers.Default + SupervisorJob()) }

        // LLM Configuration
        const val LLM_BASE_URL = "http://127.0.0.1:1234/v1/chat/completions" // TODO Local Settings
        const val LLM_API_KEY = "sk-qwerqwer"
        const val LLM_MODEL_NAME = "google/gemma-4-e4b"
    }

    override fun getTarget() =
        Target.filter("com.heytap.speechassist")

    override fun onLoad(params: XC_LoadPackage.LoadPackageParam) {
        HookSideLoader.load(scope, ContextHook(), params)
        scope.launch(Dispatchers.IO) {
            val ctx = ContextProvider.await()
            xlog("Entrance: context initialized: $ctx")

            val webSettings = ctx.getCachedSettings()
            val mockJsonObj = webSettings.props
            val targetPkg = mockJsonObj["package_name"]?.jsonPrimitive?.contentOrNull
            val configObj = mockJsonObj["config"]?.jsonObject

            if (configObj != null) {
                KVProvider.provide(configObj)
                onSettingsFetched(params, targetPkg)
            } else {
                xlog("No mock config found for package: ${params.packageName}")
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