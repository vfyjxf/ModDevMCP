# Docs Structure Refactor Implementation

Date: 2026-03-12 09:26 CST

## Status

Completed.

The repository has already switched the actively maintained plan sets to topic directories with fixed filenames:

- `docs/plans/2026-03-11-game-mcp/`
- `docs/plans/2026-03-11-hover-and-runtime-flow/`
- `docs/plans/2026-03-11-title-singleplayer-flow/`
- `docs/plans/2026-03-11-vanilla-screen-widget-targets/`
- `docs/plans/2026-03-11-live-screen-tool/`
- `docs/plans/2026-03-11-capture-failure-on-fallback/`
- `docs/plans/2026-03-11-testmod-composite-build/`
- `docs/plans/2026-03-11-playwright-style-ui-automation/`
- `docs/plans/2026-03-11-ui-live-screen-defaults/`
- `docs/plans/2026-03-12-docs-structure-refactor/`
- `docs/plans/2026-03-12-embedded-game-mcp-cleanup/`
- `docs/plans/2026-03-12-playwright-style-ui-automation/`
- `docs/plans/2026-03-12-server-hosted-embedded-game-mcp/`

`docs/guides` stays flat by design, but the active guides have been reworked to use clearer sections and sanitized path examples such as `<repo>\...`.

## This Round

- Moved these additional active plan sets from suffix filenames to directory form:
  - `2026-03-11-hover-and-runtime-flow`
  - `2026-03-11-title-singleplayer-flow`
  - `2026-03-11-ui-live-screen-defaults`
  - `2026-03-12-docs-structure-refactor`
- Moved this second batch as well:
  - `2026-03-11-vanilla-screen-widget-targets`
  - `2026-03-11-live-screen-tool`
  - `2026-03-11-capture-failure-on-fallback`
  - `2026-03-11-testmod-composite-build`
  - `2026-03-11-playwright-style-ui-automation`
- Updated moved-plan internal references from old flat filenames to folder paths.
- Sanitized one remaining absolute script link in `docs/plans/2026-03-11-title-singleplayer-flow/impl.md`.
- Updated `docs/plans/2026-03-12-embedded-game-mcp-cleanup/impl.md` to point at the new `ui-live-screen-defaults` location.

## Verification

Focused documentation-path tests:

```powershell
$env:GRADLE_USER_HOME='.gradle-user'; .\gradlew.bat :Mod:test --tests "*EmbeddedModDevMcpStdioMainTest" --no-daemon
```

Real result:

- `BUILD SUCCESSFUL`

```powershell
$env:GRADLE_USER_HOME='.gradle-user'; .\gradlew.bat :Server:test --tests "*ModDevMcpStdioMainTest" --no-daemon
```

Real result:

- `BUILD SUCCESSFUL`

Reference scan after this migration batch:

```powershell
rg -n '2026-03-11-(hover-and-runtime-flow|ui-live-screen-defaults|title-singleplayer-flow)-(design|plan|checklist|impl)\.md|2026-03-12-docs-structure-refactor-(plan|checklist|impl)\.md' README.md docs Mod\src\test\java Server\src\test\java
```

Expected interpretation:

- remaining matches should be historical prose or migration-plan text only

## Remaining Work

- If older historical design docs are rewritten in the future, keep using the same directory layout and sanitized path style.
