package a0.a0.a0.a0.a0.a0

import com.niki914.nexus.agentic.mod.HookLocalSettings
import com.niki914.nexus.agentic.mod.feat.hyper.XiaoaiChatHook
import com.niki914.nexus.agentic.mod.feat.oppo.BreenoChatHook
import com.niki914.nexus.agentic.repo.XRepo
import com.niki914.nexus.agentic.runtime.createAppRuntimeBridge
import com.niki914.nexus.agentic.runtime.settings.RuntimeEnvironment
import com.niki914.nexus.h.IXposed
import com.niki914.nexus.h.core.runtime.Hook
import com.niki914.nexus.h.core.runtime.Runtime
import com.niki914.nexus.h.core.runtime.RuntimeBootstrap
import com.niki914.nexus.h.util.ActivityHook
import com.niki914.nexus.h.util.ContextHook
import com.niki914.nexus.h.util.ContextProvider
import com.niki914.nexus.h.util.HookSideLoader
import com.niki914.nexus.h.util.xlog
import com.niki914.nexus.ipc.HostApp
import com.niki914.nexus.ipc.XValues
import de.robv.android.xposed.callbacks.XC_LoadPackage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class Entrance : IXposed() {
    companion object {
        private val scope by lazy { CoroutineScope(Dispatchers.Default + SupervisorJob()) }
    }

    override fun getTarget() =
        Target.filter(*XValues.appList.toTypedArray())

    override fun onLoad(params: XC_LoadPackage.LoadPackageParam) {
        HookSideLoader.load(scope, ContextHook(), params)
        HookSideLoader.load(scope, ActivityHook(), params)
        scope.launch(Dispatchers.IO) {
            val ctx = ContextProvider.await()
            XRepo.init(ctx)
            RuntimeEnvironment.install(createAppRuntimeBridge())
            xlog("Entrance: context initialized: $ctx")

            HookLocalSettings.update(ctx)
            val webSettingsResult = XRepo.web.await()
            val targetPkg = params.packageName
            val configObj = webSettingsResult.configOrNull()

            if (configObj != null) {
                onSettingsFetched(params, targetPkg)
            } else {
                xlog("No mock config found for package: ${params.packageName}")
            }
        }
    }

    private fun onSettingsFetched(params: XC_LoadPackage.LoadPackageParam, targetPkg: String?) {
        // 根据 targetPkg 进行映射和 Hook 路由
        val hostApp = HostApp.fromPackageName(params.packageName)
        val hookInstance: Hook? = when {
            targetPkg != params.packageName -> null
            hostApp == HostApp.Breeno -> BreenoChatHook(scope)
            hostApp == HostApp.XiaoAi -> XiaoaiChatHook(scope)
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
