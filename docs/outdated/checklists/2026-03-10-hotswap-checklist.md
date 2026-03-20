# Hotswap Tool Provider Implementation Checklist

**Date:** 2026-03-10

## Tasks

- [x] Step 0: Create project documentation (plan, checklist, implementation details)
- [x] Step 1: Create `Agent` subproject with `HotswapAgent` and `HotswapCapabilities`
- [x] Step 2: Wire agent into NeoForge run configs (Mod/build.gradle + settings.gradle)
- [x] Step 3: Create `ClassFileScanner` utility
- [x] Step 4: Create `HotswapService` (compile + reload orchestration)
- [x] Step 5: Create `HotswapToolProvider` (moddev.compile + moddev.hotswap tools)
- [x] Step 6: Register `HotswapToolProvider` in `ModDevMCP.registerBuiltinProviders()`
- [x] Step 7: Add tests (BuiltinProviderRegistrationTest assertions + ClassFileScannerTest)
- [x] Verification: `./gradlew :Mod:test` passes, `./gradlew :Agent:jar` builds with correct manifest
