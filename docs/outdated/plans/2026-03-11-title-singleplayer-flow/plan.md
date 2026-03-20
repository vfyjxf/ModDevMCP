# Title Singleplayer Flow Plan

Date: 2026-03-11 19:50 CST

## Goal

Drive the title screen into the world-selection screen by clicking the extracted `button-singleplayer` target, and save screenshots/logs for each step.

## Tasks

1. Add plan/checklist/impl docs for the title-to-singleplayer runtime flow.
2. Implement a reproducible runtime script that:
   - captures the title screen
   - queries `button-singleplayer`
   - clicks its center via `moddev.input_action`
   - polls `moddev.ui_get_live_screen` until the screen changes
   - captures the resulting world-selection screen
3. Run the real flow on `runClient` and record the actual result paths and screen transitions.
