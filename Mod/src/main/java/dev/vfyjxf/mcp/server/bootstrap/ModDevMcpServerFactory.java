package dev.vfyjxf.mcp.server.bootstrap;

import dev.vfyjxf.mcp.server.ModDevMcpServer;
import dev.vfyjxf.mcp.server.host.transport.RuntimeHost;
import dev.vfyjxf.mcp.server.protocol.McpProtocolDispatcher;

/**
 * Minimal bootstrap helpers for the built-in JSON-RPC transport stack.
 *
 * <p>The old SDK-backed factory also created Model Context Protocol SDK server wrappers.
 * Those helpers are intentionally removed now that the runtime only uses the in-repo
 * dispatcher and transport implementation.
 */
public final class ModDevMcpServerFactory {

    private ModDevMcpServerFactory() {
    }

    public static ModDevMcpServer createServer() {
        return new ModDevMcpServer();
    }

    public static McpProtocolDispatcher createDispatcher(ModDevMcpServer server) {
        return new McpProtocolDispatcher(server);
    }

    public static RuntimeHost startRuntimeHost(ModDevMcpServer server, HostEndpointConfig config) throws java.io.IOException {
        return RuntimeHost.start(server.runtimeRegistry(), config.host(), config.port(), server.callScheduler());
    }
}
