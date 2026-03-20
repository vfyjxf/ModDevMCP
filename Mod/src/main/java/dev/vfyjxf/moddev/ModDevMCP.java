package dev.vfyjxf.moddev;

import dev.vfyjxf.moddev.api.ModDevApi;
import dev.vfyjxf.moddev.api.event.RegisterClientOperationsEvent;
import dev.vfyjxf.moddev.api.event.RegisterCommonOperationsEvent;
import dev.vfyjxf.moddev.api.event.RegisterServerOperationsEvent;
import dev.vfyjxf.moddev.api.registrar.ClientRegistrar;
import dev.vfyjxf.moddev.api.registrar.ClientOperationRegistrar;
import dev.vfyjxf.moddev.api.registrar.CommonRegistrar;
import dev.vfyjxf.moddev.api.registrar.CommonOperationRegistrar;
import dev.vfyjxf.moddev.api.registrar.ServerRegistrar;
import dev.vfyjxf.moddev.api.registrar.ServerOperationRegistrar;
import dev.vfyjxf.moddev.registrar.AnnotationRegistrarLookup;
import dev.vfyjxf.moddev.runtime.RuntimeRegistries;
import dev.vfyjxf.moddev.runtime.event.RuntimeEventPublisher;
import dev.vfyjxf.moddev.runtime.hotswap.HotswapRuntimeConfig;
import dev.vfyjxf.moddev.runtime.hotswap.HotswapService;
import dev.vfyjxf.moddev.service.config.ServiceConfig;
import dev.vfyjxf.moddev.service.discovery.GameInstanceRecord;
import dev.vfyjxf.moddev.service.discovery.GameInstanceRegistry;
import dev.vfyjxf.moddev.service.export.SkillExportService;
import dev.vfyjxf.moddev.service.http.CategoriesEndpoint;
import dev.vfyjxf.moddev.service.http.HttpServiceServer;
import dev.vfyjxf.moddev.service.http.OperationsEndpoint;
import dev.vfyjxf.moddev.service.http.RequestsEndpoint;
import dev.vfyjxf.moddev.service.http.SkillsEndpoint;
import dev.vfyjxf.moddev.service.http.StatusEndpoint;
import dev.vfyjxf.moddev.service.runtime.RuntimeOperationBindings;
import dev.vfyjxf.moddev.service.skill.BuiltinSkillCatalog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Collection;
import java.util.function.Supplier;

public class ModDevMCP {
    public static final Logger LOGGER = LoggerFactory.getLogger(ModDevMCP.class);
    public static final String modId = "mod_dev_mcp";

    private final RuntimeRegistries registries;
    private final ModDevApi api;
    private final Supplier<? extends Collection<CommonOperationRegistrar>> commonRegistrarSupplier;
    private final Supplier<? extends Collection<ClientOperationRegistrar>> clientRegistrarSupplier;
    private final Supplier<? extends Collection<ServerOperationRegistrar>> serverRegistrarSupplier;
    private HttpServiceServer httpServiceServer;
    private ServiceConfig httpServiceConfig;
    private GameInstanceRegistry gameInstanceRegistry;
    private String httpServiceSide;
    private GameInstanceRecord httpServiceRecord;
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
        this(new RuntimeRegistries());
    }

    public ModDevMCP(RuntimeRegistries registries) {
        this(
                registries,
                discoveredCommonRegistrars(),
                discoveredClientRegistrars(),
                discoveredServerRegistrars()
        );
    }

    ModDevMCP(
            RuntimeRegistries registries,
            Supplier<? extends Collection<CommonOperationRegistrar>> commonRegistrarSupplier,
            Supplier<? extends Collection<ClientOperationRegistrar>> clientRegistrarSupplier,
            Supplier<? extends Collection<ServerOperationRegistrar>> serverRegistrarSupplier
    ) {
        LOGGER.info("Initializing ModDev MCP");
        this.registries = registries;
        this.api = new ModDevApi(registries);
        this.commonRegistrarSupplier = commonRegistrarSupplier;
        this.clientRegistrarSupplier = clientRegistrarSupplier;
        this.serverRegistrarSupplier = serverRegistrarSupplier;
        var hotswapService = new HotswapService(HotswapRuntimeConfig.fromSystemProperties());
        hotswapService.snapshotTimestamps();
        this.registries.registerHotswapService(hotswapService);
        registerCommonRuntime();
    }

    public synchronized void registerBuiltinProviders() {
        prepareClientServer();
    }

    public synchronized void startHttpService() {
        if (clientSideActive && !serverSideActive) {
            startHttpService("client");
            return;
        }
        if (serverSideActive && !clientSideActive) {
            startHttpService("server");
            return;
        }
        throw new IllegalStateException("startHttpService() requires exactly one active side; use startHttpService(side)");
    }

    public synchronized void startHttpService(String side) {
        if (httpServiceServer != null) {
            return;
        }
        var instanceSide = normalizeSide(side);
        var config = ServiceConfig.loadResolved();
        var registry = new GameInstanceRegistry(config.gameInstancesPath());
        var bindings = new RuntimeOperationBindings(registries, this::statusSnapshot, registries.operationRegistrations());
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
            var instanceRecord = new GameInstanceRecord(
                    baseUri.toString(),
                    baseUri.getPort(),
                    ProcessHandle.current().pid(),
                    startedAt,
                    startedAt
            );
            registry.upsert(instanceSide, instanceRecord);
            this.httpServiceServer = serviceServer;
            this.httpServiceConfig = config;
            this.gameInstanceRegistry = registry;
            this.httpServiceSide = instanceSide;
            this.httpServiceRecord = instanceRecord;
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
            if (gameInstanceRegistry != null && httpServiceSide != null && httpServiceRecord != null) {
                gameInstanceRegistry.removeIfSame(httpServiceSide, httpServiceRecord);
            }
            httpServiceServer = null;
            httpServiceConfig = null;
            gameInstanceRegistry = null;
            httpServiceSide = null;
            httpServiceRecord = null;
        }
    }

    public synchronized void registerCommonProviders() {
        if (commonProvidersRegistered) {
            return;
        }
        commonProvidersRegistered = true;
        registerCommonRegistrarProviders();
    }

    public RuntimeRegistries registries() {
        return registries;
    }

    public ModDevApi api() {
        return api;
    }

    public synchronized ModDevMCP prepareServer() {
        return new ServerRuntimeBootstrap(this).prepareServer();
    }

    public synchronized ModDevMCP prepareCommonServer() {
        registerCommonRuntime();
        registerCommonProviders();
        return this;
    }

    public synchronized ModDevMCP prepareClientServer() {
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
        registries.eventPublisher().publish(new dev.vfyjxf.moddev.api.event.EventEnvelope("runtime", "bootstrap", System.currentTimeMillis(), java.util.Map.of()));
    }

    public RuntimeEventPublisher eventPublisher() {
        return registries.eventPublisher();
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
        var event = new RegisterClientOperationsEvent(api, registries.eventPublisher());
        clientRegistrarSupplier.get().forEach(registrar -> registrar.register(event));
    }

    void registerServerRegistrarProviders() {
        var event = new RegisterServerOperationsEvent(api, registries.eventPublisher());
        serverRegistrarSupplier.get().forEach(registrar -> registrar.register(event));
    }

    private void registerCommonRegistrarProviders() {
        var event = new RegisterCommonOperationsEvent(api, registries.eventPublisher());
        commonRegistrarSupplier.get().forEach(registrar -> registrar.register(event));
    }

    private static Supplier<Collection<CommonOperationRegistrar>> discoveredCommonRegistrars() {
        return () -> new AnnotationRegistrarLookup<>(CommonOperationRegistrar.class, CommonRegistrar.class).findRegistrars();
    }

    private static Supplier<Collection<ClientOperationRegistrar>> discoveredClientRegistrars() {
        return () -> new AnnotationRegistrarLookup<>(ClientOperationRegistrar.class, ClientRegistrar.class).findRegistrars();
    }

    private static Supplier<Collection<ServerOperationRegistrar>> discoveredServerRegistrars() {
        return () -> new AnnotationRegistrarLookup<>(ServerOperationRegistrar.class, ServerRegistrar.class).findRegistrars();
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

