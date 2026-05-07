package com.niki914.breeno.a.mod

import com.niki914.breeno.h.util.call
import com.niki914.breeno.h.util.findClass
import com.niki914.breeno.h.util.hookMethod
import com.niki914.breeno.h.util.xlog
import de.robv.android.xposed.callbacks.XC_LoadPackage
import kotlinx.coroutines.CoroutineScope

class BreenoChatHook(scope: CoroutineScope) : AbstractAssistantHook(scope) {

    override val name: String = "BreenoChatHook"

    // 缓存状态
    private var dataCenterInstance: Any? = null
    private var currentRoomId: String = ""
    private var turnCount: Int = 0 // 仅用于当前 WIP 阶段的调试展示，后续由 room hook 直接接管取消与清历史
    private var viewBeanClass: Class<*>? = null

    // 生成当次唯一的 record id，供大模型卡片流式推流使用
    private var currentMockRecordId: String = ""

    // 缓存当前推流的 Bean 实例
    private var currentMockBean: Any? = null

    override fun onHook(lpparam: XC_LoadPackage.LoadPackageParam) {
        xlog("[$name] installing...")
        // 预先缓存 ViewBeanClass
        val className = BreenoConfigProvider.viewBeanClass
        if (className != null) {
            try {
                viewBeanClass = lpparam.findClass(className)
            } catch (e: Throwable) {
                xlog("[$name] findClass viewBeanClass failed: ${e.message}")
            }
        }

        // 调用父类的生命周期
        super.onHook(lpparam)

        // 独有的 Hook: 监听对话房间的重置
        hookRoomIdManager(lpparam)
    }

    private fun hookRoomIdManager(lpparam: XC_LoadPackage.LoadPackageParam) {
        val roomIdManagerClass = BreenoConfigProvider.roomIdManagerClass ?: return
        val createRoomMethod = BreenoConfigProvider.roomIdManagerCreateRoomMethod ?: return

        lpparam.hookMethod(
            className = roomIdManagerClass,
            methodName = createRoomMethod,
            String::class.java, String::class.java,
            after = { param ->
                xlog("[$name] 新对话已创建! roomMode=${param.args[0]}, src=${param.args[1]}")
                turnCount = 0
            }
        )
    }

    override fun blockNativeSkill(lpparam: XC_LoadPackage.LoadPackageParam) {
        val dataCenterClassName = BreenoConfigProvider.dataCenterClass ?: return
        val dataCenterInsertMessageMethod =
            BreenoConfigProvider.dataCenterInsertMessageMethod ?: return
        val beanClass = viewBeanClass ?: return

        val typeAnswer = BreenoConfigProvider.typeAnswer ?: return
        val answerSkillPolicyMode = BreenoConfigProvider.answerSkillPolicyMode
        val answerSkillTypes = BreenoConfigProvider.answerSkillPolicyTypes
        val myMockFlagKey = BreenoConfigProvider.selfInjectedMockFlagKey
        val getChatTypeMethod = BreenoConfigProvider.beanGetChatTypeMethod
        val getSkillTypeMethod = BreenoConfigProvider.beanGetSkillTypeMethod
        val getClientLocalDataMethod = BreenoConfigProvider.beanGetClientLocalDataMethod

        lpparam.hookMethod(
            className = dataCenterClassName,
            methodName = dataCenterInsertMessageMethod,
            beanClass,
            before = before@{ param ->
                val bean = param.args[0]
                val chatType = bean.call<Int>(getChatTypeMethod) ?: return@before

                if (chatType == typeAnswer) {
                    val skillType = bean.call<String>(getSkillTypeMethod)
                    val isMyMock =
                        bean.call<Any>(getClientLocalDataMethod, myMockFlagKey) != null

                    if (isMyMock) {
                        xlog("[$name] 放行自定义注入卡片")
                        return@before
                    }

                    val inList = skillType != null && answerSkillTypes.contains(skillType)

                    when {
                        answerSkillPolicyMode == "blacklist" && inList -> {
                            xlog("[$name] 拦截黑名单技能卡片: skillType=$skillType")
                            param.result = null
                            return@before
                        }

                        answerSkillPolicyMode == "whitelist" && !inList -> {
                            xlog("[$name] 拦截非白名单技能卡片: skillType=$skillType")
                            param.result = null
                            return@before
                        }

                        else -> {
                            xlog("[$name] 放行技能卡片: skillType=$skillType")
                        }
                    }
                }
            }
        )
    }

    override fun interceptInput(
        lpparam: XC_LoadPackage.LoadPackageParam,
        onInput: (String) -> Unit
    ) {
        val dataCenterClassName = BreenoConfigProvider.dataCenterClass ?: return
        val dataCenterInsertMessageMethod =
            BreenoConfigProvider.dataCenterInsertMessageMethod ?: return
        val beanClass = viewBeanClass ?: return

        val typeQuery = BreenoConfigProvider.typeQuery ?: return
        val getChatTypeMethod = BreenoConfigProvider.beanGetChatTypeMethod
        val getRoomIdMethod = BreenoConfigProvider.beanGetRoomIdMethod
        val getContentMethod = BreenoConfigProvider.beanGetContentMethod

        // 注意：这里由于也是 hook insertMessage，实际运行中会对同一个方法进行两次 hook (链式调用)。
        // 这是 Xposed 完全允许的，并且做到了业务逻辑解耦。
        lpparam.hookMethod(
            className = dataCenterClassName,
            methodName = dataCenterInsertMessageMethod,
            beanClass,
            before = before@{ param ->
                val bean = param.args[0]
                if (dataCenterInstance == null) {
                    dataCenterInstance = param.thisObject
                    xlog("[$name] DataCenter 实例已缓存")
                }

                val chatType = bean.call<Int>(getChatTypeMethod) ?: return@before
                val roomId = bean.call<String>(getRoomIdMethod) ?: return@before
                currentRoomId = roomId

                if (chatType == typeQuery) {
                    val query = bean.call<String>(getContentMethod)
                    xlog("[$name] 捕获用户输入: $query (roomId=$roomId)")

                    if (!query.isNullOrBlank()) {
                        turnCount++
                        currentMockRecordId =
                            "mock_record_${roomId}_${System.currentTimeMillis()}"
                        onInput(query)
                    }
                }
            }
        )
    }

    override fun dispatchQueryToLLM(query: String) {
        // 调用统一的中控请求流式数据
        LLMController.requestStream(query, scope, turnCount) { chunk, isFirst, isFinal ->
            renderStreamCard(chunk, isFirst, isFinal)
        }
    }

    override fun renderStreamCard(chunk: String, isFirst: Boolean, isFinal: Boolean) {
        xlog("[$name] renderStreamCard called: isFirst=$isFirst, isFinal=$isFinal, chunk length=${chunk.length}")
        val beanClass = viewBeanClass ?: return
        val dataCenterInsertMessageMethod =
            BreenoConfigProvider.dataCenterInsertMessageMethod ?: return
        val dataCenterUpdateMessageMethod =
            BreenoConfigProvider.dataCenterUpdateMessageMethod ?: return

        val mockBeanMethodsUnit = BreenoConfigProvider.mockBeanMethodsUnit
        val mockBeanLocalDataUnit = BreenoConfigProvider.mockBeanLocalDataUnit
        val typeAnswer = BreenoConfigProvider.typeAnswer ?: return
        val hideFeedbackViewLocalDataKey = BreenoConfigProvider.hideFeedbackViewLocalDataKey
        val setChatTypeMethod = BreenoConfigProvider.beanSetChatTypeMethod
        val setRoomIdMethod = BreenoConfigProvider.beanSetRoomIdMethod
        val setRecordIdMethod = BreenoConfigProvider.beanSetRecordIdMethod
        val setContentMethod = BreenoConfigProvider.beanSetContentMethod
        val setFinalMethod = BreenoConfigProvider.beanSetFinalMethod
        val setFirstSliceMethod = BreenoConfigProvider.beanSetFirstSliceMethod
        val addClientLocalDataMethod = BreenoConfigProvider.beanAddClientLocalDataMethod

        if (isFirst || currentMockBean == null) {
            currentMockBean = beanClass.newInstance() // TODO 用 openhook 去看官方的 bean 实例会有哪些字段
            currentMockBean?.call<Unit>(setChatTypeMethod, typeAnswer)
            currentMockBean?.call<Unit>(setRoomIdMethod, currentRoomId)
            currentMockBean?.call<Unit>(setRecordIdMethod, currentMockRecordId)

            mockBeanMethodsUnit.forEach { (methodName, value) ->
                currentMockBean?.call<Unit>(methodName, value)
            }
            mockBeanLocalDataUnit.forEach { (key, value) ->
                xlog("[$name] 正在注入 mockBeanLocalDataUnit: key=$key, value=$value")
                currentMockBean?.call<Unit>(addClientLocalDataMethod, key, value)
            }
        }

        val mockBean = currentMockBean ?: return

        mockBean.call<Unit>(setContentMethod, chunk)
        mockBean.call<Unit>(setFinalMethod, isFinal)
        mockBean.call<Unit>(setFirstSliceMethod, isFirst)
        mockBeanMethodsUnit.filter { it.first == "setHasTextPrintAnimPlayed" }
            .forEach { (methodName, value) ->
                mockBean.call<Unit>(methodName, value)
            }

        if (isFirst) {
            dataCenterInstance?.call<Unit>(dataCenterInsertMessageMethod, mockBean)
        } else {
            dataCenterInstance?.call<Unit>(dataCenterUpdateMessageMethod, mockBean, false)
        }

        if (isFinal) {
            mockBeanLocalDataUnit.firstOrNull { (key, _) -> key == hideFeedbackViewLocalDataKey }
                ?.let {
                    // 在最后恢复原本不该在推流中保留的标识
                    val bool = (it.second as? Boolean) ?: true
                    mockBean.call<Unit>(addClientLocalDataMethod, it.first, !bool)
                }
            dataCenterInstance?.call<Unit>(dataCenterUpdateMessageMethod, mockBean, false)
            currentMockBean = null // 清空缓存
        }
    }
}
