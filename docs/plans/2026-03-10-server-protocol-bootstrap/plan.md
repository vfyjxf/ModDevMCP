# Server Protocol Bootstrap Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** add a real MCP server bootstrap that supports core protocol operations over `stdio` while keeping transport wiring extensible for future embedded, socket, or remote transports.

**Architecture:** keep the existing `ModDevMcpServer` registry model as the domain core, add a transport-agnostic protocol dispatcher in `Server`, and provide one concrete `stdio` host that speaks JSON-RPC 2.0 with MCP-shaped methods. The dispatcher owns protocol method routing and payload mapping, while each transport only provides line/message IO and lifecycle.

**Tech Stack:** Java 21, existing `Server` module, JUnit 5, minimal hand-written JSON serialization/parsing already available from the JDK-free codebase constraints.

---

### Task 1: Add failing protocol dispatcher tests

**Files:**
- Create: `Server/src/test/java/dev/vfyjxf/mcp/server/protocol/McpProtocolDispatcherTest.java`
- Reference: `Server/src/main/java/dev/vfyjxf/mcp/server/ModDevMcpServer.java`
- Reference: `Server/src/main/java/dev/vfyjxf/mcp/server/runtime/McpToolRegistry.java`
- Reference: `Server/src/main/java/dev/vfyjxf/mcp/server/runtime/McpResourceRegistry.java`

**Step 1: Write the failing test**

Cover:
- `initialize` returns server info and capabilities
- `tools/list` exposes registered tools
- `tools/call` invokes a registered tool and maps success/failure
- `resources/read` returns registered resource content
- unknown methods return JSON-RPC error payloads

**Step 2: Run test to verify it fails**

Run: `.\gradlew.bat :Server:test --tests "*McpProtocolDispatcherTest" --no-daemon`

Expected: FAIL because dispatcher classes do not exist yet.

**Step 3: Write minimal implementation**

Create a protocol dispatcher and request/response model that covers only:
- JSON-RPC envelope
- `initialize`
- `notifications/initialized`
- `tools/list`
- `tools/call`
- `resources/list`
- `resources/read`

**Step 4: Run test to verify it passes**

Run: `.\gradlew.bat :Server:test --tests "*McpProtocolDispatcherTest" --no-daemon`

Expected: PASS.

### Task 2: Add transport abstraction and stdio host tests

**Files:**
- Create: `Server/src/test/java/dev/vfyjxf/mcp/server/transport/StdioMcpServerHostTest.java`
- Create: `Server/src/main/java/dev/vfyjxf/mcp/server/transport/McpServerTransport.java`
- Create: `Server/src/main/java/dev/vfyjxf/mcp/server/transport/StdioMcpServerHost.java`

**Step 1: Write the failing test**

Cover:
- host reads one JSON-RPC request from an input stream and writes one response
- host can process multiple sequential requests
- host ignores `notifications/initialized` without writing an error

**Step 2: Run test to verify it fails**

Run: `.\gradlew.bat :Server:test --tests "*StdioMcpServerHostTest" --no-daemon`

Expected: FAIL because transport host does not exist yet.

**Step 3: Write minimal implementation**

Keep transport interface small:
- `serve()` for blocking lifecycle
- transport-specific IO only
- no registry logic in transport classes

**Step 4: Run test to verify it passes**

Run: `.\gradlew.bat :Server:test --tests "*StdioMcpServerHostTest" --no-daemon`

Expected: PASS.

### Task 3: Add executable bootstrap and smoke tests

**Files:**
- Create: `Server/src/main/java/dev/vfyjxf/mcp/server/bootstrap/ModDevMcpStdioMain.java`
- Create: `Server/src/test/java/dev/vfyjxf/mcp/server/bootstrap/ModDevMcpStdioMainTest.java`
- Modify: `README.md`

**Step 1: Write the failing test**

Cover:
- bootstrap builds a server instance
- bootstrap registers demo or built-in providers passed in by factory code
- README documents how Codex `mcp_servers.*` should invoke the stdio entrypoint

**Step 2: Run test to verify it fails**

Run: `.\gradlew.bat :Server:test --tests "*ModDevMcpStdioMainTest" --no-daemon`

Expected: FAIL because bootstrap main and docs are missing.

**Step 3: Write minimal implementation**

Add:
- a `main(String[] args)` entrypoint
- a simple server factory hook so future socket/embedded transports can reuse the same dispatcher
- README section with Codex config example

**Step 4: Run test to verify it passes**

Run: `.\gradlew.bat :Server:test --tests "*ModDevMcpStdioMainTest" --no-daemon`

Expected: PASS.

### Task 4: Run full verification

**Files:**
- Verify: `Server/src/main/java/dev/vfyjxf/mcp/server/**`
- Verify: `Server/src/test/java/dev/vfyjxf/mcp/server/**`
- Verify: `README.md`

**Step 1: Run focused server tests**

Run: `.\gradlew.bat :Server:test --no-daemon`

Expected: PASS.

**Step 2: Run combined regression tests**

Run: `.\gradlew.bat :Mod:test :Server:test --no-daemon`

Expected: PASS unless blocked by external runtime downloads.

**Step 3: Run manual stdio smoke check**

Run a one-shot process that sends `initialize` and `tools/list` JSON-RPC messages to the stdio host.

Expected: valid JSON-RPC responses.

**Step 4: Run real client verification**

Run: `.\gradlew.bat :Mod:runClient --no-daemon -Dmoddevmcp.devUiCapture=true`

Expected: if runtime assets/dependencies are available, the client starts and writes a dev UI capture verification report after opening a GUI. If asset download or repository access fails, report it explicitly as an environment dependency issue.
