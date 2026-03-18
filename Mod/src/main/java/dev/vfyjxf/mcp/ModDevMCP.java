package dev.vfyjxf.mcp;

import dev.vfyjxf.mcp.api.ModMcpApi;
import dev.vfyjxf.mcp.api.event.RegisterClientMcpToolsEvent;
import dev.vfyjxf.mcp.api.event.RegisterCommonMcpToolsEvent;
import dev.vfyjxf.mcp.api.event.RegisterServerMcpToolsEvent;
import dev.vfyjxf.mcp.api.registrar.ClientMcpRegistrar;
import dev.vfyjxf.mcp.api.registrar.ClientMcpToolRegistrar;
import dev.vfyjxf.mcp.api.registrar.CommonMcpRegistrar;
import dev.vfyjxf.mcp.api.registrar.CommonMcpToolRegistrar;
import dev.vfyjxf.mcp.api.registrar.ServerMcpRegistrar;
import dev.vfyjxf.mcp.api.registrar.ServerMcpToolRegistrar;
import dev.vfyjxf.mcp.registrar.AnnotationMcpRegistrarLookup;
import dev.vfyjxf.mcp.runtime.RuntimeRegistries;
import dev.vfyjxf.mcp.runtime.event.RuntimeEventPublisher;
import dev.vfyjxf.mcp.runtime.hotswap.HotswapRuntimeConfig;
import dev.vfyjxf.mcp.runtime.hotswap.HotswapService;
import dev.vfyjxf.mcp.runtime.tool.HotswapToolProvider;
import dev.vfyjxf.mcp.server.ModDevMcpServer;
import dev.vfyjxf.mcp.server.api.McpToolProvider;
import dev.vfyjxf.mcp.server.api.ToolCallContext;
import dev.vfyjxf.mcp.service.config.ServiceConfig;
import dev.vfyjxf.mcp.service.discovery.GameInstanceRecord;
import dev.vfyjxf.mcp.service.discovery.GameInstanceRegistry;
import dev.vfyjxf.mcp.service.export.SkillExportService;
import dev.vfyjxf.mcp.service.http.CategoriesEndpoint;
import dev.vfyjxf.mcp.service.http.HttpServiceServer;
import dev.vfyjxf.mcp.service.http.OperationsEndpoint;
import dev.vfyjxf.mcp.service.http.RequestsEndpoint;
import dev.vfyjxf.mcp.service.http.SkillsEndpoint;
import dev.vfyjxf.mcp.service.http.StatusEndpoint;
import dev.vfyjxf.mcp.service.runtime.RuntimeOperationBindings;
import dev.vfyjxf.mcp.service.skill.BuiltinSkillCatalog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import java.time.Instant;
import java.util.function.Supplier;

public class ModDevMCP {
    public static final Logger LOGGER = LoggerFactory.getLogger(ModDevMCP.class);
    public static final String modId = "mod_dev_mcp";

    private final ModDevMcpServer server;
    private final RuntimeRegistries registries;
    private final ModMcpApi api;
    private final McpToolProvider hotswapToolProvider;
    private final Supplier<? extends Collection<CommonMcpToolRegistrar>> commonRegistrarSupplier;
    private final Supplier<? extends Collection<ClientMcpToolRegistrar>> clientRegistrarSupplier;
    private final Supplier<? extends Collection<ServerMcpToolRegistrar>> serverRegistrarSupplier;
    private final Set<McpToolProvider> registeredToolProviders = Collections.newSetFromMap(new IdentityHashMap<>());
    private HttpServiceServer httpServiceServer;
    private ServiceConfig httpServiceConfig;
    private GameInstanceRegistry gameInstanceRegistry;
    private String httpServiceSide;
    private String httpServiceLastError;
    private boolean commonRuntimeRegistered;
    private boolean commonProvidersRegistered;
    private boolean clientRuntimeRegistered;
    private boolean clientProvidersRegistered;
    private boolean serverRuntimeRegistered;
    private boolean serverProvidersRegistered;
    private boolean clientSideActive;
    private boolean serverSideActive;

    public ModDevMCP() {
        this(new ModDevMcpServer(), new RuntimeRegistries());
    }

    public ModDevMCP(ModDevMcpServer server) {
        this(server, new RuntimeRegistries());
    }

    public ModDevMCP(ModDevMcpServer server, RuntimeRegistries registries) {
        this(
                server,
                registries,
                discoveredCommonRegistrars(),
                discoveredClientRegistrars(),
                discoveredServerRegistrars()
        );
    }

    ModDevMCP(
            ModDevMcpServer server,
            RuntimeRegistries registries,
            Supplier<? extends Collection<CommonMcpToolRegistrar>> commonRegistrarSupplier,
            Supplier<? extends Collection<ClientMcpToolRegistrar>> clientRegistrarSupplier,
            Supplier<? extends Collection<ServerMcpToolRegistrar>> serverRegistrarSupplier
    ) {
        LOGGER.info("Initializing ModDev MCP");
        this.server = server;
        this.registries = registries;
        this.api = new ModMcpApi(registries, this::registerToolProvider);
        this.commonRegistrarSupplier = commonRegistrarSupplier;
        this.clientRegistrarSupplier = clientRegistrarSupplier;
        this.serverRegistrarSupplier = serverRegistrarSupplier;
        HotswapService hotswapService = new HotswapService(HotswapRuntimeConfig.fromSystemProperties());
        hotswapService.snapshotTimestamps();
        this.hotswapToolProvider = new HotswapToolProvider(hotswapService);
        registerCommonRuntime();
    }

    public synchronized void registerBuiltinProviders() {
        prepareClientServer();
    }

    public synchronized void startHttpService() {
        if (clientSideActive) {
            startHttpService("client");
            return;
        }
        if (serverSideActive) {
            startHttpService("server");
            return;
        }
        startHttpService("client");
    }

    public synchronized void startHttpService(String side) {
        if (httpServiceServer != null) {
            return;
        }
        var instanceSide = normalizeSide(side);
        var config = ServiceConfig.loadResolved();
        var registry = new GameInstanceRegistry(config.gameInstancesPath());
        var bindings = new RuntimeOperationBindings(this::invokeOperationTool, this::statusSnapshot);
        var catalog = new BuiltinSkillCatalog().build(config, bindings.operationRegistry());
        var exportService = new SkillExportService(config, catalog.categories(), catalog.skillRegistry());
        var statusEndpoint = new StatusEndpoint(new ServiceStatusProvider(config));
        var requestsEndpoint = new RequestsEndpoint(
                bindings.operationRegistry(),
                this::connectedSides,
                bindings::execute
        );
        var serviceServer = new HttpServiceServer(
                config,
                statusEndpoint,
                new CategoriesEndpoint(catalog.categories()),
                new SkillsEndpoint(catalog.skillRegistry(), exportService),
                new OperationsEndpoint(bindings.operationRegistry()),
                requestsEndpoint,
                exportService
        );
        var startedAt = Instant.now();
        try {
            serviceServer.start();
            var baseUri = serviceServer.baseUri();
            registry.upsert(
                    instanceSide,
                    new GameInstanceRecord(
                            baseUri.toString(),
                            baseUri.getPort(),
                            ProcessHandle.current().pid(),
                            startedAt,
                            startedAt
                    )
            );
            this.httpServiceServer = serviceServer;
            this.httpServiceConfig = config;
            this.gameInstanceRegistry = registry;
            this.httpServiceSide = instanceSide;
            this.httpServiceLastError = null;
        } catch (RuntimeException exception) {
            serviceServer.stop();
            this.httpServiceLastError = exception.getMessage();
            throw exception;
        }
    }

    public synchronized void stopHttpService() {
        if (httpServiceServer == null) {
            return;
        }
        try {
            httpServiceServer.stop();
        } finally {
            if (gameInstanceRegistry != null && httpServiceSide != null) {
                gameInstanceRegistry.remove(httpServiceSide);
            }
            httpServiceServer = null;
            httpServiceConfig = null;
            gameInstanceRegistry = null;
            httpServiceSide = null;
        }
    }

    public synchronized void registerCommonProviders() {
        if (commonProvidersRegistered) {
            return;
        }
        commonProvidersRegistered = true;
        registerToolProvider(hotswapToolProvider);
        registries.toolProviders().forEach(this::registerToolProvider);
        registerCommonRegistrarProviders();
    }

    public ModDevMcpServer server() {
        return server;
    }

    public RuntimeRegistries registries() {
        return registries;
    }

    public ModMcpApi api() {
        return api;
    }

    public synchronized ModDevMcpServer prepareServer() {
        return new ServerRuntimeBootstrap(this).prepareServer();
    }

    public synchronized ModDevMcpServer prepareCommonServer() {
        registerCommonRuntime();
        registerCommonProviders();
        return server;
    }

    public synchronized ModDevMcpServer prepareClientServer() {
        return new ClientRuntimeBootstrap(this).prepareClientServer();
    }

    public synchronized void prepareClientRuntime() {
        new ClientRuntimeBootstrap(this).prepareClientRuntime();
    }

    public synchronized void registerClientProviders() {
        new ClientRuntimeBootstrap(this).registerClientProviders();
    }

    public synchronized void prepareServerRuntime() {
        new ServerRuntimeBootstrap(this).prepareServerRuntime();
    }

    public synchronized void registerServerProviders() {
        new ServerRuntimeBootstrap(this).registerServerProviders();
    }

    public synchronized void activateClientSide() {
        clientSideActive = true;
    }

    public synchronized void deactivateClientSide() {
        clientSideActive = false;
    }

    public synchronized void activateServerSide() {
        serverSideActive = true;
    }

    public synchronized void deactivateServerSide() {
        serverSideActive = false;
    }

    private void registerCommonRuntime() {
        if (commonRuntimeRegistered) {
            return;
        }
        commonRuntimeRegistered = true;
        server.registerResourceProvider(uri -> registries.uiCaptureArtifactStore().readResource(uri));
        registries.eventPublisher().publish(new dev.vfyjxf.mcp.api.event.EventEnvelope("runtime", "bootstrap", System.currentTimeMillis(), java.util.Map.of()));
    }

    public RuntimeEventPublisher eventPublisher() {
        return registries.eventPublisher();
    }

    synchronized void registerToolProvider(McpToolProvider provider) {
        if (registeredToolProviders.add(provider)) {
            server.registerProvider(provider);
        }
    }

    boolean claimClientRuntimeRegistration() {
        if (clientRuntimeRegistered) {
            return false;
        }
        clientRuntimeRegistered = true;
        return true;
    }

    boolean claimClientProviderRegistration() {
        if (clientProvidersRegistered) {
            return false;
        }
        clientProvidersRegistered = true;
        return true;
    }

    boolean claimServerRuntimeRegistration() {
        if (serverRuntimeRegistered) {
            return false;
        }
        serverRuntimeRegistered = true;
        return true;
    }

    boolean claimServerProviderRegistration() {
        if (serverProvidersRegistered) {
            return false;
        }
        serverProvidersRegistered = true;
        return true;
    }

    void registerClientRegistrarProviders() {
        var providers = new ArrayList<McpToolProvider>();
        var event = new RegisterClientMcpToolsEvent(providers, api, registries.eventPublisher());
        clientRegistrarSupplier.get().forEach(registrar -> registrar.register(event));
        providers.forEach(this::registerToolProvider);
    }

    void registerServerRegistrarProviders() {
        var providers = new ArrayList<McpToolProvider>();
        var event = new RegisterServerMcpToolsEvent(providers, api, registries.eventPublisher());
        serverRegistrarSupplier.get().forEach(registrar -> registrar.register(event));
        providers.forEach(this::registerToolProvider);
    }

    private void registerCommonRegistrarProviders() {
        var providers = new ArrayList<McpToolProvider>();
        var event = new RegisterCommonMcpToolsEvent(providers, api, registries.eventPublisher());
        commonRegistrarSupplier.get().forEach(registrar -> registrar.register(event));
        providers.forEach(this::registerToolProvider);
    }

    private static Supplier<Collection<CommonMcpToolRegistrar>> discoveredCommonRegistrars() {
        return () -> new AnnotationMcpRegistrarLookup<>(CommonMcpToolRegistrar.class, CommonMcpRegistrar.class).findRegistrars();
    }

    private static Supplier<Collection<ClientMcpToolRegistrar>> discoveredClientRegistrars() {
        return () -> new AnnotationMcpRegistrarLookup<>(ClientMcpToolRegistrar.class, ClientMcpRegistrar.class).findRegistrars();
    }

    private static Supplier<Collection<ServerMcpToolRegistrar>> discoveredServerRegistrars() {
        return () -> new AnnotationMcpRegistrarLookup<>(ServerMcpToolRegistrar.class, ServerMcpRegistrar.class).findRegistrars();
    }

    private dev.vfyjxf.mcp.server.api.ToolResult invokeOperationTool(
            String toolName,
            String targetSide,
            java.util.Map<String, Object> input
    ) throws Exception {
        var tool = server.registry().findTool(toolName, targetSide)
                .orElseThrow(() -> new IllegalStateException("tool not available: " + toolName));
        var arguments = new java.util.LinkedHashMap<String, Object>();
        arguments.putAll(input);
        if (targetSide != null) {
            arguments.put("targetSide", targetSide);
        }
        var contextSide = targetSide == null ? "either" : targetSide;
        var metadata = java.util.Map.<String, Object>of("runtimeId", contextSide + "-runtime");
        return tool.handler().handle(new ToolCallContext(contextSide, metadata), java.util.Map.copyOf(arguments));
    }

    private RuntimeOperationBindings.StatusSnapshot statusSnapshot() {
        var connectedSides = connectedSides();
        return new RuntimeOperationBindings.StatusSnapshot(
                httpServiceServer != null,
                !connectedSides.isEmpty(),
                connectedSides,
                "moddev-usage",
                httpServiceConfig != null ? httpServiceConfig.exportRoot() : ServiceConfig.loadResolved().exportRoot(),
                httpServiceLastError
        );
    }

    private static String normalizeSide(String side) {
        if ("client".equals(side) || "server".equals(side)) {
            return side;
        }
        throw new IllegalArgumentException("side must be client or server");
    }

    private java.util.List<String> connectedSides() {
        var sides = new java.util.ArrayList<String>(2);
        if (clientSideActive) {
            sides.add("client");
        }
        if (serverSideActive) {
            sides.add("server");
        }
        return java.util.List.copyOf(sides);
    }

    private final class ServiceStatusProvider implements StatusEndpoint.StatusProvider {
        private final ServiceConfig config;

        private ServiceStatusProvider(ServiceConfig config) {
            this.config = config;
        }

        @Override
        public boolean serviceReady() {
            return httpServiceServer != null;
        }

        @Override
        public boolean gameReady() {
            return !connectedSides().isEmpty();
        }

        @Override
        public java.util.List<String> connectedSides() {
            return ModDevMCP.this.connectedSides();
        }

        @Override
        public String usageSkillId() {
            return "moddev-usage";
        }

        @Override
        public java.nio.file.Path exportRoot() {
            return config.exportRoot();
        }

        @Override
        public String lastError() {
            return httpServiceLastError;
        }
    }
}


