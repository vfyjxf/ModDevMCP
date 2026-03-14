# 2026-03-14 Remove Dev UI Capture Flag Design

## Context

当前仓库里还保留了一条仅用于开发期自动 UI capture 验证的开关链路：

- `Mod` 侧的 `moddevmcp.devUiCapture` 系统属性
- `DevUiCaptureFlags` / `ClientDevUiCaptureVerifier`
- `buildSrc` 的 `ModDevClientRunFlags`
- `TestMod/build.gradle` 对该属性的透传

这条链路不会影响正式 MCP UI capture 能力，只控制客户端启动后是否自动做一次开发验证。现在用户明确要求删除该属性和相关类，并同步清理构建脚本与文档。

## Decision

直接删除整条开关链路，不保留兼容壳：

- 删除 `DevUiCaptureFlags`
- 删除 `ClientDevUiCaptureVerifier`，并从 `ClientEntrypoint` 去掉挂载
- 删除 `buildSrc` 下仅为该属性服务的 `ModDevClientRunFlags` 及其测试
- 删除 `TestMod/build.gradle` 中对 `moddevmcp.devUiCapture` 的解析和透传
- 清理文档中的该属性示例和说明

`DevUiCaptureVerificationRunner` 本身先保留，因为它不是属性容器，也可能仍被直接测试或后续手工验证复用；本次只删“自动启用链路”。

## Non-Goals

- 不修改 `moddev.ui_capture` 工具本身
- 不重构其他 MCP host/gateway/runtime 逻辑
- 不新增替代配置项

## Validation

需要验证三件事：

1. 相关单测先按删除目标改到失败，再完成删除后恢复通过
2. `buildSrc` / `Mod` / `Plugin` / `Server` 全仓测试继续通过
3. 文档与构建脚本中不再残留 `moddevmcp.devUiCapture`
