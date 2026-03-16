package dev.vfyjxf.mcp;

import dev.vfyjxf.mcp.api.event.EventEnvelope;
import dev.vfyjxf.mcp.api.event.RegisterClientMcpToolsEvent;
import dev.vfyjxf.mcp.api.event.RegisterCommonMcpToolsEvent;
import dev.vfyjxf.mcp.api.event.RegisterServerMcpToolsEvent;
import dev.vfyjxf.mcp.api.model.OperationResult;
import dev.vfyjxf.mcp.api.runtime.*;
import dev.vfyjxf.mcp.api.ui.CaptureRequest;
import dev.vfyjxf.mcp.api.ui.SnapshotOptions;
import dev.vfyjxf.mcp.api.ui.TargetSelector;
import dev.vfyjxf.mcp.api.ui.UiInteractionDefaults;
import dev.vfyjxf.mcp.api.ui.UiSnapshot;
import dev.vfyjxf.mcp.api.ui.UiTarget;
import dev.vfyjxf.mcp.api.registrar.ClientMcpToolRegistrar;
import dev.vfyjxf.mcp.api.registrar.CommonMcpToolRegistrar;
import dev.vfyjxf.mcp.api.registrar.ServerMcpToolRegistrar;
import dev.vfyjxf.mcp.runtime.RuntimeRegistries;
import dev.vfyjxf.mcp.server.ModDevMcpServer;
import dev.vfyjxf.mcp.server.api.McpToolDefinition;
import dev.vfyjxf.mcp.server.api.McpToolProvider;
import dev.vfyjxf.mcp.server.api.ToolResult;
import dev.vfyjxf.mcp.server.runtime.McpToolRegistry;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ModDevMcpRegistrarIntegrationTest {

    @Test
    void commonRegistrarsRunDuringCommonServerPreparation() {
        var server = new ModDevMcpServer(new McpToolRegistry());
        var mod = new ModDevMCP(
                server,
                new RuntimeRegistries(),
                () -> List.of(event -> event.register(tool("demo.common"))),
                List::of,
                List::of
        );

        mod.prepareCommonServer();

        assertTrue(server.registry().findTool("demo.common").isPresent());
        assertTrue(server.registry().findTool("demo.client").isEmpty());
        assertTrue(server.registry().findTool("demo.server").isEmpty());
    }

    @Test
    void clientRegistrarsRunOnlyDuringClientPreparation() {
        var server = new ModDevMcpServer(new McpToolRegistry());
        var mod = new ModDevMCP(
                server,
                new RuntimeRegistries(),
                () -> List.of(event -> event.register(tool("demo.common"))),
                () -> List.of(event -> event.register(tool("demo.client"))),
                () -> List.of(event -> event.register(tool("demo.server")))
        );

        mod.prepareClientServer();

        assertTrue(server.registry().findTool("demo.common").isPresent());
        assertTrue(server.registry().findTool("demo.client").isPresent());
        assertTrue(server.registry().findTool("demo.server").isEmpty());
    }

    @Test
    void clientRegistrarEventExposesApiAndClientRuntimeRegistrations() {
        var server = new ModDevMcpServer(new McpToolRegistry());
        var mod = new ModDevMCP(
                server,
                new RuntimeRegistries(),
                List::of,
                () -> List.of(new ClientIntegrationRegistrar()),
                List::of
        );

        mod.prepareClientServer();

        assertTrue(server.registry().findTool("demo.client").isPresent());
        assertTrue(server.registry().findTool("demo.client.api").isPresent());
        assertEquals(1, mod.registries().uiDrivers().all().stream()
                .filter(driver -> "demo.client.driver".equals(driver.descriptor().id()))
                .count());
        assertEquals(2, mod.registries().inputControllers().size());
        var uiContext = new TestUiContext("custom.ClientScreen");
        var uiSnapshot = new UiSnapshot("screen", uiContext.screenClass(), "demo.client.driver", List.of(), List.of(), null, null, null, null, Map.of());
        assertEquals("focused", mod.registries().uiInteractionResolvers()
                .resolve("demo.client.driver", uiContext, List.of())
                .focusedTargetId());
        assertEquals("demo.client.offscreen", mod.registries().uiOffscreenCaptureProviders()
                .select(uiContext, uiSnapshot)
                .orElseThrow()
                .providerId());
        assertEquals("demo.client.framebuffer", mod.registries().uiFramebufferCaptureProviders()
                .select(uiContext, uiSnapshot)
                .orElseThrow()
                .providerId());
        assertTrue(mod.eventPublisher().recentEvents().stream()
                .anyMatch(event -> "demo".equals(event.domain()) && "client-registered".equals(event.type())));
    }

    @Test
    void serverRegistrarsRunOnlyDuringServerPreparation() {
        var server = new ModDevMcpServer(new McpToolRegistry());
        var mod = new ModDevMCP(
                server,
                new RuntimeRegistries(),
                () -> List.of(event -> event.register(tool("demo.common"))),
                () -> List.of(event -> event.register(tool("demo.client"))),
                () -> List.of(event -> event.register(tool("demo.server")))
        );

        mod.prepareServer();

        assertTrue(server.registry().findTool("demo.common").isPresent());
        assertTrue(server.registry().findTool("demo.client").isEmpty());
        assertTrue(server.registry().findTool("demo.server").isPresent());
    }

    @Test
    void commonRegistrarEventExposesApiAndEventPublisher() {
        var server = new ModDevMcpServer(new McpToolRegistry());
        var mod = new ModDevMCP(
                server,
                new RuntimeRegistries(),
                () -> List.of(new CommonIntegrationRegistrar()),
                List::of,
                List::of
        );

        mod.prepareCommonServer();

        assertTrue(server.registry().findTool("demo.common").isPresent());
        assertTrue(server.registry().findTool("demo.common.api").isPresent());
        assertTrue(mod.eventPublisher().recentEvents().stream()
                .anyMatch(event -> "demo".equals(event.domain()) && "common-registered".equals(event.type())));
    }

    private static McpToolProvider tool(String name) {
        return registry -> registry.registerTool(
                new McpToolDefinition(name, name, name, Map.of(), Map.of(), List.of(), "either", false, false, "public", "public"),
                (context, arguments) -> ToolResult.success(Map.of("tool", name))
        );
    }

    private static final class CommonIntegrationRegistrar implements CommonMcpToolRegistrar {
        @Override
        public void register(RegisterCommonMcpToolsEvent event) {
            event.registerToolProvider(tool("demo.common"));
            event.api().registerToolProvider(tool("demo.common.api"));
            event.publishEvent(new EventEnvelope("demo", "common-registered", 1L, Map.of("side", "common")));
            assertSame(event.eventPublisher(), event.api().eventPublisher());
        }
    }

    private static final class ClientIntegrationRegistrar implements ClientMcpToolRegistrar {
        @Override
        public void register(RegisterClientMcpToolsEvent event) {
            event.registerToolProvider(tool("demo.client"));
            event.api().registerToolProvider(tool("demo.client.api"));
            event.registerUiDriver(new TestUiDriver("demo.client.driver"));
            event.registerInputController((action, arguments) -> OperationResult.success(null));
            event.registerUiInteractionStateResolver(new TestUiInteractionResolver("demo.client.driver"));
            event.registerUiOffscreenCaptureProvider(new TestOffscreenCaptureProvider("demo.client.offscreen"));
            event.registerUiFramebufferCaptureProvider(new TestFramebufferCaptureProvider("demo.client.framebuffer"));
            event.publishEvent(new EventEnvelope("demo", "client-registered", 2L, Map.of("side", "client")));
            assertSame(event.eventPublisher(), event.api().eventPublisher());
        }
    }

    private static final class TestUiDriver implements UiDriver {
        private final DriverDescriptor descriptor;

        private TestUiDriver(String id) {
            this.descriptor = new DriverDescriptor(id, "test", 10, Set.of("snapshot"));
        }

        @Override
        public DriverDescriptor descriptor() {
            return descriptor;
        }

        @Override
        public boolean matches(UiContext context) {
            return true;
        }

        @Override
        public UiSnapshot snapshot(UiContext context, SnapshotOptions options) {
            return new UiSnapshot("screen", context.screenClass(), descriptor.id(), List.of(), List.of(), null, null, null, null, Map.of());
        }

        @Override
        public List<UiTarget> query(UiContext context, TargetSelector selector) {
            return List.of();
        }
    }

    private static final class TestUiInteractionResolver implements UiInteractionStateResolver {
        private final String driverId;

        private TestUiInteractionResolver(String driverId) {
            this.driverId = driverId;
        }

        @Override
        public String driverId() {
            return driverId;
        }

        @Override
        public int priority() {
            return 200;
        }

        @Override
        public boolean matches(UiContext context, List<UiTarget> targets) {
            return true;
        }

        @Override
        public UiInteractionDefaults resolve(UiContext context, List<UiTarget> targets) {
            return new UiInteractionDefaults("focused", null, null, null, "resolver");
        }
    }

    private record TestUiContext(String screenClass) implements UiContext {
    }

    private static final class TestOffscreenCaptureProvider implements UiOffscreenCaptureProvider {
        private final String providerId;

        private TestOffscreenCaptureProvider(String providerId) {
            this.providerId = providerId;
        }

        @Override
        public String providerId() {
            return providerId;
        }

        @Override
        public int priority() {
            return 200;
        }

        @Override
        public boolean matches(UiContext context, UiSnapshot snapshot) {
            return true;
        }

        @Override
        public UiCaptureImage capture(UiContext context, UiSnapshot snapshot, CaptureRequest request, List<UiTarget> capturedTargets, List<UiTarget> excludedTargets) {
            return new UiCaptureImage(providerId, "offscreen", new byte[0], 1, 1, Map.of());
        }
    }

    private static final class TestFramebufferCaptureProvider implements UiFramebufferCaptureProvider {
        private final String providerId;

        private TestFramebufferCaptureProvider(String providerId) {
            this.providerId = providerId;
        }

        @Override
        public String providerId() {
            return providerId;
        }

        @Override
        public int priority() {
            return 100;
        }

        @Override
        public boolean matches(UiContext context, UiSnapshot snapshot) {
            return true;
        }

        @Override
        public UiCaptureImage capture(UiContext context, UiSnapshot snapshot, CaptureRequest request, List<UiTarget> capturedTargets, List<UiTarget> excludedTargets) {
            return new UiCaptureImage(providerId, "framebuffer", new byte[0], 1, 1, Map.of());
        }
    }
}
