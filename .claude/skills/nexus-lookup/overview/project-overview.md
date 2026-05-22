# Project Overview

## 目的

本文件后续用于沉淀项目的稳定总览信息，帮助读者先建立整体心智，再进入具体业务或源码。

## 后续应填充的信息

- 项目是什么，核心目标是什么
- 支持哪些宿主，宿主包名分别是什么
- 主要模块有哪些，各模块的职责边界是什么
- 关键入口文件和高频阅读入口有哪些
- 哪些目录存放源码，哪些目录存放配置、任务文档、服务端样例

## 建议引用的源码位置

只写相对路径，不抄源码。优先维护这些入口的相对路径：

- `app/src/main/java/.../Entrance.kt`
- `app/src/main/java/.../AbstractAssistantHook.kt`
- `app/src/main/java/.../chat/`
- `ipc/src/main/java/.../`
- `h/src/main/java/.../`
- `server/server.py`

## 写作约束

- 不要把 `CLAUDE.md` 原文整体复制到这里
- 不要抄类定义、方法体、配置 JSON 全文
- 除非为了说明总架构，否则不要贴代码
- 默认用相对路径指向真正源码
