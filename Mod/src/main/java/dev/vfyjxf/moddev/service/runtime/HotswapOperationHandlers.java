package dev.vfyjxf.moddev.service.runtime;

import dev.vfyjxf.moddev.service.operation.OperationDefinition;

import java.util.List;
import java.util.Map;
import java.util.Set;

public final class HotswapOperationHandlers {

    private HotswapOperationHandlers() {
    }

    static List<RuntimeOperationBindings.OperationBinding> operations(RuntimeOperationBindings.ToolOperationInvoker toolInvoker) {
        return List.of(
                RuntimeOperationBindings.binding(
                        new OperationDefinition(
                                "hotswap.compile",
                                "hotswap",
                                "Compile Sources",
                                "Runs the configured Gradle compile task for hotswap.",
                                false,
                                Set.of(),
                                RuntimeOperationBindings.objectSchema(Map.of(), List.of()),
                                Map.of("operationId", "hotswap.compile", "input", Map.of())
                        ),
                        RuntimeOperationBindings.toolHandler(toolInvoker, "moddev.compile")
                ),
                RuntimeOperationBindings.binding(
                        new OperationDefinition(
                                "hotswap.reload",
                                "hotswap",
                                "Reload Classes",
                                "Compiles optionally and reloads changed classes into the running game.",
                                true,
                                Set.of("client", "server"),
                                RuntimeOperationBindings.objectSchema(
                                        Map.of("compile", Map.of("type", "boolean")),
                                        List.of()
                                ),
                                Map.of(
                                        "operationId", "hotswap.reload",
                                        "targetSide", "client",
                                        "input", Map.of("compile", true)
                                )
                        ),
                        (input, resolvedTargetSide) -> {
                            var output = RuntimeOperationBindings.invokeTool(toolInvoker, "moddev.hotswap", resolvedTargetSide, input);
                            if (Boolean.FALSE.equals(output.get("success"))) {
                                var error = output.get("error");
                                if (error instanceof String errorMessage && !errorMessage.isBlank()) {
                                    throw RuntimeOperationBindings.executionFailure(errorMessage);
                                }
                                if (output.get("errors") instanceof List<?> errors && !errors.isEmpty()) {
                                    throw RuntimeOperationBindings.executionFailure(String.valueOf(errors.getFirst()));
                                }
                                throw RuntimeOperationBindings.executionFailure("hotswap failed");
                            }
                            return output;
                        }
                )
        );
    }
}

