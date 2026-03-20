package dev.vfyjxf.moddev;

import dev.vfyjxf.moddev.api.event.EventEnvelope;
import dev.vfyjxf.moddev.api.event.RegisterClientOperationsEvent;
import dev.vfyjxf.moddev.api.event.RegisterCommonOperationsEvent;
import dev.vfyjxf.moddev.api.model.OperationResult;
import dev.vfyjxf.moddev.api.operation.OperationExecutor;
import dev.vfyjxf.moddev.api.registrar.ClientOperationRegistrar;
import dev.vfyjxf.moddev.api.registrar.CommonOperationRegistrar;
import dev.vfyjxf.moddev.api.registrar.ServerOperationRegistrar;
import dev.vfyjxf.moddev.api.ui.CaptureRequest;
import dev.vfyjxf.moddev.api.runtime.DriverDescriptor;
import dev.vfyjxf.moddev.api.runtime.UiCaptureImage;
import dev.vfyjxf.moddev.api.runtime.UiContext;
import dev.vfyjxf.moddev.api.runtime.UiDriver;
import dev.vfyjxf.moddev.api.runtime.UiFramebufferCaptureProvider;
import dev.vfyjxf.moddev.api.ui.UiInteractionDefaults;
import dev.vfyjxf.moddev.api.runtime.UiInteractionStateResolver;
import dev.vfyjxf.moddev.api.runtime.UiOffscreenCaptureProvider;
import dev.vfyjxf.moddev.api.ui.SnapshotOptions;
import dev.vfyjxf.moddev.api.ui.TargetSelector;
import dev.vfyjxf.moddev.api.ui.UiSnapshot;
import dev.vfyjxf.moddev.api.ui.UiTarget;
import dev.vfyjxf.moddev.runtime.RuntimeRegistries;
import dev.vfyjxf.moddev.service.operation.OperationDefinition;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModDevRegistrarIntegrationTest {

    @Test
    void commonRegistrarsRunDuringCommonServerPreparation() {
        var mod = new ModDevMCP(
                new RuntimeRegistries(),
                commonRegistrars(event -> event.registerOperation(operation("demo.common", false), executor("common"))),
                clientRegistrars(),
                serverRegistrars()
        );

        mod.prepareCommonServer();

        assertTrue(operationIds(mod.registries()).contains("demo.common"));
        assertFalse(operationIds(mod.registries()).contains("demo.client"));
        assertFalse(operationIds(mod.registries()).contains("demo.server"));
    }

    @Test
    void clientRegistrarsRunOnlyDuringClientPreparation() {
        var mod = new ModDevMCP(
                new RuntimeRegistries(),
                commonRegistrars(event -> event.registerOperation(operation("demo.common", false), executor("common"))),
                clientRegistrars(event -> event.registerOperation(operation("demo.client", true), executor("client"))),
                serverRegistrars(event -> event.registerOperation(operation("demo.server", true), executor("server")))
        );

        mod.prepareClientServer();

        assertTrue(operationIds(mod.registries()).contains("demo.common"));
        assertTrue(operationIds(mod.registries()).contains("demo.client"));
        assertFalse(operationIds(mod.registries()).contains("demo.server"));
    }

    @Test
    void clientRegistrarEventExposesApiAndClientRuntimeRegistrations() {
        var mod = new ModDevMCP(
                new RuntimeRegistries(),
                commonRegistrars(),
                clientRegistrars(new ClientIntegrationRegistrar()),
                serverRegistrars()
        );

        mod.prepareClientServer();

        assertTrue(operationIds(mod.registries()).contains("demo.client"));
        assertTrue(operationIds(mod.registries()).contains("demo.client.api"));
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
        var mod = new ModDevMCP(
                new RuntimeRegistries(),
                commonRegistrars(event -> event.registerOperation(operation("demo.common", false), executor("common"))),
                clientRegistrars(event -> event.registerOperation(operation("demo.client", true), executor("client"))),
                serverRegistrars(event -> event.registerOperation(operation("demo.server", true), executor("server")))
        );

        mod.prepareServer();

        assertTrue(operationIds(mod.registries()).contains("demo.common"));
        assertFalse(operationIds(mod.registries()).contains("demo.client"));
        assertTrue(operationIds(mod.registries()).contains("demo.server"));
    }

    @Test
    void commonRegistrarEventExposesApiAndEventPublisher() {
        var mod = new ModDevMCP(
                new RuntimeRegistries(),
                commonRegistrars(new CommonIntegrationRegistrar()),
                clientRegistrars(),
                serverRegistrars()
        );

        mod.prepareCommonServer();

        assertTrue(operationIds(mod.registries()).contains("demo.common"));
        assertTrue(operationIds(mod.registries()).contains("demo.common.api"));
        assertTrue(mod.eventPublisher().recentEvents().stream()
                .anyMatch(event -> "demo".equals(event.domain()) && "common-registered".equals(event.type())));
    }

    @SafeVarargs
    private static Supplier<List<CommonOperationRegistrar>> commonRegistrars(CommonOperationRegistrar... registrars) {
        return () -> List.of(registrars);
    }

    @SafeVarargs
    private static Supplier<List<ClientOperationRegistrar>> clientRegistrars(ClientOperationRegistrar... registrars) {
        return () -> List.of(registrars);
    }

    @SafeVarargs
    private static Supplier<List<ServerOperationRegistrar>> serverRegistrars(ServerOperationRegistrar... registrars) {
        return () -> List.of(registrars);
    }

    private static Set<String> operationIds(RuntimeRegistries registries) {
        return registries.operationRegistrations().stream()
                .map(registration -> registration.definition().operationId())
                .collect(java.util.stream.Collectors.toSet());
    }

    private static OperationDefinition operation(String operationId, boolean supportsTargetSide) {
        return new OperationDefinition(
                operationId,
                "status",
                operationId,
                "test operation",
                supportsTargetSide,
                supportsTargetSide ? Set.of("client", "server") : Set.of(),
                Map.of("type", "object"),
                supportsTargetSide
                        ? Map.of("operationId", operationId, "targetSide", "client", "input", Map.of())
                        : Map.of("operationId", operationId, "input", Map.of())
        );
    }

    private static OperationExecutor executor(String value) {
        return (input, targetSide) -> Map.of("value", value, "targetSide", targetSide);
    }

    private static final class CommonIntegrationRegistrar implements CommonOperationRegistrar {
        @Override
        public void register(RegisterCommonOperationsEvent event) {
            event.registerOperation(operation("demo.common", false), executor("common"));
            event.api().registerOperation(operation("demo.common.api", false), executor("common.api"));
            event.publishEvent(new EventEnvelope("demo", "common-registered", 1L, Map.of("side", "common")));
            assertSame(event.eventPublisher(), event.api().eventPublisher());
        }
    }

    private static final class ClientIntegrationRegistrar implements ClientOperationRegistrar {
        @Override
        public void register(RegisterClientOperationsEvent event) {
            event.registerOperation(operation("demo.client", true), executor("client"));
            event.api().registerOperation(operation("demo.client.api", true), executor("client.api"));
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


