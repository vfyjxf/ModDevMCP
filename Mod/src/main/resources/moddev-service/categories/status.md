---
kind: category
category_id: status
interaction_channel: moddevmcp
game_control_policy: declared_operations_only
forbidden_fallbacks:
  - keyboard_input_injection
  - pointer_input_injection
  - external_gui_automation
---

# {{title}}

Status skills describe how to inspect service readiness, game readiness, and current troubleshooting state.

Summary: {{summary}}

Operation ids:

{{operationIds}}

Minimal example:

```bash
curl {{baseUri}}/api/v1/status
```

Request example:

{{curlExample}}
