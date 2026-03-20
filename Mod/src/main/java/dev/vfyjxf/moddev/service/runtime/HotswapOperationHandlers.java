package dev.vfyjxf.moddev.service.runtime;

import dev.vfyjxf.moddev.runtime.RuntimeRegistries;
import dev.vfyjxf.moddev.runtime.hotswap.HotswapService;
import dev.vfyjxf.moddev.service.operation.OperationDefinition;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class HotswapOperationHandlers {

    private HotswapOperationHandlers() {
    }

    static List<RuntimeOperationBindings.OperationBinding> operations(RuntimeRegistries registries) {
        Objects.requireNonNull(registries, "registries");
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
                        (input, resolvedTargetSide) -> {
                            var hotswapService = hotswapService(registries);
                            var result = hotswapService.compile();
                            return Map.of(
                                    "exitCode", result.exitCode(),
                                    "stdout", result.stdout(),
                                    "stderr", result.stderr(),
                                    "success", result.exitCode() == 0
                            );
                        }
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
                            var hotswapService = hotswapService(registries);
                            var compile = !"false".equals(String.valueOf(input.getOrDefault("compile", "true")));
                            var payload = new LinkedHashMap<String, Object>();
                            if (compile) {
                                var compileResult = hotswapService.compile();
                                payload.put("compileExitCode", compileResult.exitCode());
                                payload.put("compileStdout", compileResult.stdout());
                                payload.put("compileStderr", compileResult.stderr());
                                if (compileResult.exitCode() != 0) {
                                    throw RuntimeOperationBindings.executionFailure("Compilation failed with exit code " + compileResult.exitCode());
                                }
                            }
                            var reloadResult = hotswapService.reload();
                            payload.put("reloadedClasses", reloadResult.reloadedClasses());
                            payload.put("notYetLoaded", reloadResult.notYetLoaded());
                            payload.put("errors", reloadResult.errors());
                            payload.put("capabilities", reloadResult.capabilities());
                            payload.put("diagnostics", reloadResult.diagnostics());
                            if (!reloadResult.errors().isEmpty()) {
                                throw RuntimeOperationBindings.executionFailure(String.valueOf(reloadResult.errors().values().iterator().next()));
                            }
                            payload.put("success", true);
                            return Map.copyOf(payload);
                        }
                )
        );
    }

    private static HotswapService hotswapService(RuntimeRegistries registries) {
        return registries.hotswapService()
                .orElseThrow(() -> RuntimeOperationBindings.executionFailure("hotswap runtime unavailable"));
    }
}
