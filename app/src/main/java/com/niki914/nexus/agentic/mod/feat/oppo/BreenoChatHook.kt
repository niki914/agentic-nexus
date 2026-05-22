package com.niki914.nexus.agentic.mod.feat.oppo

import com.niki914.nexus.agentic.mod.feat.AbstractAssistantHook
import com.niki914.nexus.agentic.chat.ConversationJournal
import com.niki914.nexus.agentic.chat.ConversationTurnState
import com.niki914.nexus.agentic.chat.HiddenTurnSummary
import com.niki914.nexus.agentic.mod.HookLocalSettings
import com.niki914.nexus.agentic.chat.LLMController
import com.niki914.nexus.agentic.chat.TurnMode
import com.niki914.nexus.agentic.chat.fullText
import com.niki914.nexus.agentic.chat.isFinal
import com.niki914.nexus.agentic.chat.isFirst
import com.niki914.nexus.agentic.mod.feat.oppo.subhooks.CaptureInputHook
import com.niki914.nexus.agentic.mod.feat.oppo.subhooks.BlockNativeCardHook
import com.niki914.nexus.agentic.mod.feat.oppo.subhooks.SuppressCleanupHook
import com.niki914.nexus.agentic.mod.feat.oppo.subhooks.ResetConversationSignalHook
import com.niki914.nexus.h.util.call
import com.niki914.nexus.h.util.findClass
import com.niki914.nexus.h.util.xlog
import de.robv.android.xposed.callbacks.XC_LoadPackage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Breeno 宿主主 Hook，走回答卡片层做注入：复用同一张卡片并持续用累计文本全量刷新内容。 */
class BreenoChatHook(scope: CoroutineScope) : AbstractAssistantHook(scope) {

    override val name: String = "BreenoChatHook"

    private var dataCenterInstance: Any? = null
    private var viewBeanClass: Class<*>? = null
    private val renderSessionMutex = Mutex()
    private var currentRenderSession: BreenoRenderSession? = null

    private data class BreenoRenderSession(
        val turnId: Long,
        val roomId: String,
        val recordId: String,
        var bean: Any? = null
    )

    override fun onBeforeInstallHooks(lpparam: XC_LoadPackage.LoadPackageParam) {
        xlog("[$name] installing...")
        viewBeanClass = lpparam.findClass(BreenoConfigProvider.RenderCard.viewBeanClass)
    }

    override suspend fun onTurnStateChanged(state: ConversationTurnState) {
        if (state.mode == TurnMode.NativeTakeover) {
            clearRenderSession(state.turnId)
        }
    }

    override suspend fun onSessionReset(roomId: String) {
        val previousRoomId = turnState.roomId
        super.onSessionReset(roomId)
        LLMController.resetConversation()
        clearRenderSessionByRoom(previousRoomId, roomId)
    }

    override fun shouldTakeOver(query: String): Boolean {
        return HookLocalSettings.current().takeoverKeywords.any { keyword ->
            keyword.isNotBlank() && query.contains(keyword)
        }
    }

    override suspend fun onTakeoverTriggered(turnId: Long, roomId: String, query: String) {
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
        ResetConversationSignalHook(
            onSessionReset = { roomId ->
                scope.launch {
                    onSessionReset(roomId)
                }
            }
        ).onHook(lpparam)
        installFloatScreenDetachHooks(
            lpparam = lpparam,
            detachTarget = BreenoConfigProvider.FloatScreenDetach.detachTarget,
            resumeTarget = BreenoConfigProvider.FloatScreenDetach.resumeTarget
        )
    }

    override fun installResponseHooks(lpparam: XC_LoadPackage.LoadPackageParam) {
        BlockNativeCardHook(
            resolveTurnState = { roomId -> resolveTurnState(roomId) },
            selfInjectedFlagKey = BreenoConfigProvider.CaptureResponseTarget.selfInjectedFlagKey
        ).onHook(lpparam)

        SuppressCleanupHook(
            resolveTurnState = { turnState }
        ).onHook(lpparam)
    }

    override fun installInputHooks(
        lpparam: XC_LoadPackage.LoadPackageParam,
        onInput: (roomId: String, query: String) -> Unit
    ) {
        CaptureInputHook(
            onDataCenterInstanceResolved = { instance ->
                if (dataCenterInstance == null) {
                    dataCenterInstance = instance
                    xlog("[$name] DataCenter 实例已缓存")
                }
            },
            onInput = onInput
        ).onHook(lpparam)
    }

    override suspend fun dispatchQueryToLLM(turnId: Long, roomId: String, query: String) {
        LLMController.stream(query).collect { event ->
            renderStreamCard(turnId, roomId, event.fullText, event.isFirst, event.isFinal)
        }
    }

    override suspend fun renderStreamCard(
        turnId: Long,
        roomId: String,
        fullText: String,
        isFirst: Boolean,
        isFinal: Boolean
    ) {
        val activeTurn = resolveTurnState(roomId)
        if (activeTurn?.turnId != turnId || activeTurn.mode != TurnMode.InjectedLLM) {
            if (isFinal) {
                clearRenderSession(turnId)
            }
            xlog(
                "[$name] 丢弃非当前注入轮次的渲染片段: roomId=$roomId, turnId=$turnId, activeTurn=${activeTurn?.turnId}, mode=${activeTurn?.mode}"
            )
            return
        }

        val beanClass = viewBeanClass ?: return
        val dataCenterInsertMessageMethod = BreenoConfigProvider.RenderCard.dataCenterInsertMessageMethod
        val dataCenterUpdateMessageMethod = BreenoConfigProvider.RenderCard.dataCenterUpdateMessageMethod

        val mockBeanMethodsUnit = BreenoConfigProvider.RenderCard.mockBeanMethodsUnit
        val mockBeanLocalDataUnit = BreenoConfigProvider.RenderCard.mockBeanLocalDataUnit
        val typeAnswer = BreenoConfigProvider.RenderCard.chatTypeAnswer
        val hideFeedbackViewLocalDataKey = BreenoConfigProvider.RenderCard.hideFeedbackViewLocalDataKey
        val setChatTypeMethod = BreenoConfigProvider.RenderCard.beanSetChatTypeMethod
        val setRoomIdMethod = BreenoConfigProvider.RenderCard.beanSetRoomIdMethod
        val setRecordIdMethod = BreenoConfigProvider.RenderCard.beanSetRecordIdMethod
        val setContentMethod = BreenoConfigProvider.RenderCard.beanSetContentMethod
        val setFinalMethod = BreenoConfigProvider.RenderCard.beanSetFinalMethod
        val setFirstSliceMethod = BreenoConfigProvider.RenderCard.beanSetFirstSliceMethod
        val addClientLocalDataMethod = BreenoConfigProvider.RenderCard.beanAddClientLocalDataMethod

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

        mockBean.call<Unit>(setContentMethod, fullText)
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
            clearRenderSession(turnId)
        }
    }

    private fun resolveTurnState(roomId: String?): ConversationTurnState? =
        roomId
            ?.takeIf { it.isNotBlank() }
            ?.takeIf { it == turnState.roomId }
            ?.let { turnState }

    private suspend fun obtainRenderSession(turnId: Long, roomId: String): BreenoRenderSession =
        renderSessionMutex.withLock {
            currentRenderSession?.takeIf { it.turnId == turnId } ?: BreenoRenderSession(
                turnId = turnId,
                roomId = roomId,
                recordId = "mock_record_${roomId}_${turnId}"
            ).also { currentRenderSession = it }
        }

    private suspend fun clearRenderSession(turnId: Long) {
        renderSessionMutex.withLock {
            if (currentRenderSession?.turnId == turnId) {
                currentRenderSession = null
            }
        }
    }

    private suspend fun clearRenderSessionByRoom(previousRoomId: String, roomId: String) {
        renderSessionMutex.withLock {
            val session = currentRenderSession ?: return@withLock
            if (session.roomId == previousRoomId || (roomId.isNotBlank() && session.roomId == roomId)) {
                currentRenderSession = null
            }
        }
    }
}
