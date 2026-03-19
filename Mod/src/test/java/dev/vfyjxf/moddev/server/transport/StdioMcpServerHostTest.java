package dev.vfyjxf.moddev.server.transport;

import dev.vfyjxf.moddev.server.ModDevMcpServer;
import dev.vfyjxf.moddev.server.api.McpToolDefinition;
import dev.vfyjxf.moddev.server.api.ToolResult;
import dev.vfyjxf.moddev.server.protocol.McpProtocolDispatcher;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class StdioMcpServerHostTest {

    private static final String DEBUG_LOG_PROPERTY = "moddevmcp.stdio.debugLog";

    @Test
    void hostProcessesSingleInitializeRequest() {
        var output = new ByteArrayOutputStream();
        var host = new StdioMcpServerHost(
                new ByteArrayInputStream(frame("""
                        {"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-11-05"}}
                        """)),
                output,
                new McpProtocolDispatcher(demoServer())
        );

        host.serve();

        var body = extractSingleBody(output.toString(StandardCharsets.UTF_8));
        assertTrue(body.contains("\"jsonrpc\":\"2.0\""));
        assertTrue(body.contains("\"protocolVersion\":\"2025-11-05\""));
        assertTrue(body.contains("\"serverInfo\""));
    }

    @Test
    void hostPreservesIntegralJsonRpcIdsWithoutDecimalSuffix() {
        var output = new ByteArrayOutputStream();
        var host = new StdioMcpServerHost(
                new ByteArrayInputStream(frame("""
                        {"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-11-05"}}
                        """)),
                output,
                new McpProtocolDispatcher(demoServer())
        );

        host.serve();

        var body = extractSingleBody(output.toString(StandardCharsets.UTF_8));
        assertFalse(body.contains("\"id\":1.0"));
        var response = new JsonCodec().parseObject(body.getBytes(StandardCharsets.UTF_8));
        assertEquals(1L, ((Number) response.get("id")).longValue());
    }

    @Test
    void hostAcceptsLfOnlyFraming() {
        var output = new ByteArrayOutputStream();
        var host = new StdioMcpServerHost(
                new ByteArrayInputStream(frameWithLfOnly("""
                        {"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-11-05"}}
                        """)),
                output,
                new McpProtocolDispatcher(demoServer())
        );

        host.serve();

        var raw = output.toString(StandardCharsets.UTF_8);
        assertTrue(raw.contains("Content-Length: "));
        assertTrue(raw.contains("\"protocolVersion\":\"2025-11-05\""));
    }

    @Test
    void hostIgnoresLeadingBlankLinesBeforeFirstFrame() {
        var output = new ByteArrayOutputStream();
        var host = new StdioMcpServerHost(
                new ByteArrayInputStream(concat(
                        "\r\n\r\n".getBytes(StandardCharsets.UTF_8),
                        frame("""
                                {"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-11-05"}}
                                """)
                )),
                output,
                new McpProtocolDispatcher(demoServer())
        );

        assertDoesNotThrow(host::serve);

        var body = extractSingleBody(output.toString(StandardCharsets.UTF_8));
        assertTrue(body.contains("\"id\":1"));
        assertTrue(body.contains("\"protocolVersion\":\"2025-11-05\""));
    }

    @Test
    void hostSupportsJsonLineInitializeRequestsForCodexCompatibility() {
        var output = new ByteArrayOutputStream();
        var host = new StdioMcpServerHost(
                new ByteArrayInputStream(("""
                        {"jsonrpc":"2.0","id":0,"method":"initialize","params":{"protocolVersion":"2025-06-18","capabilities":{},"clientInfo":{"name":"codex-mcp-client","title":"Codex","version":"0.112.0"}}}
                        """).getBytes(StandardCharsets.UTF_8)),
                output,
                new McpProtocolDispatcher(demoServer())
        );

        assertDoesNotThrow(host::serve);

        var raw = output.toString(StandardCharsets.UTF_8).strip();
        assertFalse(raw.contains("Content-Length: "));
        assertTrue(raw.contains("\"id\":0"));
        assertTrue(raw.contains("\"protocolVersion\":\"2025-06-18\""));
        assertTrue(raw.contains("\"serverInfo\""));
    }

    @Test
    void hostAcceptsWhitespaceAfterObjectCommas() {
        var output = new ByteArrayOutputStream();
        var host = new StdioMcpServerHost(
                new ByteArrayInputStream(frame("""
                        {"jsonrpc":"2.0", "id":1,
                         "method":"initialize", "params":{"protocolVersion":"2025-11-05", "capabilities":{}, "clientInfo":{"name":"whitespace-test", "version":"0.0.0"}}}
                        """)),
                output,
                new McpProtocolDispatcher(demoServer())
        );

        host.serve();

        var body = extractSingleBody(output.toString(StandardCharsets.UTF_8));
        assertTrue(body.contains("\"protocolVersion\":\"2025-11-05\""));
    }

    @Test
    void hostProcessesMultipleSequentialRequests() {
        var output = new ByteArrayOutputStream();
        var host = new StdioMcpServerHost(
                new ByteArrayInputStream(concat(
                        frame("""
                                {"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-11-05"}}
                                """),
                        frame("""
                                {"jsonrpc":"2.0","id":2,"method":"tools/list"}
                                """)
                )),
                output,
                new McpProtocolDispatcher(demoServer())
        );

        host.serve();

        var raw = output.toString(StandardCharsets.UTF_8);
        assertEquals(2, countFrames(raw));
        assertTrue(raw.contains("\"id\":1"));
        assertTrue(raw.contains("\"id\":2"));
        assertTrue(raw.contains("\"demo.echo\""));
    }

    @Test
    void hostWritesStatusNotificationAfterInitialized() {
        var output = new ByteArrayOutputStream();
        var host = new StdioMcpServerHost(
                new ByteArrayInputStream(frame("""
                        {"jsonrpc":"2.0","method":"notifications/initialized"}
                        """)),
                output,
                new McpProtocolDispatcher(demoServer())
        );

        host.serve();

        var body = extractSingleBody(output.toString(StandardCharsets.UTF_8));
        assertTrue(body.contains("\"method\":\"notifications/moddev/status\""));
        assertTrue(body.contains("\"hostReady\":true"));
        assertTrue(body.contains("\"gameConnected\":false"));
    }

    @Test
    void hostReturnsErrorFrameForUnknownMethod() {
        var output = new ByteArrayOutputStream();
        var host = new StdioMcpServerHost(
                new ByteArrayInputStream(frame("""
                        {"jsonrpc":"2.0","id":7,"method":"prompts/list"}
                        """)),
                output,
                new McpProtocolDispatcher(demoServer())
        );

        host.serve();

        var body = extractSingleBody(output.toString(StandardCharsets.UTF_8));
        assertTrue(body.contains("\"error\""));
        assertTrue(body.contains("\"code\":-32601"));
        assertFalse(body.contains("\"result\""));
    }

    @Test
    void hostWritesPartialHeaderContextToDebugLogWhenStreamEndsMidHeader() throws Exception {
        var logFile = Files.createTempFile("moddevmcp-stdio-debug", ".log");
        var previous = System.getProperty(DEBUG_LOG_PROPERTY);
        System.setProperty(DEBUG_LOG_PROPERTY, logFile.toString());
        try {
            var output = new ByteArrayOutputStream();
            var host = new StdioMcpServerHost(
                    new ByteArrayInputStream("Content-Length: 42\r\n".getBytes(StandardCharsets.UTF_8)),
                    output,
                    new McpProtocolDispatcher(demoServer())
            );

            assertDoesNotThrow(host::serve);

            var log = Files.readString(logFile);
            assertTrue(log.contains("event=eof-before-frame"));
            assertTrue(log.contains("Content-Length: 42"));
        } finally {
            if (previous == null) {
                System.clearProperty(DEBUG_LOG_PROPERTY);
            } else {
                System.setProperty(DEBUG_LOG_PROPERTY, previous);
            }
            Files.deleteIfExists(logFile);
        }
    }

    @Test
    void hostWritesRequestAndResponseBodiesToDebugLogWhenConfigured() throws Exception {
        var logFile = Files.createTempFile("moddevmcp-stdio-debug", ".log");
        var previous = System.getProperty(DEBUG_LOG_PROPERTY);
        System.setProperty(DEBUG_LOG_PROPERTY, logFile.toString());
        try {
            var output = new ByteArrayOutputStream();
            var host = new StdioMcpServerHost(
                    new ByteArrayInputStream(frame("""
                            {"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-11-05"}}
                            """)),
                    output,
                    new McpProtocolDispatcher(demoServer())
            );

            host.serve();

            var log = Files.readString(logFile);
            assertTrue(log.contains("direction=in"));
            assertTrue(log.contains("\"method\":\"initialize\""));
            assertTrue(log.contains("direction=out"));
            assertTrue(log.contains("\"serverInfo\""));
        } finally {
            if (previous == null) {
                System.clearProperty(DEBUG_LOG_PROPERTY);
            } else {
                System.setProperty(DEBUG_LOG_PROPERTY, previous);
            }
            Files.deleteIfExists(logFile);
        }
    }

    private ModDevMcpServer demoServer() {
        var server = new ModDevMcpServer();
        server.registerProvider(registry -> registry.registerTool(
                new McpToolDefinition(
                        "demo.echo",
                        "Echo",
                        "Echoes the input",
                        Map.of("type", "object"),
                        Map.of("type", "object"),
                        List.of("demo"),
                        "either",
                        false,
                        false,
                        "public",
                        "public"
                ),
                (context, arguments) -> ToolResult.success(arguments)
        ));
        return server;
    }

    private byte[] frame(String json) {
        var normalized = json.strip();
        var bytes = normalized.getBytes(StandardCharsets.UTF_8);
        var header = "Content-Length: " + bytes.length + "\r\n\r\n";
        return concat(header.getBytes(StandardCharsets.UTF_8), bytes);
    }

    private byte[] frameWithLfOnly(String json) {
        var normalized = json.strip();
        var bytes = normalized.getBytes(StandardCharsets.UTF_8);
        var header = "Content-Length: " + bytes.length + "\n\n";
        return concat(header.getBytes(StandardCharsets.UTF_8), bytes);
    }

    private byte[] concat(byte[] first, byte[] second) {
        var combined = new byte[first.length + second.length];
        System.arraycopy(first, 0, combined, 0, first.length);
        System.arraycopy(second, 0, combined, first.length, second.length);
        return combined;
    }

    private String extractSingleBody(String raw) {
        var parts = raw.split("\\r\\n\\r\\n", 2);
        return parts.length < 2 ? "" : parts[1];
    }

    private int countFrames(String raw) {
        return raw.split("Content-Length: ", -1).length - 1;
    }
}


