package a0.a0.a0.a0.a0.a0

import androidx.annotation.Keep
import com.niki914.nexus.agentic.mod.HookLocalSettings
import com.niki914.nexus.agentic.mod.XService
import com.niki914.nexus.agentic.mod.feat.hyper.XiaoaiChatHook
import com.niki914.nexus.agentic.mod.feat.oppo.BreenoChatHook
import com.niki914.nexus.h.IXposed
import com.niki914.nexus.h.core.runtime.Hook
import com.niki914.nexus.h.core.runtime.Runtime
import com.niki914.nexus.h.core.runtime.RuntimeBootstrap
import com.niki914.nexus.h.util.ContextHook
import com.niki914.nexus.h.util.ContextProvider
import com.niki914.nexus.h.util.HookSideLoader
import com.niki914.nexus.h.util.KVProvider
import com.niki914.nexus.h.util.xlog
import com.niki914.nexus.ipc.XValues
import de.robv.android.xposed.callbacks.XC_LoadPackage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@Keep
class Entrance : IXposed() {
    companion object {
        private val scope by lazy { CoroutineScope(Dispatchers.Default + SupervisorJob()) }
    }

    override fun getTarget() =
        Target.filter(*XValues.appList.toTypedArray())

    override fun onLoad(params: XC_LoadPackage.LoadPackageParam) {
        HookSideLoader.load(scope, ContextHook(), params)
        scope.launch(Dispatchers.IO) {
            val ctx = ContextProvider.await()
            xlog("Entrance: context initialized: $ctx")

            HookLocalSettings.update(ctx)
            val webSettings = XService.getWebSettings(ctx) // TODO XService 与 KVProvider 职责有点重合，考虑重构
            val targetPkg = webSettings.packageName.takeIf { it.isNotBlank() }
            val configObj = webSettings.config

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
        val hookInstance: Hook? = when {
            targetPkg != params.packageName -> null
            params.packageName == "com.heytap.speechassist" -> BreenoChatHook(scope)
            params.packageName == "com.miui.voiceassist" -> XiaoaiChatHook(scope)
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
