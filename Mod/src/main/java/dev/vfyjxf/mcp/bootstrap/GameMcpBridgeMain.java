package dev.vfyjxf.mcp.bootstrap;

import dev.vfyjxf.mcp.server.bootstrap.EmbeddedGameMcpConfig;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public final class GameMcpBridgeMain {

    private GameMcpBridgeMain() {
    }

    public static void main(String[] args) throws IOException {
        run(System.in, System.out, EmbeddedGameMcpConfig.loadResolved());
    }

    static void run(InputStream input, OutputStream output, EmbeddedGameMcpConfig config) throws IOException {
        try (var socket = new Socket(config.host(), config.port())) {
            SocketBridgeProxy.proxy(input, output, socket);
        }
    }
}
