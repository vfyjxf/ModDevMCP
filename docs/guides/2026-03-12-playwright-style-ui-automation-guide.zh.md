# 2026-03-12 UI 自动化指南

日期：2026-03-12 00:27 CST
更新：2026-03-20 00:20 CST

## 目的

使用当前 HTTP operation 完成 UI 读取、动作执行与截图导出，完全不依赖旧工具流。

## 推荐 Operation

- `status.live_screen`
- `ui.inspect`
- `ui.action`
- `ui.snapshot`
- `ui.capture`
- `input.action`（原始键盘/鼠标事件）

## 推荐流程

1. 把生成的 service 配置安装到 agent 客户端
2. `GET /api/v1/status`
3. 仅当 `gameReady=true` 继续
4. 用 `status.live_screen` 查询当前屏幕
5. 用 `ui.inspect` 读取 UI
6. 用 `ui.action` 执行动作
7. 用 `ui.capture` 导出截图

需要绕过 UI 语义时，直接用 `input.action`。

## 最小示例

UI 读取：

```json
{
  "operationId": "ui.inspect",
  "input": {}
}
```

点击目标：

```json
{
  "operationId": "ui.action",
  "input": {
    "action": "click",
    "target": {
      "role": "button",
      "text": "Create New World"
    }
  }
}
```

截图导出（推荐默认值，`auto` 会优先走 framebuffer）：

```json
{
  "operationId": "ui.capture",
  "input": {
    "source": "auto",
    "mode": "full"
  }
}
```

原始按键：

```json
{
  "operationId": "input.action",
  "input": {
    "action": "key_press",
    "keyCode": 69,
    "modifiers": 0
  }
}
```

## Capture 参数

`ui.capture` 支持：

- `source`: `auto` / `offscreen` / `framebuffer` / `render`
- `mode`: `full` / `crop`
- `target`: 仅导出目标列表
- `exclude`: 排除导出目标列表

若要做“仅导出”，使用 `mode=crop` 并传入 `target`。

