package dev.vfyjxf.mcp.service.http;

import com.sun.net.httpserver.HttpServer;
import dev.vfyjxf.mcp.service.config.ServiceConfig;
import dev.vfyjxf.mcp.service.export.SkillExportService;

import java.net.BindException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.List;
import java.util.Objects;

public final class HttpServiceServer {

    public interface Endpoint {
        void register(HttpServer server);
    }

    private final ServiceConfig config;
    private final HttpServer server;
    private final SkillExportService skillExportService;

    public HttpServiceServer(
            ServiceConfig config,
            StatusEndpoint statusEndpoint,
            CategoriesEndpoint categoriesEndpoint,
            SkillsEndpoint skillsEndpoint,
            OperationsEndpoint operationsEndpoint,
            RequestsEndpoint requestsEndpoint
    ) {
        this(config, statusEndpoint, categoriesEndpoint, skillsEndpoint, operationsEndpoint, requestsEndpoint, null);
    }

    public HttpServiceServer(
            ServiceConfig config,
            StatusEndpoint statusEndpoint,
            CategoriesEndpoint categoriesEndpoint,
            SkillsEndpoint skillsEndpoint,
            OperationsEndpoint operationsEndpoint,
            RequestsEndpoint requestsEndpoint,
            SkillExportService skillExportService
    ) {
        this.config = Objects.requireNonNull(config, "config");
        this.server = createServer(config);
        this.skillExportService = skillExportService;
        registerAll(List.of(
                Objects.requireNonNull(statusEndpoint, "statusEndpoint"),
                Objects.requireNonNull(categoriesEndpoint, "categoriesEndpoint"),
                Objects.requireNonNull(skillsEndpoint, "skillsEndpoint"),
                Objects.requireNonNull(operationsEndpoint, "operationsEndpoint"),
                Objects.requireNonNull(requestsEndpoint, "requestsEndpoint")
        ));
    }

    public void start() {
        if (skillExportService != null) {
            skillExportService.exportAll();
        }
        server.start();
    }

    public void stop() {
        server.stop(0);
    }

    public URI baseUri() {
        return buildBaseUri(config.host(), server.getAddress().getPort());
    }

    public static URI buildBaseUri(String host, int port) {
        var authorityHost = host;
        if (host.indexOf(':') >= 0 && !(host.startsWith("[") && host.endsWith("]"))) {
            authorityHost = "[" + host + "]";
        }
        return URI.create("http://" + authorityHost + ":" + port);
    }

    private static HttpServer createServer(ServiceConfig config) {
        IOException lastFailure = null;
        for (int candidatePort = config.port(); candidatePort <= 65535; candidatePort++) {
            try {
                return HttpServer.create(new InetSocketAddress(config.host(), candidatePort), 0);
            } catch (BindException bindException) {
                lastFailure = bindException;
            } catch (IOException exception) {
                throw new IllegalStateException("failed to create HTTP service server", exception);
            }
        }
        throw new IllegalStateException("failed to create HTTP service server", lastFailure);
    }

    private void registerAll(List<Endpoint> endpoints) {
        for (var endpoint : endpoints) {
            endpoint.register(server);
        }
    }
}
