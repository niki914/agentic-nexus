package com.niki914.breeno.a.mod

import com.niki914.breeno.h.core.runtime.Hook
import com.niki914.breeno.h.util.call
import com.niki914.breeno.h.util.findClass
import com.niki914.breeno.h.util.hookMethod
import com.niki914.breeno.h.util.xlog
import de.robv.android.xposed.callbacks.XC_LoadPackage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class BreenoChatHook(private val scope: CoroutineScope) : Hook {

    override val name: String = "BreenoChatHook"

    // 缓存 AIChatDataCenter 的单例
    private var dataCenterInstance: Any? = null
    // 缓存最新的 roomId
    private var currentRoomId: String = ""
    // 缓存全局对话轮数
    private var turnCount: Int = 0

    override fun onHook(lpparam: XC_LoadPackage.LoadPackageParam) {
        xlog("BreenoChatHook installing...")

        scope.launch {
            val roomIdManagerClass = BreenoConfigProvider.getRoomIdManagerClass() ?: return@launch
            val roomIdManagerMethodP = BreenoConfigProvider.getRoomIdManagerMethodP() ?: return@launch

            val viewBeanClassName = BreenoConfigProvider.getViewBeanClass() ?: return@launch
            val typeQuery = BreenoConfigProvider.getTypeQueryValue() ?: return@launch
            val typeAnswer = BreenoConfigProvider.getTypeAnswerValue() ?: return@launch

            val dataCenterClassName = BreenoConfigProvider.getDataCenterClass() ?: return@launch
            val dataCenterMethodR = BreenoConfigProvider.getDataCenterMethodR() ?: return@launch

            try {
                lpparam.hookMethod(
                    className = roomIdManagerClass,
                    methodName = roomIdManagerMethodP,
                    String::class.java, String::class.java,
                    after = { param ->
                        xlog("[BreenoChatHook] 新对话已创建! roomMode=${param.args[0]}, src=${param.args[1]}")
                        turnCount = 0
                    }
                )
            } catch (e: Throwable) {
                xlog("[BreenoChatHook] Hook RoomIdManager failed: ${e.message}")
            }

            val myMockFlagKey = BreenoConfigProvider.getMockBeanLocalDataUnit().find { it.second == true }?.first ?: "MY_MOCK_FLAG"
            val blockedSkillTypes = BreenoConfigProvider.getBlockedSkillTypes()
            try {
                val viewBeanClass = lpparam.findClass(viewBeanClassName)

                lpparam.hookMethod(
                    className = dataCenterClassName,
                    methodName = dataCenterMethodR,
                    viewBeanClass,
                    before = before@ { param ->
                        val bean = param.args[0]
                        if (dataCenterInstance == null) {
                            dataCenterInstance = param.thisObject
                            xlog("[BreenoChatHook] DataCenter 实例已缓存")
                        }

                        val chatType = bean.call<Int>("getChatType") ?: return@before
                        val roomId = bean.call<String>("getRoomId") ?: ""
                        currentRoomId = roomId

                        if (chatType == typeQuery) {
                            val query = bean.call<String>("getContent")
                            xlog("[BreenoChatHook] 捕获用户输入: $query (roomId=$roomId)")
                            
                            if (!query.isNullOrBlank()) {
                                turnCount++
                                triggerMockStream(roomId, viewBeanClass)
                            }
                            return@before
                        }

                        if (chatType == typeAnswer) {
                            val skillType = bean.call<String>("getSkillType")
                            val isMyMock = bean.call<Any>("getClientLocalData", myMockFlagKey) != null

                            if (isMyMock) {
                                xlog("[BreenoChatHook] 放行自定义注入卡片")
                                return@before
                            }

                            if (skillType != null && blockedSkillTypes.contains(skillType)) {
                                xlog("[BreenoChatHook] 拦截命中黑名单技能卡片: skillType=$skillType")
                                param.result = null
                                return@before
                            }

                            xlog("[BreenoChatHook] 放行未知/非黑名单技能卡片: skillType=$skillType")
                        }
                    }
                )
            } catch (e: Throwable) {
                xlog("[BreenoChatHook] Hook DataCenter failed: ${e.message}")
            }
        }
    }

    // MVP 模拟打字机流式响应
    private fun triggerMockStream(roomId: String, beanClass: Class<*>) {
        scope.launch(Dispatchers.IO) {
            delay(500)
            
            val mockData = listOf(
                "你好！", "我是由", " OpenHook ", "强力驱动的", "自定义", "大模型助手！\n\n", 
                "我拦截了官方的回复，", "并且使用了", "最优雅的", "领域模型注入方案。\n\n",
                "【调试信息】", "当前是你的第 $turnCount 次回答。"
            )
            val accumulator = StringBuilder()
            
            val dataCenterMethodR = BreenoConfigProvider.getDataCenterMethodR() ?: return@launch
            val dataCenterMethodG1 = BreenoConfigProvider.getDataCenterMethodG1() ?: return@launch
            
            val mockBeanMethodsUnit = BreenoConfigProvider.getMockBeanMethodsUnit()
            val mockBeanLocalDataUnit = BreenoConfigProvider.getMockBeanLocalDataUnit()
            val typeAnswer = BreenoConfigProvider.getTypeAnswerValue() ?: return@launch
            
            val mockBean = beanClass.newInstance()
            val uniqueRecordId = "mock_record_${roomId}_${System.currentTimeMillis()}"
            
            mockBean.call<Unit>("setChatType", typeAnswer)
            mockBean.call<Unit>("setRoomId", roomId)
            mockBean.call<Unit>("setRecordId", uniqueRecordId) 
            
            mockBeanMethodsUnit.forEach { (methodName, value) ->
                mockBean.call<Unit>(methodName, value)
            }
            mockBeanLocalDataUnit.forEach { (key, value) ->
                mockBean.call<Unit>("addClientLocalData", key, value)
            }

            mockData.forEachIndexed { index, segment ->
                accumulator.append(segment)
                val isFinal = (index == mockData.size - 1)
                val isFirst = (index == 0)

                mockBean.call<Unit>("setContent", accumulator.toString())
                mockBean.call<Unit>("setFinal", isFinal)
                mockBean.call<Unit>("setFirstSlice", isFirst)
                mockBeanMethodsUnit.filter { it.first == "setHasTextPrintAnimPlayed" }.forEach { (methodName, value) ->
                    mockBean.call<Unit>(methodName, value)
                }
                
                if (isFirst) {
                    dataCenterInstance?.call<Unit>(dataCenterMethodR, mockBean)
                } else {
                    dataCenterInstance?.call<Unit>(dataCenterMethodG1, mockBean, false)
                }
                
                delay(150)
            }
            mockBeanLocalDataUnit.forEach { (key, value) ->
                // 在最后恢复原本不该在推流中保留的标识
                if (key == "bean_client_key_hide_feedback_view" && value is Boolean) {
                    mockBean.call<Unit>("addClientLocalData", key, !value)
                }
            }
            dataCenterInstance?.call<Unit>(dataCenterMethodG1, mockBean, false)
        }
    }
}
