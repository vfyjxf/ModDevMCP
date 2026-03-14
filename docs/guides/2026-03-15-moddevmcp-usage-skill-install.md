# 2026-03-15 ModDevMCP Usage Skill Install Guide

Date: 2026-03-15 02:10 CST

## Purpose

Install the reusable `moddevmcp-usage` skill from this repository into a local Codex setup.

## Skill Location

Repository path:

- `skills/moddevmcp-usage/`

Required files:

- `skills/moddevmcp-usage/SKILL.md`
- `skills/moddevmcp-usage/agents/openai.yaml`

## Install

Copy the skill directory into your Codex skills directory:

```powershell
Copy-Item -Recurse -Force .\skills\moddevmcp-usage $env:USERPROFILE\.codex\skills\
```

Or create a symlink so local updates in the repository stay visible:

```powershell
New-Item -ItemType SymbolicLink -Path $env:USERPROFILE\.codex\skills\moddevmcp-usage -Target (Resolve-Path .\skills\moddevmcp-usage)
```

## Verify

Validate the skill folder:

```powershell
python C:\Users\vfyjx\.codex\skills\.system\skill-creator\scripts\quick_validate.py .\skills\moddevmcp-usage
```

Then restart Codex or open a new session so the skill list refreshes.

## Use

Invoke it explicitly when needed:

```text
$moddevmcp-usage
```

Or ask for ModDevMCP help in a way that matches the skill description, such as:

```text
Use $moddevmcp-usage to check whether ModDevMCP is installed correctly and whether the game is ready before taking screenshots.
```
