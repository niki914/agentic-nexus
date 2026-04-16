package ${BASE_PACKAGE}.a.mod

import androidx.annotation.Keep
import de.robv.android.xposed.callbacks.XC_LoadPackage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import ${BASE_PACKAGE}.h.IXposed
import ${BASE_PACKAGE}.h.core.runtime.Runtime
import ${BASE_PACKAGE}.h.core.runtime.RuntimeBootstrap
import ${BASE_PACKAGE}.h.util.ContextHook
import ${BASE_PACKAGE}.h.util.ContextProvider
import ${BASE_PACKAGE}.h.util.HookSideLoader
import ${BASE_PACKAGE}.h.util.KVProvider
import ${BASE_PACKAGE}.h.util.xlog

@Keep
class Entrance : IXposed() {
    companion object {
        const val SETTINGS_URL = "http://127.0.0.1:8787/settings"
        private val scope by lazy { CoroutineScope(Dispatchers.Default + SupervisorJob()) }
    }

    override fun getTarget() =
        Target.all(mainProcessOnly = true)

    override fun onLoad(params: XC_LoadPackage.LoadPackageParam) {
        if (params.packageName == "${BASE_PACKAGE}.app") {
            xlog("Filtered me myself: ${params.packageName}")
            return
        }
        HookSideLoader.load(scope, ContextHook(), params)
        scope.launch(Dispatchers.IO) {
            val ctx = ContextProvider.await()
            val settings = ctx.getLocalSettings()
            xlog("Entrance: context initialized: $ctx")
            if (settings == null) {
                xlog("LocalSettings is null: ${params.packageName}")
            } else {
                KVProvider.provide(settings.props)
                onSettingsFetched(params)
            }
        }
    }

    fun onSettingsFetched(params: XC_LoadPackage.LoadPackageParam) {
        RuntimeBootstrap.installIfNeeded(
            params,
            create = {
                Runtime(
                    scope = scope,
                    hooks = listOf(
                        ActivityHook()
                    )
                )
            }
        )
    }
}