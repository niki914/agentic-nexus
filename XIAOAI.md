# XIAOAI 调查记录

## 目标

- 构建一个可按 `BreenoChatHook` 模式实现的小爱模块。
- 核心能力是同时掌握：
  - 用户输入
  - 同一轮回答的流式指令序列
  - `dialogId` 级关联关系
  - 原生正文与原生 TTS 的拦截面
  - 自定义回答的最终注入面

## 已确认路径

- 包名：`com.miui.voiceassist`
- 当前工作区反编译目录：`/Users/local/repo/android/personal/openhook/.workspace/breeno_openai_reborn/.apk_decs/com_miui_voiceassist_apk`
- 源码目录：`/Users/local/repo/android/personal/openhook/.workspace/breeno_openai_reborn/.apk_decs/com_miui_voiceassist_apk/sources/`
- 资源目录：`/Users/local/repo/android/personal/openhook/.workspace/breeno_openai_reborn/.apk_decs/com_miui_voiceassist_apk/resources/`
- Manifest：`/Users/local/repo/android/personal/openhook/.workspace/breeno_openai_reborn/.apk_decs/com_miui_voiceassist_apk/resources/AndroidManifest.xml`
- 原始提取 APK 路径：`/Users/local/repo/android/personal/openhook/.apk_decs/extracted_apps/com_miui_voiceassist.apk`

## 核心运行时边界

- 输入态边界：
  - `com.xiaomi.voiceassistant.instruction.base.OperationManager#setQueryInfo(java.lang.String,java.lang.String,org.json.JSONObject)`
  - 运行时可稳定拿到：
    - `dialogId`
    - `query`
    - `extraInfo`
- 统一流式入口：
  - `com.xiaomi.voiceassistant.instruction.base.OperationManager#onFlowableInstruction(com.xiaomi.ai.api.common.Instruction,boolean)`
  - 同一轮回答内的正文流、TTS 流、附件、建议、元信息都在这里汇聚。
- 回答同步落卡边界：
  - `com.xiaomi.voiceassistant.instruction.card.TemplateReactNativeCard#p1(org.json.JSONObject)`
  - 运行时可看到：
    - `totalText`
    - `isLlmContentDisplayComplete`
    - `isIllegalContent`

## 统一时序样本

同一 `dialogId=837168ef6c94f8ebbae7cbd220f841d` 下观测到的完整主序列：

```text
Nlp.ExpectStream
-> Template.Query
-> Template.LLMLoadingCard
-> Template.ResultOperationInfo
-> Template.FrontendPage
-> Nlp.UpdateStreamProperties
-> Template.ToastStream
-> SpeechSynthesizer.Speak
-> Template.Attachment
-> Template.ToastStream
-> SpeechSynthesizer.SpeakStream
-> Template.ToastStream
-> Template.ToastStream
-> SpeechSynthesizer.SpeakStream
-> Template.ToastStream
-> Template.ToastStream
-> Template.ToastStream
-> SpeechSynthesizer.SpeakStream
-> Suggestion.ShowContextSuggestions
-> SpeechSynthesizer.SpeakStream
```

这条样本说明：

- `OperationManager#onFlowableInstruction(...)` 是统一主入口。
- 正文回答从第一个 `Template.ToastStream` 开始进入主干。
- `SpeechSynthesizer.Speak` 与 `SpeechSynthesizer.SpeakStream` 在同一入口下和正文流交错下发。
- `Template.Attachment` 与 `Suggestion.ShowContextSuggestions` 是同轮回答的伴随指令。
- `Template.LLMLoadingCard`、`Template.ResultOperationInfo`、`Template.FrontendPage`、`Nlp.UpdateStreamProperties` 属于正文前的元信息与壳层配置。

## 已确认的高价值 model

- `Template.Query`
  - 类型：`com.xiaomi.ai.api.Template$Query`
  - 高价值字段：
    - `getText`
  - 当前样本值：
    - `queryText=介绍自己`
- `Template.ToastStream`
  - 类型：`com.xiaomi.ai.api.Template$ToastStream`
  - 高价值字段：
    - `getMarkdownText`
  - 当前样本值：
    - `toastFirst=我是小爱同`
    - `toastLast=。`
  - 当前样本计数：
    - `count=7`
- `Template.FrontendPage`
  - 类型：`com.xiaomi.ai.api.Template$FrontendPage`
  - 高价值字段：
    - `getInstructions`
  - 当前样本值：
    - `frontendInstructionsType=lv.a`
- `Template.ResultOperationInfo`
  - 类型：`com.xiaomi.ai.api.Template$ResultOperationInfo`
  - 当前样本里是回答卡底部操作区配置载体。
- `Nlp.UpdateStreamProperties`
  - 类型：`com.xiaomi.ai.api.Nlp$UpdateStreamProperties`
  - 高价值字段：
    - `isSimplySpeak`
- `SpeechSynthesizer.Speak`
  - 类型：`com.xiaomi.ai.api.SpeechSynthesizer$Speak`
  - 属于原生 TTS 启动指令。
- `SpeechSynthesizer.SpeakStream`
  - 类型：`com.xiaomi.ai.api.SpeechSynthesizer$SpeakStream`
  - 属于原生 TTS 流式补充分片指令。
- `Template.Attachment`
  - 类型：`com.xiaomi.ai.api.Template$Attachment`
  - 属于回答伴随资源载体。
- `Suggestion.ShowContextSuggestions`
  - 类型：`com.xiaomi.ai.api.Suggestion$ShowContextSuggestions`
  - 属于尾部建议指令。

## 当前架构判断

- 小爱当前最稳定的主链是：

```text
OperationManager#setQueryInfo(dialogId, query, extraInfo)
-> OperationManager#onFlowableInstruction(dialogId, instruction, isOffline)
-> 下游 Operation / Card / TTS / Suggestion 消费
-> TemplateReactNativeCard#p1(JSONObject)
```

- `OperationManager` 已经同时承担：
  - 输入态写入
  - 统一流式分发
  - `dialogId` 级会话关联
- `TemplateReactNativeCard#p1(JSONObject)` 是落卡同步面，不是最上游主入口。
- 播放器链是下游消费链，适合作为锚点，不适合作为核心状态边界。

## 面向模块实现的直接结论

- 小爱版模块应围绕 `dialogId` 建状态机。
- 最合理的三层边界是：
  - 输入边界：`OperationManager#setQueryInfo(...)`
  - 响应边界：`OperationManager#onFlowableInstruction(...)`
  - 注入边界：`cb0.eb#A0(Instruction)` 或等价 operation 调用点
- 最小接管策略应优先覆盖：
  - `Template.ToastStream`
  - `SpeechSynthesizer.Speak`
  - `SpeechSynthesizer.SpeakStream`
- 可以先放行的伴随指令：
  - `Template.Query`
  - `Template.LLMLoadingCard`
  - `Template.ResultOperationInfo`
  - `Template.FrontendPage`
  - `Nlp.UpdateStreamProperties`
  - `Template.Attachment`
  - `Suggestion.ShowContextSuggestions`

## 当前实现建议

- 输入捕获继续使用 `OperationManager#setQueryInfo(...)`。
- 统一摘要探针长期保留在 `OperationManager#onFlowableInstruction(...)`。
- 原生正文与原生 TTS 的拦截优先放在统一流入口层完成。
- 自定义回答注入继续围绕 `cb0.eb#A0(Instruction)` 这一类 operation 执行点确认最终目标。
- `TemplateReactNativeCard#p1(JSONObject)` 保留为观察落卡同步结果的辅助边界。

## 低优先级方向

- 发送按钮一类 UI 末梢回调。
- 播放器底层实现链本身。
- 仅围绕某个具体卡片类继续深挖字段级注入面。
