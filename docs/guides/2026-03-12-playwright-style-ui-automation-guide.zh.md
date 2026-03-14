# 2026-03-12 Playwright 风格 UI 自动化指南

Date: 2026-03-12 00:27 CST
Updated: 2026-03-15 00:05 CST

## 用途

用高层 UI 工具形成一条接近 Playwright 的简短调试闭环：

1. inspect
2. act
3. wait
4. screenshot
5. 查看 trace

只有在同一 screen 上需要较长流程并且确实需要稳定引用时，才降级使用更底层的 session/ref 工具。

## 首选工具

- `moddev.ui_run_intent`
- `moddev.ui_inspect`
- `moddev.ui_act`
- `moddev.ui_wait`
- `moddev.ui_screenshot`
- `moddev.ui_trace_recent`

## 底层回退工具

- `moddev.ui_session_open`
- `moddev.ui_session_refresh`
- `moddev.ui_click_ref`
- `moddev.ui_hover_ref`
- `moddev.ui_switch`
- `moddev.ui_press_key`
- `moddev.ui_type_text`
- `moddev.ui_wait_for`
- `moddev.ui_batch`
- `moddev.ui_trace_get`

## 推荐流程

1. 先把生成的 MCP 配置安装到你的 MCP client
2. 调用 `moddev.status`
3. 只有在 `gameConnected=true` 时才继续
4. 调用 `moddev.ui_get_live_screen`
5. 如果需要进入顶层界面，例如 `inventory`、`chat`、`pause_menu`，调用 `moddev.ui_run_intent`
6. 调用 `moddev.ui_inspect`
7. 调用 `moddev.ui_act`
8. 调用 `moddev.ui_wait`
9. 在检查点调用 `moddev.ui_screenshot`
10. 如果需要一段简短的动作历史，调用 `moddev.ui_trace_recent`

## 最小示例

进入顶层界面：

```json
{
  "name": "moddev.ui_run_intent",
  "arguments": {
    "intent": "inventory"
  }
}
```

检查当前 UI：

```json
{
  "name": "moddev.ui_inspect",
  "arguments": {}
}
```

点击目标：

```json
{
  "name": "moddev.ui_act",
  "arguments": {
    "action": "click",
    "locator": {
      "role": "button",
      "text": "Create New World"
    }
  }
}
```

等待下一可见状态：

```json
{
  "name": "moddev.ui_wait",
  "arguments": {
    "condition": "targetAppeared",
    "locator": {
      "role": "button",
      "text": "Create World"
    },
    "timeoutMs": 1000,
    "pollIntervalMs": 50
  }
}
```

截取证明图：

```json
{
  "name": "moddev.ui_screenshot",
  "arguments": {
    "locator": {
      "role": "button",
      "text": "Create World"
    },
    "source": "auto"
  }
}
```

## 稳定失败码

- `runtime_unavailable`
- `screen_unavailable`
- `session_not_found`
- `session_stale`
- `target_stale`
- `target_not_found`
- `batch_step_failed`
- `capture_unavailable`

## 实际规则

对常见 UI 调试流程来说，除非确实需要，否则不要一开始就抓原始完整 snapshot。

优先使用：

1. 先安装生成的 MCP 配置
2. `moddev.status`
3. `moddev.ui_get_live_screen`
4. `moddev.ui_run_intent`
5. `moddev.ui_inspect`
6. `moddev.ui_act`
7. `moddev.ui_wait`
8. `moddev.ui_screenshot`
9. `moddev.ui_trace_recent`

普通消费者接入时，不需要为了使用这条流程而额外写 `modDevMcp {}`。
