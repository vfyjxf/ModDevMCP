package dev.vfyjxf.mcp.runtime;

import dev.vfyjxf.mcp.api.runtime.DriverDescriptor;
import dev.vfyjxf.mcp.api.runtime.UiContext;
import dev.vfyjxf.mcp.api.runtime.UiDriver;
import dev.vfyjxf.mcp.api.ui.SnapshotOptions;
import dev.vfyjxf.mcp.api.ui.TargetSelector;
import dev.vfyjxf.mcp.api.ui.UiSnapshot;
import dev.vfyjxf.mcp.api.ui.UiTarget;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertSame;

class UiDriverRegistryTest {

    @Test
    void selectPrefersHigherPriorityDriverThatMatchesLiveScreenHandle() {
        var registry = new UiDriverRegistry();
        var expectedHandle = new Object();
        var fallbackDriver = new TestUiDriver("fallback", 100, context -> true);
        var handleAwareDriver = new TestUiDriver("handle-aware", 1_000, context -> context.screenHandle() == expectedHandle);

        registry.register(fallbackDriver);
        registry.register(handleAwareDriver);

        assertSame(handleAwareDriver, registry.select(contextWithHandle(expectedHandle)).orElseThrow());
    }

    private static UiContext contextWithHandle(Object screenHandle) {
        return new UiContext() {
            @Override
            public String screenClass() {
                return "custom.LiveScreen";
            }

            @Override
            public Object screenHandle() {
                return screenHandle;
            }
        };
    }

    private record TestUiDriver(DriverDescriptor descriptor, Predicate<UiContext> matcher) implements UiDriver {

        private TestUiDriver(String id, int priority, Predicate<UiContext> matcher) {
            this(new DriverDescriptor(id, "test", priority, Set.of("snapshot")), matcher);
        }

        @Override
        public boolean matches(UiContext context) {
            return matcher.test(context);
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
}
