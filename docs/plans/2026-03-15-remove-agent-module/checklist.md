# 2026-03-15 Remove Agent Module Checklist

Updated: 2026-03-15 00:45 CST

- [x] 写设计文档
- [x] 写实现计划
- [x] 先写插件失败测试，固定“不再注入 `-javaagent` / 不再暴露 agent DSL”
- [x] 先写 hotswap 失败测试，固定“不再依赖 `HotswapAgent`”
- [x] 删除插件里的 agent 字段与注入逻辑
- [x] 将 `HotswapService` 改为直接使用 `Reflect Agents`
- [x] 从构建中移除独立 `Agent` 模块
- [x] 更新用户文档中的 agent 相关示例
- [x] 跑插件测试
- [x] 跑 `Mod` hotswap 定向测试
- [x] 跑 `TestMod createMcpClientFiles`
