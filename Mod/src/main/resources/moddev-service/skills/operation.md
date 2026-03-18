---
kind: operation
operation_id: {{operationId}}
category_id: {{categoryId}}
interaction_channel: moddevmcp
game_control_policy: declared_operations_only
forbidden_fallbacks:
  - keyboard_input_injection
  - pointer_input_injection
  - external_gui_automation
---

# {{title}}

Operation id: `{{operationId}}`

Category: `{{categoryId}}`

{{summary}}

Use this operation through ModDevMCP request APIs. Do not replace it with shell-driven keyboard input, mouse movement, or other OS-level automation.

Target side rule:

{{targetSideRule}}

Request example:

{{curlExample}}

Common failure codes:

{{commonFailureCodes}}
