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

## 稳定下游锚点

这一节只记录当前已经确认、且适合以后版本更新时快速复找的下游点位。

原则：

- 优先找“宿主已经把复杂对象处理完毕”的点。
- 优先找“只改 1 到 2 个字段即可生效”的点。
- 记录方法轮廓时，除了签名，还要记录典型内部调用、日志字串、硬编码字串、字段名。

### 1. UI 最终文本模型面

- 锚点：`com.xiaomi.voiceassistant.instruction.card.stream.c#updateText(String)`
- 当前价值：
  - 这是原生流卡正文的最近最终文本字段。
  - 它不是只改 View，而是先改 model 的 `totalText`，再同步到 View。
  - 如果后续要做“只改显示文字但尽量不碰复杂协议对象”，这是当前最优 UI 下游点。
- 方法轮廓：
  - 日志字串：`"updateText: " + text + " " + text.length()`
  - 先做：`dVar.setTotalText(dVar.getTotalText() + text)`
  - 再做：`r30.a.f105503a.convert(dVar2.getTotalText())`
  - 再调：`interfaceC0488c.updateText(this.f37535y2.getTotalText())`
  - 末尾更新：`this.V2 = this.f37535y2.getTotalText().length()`
- 版本更新时的快速定位特征：
  - 搜 `updateText: `
  - 搜 `setTotalText(dVar.getTotalText() + text)`
  - 搜 `convert(dVar2.getTotalText())`
  - 搜 `interfaceC0488c.updateText(this.f37535y2.getTotalText())`
- 直接结论：
  - 这个点适合做最终 UI 文本接管。
  - 不建议只 hook `StreamCardTextViewHolder#updateText(...)`，因为那样只改屏幕字面，不改状态模型。

### 2. UI 流分片汇入面

- 锚点：`pb0.s` 内部处理 `Template.ToastStream` 的分片消费点
- 当前价值：
  - 这是原生 `ToastStream` 进入流卡正文模型前的最后一层增量处理。
  - 如果后续要做“增量流接管”而不是“最终总文本覆盖”，这个点比 `stream.c#updateText(...)` 更上游。
- 方法轮廓：
  - 强转 payload：`Template.ToastStream`
  - 读取字段：`getMarkdownText()`
  - 旧文本来源：`s.this.P.toString()`
  - 增量处理：`a40.e.f513a.handleMarkdown(markdownText, string)`
  - 非结束帧：
    - 判定字串：`"<FINAL>"`
    - 日志字串：`"handle nextIns updateText"`
    - 累加：`s.this.P.append(strHandleMarkdown)`
    - 下游调用：`cVar.updateText(strHandleMarkdown)`
  - 结束帧：
    - 日志字串：`"handle nextIns: onFinish " + instruction.getFullName()`
    - 调用：`sVar.r0(opExecProcedure)`
- 版本更新时的快速定位特征：
  - 搜 `handleMarkdown(markdownText, string)`
  - 搜 `"<FINAL>"`
  - 搜 `handle nextIns updateText`
  - 搜 `handle nextIns: onFinish`
  - 搜 `cVar.updateText(strHandleMarkdown)`
- 直接结论：
  - 这个点适合做正文流分片级接管。
  - 如果只想要最终文本字段，优先级低于 `stream.c#updateText(...)`。

### 3. TTS 最终待播文本面

- 锚点：`cb0.p9`
- 当前价值：
  - 这是当前确认最强的 TTS 下游点。
  - 宿主内部已经大量通过 `setRedefinedTts(...)` 改写它，说明这不是旁门左道，而是官方认可的重定义面。
  - 它符合“只改一个字段就能生效”的目标。
- 关键方法：
  - `getOriginalTts()`
  - `getRedefinedTts()`
  - `getToSpeak()`
  - `onCreateOp(h90.c)`
  - `setRedefinedTts(String)`
- 方法轮廓：
  - 原始文本字段：`this.G`
  - 重定义文本字段：`this.F`
  - 实际待播文本：`return !TextUtils.isEmpty(this.F) ? this.F : this.G`
  - 初始化原始文本：
    - `this.G = ((SpeechSynthesizer.Speak) this.f36063a.getPayload()).getText()`
  - 重定义逻辑：
    - 条件：`if (c0())`
    - 日志字串：`"SpeakOperation"`
    - 日志字串：`"setRedefinedTts: text only"`
    - 真正赋值：`this.F = tts`
- 官方桥接证据：
  - `com.xiaomi.voiceassistant.instruction.base.l0#setRedefinedTts(Object, String)` 会主动找 `p9.class`
  - 如果 `str != null`：`p9Var.setRedefinedTts(str)`
  - 如果 `str == null`：`p9Var.setRedefinedTts(p9Var.getToSpeak())`
- 额外定位锚点：
  - `cb0.p9#generateSpeakOperation(String)` 里有硬编码：
    - `fakeSpeakId`
    - `fakeDialogId`
  - 它会构造 `InstructionHeader("SpeechSynthesizer", "Speak")`
  - 再调用 `p9Var.setRedefinedTts(tts)`
- 版本更新时的快速定位特征：
  - 搜 `SpeakOperation`
  - 搜 `setRedefinedTts: text only`
  - 搜 `return !TextUtils.isEmpty(this.F) ? this.F : this.G`
  - 搜 `findOperation(p9.class)`
  - 搜 `fakeSpeakId`
  - 搜 `fakeDialogId`
- 直接结论：
  - 如果后续要做“改最终播报文本”，优先围绕 `p9`，不要优先去碰上游 `Instruction` 构造。

### 4. FrontendPage 流式桥接面

- 锚点：`cb0.eb#A0(Instruction)`
- 当前价值：
  - 这是原生 `FrontendPage` 流式 operation 的关键桥接点。
  - 同一个方法里同时接到了：
    - `Template.ToastStream`
    - `SpeechSynthesizer.SpeakStream`
    - `Template.StyleToastStreamStart`
    - `Template.StyleToastStreamFinish`
    - `Template.PassbyFrontEndData`
  - 它不是最适合最终字段覆盖的点，但非常适合做“UI/TTS 流交汇”研究和注入桥接。
- 方法轮廓：
  - 入口日志字串：`"handle NextIns: id -> " + instruction`
  - 开头状态：`this.Q = true`
  - `Template.ToastStream` 分支：
    - 读取：`getMarkdownText()`
    - 调用：`this.N.sendStreamDataToFront(instruction)`
    - 结束判定字串：`"<FINAL>"`
    - 结束动作：`t0()`
    - 非结束动作：`this.T.append(markdownText)`
  - `SpeechSynthesizer.SpeakStream` 分支：
    - 调用：`com.xiaomi.voiceassistant.instruction.utils.b2.f38870a.addFragment(getDialogId(), ((SpeechSynthesizer.SpeakStream) payload).getText())`
  - `Template.StyleToastStreamStart` 分支：
    - 日志字串：`"StyleToastStreamStart"`
    - 调用：`this.N.setIsTTSPlayShouldShow(false)`
  - `Template.StyleToastStreamFinish` 分支：
    - 调用：`this.N.setIsTTSPlayShouldShow(true)`
  - 其他流指令：
    - 调用：`this.N.sendStreamDataToFront(instruction)`
  - `Template.PassbyFrontEndData`：
    - 调用：`u0((Template.PassbyFrontEndData) payload)`
- 版本更新时的快速定位特征：
  - 搜 `handle NextIns: id ->`
  - 搜 `"<FINAL>".equals(markdownText)`
  - 搜 `StyleToastStreamStart`
  - 搜 `setIsTTSPlayShouldShow(false)`
  - 搜 `setIsTTSPlayShouldShow(true)`
  - 搜 `addFragment(getDialogId()`
  - 搜 `sendStreamDataToFront(instruction)`
- 直接结论：
  - 这个点非常适合打栈和做桥接研究。
  - 但如果目标是“最稳定的最终 UI 字段替换”或“最稳定的最终待播 TTS 字段替换”，优先级都低于前两个最终字段面。

## 当前版本实测结论

这一节优先级高于上面的“候选锚点”分析。上面是静态逆向推测，这一节是已经跑过探针后的运行时结论。

### 已验证主链

在当前版本、当前实测样本里，命中的真实链路是：

```text
OperationManager#onFlowableInstruction(SpeechSynthesizer.Speak)
-> cb0.n9#onProcessOp(...)
-> cb0.eb#A0(Nlp.UpdateStreamProperties)
-> cb0.eb#A0(Template.ToastStream)
-> cb0.eb#A0(SpeechSynthesizer.SpeakStream)
-> cb0.eb#A0(Template.ToastStream "<FINAL>")
```

这条链路的含义是：

- `SpeechSynthesizer.Speak` 确实会在统一流入口 `OperationManager#onFlowableInstruction(...)` 出现。
- 当前实测命中的 `Speak` 是 URL 型，不是纯文本型：
  - `urlPresent=true`
  - `textPreview=` 为空
- `cb0.n9#onProcessOp(...)` 才是当前样本里真正落到 TTS 播放链的 operation。
- `cb0.eb#A0(...)` 同时收到了：
  - `Nlp.UpdateStreamProperties`
  - `Template.ToastStream`
  - `SpeechSynthesizer.SpeakStream`
  - `Template.ToastStream "<FINAL>"`
- 因此当前版本里，`cb0.eb#A0(...)` 是最稳定的 UI/TTS 文本交汇点。

### 对旧判断的修正

- 旧判断里把 `cb0.p9` 视为最强 TTS 下游点，这在静态逆向上是合理的，但当前版本实测样本并不支持它作为主链。
- 当前样本里没有命中 `cb0.p9#onCreateOp(...)`，也没有命中 `cb0.p9` 相关文本型 TTS 行为。
- 因此：
  - `cb0.p9` 先降级为“另一类 `Speak(text)` 场景的备用候选”
  - `cb0.n9` 升级为“当前版本 URL 型 TTS 主链的正式候选”

### 当前版本的正式优先级

- `第一优先`：`cb0.eb#A0(Instruction)`
  - 用途：UI 接管主锚点
  - 用途：观察 `Template.ToastStream`
  - 用途：观察 `SpeechSynthesizer.SpeakStream`
  - 备注：当前版本最值得优先围绕它做接管
- `第二优先`：`cb0.n9#onProcessOp(...)`
  - 用途：TTS 播放主锚点
  - 备注：当前版本样本里，TTS 真正落到这里
- `第三优先`：`OperationManager#onFlowableInstruction(SpeechSynthesizer.Speak)`
  - 用途：统一入口拦截原生 `Speak`
  - 备注：适合做前置状态机和阻断
- `第四优先`：`cb0.p9`
  - 用途：备用锚点
  - 备注：保留给后续出现 `Speak(text)` 样本时再验证
- `降级处理`：`stream.c#updateText(String)`、`pb0.s` 内部分片消费点
  - 备注：这两个点在当前实测样本里没有命中主链，先不作为正式接管面

### 当前版本对 TTS 的理解

当前实测样本显示，TTS 不是一个点完成的，而是两条协同链：

- `Speak(url)` 负责启动真实播报：
  - 统一流入口能看到 `SpeechSynthesizer.Speak`
  - 播放 operation 落在 `cb0.n9`
- `SpeakStream(text)` 负责文本同步：
  - 在 `cb0.eb#A0(...)` 内看到 `SpeechSynthesizer.SpeakStream`
  - 当前样本的 `textPreview=刚刚已经介绍过啦，`

这意味着当前版本的 TTS 接管，不应只围绕一个文本字段思考，而应拆成：

- 原生音频播放链：`OperationManager(Speak)` + `cb0.n9`
- UI/TTS 同步文本链：`cb0.eb#A0(SpeakStream)`

### 给接手 RD 的直接结论

- 如果要最快做出当前版本可工作的正文接管，先盯 `cb0.eb#A0(...)`。
- 如果要最快做出当前版本可工作的 TTS 接管，先盯：
  - `OperationManager#onFlowableInstruction(SpeechSynthesizer.Speak)`
  - `cb0.n9#onProcessOp(...)`
- 不要一上来围绕 `cb0.p9` 做正式实现，除非后续又抓到新的 `Speak(text)` 样本。
- 不要优先围绕 `stream.c#updateText(...)` 或 `pb0.s` 做正式实现，它们更像备用支路，不是当前主链。

### 当前探针的最小样本策略

为了避免日志过载，当前探针已经收缩为“每个 `dialogId` 只打最小必要样本”：

- `OperationManager#onFlowableInstruction(...)`
  - 只打印 `fullName == SpeechSynthesizer.Speak`
- `cb0.n9#onProcessOp(...)`
  - 每个 `dialogId` 只打一次
- `cb0.p9#onCreateOp(...)`
  - 每个 `dialogId` 只打一次
- `cb0.eb#A0(...)`
  - 每个 `dialogId` 只打印：
    - 首个 `Nlp.UpdateStreamProperties`
    - 首个非 `<FINAL>` 的 `Template.ToastStream`
    - 首个 `<FINAL>` 的 `Template.ToastStream`
    - 首个空 `SpeechSynthesizer.SpeakStream`
    - 首个非空 `SpeechSynthesizer.SpeakStream`

### 日志阅读注意事项

- 如果看到日志成对重复，优先怀疑是日志同时从两个通道被收集：
  - `Log.d(...)`
  - `xlog(...)`
- 当前探针的栈已经过滤了以下噪音：
  - `android.os.*`
  - `com.niki914.*`
  - `java.lang.reflect.*`
  - `de.robv.android.xposed.*`
  - `dEgk.*`
  - `uu0.*`
  - `ju0.*`
  - `android.graphics.DurktPrork`
  - 精确类名 `o`
  - 精确类名 `h`

## 低优先级方向

- 发送按钮一类 UI 末梢回调。
- 播放器底层实现链本身。
- 仅围绕某个具体卡片类继续深挖字段级注入面。
