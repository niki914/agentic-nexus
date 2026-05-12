# XIAOAI 调查记录

## 目标

- 目标不是盯住 TTS 播报本身。
- TTS/播放器日志目前只作为锚点，用来确认小爱内部的回答播放生命周期。
- 真正目标是找到一个类似 Breeno 那边 `DataCenter` / `ViewModel` / 会话状态中心的东西，能同时拿到：
  - 用户输入
  - 小爱的回答增量或分帧更新
  - room/session/record 之类的关联 id

## 已确认路径

- 包名：`com.miui.voiceassist`
- 当前工作区反编译目录：`/Users/local/repo/android/personal/openhook/.workspace/breeno_openai_reborn/.apk_decs/com_miui_voiceassist_apk`
- 源码目录：`/Users/local/repo/android/personal/openhook/.workspace/breeno_openai_reborn/.apk_decs/com_miui_voiceassist_apk/sources/`
- 资源目录：`/Users/local/repo/android/personal/openhook/.workspace/breeno_openai_reborn/.apk_decs/com_miui_voiceassist_apk/resources/`
- Manifest：`/Users/local/repo/android/personal/openhook/.workspace/breeno_openai_reborn/.apk_decs/com_miui_voiceassist_apk/resources/AndroidManifest.xml`
- 原始提取 APK 路径：`/Users/local/repo/android/personal/openhook/.apk_decs/extracted_apps/com_miui_voiceassist.apk`

## 做过的无用功

- 先前把 hook 点放在一个很窄的 UI 回调上：
  - `com.xiaomi.voiceassistant.ConversationFragment$d#onSendClick(java.lang.String,java.lang.String)`
  - label: `super_xiaoai_input_probe`
  - 运行时验证时间窗：`23:20~23:21`
  - 结果：`rule_fired=false`
  - 结论：这个点太窄，不适合第一轮探测
- 后续改成更外层的统一查询分发入口：
  - `q20.h0#startQuery(q20.h0$c)`
  - label: `super_xiaoai_query_sender_probe`
  - 这是广度优先的方向，但当时还没拿到有效 openhook 命中证据
- 教训：
  - 不能先死盯“发送按钮回调”
  - 第一轮应优先找更稳的分发层、状态中心、数据中心，而不是 UI 末梢

## 已确认的日志锚点

这些日志和语音助手行为时机 100% 对得上，说明它们有追踪价值：

- `stop state = 0`
- `stop state = -1`
- `setVolume see ExoPlayCall volume = 1.0`
- `onStart`
- `onComplete`
- `Release ... [AndroidXMedia3/1.8.0] ...`

## 这些日志在源码里的落点

- `o20.i#stop()`
  - 源码里是 `"stop state = " + this.f92093c.getState()`
  - 所以 `0/-1` 不是硬编码常量，而是运行时状态值
- `o20.i#setVolume(float)`
  - 源码里是 `"setVolume see ExoPlayCall volume = " + f12`
- `o20.i$b#onStart()`
  - 直接打 `"onStart"`
- `o20.i$d#onComplete()`
  - 直接打 `"onComplete"`
- `uw.m`
  - 是 `o20.i` 下游的状态机包装层，负责维护和流转播放状态
- `com.xiaomi.ai.ttsplayer.player.exo.ExoBytesImpl`
  - 更底层的 Exo/Media3 播放实现，`onPlaybackStateChanged` 会映射到 `onStart/onComplete`
- `Release ... [AndroidXMedia3/1.8.0] ...`
  - 大概率不是业务代码自己打的，而是 Media3/ExoPlayer 库内部 release 日志

## 当前能站住脚的 TTS/播报链路

目前最像“小爱回答播报生命周期”的链路：

- `cb0.n9#c0(g90.l)`
  - 从 `SpeechSynthesizer.Speak.url` 往下走
- `o20.e#startPlay(String,kz.b)`
  - 业务侧开始播报任务的入口
- `yo.u`
  - 工厂，创建具体播放器实现
- `o20.i`
  - Exo 包装播放器，打出目前已确认的几条关键日志
- `uw.m`
  - 状态机包装
- `com.xiaomi.ai.ttsplayer.player.exo.ExoBytesImpl`
  - 底层 Exo/Media3 实现
- `r00.g`
  - 更接近助手侧生命周期消费层，会接住 `onPlayBegin/onRealPlayBegin/onPlayFinish`

## 当前判断

- 这条链路更像“回答播报控制链”，不是“用户输入 + 回答流式分帧数据中心”
- 它有价值，但价值在于：
  - 作为稳定锚点
  - 借助播报生命周期往上回溯业务层
  - 找到谁在持有 query/session/record/answer 状态
- 它本身不是最终目标

## 已运行时证实的关键点

- `com.xiaomi.voiceassistant.instruction.base.OperationManager#setQueryInfo(java.lang.String,java.lang.String,org.json.JSONObject)`
  - 当前最稳的输入态入口。
  - 运行时已确认能稳定拿到：
    - `dialogId`
    - `query`
    - `extraInfo`（当前样本里为 `null`）
  - 这个点不像 UI 发送按钮，更像会话 query 状态写入点。
- `com.xiaomi.voiceassistant.instruction.card.TemplateReactNativeCard#p1(org.json.JSONObject)`
  - 当前最有价值的回答同步/注入候选点。
  - 运行时已确认会收到近似平坦的 JSON：
    - `totalText`
    - `isLlmContentDisplayComplete`
    - `isIllegalContent`
  - 当前样本显示它至少覆盖：
    - 文本内容写入
    - 完成态翻转
  - 这说明小爱这一路回答卡并不是先落到一个早期 POJO/Bean，再统一渲染；更像是前面经过 `Instruction/Operation/JSON` 链路，最后在卡片同步口压成接近平坦的结果结构。

## 当前最值得继续盯的类

- `com.xiaomi.voiceassistant.instruction.base.OperationManager`
  - 当前最稳的 query/session 侧状态入口。
  - `setQueryInfo(...)` 已被运行时证实。
- `com.xiaomi.voiceassistant.instruction.card.TemplateReactNativeCard`
  - 当前最稳的回答同步/注入候选。
  - `p1(JSONObject)` 已被运行时证实。
- `com.xiaomi.voiceassistant.instruction.card.stream.c`
  - 当前最值得继续往下追的 native 文本落卡层。
  - `updateText(String)`、`replaceTotalText(String)` 这类点仍然有价值，但还没有拿到这一路的运行时命中证据。
- `com.xiaomi.voiceassistant.instruction.card.stream.c$d`
  - 当前最像 Breeno 那种“字段较平坦”的本地 model。
  - 需要继续确认 `TemplateReactNativeCard.p1(JSONObject)` 之后，值是否会继续灌入这个 model。

## 当前不应高优先级继续投入的点

- `com.xiaomi.voiceassistant.ConversationFragment$d#onSendClick(java.lang.String,java.lang.String)`
  - 太窄，已验证无命中价值。
- 播放器/TTS 链路本身
  - 只适合作为稳定锚点，不是最终注入面。
- `com.xiaomi.voiceassistant.mainui.flowableresult.g$c#onFlowableInstruction(com.xiaomi.ai.api.common.Instruction,boolean)`
  - 静态上合理，但当前这轮真实回答路径没有命中。
  - 说明它不是最通用的首选 probe，至少这次回答更偏 `TemplateReactNativeCard` 这条回流链。

## 后续搜索方向

- 不再继续扩大播放器 hook 面。
- 优先把 `OperationManager#setQueryInfo(...)` 和 `TemplateReactNativeCard#p1(JSONObject)` 作为一前一后的核心边界：
  - 前者负责拿输入态的 `dialogId/query`
  - 后者负责拿回答侧的 `totalText/完成态`
- 继续静态确认 `TemplateReactNativeCard#p1(JSONObject)` 之后是否会落到：
  - `com.xiaomi.voiceassistant.instruction.card.stream.c$d`
  - `com.xiaomi.voiceassistant.instruction.card.stream.c#updateText(java.lang.String)`
- 如果目标是做最小注入，优先考虑：
  - 直接在 `TemplateReactNativeCard#p1(JSONObject)` 这一层构造最小 JSON
  - 或继续下钻到 `c$d` / `c#updateText(...)`，寻找更像 Breeno 平坦 bean 的字段级注入面

## 现阶段结论

- 小爱内部确实存在一条可追踪的播报链路，但它的价值主要是稳定锚点，不是最终目标。
- 当前最理想的两个点已经收敛出来：
  - `OperationManager#setQueryInfo(...)`
  - `TemplateReactNativeCard#p1(JSONObject)`
- 前者解决“用户输入和 dialogId 在哪里落下”；后者解决“回答文本和完成态在哪里同步”。
- 这两个点组合起来，已经比继续深挖播放器链、发送按钮链、或单纯追 `flowableresult` 更有现实价值。
