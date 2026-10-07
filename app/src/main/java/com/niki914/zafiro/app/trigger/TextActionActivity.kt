package com.niki914.zafiro.app.trigger

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.ui.res.stringResource
import com.niki914.logging.Logger
import com.niki914.uikit.base.BaseTheme
import com.niki914.uikit.infra.component.OptionRow
import com.niki914.uikit.infra.component.OptionSheet
import com.niki914.zafiro.api.Agent
import com.niki914.zafiro.api.model.ConversationId
import com.niki914.zafiro.app.MainActivity
import com.niki914.zafiro.app.R
import com.niki914.zafiro.app.conversation.ConversationRepo
import com.niki914.zafiro.repo.TextAction
import com.niki914.zafiro.repo.XRepo
import com.niki914.zafiro.service.requireService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * ACTION_PROCESS_TEXT 入口：系统文本选择菜单里的「Zafiro」。
 *
 * 一次性透明 Activity，不驻留、不进最近任务。流程：
 * 解析选中文本 → 弹 OptionSheet 选动作 → 拼提示词 → 建新会话并发起回合
 * → 静默 finish 或拉起 MainActivity。
 *
 * 启动模式默认 standard：PROCESS_TEXT 总是携带全新 Intent 启动，
 * 没有需要跨实例保留的状态。
 */
class TextActionActivity : ComponentActivity() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** 已选中动作并进入投递：`dismissThen` 会先回调 onDismissRequest，不能再顺手 finish。 */
    @Volatile
    private var dispatched = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val selectedText = TextSelectionInput.parse(
            intent?.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)
        )
        if (selectedText == null) {
            Logger.i(LOG_TAG, "no usable selection, finishing")
            finish()
            return
        }

        scope.launch {
            val actions = runCatching { XRepo.textActions.listEnabled() }
                .onFailure { Logger.w(LOG_TAG, "list actions failed reason=${it.message}") }
                .getOrDefault(emptyList())
            if (actions.isEmpty()) {
                Logger.i(LOG_TAG, "no enabled actions, finishing")
                finish()
                return@launch
            }

            // 保活载体检查：悬浮球或常驻通知任一启用才真正静默；都没有则回落到
            // 打开主应用（后台回合失去保活会被系统杀掉）。
            // 用挂起 getter 而不是 flow.value：本 Activity 可能冷启动整个进程，
            // 此时 MainActivity 的 hydrateSettings 还没跑，flow 仍是默认值。
            val silentRequested = runCatching { XRepo.textActionSilent() }.getOrDefault(false)
            val carrierPresent =
                runCatching { XRepo.floatingBallEnabled() }.getOrDefault(false) ||
                    runCatching { XRepo.residentNotificationEnabled() }.getOrDefault(false)

            runOnUiThread {
                showActionSheet(actions, selectedText, silentRequested && carrierPresent)
            }
        }
    }

    private fun showActionSheet(actions: List<TextAction>, selectedText: String, silent: Boolean) {
        setContent {
            BaseTheme {
                OptionSheet(
                    visible = true,
                    title = stringResource(R.string.text_actions_sheet_title),
                    onDismissRequest = {
                        if (!dispatched) finish()
                    },
                ) { dismissThen ->
                    actions.forEach { action ->
                        OptionRow(
                            title = action.name,
                            onClick = {
                                dispatched = true
                                dismissThen {
                                    dispatch(action, selectedText, silent)
                                }
                            },
                        )
                    }
                }
            }
        }
    }

    /**
     * 发起新会话：先建档再 `load`（挂起且确定性），避免 `discard()` 的引擎
     * 复位协程与紧随其后的 `stream()` 竞争。随后写草稿并 stream，回合跑在
     * AgentImpl 的应用级 scope 里，Activity 结束不影响执行。
     */
    private fun dispatch(action: TextAction, selectedText: String, silent: Boolean) {
        scope.launch {
            val prompt = TextActionPromptComposer.compose(action.promptTemplate, selectedText)
            val agent = requireService<Agent>()
            val conversationId = UUID.randomUUID().toString()
            val prepared = runCatching {
                if (!ConversationRepo.exists(conversationId)) {
                    ConversationRepo.createConversation(conversationId, prompt)
                }
                agent.load(ConversationId(conversationId))
            }
            if (prepared.isFailure) {
                Logger.w(LOG_TAG, "prepare conversation failed reason=${prepared.exceptionOrNull()?.message}")
                runOnUiThread { finish() }
                return@launch
            }

            agent.updateDraft { it.copy(text = prompt, images = emptyList(), files = emptyList()) }
            val startResult = agent.stream()
            Logger.i(
                LOG_TAG,
                "dispatch action=${action.id} result=$startResult " +
                    "silent=$silent promptLength=${prompt.length}",
            )

            runOnUiThread {
                if (!silent) {
                    // MainActivity 是 singleTask：复用现有任务并置前，Home 订阅
                    // agent.conversation 展示正在进行的回合。
                    val intent = Intent(this@TextActionActivity, MainActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    }
                    runCatching { startActivity(intent) }
                        .onFailure { Logger.w(LOG_TAG, "launch main failed reason=${it.message}") }
                }
                finish()
            }
        }
    }

    private companion object {
        private const val LOG_TAG = "niki914_zafiro_TextActionActivity"
    }
}
