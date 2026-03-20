# Capture Failure On Fallback Checklist

Date: 2026-03-11

- [x] Task 1: reproduce that stable-server MCP `ui_capture` could still return placeholder images under fallback UI context
- [x] Task 2: convert the placeholder capture expectation into a failing test first
- [x] Task 3: change `UiToolProvider` so `ui_capture` returns `capture_unavailable` instead of generating fallback screenshots
- [x] Task 4: update affected UI capture tests so real-provider paths still verify multi-target and exclude behavior
- [x] Task 5: run focused UI tool tests and bootstrap/stable-server regression tests
- [x] Task 6: restart `:Mod:runClient` and verify real MCP calls now return failure for fallback capture instead of placeholder images
