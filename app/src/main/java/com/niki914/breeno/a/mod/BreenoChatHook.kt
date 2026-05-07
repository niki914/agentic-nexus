package com.niki914.breeno.a.mod

import com.niki914.breeno.h.util.call
import com.niki914.breeno.h.util.findClass
import com.niki914.breeno.h.util.hookMethod
import com.niki914.breeno.h.util.xlog
import de.robv.android.xposed.callbacks.XC_LoadPackage
import kotlinx.coroutines.CoroutineScope
import java.util.LinkedHashMap

class BreenoChatHook(scope: CoroutineScope) : AbstractAssistantHook(scope) {

    override val name: String = "BreenoChatHook"

    private var dataCenterInstance: Any? = null
    private var viewBeanClass: Class<*>? = null
    private val stateLock = Any()
    private val roomTurnStates = LinkedHashMap<String, ConversationTurnState>()
    private val renderSessions = LinkedHashMap<Long, StreamRenderSession>()

    private data class StreamRenderSession(
        val turnId: Long,
        val roomId: String,
        val recordId: String,
        var bean: Any? = null
    )

    override fun onBeforeInstallHooks(lpparam: XC_LoadPackage.LoadPackageParam) {
        xlog("[$name] installing...")
        BreenoConfigProvider.viewBeanClass?.let {
            viewBeanClass = lpparam.findClass(it)
        }
    }

    override fun onTurnStateChanged(state: ConversationTurnState) {
        synchronized(stateLock) {
            roomTurnStates[state.roomId] = state
            if (state.mode == TurnMode.NativeTakeover) {
                renderSessions.remove(state.turnId)
            }
        }
    }

    override fun onSessionReset(roomId: String) {
        val previousRoomId = turnState.roomId
        super.onSessionReset(roomId)
        synchronized(stateLock) {
            if (previousRoomId.isNotBlank()) {
                roomTurnStates.remove(previousRoomId)
            }
            renderSessions.entries.removeAll { (_, session) ->
                session.roomId == previousRoomId || (roomId.isNotBlank() && session.roomId == roomId)
            }
        }
    }

    override fun shouldTakeOver(query: String): Boolean {
        return BreenoConfigProvider.takeoverKeywords.any { keyword ->
            keyword.isNotBlank() && query.contains(keyword)
        }
    }

    override fun onTakeoverTriggered(turnId: Long, roomId: String, query: String) {
        ConversationJournal.appendHiddenSummary(
            HiddenTurnSummary(
                turnId = turnId,
                roomId = roomId,
                query = query,
                summary = "本轮用户请求已由系统助手处理：$query" // TODO Muti-Languages
            )
        )
    }

    override fun installSessionHooks(lpparam: XC_LoadPackage.LoadPackageParam) {
        hookRoomIdManager(lpparam)
    }

    override fun installResponseHooks(lpparam: XC_LoadPackage.LoadPackageParam) {
        hookNativeCardPolicy(lpparam)
        hookOperationFactory(lpparam)
    }

    override fun installInputHooks(
        lpparam: XC_LoadPackage.LoadPackageParam,
        onInput: (roomId: String, query: String) -> Unit
    ) {
        val dataCenterClassName = BreenoConfigProvider.dataCenterClass ?: return
        val dataCenterInsertMessageMethod =
            BreenoConfigProvider.dataCenterInsertMessageMethod ?: return
        val beanClass = viewBeanClass ?: return

        val typeQuery = BreenoConfigProvider.typeQuery ?: return
        val getChatTypeMethod = BreenoConfigProvider.beanGetChatTypeMethod
        val getRoomIdMethod = BreenoConfigProvider.beanGetRoomIdMethod
        val getContentMethod = BreenoConfigProvider.beanGetContentMethod

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

                if (chatType == typeQuery) {
                    val query = bean.call<String>(getContentMethod)
                    xlog("[$name] 捕获用户输入: $query (roomId=$roomId)")

                    if (!query.isNullOrBlank()) {
                        onInput(roomId, query)
                    }
                }
            }
        )
    }

    override fun dispatchQueryToLLM(turnId: Long, roomId: String, query: String) {
        LLMController.requestStream(query, scope) { chunk, isFirst, isFinal ->
            renderStreamCard(turnId, roomId, chunk, isFirst, isFinal)
        }
    }

    override fun renderStreamCard(
        turnId: Long,
        roomId: String,
        chunk: String,
        isFirst: Boolean,
        isFinal: Boolean
    ) {
        val activeTurn = resolveTurnState(roomId)
        if (activeTurn?.turnId != turnId || activeTurn.mode != TurnMode.InjectedLLM) {
            if (isFinal) {
                removeRenderSession(turnId)
            }
            xlog(
                "[$name] 丢弃非当前注入轮次的渲染片段: roomId=$roomId, turnId=$turnId, activeTurn=${activeTurn?.turnId}, mode=${activeTurn?.mode}"
            )
            return
        }

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

        val renderSession = obtainRenderSession(turnId, roomId)
        if (isFirst || renderSession.bean == null) {
            val bean = beanClass.newInstance()
            bean.call<Unit>(setChatTypeMethod, typeAnswer)
            bean.call<Unit>(setRoomIdMethod, roomId)
            bean.call<Unit>(setRecordIdMethod, renderSession.recordId)
            mockBeanMethodsUnit.forEach { (methodName, value) ->
                bean.call<Unit>(methodName, value)
            }
            mockBeanLocalDataUnit.forEach { (key, value) ->
                xlog("[$name] 正在注入 mockBeanLocalDataUnit: key=$key, value=$value")
                bean.call<Unit>(addClientLocalDataMethod, key, value)
            }
            BreenoFeedbackAssembler.attachIfNeeded(bean)
            renderSession.bean = bean
        }

        val mockBean = renderSession.bean ?: return

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
                    val bool = (it.second as? Boolean) ?: true
                    mockBean.call<Unit>(addClientLocalDataMethod, it.first, !bool)
                }
            dataCenterInstance?.call<Unit>(dataCenterUpdateMessageMethod, mockBean, false)
            removeRenderSession(turnId)
        }
    }

    private fun hookRoomIdManager(lpparam: XC_LoadPackage.LoadPackageParam) {
        val roomIdManagerClass = BreenoConfigProvider.roomIdManagerClass ?: return
        val createRoomMethod = BreenoConfigProvider.roomIdManagerCreateRoomMethod ?: return

        lpparam.hookMethod(
            className = roomIdManagerClass,
            methodName = createRoomMethod,
            String::class.java,
            String::class.java,
            after = { param ->
                val newRoomId = param.result as? String ?: ""
                xlog(
                    "[$name] 新对话已创建! roomMode=${param.args[0]}, src=${param.args[1]}, roomId=$newRoomId"
                )
                onSessionReset(newRoomId)
            }
        )
    }

    private fun hookNativeCardPolicy(lpparam: XC_LoadPackage.LoadPackageParam) {
        val dataCenterClassName = BreenoConfigProvider.dataCenterClass ?: return
        val dataCenterInsertMessageMethod =
            BreenoConfigProvider.dataCenterInsertMessageMethod ?: return
        val beanClass = viewBeanClass ?: return

        val typeAnswer = BreenoConfigProvider.typeAnswer ?: return
        val myMockFlagKey = BreenoConfigProvider.selfInjectedMockFlagKey
        val getChatTypeMethod = BreenoConfigProvider.beanGetChatTypeMethod
        val getRoomIdMethod = BreenoConfigProvider.beanGetRoomIdMethod
        val getClientLocalDataMethod = BreenoConfigProvider.beanGetClientLocalDataMethod

        lpparam.hookMethod(
            className = dataCenterClassName,
            methodName = dataCenterInsertMessageMethod,
            beanClass,
            before = before@{ param ->
                val bean = param.args[0]
                val chatType = bean.call<Int>(getChatTypeMethod) ?: return@before
                if (chatType != typeAnswer) {
                    return@before
                }

                val isMyMock = bean.call<Any>(getClientLocalDataMethod, myMockFlagKey) != null
                if (isMyMock) {
                    xlog("[$name] 放行自定义注入卡片")
                    return@before
                }

                val roomId = bean.call<String>(getRoomIdMethod)
                if (roomId.isNullOrBlank()) {
                    xlog("[$name] 回答卡片缺失 roomId，保守放行原生回答卡片，不回退全局 turnState")
                    return@before
                }

                when (resolveTurnState(roomId)?.mode) {
                    TurnMode.NativeTakeover -> {
                        xlog("[$name] takeover 模式，放行原生回答卡片: roomId=$roomId")
                    }

                    TurnMode.InjectedLLM -> {
                        xlog("[$name] 注入模式，拦截原生回答卡片: roomId=$roomId")
                        param.result = null
                    }

                    null -> {
                        xlog("[$name] 未命中 room 级轮次状态，保守放行原生回答卡片: roomId=$roomId")
                    }
                }
            }
        )
    }

    private fun hookOperationFactory(lpparam: XC_LoadPackage.LoadPackageParam) {
        val factoryClass = BreenoConfigProvider.operationFactoryClass ?: return
        val createMethod = BreenoConfigProvider.operationFactoryCreateMethod ?: return
        val directiveClassName = BreenoConfigProvider.directiveClass ?: return
        val doNothingOperationClass = BreenoConfigProvider.doNothingOperationClass ?: return
        val directiveClass = lpparam.findClass(directiveClassName)
        val getDirectiveRoomIdMethod = BreenoConfigProvider.directiveGetRoomIdMethod
        val cleanOperationClass = BreenoConfigProvider.cleanOperationClass

        lpparam.hookMethod(
            className = factoryClass,
            methodName = createMethod,
            directiveClass,
            after = after@{ param ->
                val directive = param.args[0]
                val roomId = directive.call<String>(getDirectiveRoomIdMethod)
                if (roomId.isNullOrBlank()) {
                    xlog("[$name] Operation directive 缺失 roomId，保守放行原生 Operation，不回退全局 turnState")
                    return@after
                }

                when (resolveTurnState(roomId)?.mode) {
                    TurnMode.NativeTakeover -> {
                        xlog("[$name] takeover 模式，放行原生 Operation: roomId=$roomId")
                        return@after
                    }

                    TurnMode.InjectedLLM -> Unit
                    null -> {
                        xlog("[$name] 未命中 room 级轮次状态，保守放行原生 Operation: roomId=$roomId")
                        return@after
                    }
                }

                val result = param.result ?: return@after
                val resultClass = result.javaClass
                val isCleanOperation = resultClass.name == cleanOperationClass ||
                    resultClass.simpleName == cleanOperationClass
                if (!isCleanOperation) {
                    return@after
                }

                val classLoader = resultClass.classLoader ?: javaClass.classLoader
                val replacement = Class.forName(doNothingOperationClass, false, classLoader)
                    .getDeclaredConstructor()
                    .newInstance()
                xlog("[$name] 注入模式，CleanOperation -> DoNothingOperation: roomId=$roomId")
                param.result = replacement
            }
        )
    }

    private fun resolveTurnState(roomId: String?): ConversationTurnState? = synchronized(stateLock) {
        roomId?.takeIf { it.isNotBlank() }?.let(roomTurnStates::get)
    }

    private fun obtainRenderSession(turnId: Long, roomId: String): StreamRenderSession =
        synchronized(stateLock) {
            renderSessions.getOrPut(turnId) {
                StreamRenderSession(
                    turnId = turnId,
                    roomId = roomId,
                    recordId = "mock_record_${roomId}_${turnId}"
                )
            }
        }

    private fun removeRenderSession(turnId: Long) {
        synchronized(stateLock) {
            renderSessions.remove(turnId)
        }
    }
}
