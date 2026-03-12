# Hover And Runtime Flow Design

Date: 2026-03-11 18:35 CST

## Goal

Add GUI-coordinate hover input and a project-local real runtime regression flow that captures a screenshot after every step.

## Scope

### Input

- extend `moddev.input_action`
- add `action = "move"`
- add `action = "hover"`
- default `coordinateSpace = "gui"`
- preserve explicit `coordinateSpace = "framebuffer"`

### Runtime flow

- add a project-local tool to drive the existing `game MCP` bridge
- each step must:
  - call `moddev.ui_get_live_screen`
  - execute one MCP action
  - call `moddev.ui_capture`
  - persist step metadata plus screenshot path

## Recommended Approach

- keep hover inside the existing input controller path instead of creating a new MCP tool
- implement `hover` as `mouseMoved(x, y)` plus optional small delay
- implement a minimal scripted runtime flow runner in the repository so future real-game checks are reproducible

## First Scenario

- title screen only
- move/hover to a stable button region
- save screenshots before and after hover
- verify screenshot artifacts exist

## Non-Goal

- do not add drag-path simulation in this step
- do not automate full world creation flow yet
