# UI Intent Actions Checklist

Date: 2026-03-12 10:40 CST
Updated: 2026-03-12 10:40 CST

- [x] Task 1: replace registry/schema coverage from `ui_open` to `ui_run_intent`
- [x] Task 2: replace tool invocation behavior and remove `ui_open`
- [x] Task 3: wire `ui_run_intent` to real input execution and keep multi-controller extensibility
- [x] Task 4: update active docs and historical references touched by this change set
- [ ] Task 5: run real runtime verification in game client

## Verification Summary

- `:Mod:test --tests "*MinecraftInputControllerTest" --tests "*InputToolProviderTest" --tests "*UiToolInvocationTest" --no-daemon`
  - result: `BUILD SUCCESSFUL`
- `:Mod:test --tests "*MinecraftInputControllerTest" --tests "*InputToolProviderTest" --tests "*UiToolInvocationTest" --tests "*UiToolProviderTest" --no-daemon`
  - result: `BUILD SUCCESSFUL`

## Notes

- this round fixed real code issues found by review:
  - `unsupported_intent` no longer stops at the first controller
  - `ui_run_intent` no longer depends on a fallback-driver special case
  - `ui_run_intent` no longer leaks `controller` into its payload
- Task 5 is still pending because no live `runClient` verification was executed in this round
