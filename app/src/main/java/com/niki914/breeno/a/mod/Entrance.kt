package com.niki914.breeno.a.mod

import androidx.annotation.Keep
import de.robv.android.xposed.callbacks.XC_LoadPackage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import com.niki914.breeno.h.IXposed
import com.niki914.breeno.h.core.runtime.Runtime
import com.niki914.breeno.h.core.runtime.RuntimeBootstrap
import com.niki914.breeno.h.util.ContextHook
import com.niki914.breeno.h.util.ContextProvider
import com.niki914.breeno.h.util.HookSideLoader
import com.niki914.breeno.h.util.KVProvider
import com.niki914.breeno.h.util.xlog

@Keep
class Entrance : IXposed() {
    companion object {
        const val SETTINGS_URL = "http://127.0.0.1:8787/settings"
        private val scope by lazy { CoroutineScope(Dispatchers.Default + SupervisorJob()) }
    }

    override fun getTarget() =
        Target.all(mainProcessOnly = true)

    override fun onLoad(params: XC_LoadPackage.LoadPackageParam) {
        if (params.packageName == "com.niki914.breeno.app") {
            xlog("Filtered me myself: ${params.packageName}")
            return
        }
        HookSideLoader.load(scope, ContextHook(), params)
        scope.launch(Dispatchers.IO) {
            val ctx = ContextProvider.await()
            val settings = ctx.getLocalSettings()
            xlog("Entrance: context initialized: $ctx")
            if (settings == null) {
                xlog("LocalSettings is null: ${params.packageName}, using mock json")
                val mockJson = """
                    {
                        "room_id_manager_class": "com.heytap.speechassist.aichat.AIChatRoomIdManager",
                        "room_id_manager_method_p": "p",
                        "view_bean_class": "com.heytap.speechassist.aichat.bean.AIChatViewBean",
                        "type_query_value": 1,
                        "type_answer_value": 2,
                        "data_center_class": "com.heytap.speechassist.aichat.AIChatDataCenter",
                        "data_center_method_r": "r",
                        "data_center_method_g1": "g1",
                        "mock_bean_methods_unit": [
                            ["setSkillType", "MyAI.StreamTextCard"],
                            ["setMsPerChar", 50],
                            ["setHasTextPrintAnimPlayed", false]
                        ],
                        "mock_bean_local_data_unit": [
                            ["MY_MOCK_FLAG", true],
                            ["bean_client_key_hide_feedback_view", true]
                        ],
                        "blocked_skill_types": [
                            "MyAI.StreamTextCard",
                            "MyAI.TextCard",
                            "MyAI.RichTextCard"
                        ]
                    }
                """.trimIndent()
                KVProvider.provide(mockJson.toXSettings().props)
            } else {
                KVProvider.provide(settings.props)
            }
            onSettingsFetched(params)
        }
    }

    fun onSettingsFetched(params: XC_LoadPackage.LoadPackageParam) {
        RuntimeBootstrap.installIfNeeded(
            params,
            create = {
                Runtime(
                    scope = scope,
                    hooks = listOf(
                        ActivityHook(),
                        BreenoChatHook(scope)
                    )
                )
            }
        )
    }
}