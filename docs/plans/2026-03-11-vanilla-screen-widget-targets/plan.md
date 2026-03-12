# Vanilla Screen Widget Targets Plan

> **For Codex:** continue with TDD, then verify with real `runClient` screenshots.

Date: 2026-03-11 19:20 CST

## Goal

Add button-level targets for `vanilla-screen` so title-screen hover and future click flows can address real widgets instead of only `screen-root`.

## Tasks

1. Add red tests for extracted button targets, pointer-derived hover, and direct driver query behavior.
2. Implement a minimal widget extractor for `VanillaScreenUiDriver`, keeping non-game unit tests isolated from hard client-class loading.
3. Re-run real title-screen hover flow and confirm `hoveredTarget` and target list in `step-log.json`.
