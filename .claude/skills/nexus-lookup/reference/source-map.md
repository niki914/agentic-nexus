# Source Map

## 目的

本文件后续用于维护高频源码入口的相对路径地图，帮助读者从主题直接跳到真正源码。

## 后续应填充的信息

- Xposed 入口
- 通用 Hook 模板
- Breeno 入口与 subhooks
- XiaoAi 入口与 subhooks
- LLM runtime 入口
- 设置、IPC、server 入口
- 需要高频查看的配置样例路径

## 建议组织方式

建议按主题分组列路径，而不是按文件系统全量转储：

- 启动与入口
- 会话与渲染
- Breeno
- XiaoAi
- 配置与 IPC
- LLM runtime
- UI / PRD / task docs

## 写作约束

- 这里只维护相对路径地图，不写源码解释
- 不要粘贴代码
- 不要把整个目录树无差别复制进来
