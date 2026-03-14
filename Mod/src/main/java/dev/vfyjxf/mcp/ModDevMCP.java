package dev.vfyjxf.mcp;

import dev.vfyjxf.mcp.api.ModMcpApi;
import dev.vfyjxf.mcp.runtime.RuntimeRegistries;
import dev.vfyjxf.mcp.runtime.event.RuntimeEventPublisher;
import dev.vfyjxf.mcp.runtime.hotswap.HotswapRuntimeConfig;
import dev.vfyjxf.mcp.runtime.hotswap.HotswapService;
import dev.vfyjxf.mcp.runtime.tool.EventToolProvider;
import dev.vfyjxf.mcp.runtime.tool.HotswapToolProvider;
import dev.vfyjxf.mcp.server.ModDevMcpServer;
import dev.vfyjxf.mcp.server.api.McpToolProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

public class ModDevMCP {
    public static final Logger LOGGER = LoggerFactory.getLogger(ModDevMCP.class);
    public static final String modId = "mod_dev_mcp";

    private final ModDevMcpServer server;
    private final RuntimeRegistries registries;
    private final ModMcpApi api;
    private final McpToolProvider eventToolProvider;
    private final McpToolProvider hotswapToolProvider;
    private final Set<McpToolProvider> registeredToolProviders = Collections.newSetFromMap(new IdentityHashMap<>());
    private boolean commonRuntimeRegistered;
    private boolean clientRuntimeRegistered;
    private boolean clientProvidersRegistered;


    public ModDevMCP() {
        this(new ModDevMcpServer(), new RuntimeRegistries());
    }

    public ModDevMCP(ModDevMcpServer server) {
        this(server, new RuntimeRegistries());
    }

    public ModDevMCP(ModDevMcpServer server, RuntimeRegistries registries) {
        LOGGER.info("Initializing ModDev MCP");
        this.server = server;
        this.registries = registries;
        this.api = new ModMcpApi(registries);
        this.eventToolProvider = new EventToolProvider(registries);
        HotswapService hotswapService = new HotswapService(HotswapRuntimeConfig.fromSystemProperties());
        hotswapService.snapshotTimestamps();
        this.hotswapToolProvider = new HotswapToolProvider(hotswapService);
        registerCommonRuntime();
    }

    public synchronized void registerBuiltinProviders() {
        prepareClientServer();
    }

    public synchronized void registerCommonProviders() {
        registerToolProvider(eventToolProvider);
        registerToolProvider(hotswapToolProvider);
        registries.toolProviders().forEach(this::registerToolProvider);
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
        return prepareClientServer();
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

    void registerToolProvider(McpToolProvider provider) {
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
}
