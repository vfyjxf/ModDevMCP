---
kind: guidance
skill_id: moddev-usage
category_id: status
interaction_channel: moddevmcp
game_control_policy: declared_operations_only
forbidden_fallbacks:
  - keyboard_input_injection
  - pointer_input_injection
  - external_gui_automation
---

# moddev-usage

Start with the local HTTP service, not an external bridge.

All game interaction must go through ModDevMCP operations. Do not use shell scripts, PowerShell input helpers, or OS-level mouse and keyboard injection as a fallback.

1. Check readiness:

```bash
curl {{baseUri}}/api/v1/status
```

2. Discover categories and skills:

```bash
curl {{baseUri}}/api/v1/categories
curl {{baseUri}}/api/v1/skills
```

3. Read a skill markdown page:

```bash
curl {{baseUri}}/api/v1/skills/status/markdown
```

4. Execute an operation:

```bash
curl -X POST {{baseUri}}/api/v1/requests \
  -H "Content-Type: application/json" \
  -d '{"operationId":"status.get","input":{}}'
```

`targetSide` is optional unless multiple eligible sides are connected for the requested operation. If both client and server can handle the operation, send `targetSide` explicitly.
