# Server-Hosted Embedded Game MCP Checklist

Date: 2026-03-12 07:53 CST
Updated: 2026-03-12 08:00 CST

- [x] Task 1: add server-owned embedded socket runtime bootstrap
- [x] Task 2: switch mod-side game runtime startup to the new server bootstrap
- [x] Task 3: move embedded stdio bootstrap ownership to `Server`
- [x] Task 4: clean up duplicated/aged bootstrap code and naming
- [x] Task 5: run verification and record exact results

## Verification Summary

- `:Server:test --tests '*EmbeddedGameMcpRuntimeTest' --tests '*ModDevMcpServerFactoryTest' --tests '*McpProtocolDispatcherTest'`
  - result: `BUILD SUCCESSFUL`
- `:Mod:test --tests '*GameMcpBridgeMainTest' --tests '*EmbeddedModDevMcpStdioMainTest' --tests '*BuiltinProviderRegistrationTest'`
  - result: `BUILD SUCCESSFUL`
- combined focused run of `:Server:test` and `:Mod:test`
  - first result: failed because Gradle could not delete `Server/build/test-results/test/binary/output.bin` and `Mod/build/test-results/test/binary/output.bin`
  - classification: local file-lock/build-output issue, not a Java compile/test assertion failure
  - rerun result: `BUILD SUCCESSFUL`
- broad verification:
  - `.\gradlew.bat :Server:test :Mod:test --no-daemon`
  - `TestMod\gradlew.bat compileJava --no-daemon`
  - both `BUILD SUCCESSFUL`
