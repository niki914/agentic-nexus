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
    private var turnCount: Int = 0 // TODO 改为通知 chat 去 cancel 旧的以及重置聊天状态。这个值本身的意义不大，主要是用这个 hook 来做监听
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
        val roomIdManagerMethodP = BreenoConfigProvider.roomIdManagerMethodP ?: return

        lpparam.hookMethod(
            className = roomIdManagerClass,
            methodName = roomIdManagerMethodP,
            String::class.java, String::class.java,
            after = { param ->
                xlog("[$name] 新对话已创建! roomMode=${param.args[0]}, src=${param.args[1]}")
                turnCount = 0
            }
        )
    }

    override fun blockNativeSkill(lpparam: XC_LoadPackage.LoadPackageParam) {
        val dataCenterClassName = BreenoConfigProvider.dataCenterClass ?: return
        val dataCenterMethodR = BreenoConfigProvider.dataCenterMethodR ?: return
        val beanClass = viewBeanClass ?: return

        val typeAnswer = BreenoConfigProvider.typeAnswer ?: return
        val allowedSkillTypes = BreenoConfigProvider.allowedSkillTypes
        val myMockFlagKey =
            BreenoConfigProvider.mockBeanLocalDataUnit.find { it.second == true }?.first
                ?: "MY_MOCK_FLAG"

        lpparam.hookMethod(
            className = dataCenterClassName,
            methodName = dataCenterMethodR,
            beanClass,
            before = before@{ param ->
                val bean = param.args[0]
                val chatType = bean.call<Int>("getChatType") ?: return@before // TODO 是否上云

                if (chatType == typeAnswer) {
                    val skillType = bean.call<String>("getSkillType") // TODO 是否上云
                    val isMyMock = bean.call<Any>("getClientLocalData", myMockFlagKey) != null

                    if (isMyMock) {
                        xlog("[$name] 放行自定义注入卡片")
                        return@before
                    }

                    // 白名单逻辑：如果不包含在允许列表中，则默认拦截
                    if (skillType == null || !allowedSkillTypes.contains(skillType)) {
                        xlog("[$name] 拦截非白名单技能卡片: skillType=$skillType")
                        param.result = null
                        return@before
                    }

                    xlog("[$name] 放行白名单技能卡片: skillType=$skillType")
                }
            }
        )
    }

    override fun interceptInput(
        lpparam: XC_LoadPackage.LoadPackageParam,
        onInput: (String) -> Unit
    ) {
        val dataCenterClassName = BreenoConfigProvider.dataCenterClass ?: return
        val dataCenterMethodR = BreenoConfigProvider.dataCenterMethodR ?: return
        val beanClass = viewBeanClass ?: return

        val typeQuery = BreenoConfigProvider.typeQuery ?: return

        // 注意：这里由于也是 hook dataCenterMethodR，实际运行中会对同一个方法进行两次 hook (链式调用)。
        // 这是 Xposed 完全允许的，并且做到了业务逻辑解耦。
        lpparam.hookMethod(
            className = dataCenterClassName,
            methodName = dataCenterMethodR,
            beanClass,
            before = before@{ param ->
                val bean = param.args[0]
                if (dataCenterInstance == null) {
                    dataCenterInstance = param.thisObject
                    xlog("[$name] DataCenter 实例已缓存")
                }

                val chatType = bean.call<Int>("getChatType") ?: return@before
                val roomId = bean.call<String>("getRoomId") ?: ""
                currentRoomId = roomId

                if (chatType == typeQuery) {
                    val query = bean.call<String>("getContent")
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
        val dataCenterMethodR = BreenoConfigProvider.dataCenterMethodR ?: return
        val dataCenterMethodG1 = BreenoConfigProvider.dataCenterMethodG1 ?: return

        val mockBeanMethodsUnit = BreenoConfigProvider.mockBeanMethodsUnit
        val mockBeanLocalDataUnit = BreenoConfigProvider.mockBeanLocalDataUnit
        val typeAnswer = BreenoConfigProvider.typeAnswer ?: return

        if (isFirst || currentMockBean == null) {
            currentMockBean = beanClass.newInstance() // TODO 用 openhook 去看官方的 bean 实例会有哪些字段
            currentMockBean?.call<Unit>("setChatType", typeAnswer)
            currentMockBean?.call<Unit>("setRoomId", currentRoomId)
            currentMockBean?.call<Unit>("setRecordId", currentMockRecordId)

            mockBeanMethodsUnit.forEach { (methodName, value) ->
                currentMockBean?.call<Unit>(methodName, value)
            }
            mockBeanLocalDataUnit.forEach { (key, value) ->
                xlog("[$name] 正在注入 mockBeanLocalDataUnit: key=$key, value=$value")
                currentMockBean?.call<Unit>("addClientLocalData", key, value)
            }
        }

        val mockBean = currentMockBean ?: return

        mockBean.call<Unit>("setContent", chunk)
        mockBean.call<Unit>("setFinal", isFinal)
        mockBean.call<Unit>("setFirstSlice", isFirst)
        mockBeanMethodsUnit.filter { it.first == "setHasTextPrintAnimPlayed" }
            .forEach { (methodName, value) ->
                mockBean.call<Unit>(methodName, value)
            }

        if (isFirst) {
            dataCenterInstance?.call<Unit>(dataCenterMethodR, mockBean)
        } else {
            dataCenterInstance?.call<Unit>(dataCenterMethodG1, mockBean, false)
        }

        if (isFinal) {
            mockBeanLocalDataUnit.firstOrNull { (key, _) -> key == "bean_client_key_hide_feedback_view" }
                ?.let {
                    // 在最后恢复原本不该在推流中保留的标识
                    val bool = (it.second as? Boolean) ?: true
                    mockBean.call<Unit>("addClientLocalData", it.first, !bool)
                }
            dataCenterInstance?.call<Unit>(dataCenterMethodG1, mockBean, false)
            currentMockBean = null // 清空缓存
        }
    }
}
