package dev.vfyjxf.moddev.runtime;

import dev.vfyjxf.moddev.api.runtime.DriverDescriptor;
import dev.vfyjxf.moddev.api.runtime.UiContext;
import dev.vfyjxf.moddev.api.runtime.UiDriver;
import dev.vfyjxf.moddev.api.ui.SnapshotOptions;
import dev.vfyjxf.moddev.api.ui.TargetSelector;
import dev.vfyjxf.moddev.api.ui.UiSnapshot;
import dev.vfyjxf.moddev.api.ui.UiTarget;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

    @Test
    void matchingDriversReturnsAllMatchesInPriorityOrder() {
        var registry = new UiDriverRegistry();
        var expectedHandle = new Object();
        var baseDriver = new TestUiDriver("base", 100, context -> true);
        var addonDriver = new TestUiDriver("addon", 300, context -> context.screenHandle() == expectedHandle);
        var unrelatedDriver = new TestUiDriver("other", 500, context -> false);

        registry.register(baseDriver);
        registry.register(addonDriver);
        registry.register(unrelatedDriver);

        assertEquals(
                List.of("addon", "base"),
                registry.matchingDrivers(contextWithHandle(expectedHandle)).stream()
                        .map(driver -> driver.descriptor().id())
                        .toList()
        );
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

