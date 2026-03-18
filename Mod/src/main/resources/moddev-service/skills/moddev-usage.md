# moddev-usage

Start with the local HTTP service, not an external bridge.

All game interaction must go through ModDevMCP operations. Do not use shell scripts, PowerShell input helpers, or OS-level mouse and keyboard injection as a fallback.

1. Try the default probe:

```bash
curl http://127.0.0.1:47812/api/v1/status
```

2. If default probe fails, read the project-local registry:

```text
{{projectRegistryPathHint}}
```

3. Probe each candidate baseUrl from the registry with `/api/v1/status` and keep only live instances.

4. Route by operation side support. If both sides are eligible and live, send `targetSide` explicitly.

5. Discover categories and skills:

```bash
curl {{baseUri}}/api/v1/categories
curl {{baseUri}}/api/v1/skills
```

6. Read a skill markdown page:

```bash
curl {{baseUri}}/api/v1/skills/status/markdown
```

7. Execute an operation:

```bash
curl -X POST {{baseUri}}/api/v1/requests \
  -H "Content-Type: application/json" \
  -d '{"operationId":"status.get","input":{}}'
```

`targetSide` is required only when both eligible sides are live for the operation. Omit it when the operation does not support side routing or only one eligible side is live.
