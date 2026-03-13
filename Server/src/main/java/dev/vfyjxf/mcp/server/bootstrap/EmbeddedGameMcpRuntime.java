package dev.vfyjxf.mcp.server.bootstrap;

import dev.vfyjxf.mcp.server.ModDevMcpServer;
import dev.vfyjxf.mcp.server.transport.SocketMcpServerHost;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public final class EmbeddedGameMcpRuntime implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmbeddedGameMcpRuntime.class);

    private final SocketMcpServerHost socketHost;

    private EmbeddedGameMcpRuntime(SocketMcpServerHost socketHost) {
        this.socketHost = socketHost;
    }

    public static EmbeddedGameMcpRuntime start(ModDevMcpServer server) throws IOException {
        return start(server, EmbeddedGameMcpConfig.loadResolved());
    }

    public static EmbeddedGameMcpRuntime start(ModDevMcpServer server, EmbeddedGameMcpConfig config) throws IOException {
        var socketHost = SocketMcpServerHost.start(server, config.host(), config.port());
        LOGGER.info("Started embedded game MCP on {}:{}", config.host(), socketHost.port());
        return new EmbeddedGameMcpRuntime(socketHost);
    }

    public int port() {
        return socketHost.port();
    }

    @Override
    public void close() throws IOException {
        socketHost.close();
    }
}
