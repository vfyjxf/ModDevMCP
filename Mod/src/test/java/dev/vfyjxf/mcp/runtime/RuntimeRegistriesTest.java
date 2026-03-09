package dev.vfyjxf.mcp.runtime;

import dev.vfyjxf.mcp.api.ModMcpApi;
import dev.vfyjxf.mcp.api.runtime.DriverDescriptor;
import dev.vfyjxf.mcp.api.runtime.UiContext;
import dev.vfyjxf.mcp.api.runtime.UiDriver;
import dev.vfyjxf.mcp.api.ui.SnapshotOptions;
import dev.vfyjxf.mcp.api.ui.TargetSelector;
import dev.vfyjxf.mcp.api.ui.UiSnapshot;
import dev.vfyjxf.mcp.api.ui.UiTarget;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RuntimeRegistriesTest {

    @Test
    void modApiRegistersUiDriverIntoRuntimeRegistry() {
        var registries = new RuntimeRegistries();
        var api = new ModMcpApi(registries);

        api.registerUiDriver(new TestUiDriver("custom-ui"));

        assertEquals(1, registries.uiDrivers().size());
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
            return new UiSnapshot("screen", context.screenClass(), descriptor.id(), List.of(), List.of(), null, null, null, null, java.util.Map.of());
        }

        @Override
        public List<UiTarget> query(UiContext context, TargetSelector selector) {
            return List.of();
        }
    }
}
