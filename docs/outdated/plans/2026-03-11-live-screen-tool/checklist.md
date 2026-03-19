# Live Screen Tool Checklist

Date: 2026-03-11 16:40 CST

- [x] Add `ClientScreenMetrics` / `ClientScreenProbe` abstraction on Mod side
- [x] Add live probe implementation backed by `Minecraft.getInstance()`
- [x] Register `moddev.ui_get_live_screen` in `UiToolProvider`
- [x] Expose stable-server metadata for `moddev.ui_get_live_screen`
- [x] Cover provider schema and invocation behavior with unit tests
- [x] Cover Mod bootstrap registration with regression tests
- [x] Real-verify against `TestMod:runClient`
- [x] Record real cold-start issue caused by stale stable-server process
- [x] Save one real framebuffer capture after live-screen tool verification
