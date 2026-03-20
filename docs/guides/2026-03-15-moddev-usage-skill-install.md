# 2026-03-15 ModDev Usage Skill Install Guide

Date: 2026-03-15 02:10 CST

## Purpose

Install the reusable `moddev-usage` skill from this repository into a local Codex setup.

## Skill Location

Repository path:

- `skills/moddev-usage/`

Required files:

- `skills/moddev-usage/SKILL.md`
- `skills/moddev-usage/agents/openai.yaml`

## Install

Copy the skill directory into your Codex skills directory:

```powershell
Copy-Item -Recurse -Force .\skills\moddev-usage $env:USERPROFILE\.codex\skills\
```

Or create a symlink so local updates in the repository stay visible:

```powershell
New-Item -ItemType SymbolicLink -Path $env:USERPROFILE\.codex\skills\moddev-usage -Target (Resolve-Path .\skills\moddev-usage)
```

## Verify

Validate the skill folder:

```powershell
python C:\Users\vfyjx\.codex\skills\.system\skill-creator\scripts\quick_validate.py .\skills\moddev-usage
```

Then restart Codex or open a new session so the skill list refreshes.

## Use

Invoke it explicitly when needed:

```text
$moddev-usage
```

Or ask for ModDev help in a way that matches the skill description, such as:

```text
Use $moddev-usage to check whether the ModDev HTTP workflow is installed correctly and whether the game is ready before taking screenshots.
```
