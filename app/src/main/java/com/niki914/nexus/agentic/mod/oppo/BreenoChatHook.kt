package com.niki914.nexus.agentic.mod.oppo

import com.niki914.nexus.agentic.mod.AbstractAssistantHook
import com.niki914.nexus.agentic.mod.BreenoConfigProvider
import com.niki914.nexus.agentic.mod.BreenoFeedbackAssembler
import com.niki914.nexus.agentic.mod.ConversationJournal
import com.niki914.nexus.agentic.mod.ConversationTurnState
import com.niki914.nexus.agentic.mod.HiddenTurnSummary
import com.niki914.nexus.agentic.mod.HookLocalSettings
import com.niki914.nexus.agentic.mod.LLMController
import com.niki914.nexus.agentic.mod.TurnMode
import com.niki914.nexus.agentic.mod.oppo.subhooks.InputHook
import com.niki914.nexus.agentic.mod.oppo.subhooks.NativeCardPolicyHook
import com.niki914.nexus.agentic.mod.oppo.subhooks.OperationFactoryHook
import com.niki914.nexus.agentic.mod.oppo.subhooks.RoomIdManagerHook
import com.niki914.nexus.h.util.call
import com.niki914.nexus.h.util.findClass
import com.niki914.nexus.h.util.xlog
import de.robv.android.xposed.callbacks.XC_LoadPackage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.LinkedHashMap

class BreenoChatHook(scope: CoroutineScope) : AbstractAssistantHook(scope) {

    override val name: String = "BreenoChatHook"

    private var dataCenterInstance: Any? = null
    private var viewBeanClass: Class<*>? = null
    private val stateMutex = Mutex()
    private val roomTurnStates = LinkedHashMap<String, ConversationTurnState>()
    private val renderSessions = LinkedHashMap<Long, StreamRenderSession>()
    @Volatile
    private var roomTurnStateSnapshot: Map<String, ConversationTurnState> = emptyMap()

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

    override suspend fun onTurnStateChanged(state: ConversationTurnState) {
        stateMutex.withLock {
            roomTurnStates[state.roomId] = state
            if (state.mode == TurnMode.NativeTakeover) {
                renderSessions.remove(state.turnId)
            }
            roomTurnStateSnapshot = roomTurnStates.toMap()
        }
    }

    override suspend fun onSessionReset(roomId: String) {
        val previousRoomId = turnState.roomId
        super.onSessionReset(roomId)
        LLMController.resetConversation()
        stateMutex.withLock {
            if (previousRoomId.isNotBlank()) {
                roomTurnStates.remove(previousRoomId)
            }
            renderSessions.entries.removeAll { (_, session) ->
                session.roomId == previousRoomId || (roomId.isNotBlank() && session.roomId == roomId)
            }
            roomTurnStateSnapshot = roomTurnStates.toMap()
        }
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
        RoomIdManagerHook(
            onSessionReset = { roomId ->
                scope.launch {
                    onSessionReset(roomId)
                }
            }
        ).onHook(lpparam)
    }

    override fun installResponseHooks(lpparam: XC_LoadPackage.LoadPackageParam) {
        NativeCardPolicyHook(
            viewBeanClassProvider = { viewBeanClass },
            resolveTurnState = { roomId -> resolveTurnState(roomId) }
        ).onHook(lpparam)

        OperationFactoryHook(
            resolveTurnState = { roomId -> resolveTurnState(roomId) }
        ).onHook(lpparam)
    }

    override fun installInputHooks(
        lpparam: XC_LoadPackage.LoadPackageParam,
        onInput: (roomId: String, query: String) -> Unit
    ) {
        InputHook(
            viewBeanClassProvider = { viewBeanClass },
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
        LLMController.send(query, scope) { chunk, pos ->
            val isFirst = pos == LLMController.Pos.First
            val isFinal = pos == LLMController.Pos.Final
            renderStreamCard(turnId, roomId, chunk, isFirst, isFinal)
        }
    }

    override suspend fun renderStreamCard(
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
        val hideFeedbackViewLocalDataKey = BreenoConfigProvider.hideFeedbackViewLocalDataKey ?: return
        val setChatTypeMethod = BreenoConfigProvider.beanSetChatTypeMethod ?: return
        val setRoomIdMethod = BreenoConfigProvider.beanSetRoomIdMethod ?: return
        val setRecordIdMethod = BreenoConfigProvider.beanSetRecordIdMethod ?: return
        val setContentMethod = BreenoConfigProvider.beanSetContentMethod ?: return
        val setFinalMethod = BreenoConfigProvider.beanSetFinalMethod ?: return
        val setFirstSliceMethod = BreenoConfigProvider.beanSetFirstSliceMethod ?: return
        val addClientLocalDataMethod = BreenoConfigProvider.beanAddClientLocalDataMethod ?: return

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

    private fun resolveTurnState(roomId: String?): ConversationTurnState? =
        roomId?.takeIf { it.isNotBlank() }?.let(roomTurnStateSnapshot::get)

    private suspend fun obtainRenderSession(turnId: Long, roomId: String): StreamRenderSession =
        stateMutex.withLock {
            renderSessions.getOrPut(turnId) {
                StreamRenderSession(
                    turnId = turnId,
                    roomId = roomId,
                    recordId = "mock_record_${roomId}_${turnId}"
                )
            }
        }

    private suspend fun removeRenderSession(turnId: Long) {
        stateMutex.withLock {
            renderSessions.remove(turnId)
        }
    }
}
