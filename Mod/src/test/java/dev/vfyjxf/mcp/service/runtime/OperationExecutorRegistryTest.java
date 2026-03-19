package dev.vfyjxf.mcp.service.runtime;

import dev.vfyjxf.mcp.service.request.OperationError;
import dev.vfyjxf.mcp.service.request.OperationExecutionException;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OperationExecutorRegistryTest {

    @Test
    void executesRegisteredOperationById() throws Exception {
        var registry = new OperationExecutorRegistry(Map.of(
                "status.get",
                (input, side) -> Map.of("ok", true, "targetSide", side)
        ));

        var output = registry.execute("status.get", Map.of(), "client");

        assertEquals(true, output.get("ok"));
        assertEquals("client", output.get("targetSide"));
    }

    @Test
    void throwsStructuredErrorWhenOperationIsMissing() {
        var registry = new OperationExecutorRegistry(Map.of());

        var exception = assertThrows(
                OperationExecutionException.class,
                () -> registry.execute("missing.operation", Map.of(), null)
        );

        OperationError error = exception.error();
        assertEquals("operation_not_found", error.errorCode());
        assertEquals("operation was not found", error.errorMessage());
    }
}
