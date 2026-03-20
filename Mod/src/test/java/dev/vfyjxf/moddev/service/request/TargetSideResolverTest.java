package dev.vfyjxf.moddev.service.request;

import dev.vfyjxf.moddev.service.operation.OperationDefinition;
import dev.vfyjxf.moddev.service.runtime.TargetSideResolver;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TargetSideResolverTest {

    private final TargetSideResolver resolver = new TargetSideResolver();

    @Test
    void rejectsTargetSideForOperationWithoutSideSelection() {
        var operation = new OperationDefinition(
                "status.check",
                "status",
                "Status Check",
                "Check status.",
                false,
                Set.of(),
                Map.of("type", "object"),
                Map.of("operationId", "status.check")
        );

        var exception = assertThrows(
                TargetSideResolver.TargetSideResolutionException.class,
                () -> resolver.resolve(operation, "client", List.of("client"))
        );

        assertEquals("target_side_not_supported", exception.error().errorCode());
    }

    @Test
    void omittingTargetSideAutoResolvesWhenExactlyOneEligibleSideIsConnected() {
        var operation = new OperationDefinition(
                "world.inspect",
                "world",
                "Inspect World",
                "Inspect world.",
                true,
                Set.of("client", "server"),
                Map.of("type", "object"),
                Map.of("operationId", "world.inspect", "targetSide", "client")
        );

        var resolved = resolver.resolve(operation, null, List.of("client"));

        assertEquals("client", resolved);
    }

    @Test
    void omittingTargetSideReturnsTargetSideRequiredWhenBothSidesAreEligible() {
        var operation = new OperationDefinition(
                "world.inspect",
                "world",
                "Inspect World",
                "Inspect world.",
                true,
                Set.of("client", "server"),
                Map.of("type", "object"),
                Map.of("operationId", "world.inspect", "targetSide", "client")
        );

        var exception = assertThrows(
                TargetSideResolver.TargetSideResolutionException.class,
                () -> resolver.resolve(operation, null, List.of("client", "server"))
        );

        assertEquals("target_side_required", exception.error().errorCode());
    }
}

