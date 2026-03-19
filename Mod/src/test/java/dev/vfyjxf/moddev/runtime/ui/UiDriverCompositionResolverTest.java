package dev.vfyjxf.moddev.runtime.ui;

import dev.vfyjxf.moddev.api.runtime.DriverDescriptor;
import dev.vfyjxf.moddev.api.runtime.UiContext;
import dev.vfyjxf.moddev.api.runtime.UiDriver;
import dev.vfyjxf.moddev.api.ui.SnapshotOptions;
import dev.vfyjxf.moddev.api.ui.TargetSelector;
import dev.vfyjxf.moddev.api.ui.UiSnapshot;
import dev.vfyjxf.moddev.api.ui.UiTarget;
import dev.vfyjxf.moddev.runtime.UiDriverRegistry;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UiDriverCompositionResolverTest {

    @Test
    void compositionResolverKeepsDefaultDriverAsHighestPriorityMatch() {
        var registry = new UiDriverRegistry();
        var expectedHandle = new Object();
        registry.register(new TestUiDriver("base", 100, context -> true));
        registry.register(new TestUiDriver("addon", 300, context -> context.screenHandle() == expectedHandle));

        var resolver = new UiDriverCompositionResolver(registry);
        var composition = resolver.resolve(contextWithHandle(expectedHandle));

        assertEquals("addon", composition.defaultDriverId());
        assertEquals(
                List.of("addon", "base"),
                composition.drivers().stream().map(driver -> driver.descriptor().id()).toList()
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

