# Embedded Game MCP Cleanup Checklist

Date: 2026-03-12 07:22 CST
Updated: 2026-03-12 07:47 CST

- [x] Task 1: remove stable-server/backend runtime and bootstrap code from `Server`
- [x] Task 2: remove stable-server/backend tests and references
- [x] Task 3: remove mod-side stable/reconnect leftovers and keep embedded runtime path
- [x] Task 4: update docs and sanitize paths
- [x] Task 5: run formatting/verification and record exact results

## Verification Summary

- `:Server:compileJava :Server:test --tests '*McpProtocolDispatcherTest' --tests '*ModDevMcpServerFactoryTest' --no-daemon`
  - result: `BUILD SUCCESSFUL`
- `:Mod:compileJava :Mod:test --tests '*EmbeddedModDevMcpStdioMainTest' --tests '*BuiltinProviderRegistrationTest' --tests '*UiToolInvocationTest' --no-daemon`
  - result: `BUILD SUCCESSFUL`
- `:Mod:test --tests '*DevUiCaptureVerificationRunnerTest' --no-daemon`
  - result: `BUILD SUCCESSFUL`
- `:Server:test :Mod:test --no-daemon`
  - result: `BUILD SUCCESSFUL`
- `TestMod\gradlew.bat compileJava --no-daemon`
  - result: `BUILD SUCCESSFUL`

## Cleanup Notes

- deleted the dedicated stable-server guide and the stable-server/simple-stable/runtime-status plan sets
- sanitized current user-facing path examples to `<repo>\...`
- repository search for configured formatter tasks returned no matches, so this round used compile-safe manual cleanup instead of a formatter task
