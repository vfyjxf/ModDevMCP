# moddev-entry

Start with the local HTTP service, not an external bridge.

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
