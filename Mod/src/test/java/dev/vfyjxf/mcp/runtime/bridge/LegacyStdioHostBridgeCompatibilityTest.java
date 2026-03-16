package dev.vfyjxf.mcp.runtime.bridge;

import dev.vfyjxf.mcp.ModDevMCP;
import dev.vfyjxf.mcp.server.ModDevMcpServer;
import dev.vfyjxf.mcp.server.protocol.McpProtocolDispatcher;
import dev.vfyjxf.mcp.server.transport.StdioMcpServerHost;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertTrue;

class LegacyStdioHostBridgeCompatibilityTest {

    @Test
    void legacyHostListsBuiltinModToolsUsingContentLengthFrames() {
        var mod = new ModDevMCP(new ModDevMcpServer());
        mod.registerBuiltinProviders();

        var output = new ByteArrayOutputStream();
        var host = new StdioMcpServerHost(
                new ByteArrayInputStream(frame("""
                        {"jsonrpc":"2.0","id":1,"method":"tools/list"}
                        """)),
                output,
                new McpProtocolDispatcher(mod.server())
        );

        host.serve();

        var raw = output.toString(StandardCharsets.UTF_8);
        assertTrue(raw.contains("\"moddev.ui_snapshot\""), raw);
        assertTrue(raw.contains("\"moddev.hotswap\""), raw);
    }

    private static byte[] frame(String json) {
        var normalized = json.strip();
        var bytes = normalized.getBytes(StandardCharsets.UTF_8);
        var header = "Content-Length: " + bytes.length + "\r\n\r\n";
        var headerBytes = header.getBytes(StandardCharsets.UTF_8);
        var combined = new byte[headerBytes.length + bytes.length];
        System.arraycopy(headerBytes, 0, combined, 0, headerBytes.length);
        System.arraycopy(bytes, 0, combined, headerBytes.length, bytes.length);
        return combined;
    }
}
