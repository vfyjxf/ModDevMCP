package dev.vfyjxf.mcp.server.bootstrap;

import io.modelcontextprotocol.spec.McpServerSession;
import io.modelcontextprotocol.spec.McpServerTransportProvider;
import reactor.core.publisher.Mono;

import java.util.List;

final class NoopServerTransport implements McpServerTransportProvider {

    @Override
    public List<String> protocolVersions() {
        return List.of("2025-06-18", "2025-11-05");
    }

    @Override
    public void setSessionFactory(McpServerSession.Factory sessionFactory) {
    }

    @Override
    public Mono<Void> notifyClients(String method, Object params) {
        return Mono.empty();
    }

    @Override
    public Mono<Void> closeGracefully() {
        return Mono.empty();
    }
}
