# 2026-03-15 ModDevMCP Usage Skill 安装指南

Date: 2026-03-15 02:10 CST

## 用途

把这个仓库里的可复用 `moddevmcp-usage` skill 安装到本地 Codex 环境中。

## Skill 位置

仓库路径：

- `skills/moddevmcp-usage/`

必要文件：

- `skills/moddevmcp-usage/SKILL.md`
- `skills/moddevmcp-usage/agents/openai.yaml`

## 安装

把 skill 目录复制到你的 Codex skills 目录：

```powershell
Copy-Item -Recurse -Force .\skills\moddevmcp-usage $env:USERPROFILE\.codex\skills\
```

或者创建符号链接，让仓库内更新能直接反映过去：

```powershell
New-Item -ItemType SymbolicLink -Path $env:USERPROFILE\.codex\skills\moddevmcp-usage -Target (Resolve-Path .\skills\moddevmcp-usage)
```

## 校验

校验这个 skill 目录：

```powershell
python C:\Users\vfyjx\.codex\skills\.system\skill-creator\scripts\quick_validate.py .\skills\moddevmcp-usage
```

然后重启 Codex 或打开一个新会话，让 skill 列表刷新。

## 使用

需要时显式调用：

```text
$moddevmcp-usage
```

或者直接这样要求它工作：

```text
Use $moddevmcp-usage to check whether ModDevMCP is installed correctly and whether the game is ready before taking screenshots.
```
