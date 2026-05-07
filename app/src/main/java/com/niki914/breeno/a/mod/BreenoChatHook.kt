package com.niki914.breeno.a.mod

import com.niki914.breeno.h.core.runtime.Hook
import com.niki914.breeno.h.util.call
import com.niki914.breeno.h.util.findClass
import com.niki914.breeno.h.util.hookMethod
import com.niki914.breeno.h.util.xlog
import de.robv.android.xposed.XposedHelpers
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

        // 1. 生命周期管控：监听新对话创建
        lpparam.hookMethod(
            className = "com.heytap.speechassist.aichat.AIChatRoomIdManager",
            methodName = "p",
            String::class.java, String::class.java,
            after = { param ->
                xlog("[BreenoChatHook] 新对话已创建! roomMode=${param.args[0]}, src=${param.args[1]}")
                // 重置对话轮数
                turnCount = 0
                // TODO: 在这里清空大模型历史对话上下文
                // MyLLMContextManager.clearHistory()
            }
        )

        // 2. 数据流上帝控制台：拦截与注入
        val viewBeanClass = lpparam.findClass("com.heytap.speechassist.aichat.bean.AIChatViewBean")
        val typeQuery = XposedHelpers.getStaticIntField(viewBeanClass, "TYPE_QUERY")
        val typeAnswer = XposedHelpers.getStaticIntField(viewBeanClass, "TYPE_ANSWER")

        lpparam.hookMethod(
            className = "com.heytap.speechassist.aichat.AIChatDataCenter",
            methodName = "r",
            viewBeanClass,
            before = before@ { param ->
                val bean = param.args[0]
                if (dataCenterInstance == null) {
                    dataCenterInstance = param.thisObject
                    xlog("[BreenoChatHook] DataCenter 实例已缓存")
                }

                val chatType = bean.call<Int>("getChatType") ?: return@before
                val roomId = bean.call<String>("getRoomId") ?: ""
                currentRoomId = roomId // 更新最新的 roomId

                if (chatType == typeQuery) {
                    val query = bean.call<String>("getContent")
                    xlog("[BreenoChatHook] 捕获用户输入: $query (roomId=$roomId)")
                    
                    // 拦截到了输入，增加对话轮数并触发我们的 MVP Mock 流
                    if (!query.isNullOrBlank()) {
                        turnCount++
                        triggerMockStream(roomId, viewBeanClass)
                    }
                    return@before
                }

                if (chatType == typeAnswer) {
                    val skillType = bean.call<String>("getSkillType")
                    val isMyMock = bean.call<Any>("getClientLocalData", "MY_MOCK_FLAG") != null

                    // 自己注入的数据，绝对放行
                    if (isMyMock) {
                        xlog("[BreenoChatHook] 放行自定义注入卡片")
                        return@before
                    }

                    // 放行原生垂直领域技能 (例如闹钟 Countdown, 天气 Weather 等)
                    if (skillType != null && skillType != "MyAI.StreamTextCard") {
                        xlog("[BreenoChatHook] 放行原生技能卡片: $skillType")
                        return@before
                    }

                    // 否则，这大概率是官方的废话、闲聊或者大模型回复，静默掐断！
                    xlog("[BreenoChatHook] 拦截官方卡片: skillType=$skillType")
                    param.result = null 
                }
            }
        )
    }

    // MVP 模拟打字机流式响应
    private fun triggerMockStream(roomId: String, beanClass: Class<*>) {
        scope.launch(Dispatchers.IO) {
            // 稍等一会，模拟网络延迟
            delay(500)
            
            val mockData = listOf(
                "你好！", "我是由", " OpenHook ", "强力驱动的", "自定义", "大模型助手！\n\n", 
                "我拦截了官方的回复，", "并且使用了", "最优雅的", "领域模型注入方案。\n\n",
                "【调试信息】", "当前是你的第 $turnCount 次回答。"
            )
            val accumulator = StringBuilder()
            
            // 实例化一个新的 Bean 用于承载整条流式回答
            val mockBean = beanClass.newInstance()
            val typeAnswer = XposedHelpers.getStaticIntField(beanClass, "TYPE_ANSWER")
            val uniqueRecordId = "mock_record_${roomId}_${System.currentTimeMillis()}"
            
            // 基础属性初始化
            mockBean.call<Unit>("setChatType", typeAnswer)
            mockBean.call<Unit>("setRoomId", roomId)
            mockBean.call<Unit>("setRecordId", uniqueRecordId) 
            mockBean.call<Unit>("addClientLocalData", "MY_MOCK_FLAG", true)
            // 极其关键的修复：必须伪装成官方的流式卡片类型，否则 UI 拒绝播放打字机动画！
            mockBean.call<Unit>("setSkillType", "MyAI.StreamTextCard")
            // 强制显示底部反馈按钮（复制/点赞）
            mockBean.call<Unit>("addClientLocalData", "bean_client_key_hide_feedback_view", true)
            // 修复打字机动画：设置打字速度，并将播放标记设为 false，通知 UI 播放动画
            mockBean.call<Unit>("setMsPerChar", 50)
            mockBean.call<Unit>("setHasTextPrintAnimPlayed", false)

            mockData.forEachIndexed { index, segment ->
                accumulator.append(segment)
                val isFinal = (index == mockData.size - 1)
                val isFirst = (index == 0)

                // 更新状态
                mockBean.call<Unit>("setContent", accumulator.toString())
                mockBean.call<Unit>("setFinal", isFinal)
                mockBean.call<Unit>("setFirstSlice", isFirst)
                // 极其关键：每次推流都必须重置动画锁，告诉 UI 有新字来了，赶紧播动画！
                mockBean.call<Unit>("setHasTextPrintAnimPlayed", false)
                
                if (isFirst) {
                    // 第一帧：调用 r() 方法，把气泡【添加】到列表中
                    dataCenterInstance?.call<Unit>("r", mockBean)
                } else {
                    // 后续帧：调用 g1() 方法，只【通知 UI 刷新】已存在的这个气泡
                    // public final void g1(AIChatViewBean bean, boolean z11)
                    dataCenterInstance?.call<Unit>("g1", mockBean, false)
                }
                
                // 模拟打字机推流间隔
                delay(150)
            }
            // 强制显示底部反馈按钮（复制/点赞/分享）
            mockBean.call<Unit>("addClientLocalData", "bean_client_key_hide_feedback_view", false)
            dataCenterInstance?.call<Unit>("g1", mockBean, false)
        }
    }
}
