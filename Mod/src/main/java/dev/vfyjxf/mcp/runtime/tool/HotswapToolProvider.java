package dev.vfyjxf.mcp.runtime.tool;

import dev.vfyjxf.mcp.runtime.hotswap.HotswapService;
import dev.vfyjxf.mcp.server.api.McpToolDefinition;
import dev.vfyjxf.mcp.server.api.McpToolProvider;
import dev.vfyjxf.mcp.server.api.ToolResult;
import dev.vfyjxf.mcp.server.runtime.McpToolRegistry;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class HotswapToolProvider implements McpToolProvider {

    private final HotswapService hotswapService;

    public HotswapToolProvider(HotswapService hotswapService) {
        this.hotswapService = hotswapService;
    }

    @Override
    public void register(McpToolRegistry registry) {
        registry.registerTool(
                new McpToolDefinition(
                        "moddev.compile",
                        "moddev.compile",
                        "Compile mod source via Gradle. Returns exit code, stdout, and stderr.",
                        Map.of(),
                        Map.of(),
                        List.of("hotswap", "compile"),
                        "either",
                        false,
                        false,
                        "public",
                        "public"
                ),
                (context, arguments) -> {
                    var result = hotswapService.compile();
                    Map<String, Object> response = new LinkedHashMap<>();
                    response.put("tool", "moddev.compile");
                    response.put("exitCode", result.exitCode());
                    response.put("stdout", result.stdout());
                    response.put("stderr", result.stderr());
                    response.put("success", result.exitCode() == 0);
                    return ToolResult.success(response);
                }
        );

        registry.registerTool(
                new McpToolDefinition(
                        "moddev.hotswap",
                        "moddev.hotswap",
                        "Compile (optional) and reload changed classes into the running game via JVM hotswap.",
                        Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "compile", Map.of(
                                                "type", "boolean",
                                                "description", "Whether to compile before attempting hotswap. Defaults to true."
                                        )
                                )
                        ),
                        Map.of(),
                        List.of("hotswap", "reload"),
                        "client",
                        false,
                        false,
                        "public",
                        "public"
                ),
                (context, arguments) -> {
                    boolean compile = !"false".equals(String.valueOf(arguments.getOrDefault("compile", "true")));

                    Map<String, Object> response = new LinkedHashMap<>();
                    response.put("tool", "moddev.hotswap");

                    if (compile) {
                        var compileResult = hotswapService.compile();
                        response.put("compileExitCode", compileResult.exitCode());
                        response.put("compileStdout", compileResult.stdout());
                        response.put("compileStderr", compileResult.stderr());
                        if (compileResult.exitCode() != 0) {
                            response.put("success", false);
                            response.put("error", "Compilation failed with exit code " + compileResult.exitCode());
                            return ToolResult.success(response);
                        }
                    }

                    var reloadResult = hotswapService.reload();
                    response.put("reloadedClasses", reloadResult.reloadedClasses());
                    response.put("notYetLoaded", reloadResult.notYetLoaded());
                    response.put("errors", reloadResult.errors());
                    response.put("capabilities", reloadResult.capabilities());
                    response.put("diagnostics", reloadResult.diagnostics());
                    response.put("success", reloadResult.errors().isEmpty());
                    return ToolResult.success(response);
                }
        );
    }
}
