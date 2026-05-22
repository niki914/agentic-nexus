# Config Resolution

## 目的

本文件后续用于描述本地配置、远程配置、IPC 桥接、server 回退策略的完整读写链路。

## 后续应填充的信息

- 主 App 如何刷新远程配置
- 宿主进程如何读取已持久化配置
- `XIpcBridge`、`SettingsContentProvider`、持久化层分别负责什么
- server 端路径匹配和最近版本回退规则是什么
- 配置优先级如何定义

## 建议引用的源码位置

- `app/src/main/java/.../mod/XService.kt`
- `app/src/main/java/.../mod/HookLocalSettings.kt`
- `ipc/src/main/java/.../XIpcBridge.kt`
- `ipc/src/main/java/.../cp/SettingsContentProvider.kt`
- `ipc/src/main/java/.../store/`
- `server/server.py`
- `server/com.heytap.speechassist/120803/config.json`
- `server/com.miui.voiceassist/507013003/config.json`

## 写作约束

- 不要复制 JSON 样例全文，除非是非常小的结构说明
- 不要抄源码中的解析逻辑
- 重点写来源、优先级、读写方向、相对路径
