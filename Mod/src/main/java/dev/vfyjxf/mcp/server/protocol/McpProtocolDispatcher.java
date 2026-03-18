package dev.vfyjxf.mcp.server.protocol;

import dev.vfyjxf.mcp.server.ModDevMcpServer;
import dev.vfyjxf.mcp.server.api.McpResource;
import dev.vfyjxf.mcp.server.api.ToolCallContext;

import java.nio.charset.StandardCharsets;
import java.util.*;

public final class McpProtocolDispatcher {

    private static final int METHOD_NOT_FOUND = -32601;
    private static final int INVALID_PARAMS = -32602;
    private static final int INTERNAL_ERROR = -32603;
    private static final int RESOURCE_NOT_FOUND = -32002;

    private final ModDevMcpServer server;
    private final String serverName;
    private final String serverVersion;

    public McpProtocolDispatcher(ModDevMcpServer server) {
        this(server, "moddev-mcp", "0.1.0");
    }

    public McpProtocolDispatcher(ModDevMcpServer server, String serverName, String serverVersion) {
        this.server = server;
        this.serverName = serverName;
        this.serverVersion = serverVersion;
    }

    @SuppressWarnings("unchecked")
    public Optional<Map<String, Object>> handle(Map<String, Object> request) {
        var method = request.get("method");
        if (!(method instanceof String methodName) || methodName.isBlank()) {
            return Optional.of(errorResponse(request.get("id"), INVALID_PARAMS, "Invalid params", Map.of("method", "missing")));
        }
        if ("notifications/initialized".equals(methodName)) {
            return Optional.empty();
        }
        try {
            return Optional.of(switch (methodName) {
                case "initialize" -> successResponse(request.get("id"), initializeResult(request));
                case "tools/list" -> successResponse(request.get("id"), Map.of("tools", listTools()));
                case "tools/call" -> handleToolCall(request.get("id"), (Map<String, Object>) request.get("params"));
                case "resources/list" -> successResponse(request.get("id"), Map.of(
                        "resources", server.resourceRegistry().list().stream().map(this::resourceDescriptor).toList()
                ));
                case "resources/read" -> handleResourceRead(request.get("id"), (Map<String, Object>) request.get("params"));
                default -> errorResponse(request.get("id"), METHOD_NOT_FOUND, "Method not found", Map.of("method", methodName));
            });
        } catch (ClassCastException exception) {
            return Optional.of(errorResponse(request.get("id"), INVALID_PARAMS, "Invalid params", Map.of("reason", exception.getMessage())));
        } catch (RuntimeException exception) {
            return Optional.of(errorResponse(request.get("id"), INTERNAL_ERROR, "Internal error", Map.of("reason", exception.getMessage())));
        }
    }

    public Map<String, Object> initializedNotification() {
        var statusTool = server.registry().findTool("moddev.status");
        var payload = statusTool
                .map(tool -> tool.handler().handle(ToolCallContext.empty(), Map.of()))
                .filter(result -> result.success())
                .map(dev.vfyjxf.mcp.server.api.ToolResult::value)
                .filter(Map.class::isInstance)
                .map(Map.class::cast)
                .map(this::castMap)
                .orElse(Map.ofEntries(
                        Map.entry("hostReady", true),
                        Map.entry("gameConnected", false),
                        Map.entry("gameConnecting", false),
                        Map.entry("clientConnected", false),
                        Map.entry("serverConnected", false),
                        Map.entry("connectedAgentCount", 0),
                        Map.entry("queueDepth", 0),
                        Map.entry("runtimeId", ""),
                        Map.entry("runtimeSide", ""),
                        Map.entry("availableScopes", List.of()),
                        Map.entry("runtimeSides", List.of()),
                        Map.entry("connectedRuntimes", List.of())
                ));
        return Map.of(
                "jsonrpc", "2.0",
                "method", "notifications/moddev/status",
                "params", payload
        );
    }

    private Map<String, Object> initializeResult(Map<String, Object> request) {
        var params = request.get("params") instanceof Map<?, ?> rawParams
                ? castMap(rawParams)
                : Map.<String, Object>of();
        var protocolVersion = params.get("protocolVersion") instanceof String version && !version.isBlank()
                ? version
                : "2025-11-05";
        return Map.of(
                "protocolVersion", protocolVersion,
                "capabilities", Map.of(
                        "tools", Map.of("listChanged", true),
                        "resources", Map.of("subscribe", false, "listChanged", false)
                ),
                "serverInfo", Map.of(
                        "name", serverName,
                        "version", serverVersion
                )
        );
    }

    private List<Map<String, Object>> listTools() {
        var tools = new java.util.ArrayList<Map<String, Object>>();
        server.registry().listTools().forEach(tool -> tools.add(toolDescriptor(tool.definition())));
        server.runtimeRegistry().listDynamicTools().forEach(tool -> tools.add(toolDescriptor(tool.definition())));
        return List.copyOf(tools);
    }

    private Map<String, Object> toolDescriptor(dev.vfyjxf.mcp.server.api.McpToolDefinition definition) {
        return Map.of(
                "name", definition.name(),
                "title", definition.title(),
                "description", definition.description(),
                "inputSchema", definition.inputSchema()
        );
    }

    private Map<String, Object> handleToolCall(Object id, Map<String, Object> params) {
        if (params == null || !(params.get("name") instanceof String toolName) || toolName.isBlank()) {
            return errorResponse(id, INVALID_PARAMS, "Invalid params", Map.of("name", "missing"));
        }
        Object rawArguments = params.getOrDefault("arguments", Map.of());
        if (!(rawArguments instanceof Map<?, ?> arguments)) {
            return errorResponse(id, INVALID_PARAMS, "Invalid params", Map.of("arguments", "must be object"));
        }
        var toolArguments = castMap(arguments);
        var tool = server.registry().findTool(toolName);
        if (tool.isPresent()) {
            return handleLocalToolCall(id, tool.get().handler().handle(ToolCallContext.empty(), toolArguments));
        }
        var selection = server.runtimeRegistry().resolveDynamicTool(toolName, toolArguments);
        if (selection.resolved()) {
            return handleLocalToolCall(id, server.callScheduler().call(
                    selection.session(),
                    selection.descriptor(),
                    selection.argumentsWithoutRoutingKeys(toolArguments)
            ));
        }
        if (toolName.startsWith("moddev.")) {
            return handleLocalToolCall(id, dev.vfyjxf.mcp.server.api.ToolResult.failure(selection.error()));
        }
        return errorResponse(id, METHOD_NOT_FOUND, "Method not found", Map.of("tool", toolName));
    }

    private Map<String, Object> handleLocalToolCall(Object id, dev.vfyjxf.mcp.server.api.ToolResult result) {
        if (result.success()) {
            var structuredContent = result.value();
            return successResponse(id, Map.of(
                    "content", List.of(Map.of(
                            "type", "text",
                            "text", new dev.vfyjxf.mcp.server.transport.JsonCodec().writeString(structuredContent)
                    )),
                    "structuredContent", structuredContent,
                    "isError", false
            ));
        }
        return successResponse(id, Map.of(
                "content", List.of(Map.of(
                        "type", "text",
                        "text", result.error() == null ? "" : result.error()
                )),
                "isError", true
        ));
    }

    private Map<String, Object> handleResourceRead(Object id, Map<String, Object> params) {
        if (params == null || !(params.get("uri") instanceof String uri) || uri.isBlank()) {
            return errorResponse(id, INVALID_PARAMS, "Invalid params", Map.of("uri", "missing"));
        }
        var resource = server.resourceRegistry().read(uri);
        if (resource.isEmpty()) {
            return errorResponse(id, RESOURCE_NOT_FOUND, "Resource not found", Map.of("uri", uri));
        }
        return successResponse(id, Map.of(
                "contents", List.of(resourceContent(resource.get()))
        ));
    }

    private Map<String, Object> resourceDescriptor(McpResource resource) {
        var descriptor = new LinkedHashMap<String, Object>();
        descriptor.put("uri", resource.uri());
        descriptor.put("name", resource.displayName());
        descriptor.put("mimeType", resource.mimeType());
        if (!resource.metadata().isEmpty()) {
            descriptor.put("metadata", resource.metadata());
        }
        return Map.copyOf(descriptor);
    }

    private Map<String, Object> resourceContent(McpResource resource) {
        var result = new LinkedHashMap<String, Object>();
        result.put("uri", resource.uri());
        result.put("mimeType", resource.mimeType());
        if (isTextResource(resource.mimeType())) {
            result.put("text", new String(resource.content(), StandardCharsets.UTF_8));
        } else {
            result.put("blob", Base64.getEncoder().encodeToString(resource.content()));
        }
        return Map.copyOf(result);
    }

    private boolean isTextResource(String mimeType) {
        return mimeType != null && (mimeType.startsWith("text/") || "application/json".equals(mimeType));
    }

    private Map<String, Object> successResponse(Object id, Map<String, Object> result) {
        return Map.of(
                "jsonrpc", "2.0",
                "id", normalizeJsonRpcId(id),
                "result", result
        );
    }

    private Map<String, Object> errorResponse(Object id, int code, String message, Map<String, Object> data) {
        return Map.of(
                "jsonrpc", "2.0",
                "id", normalizeJsonRpcId(id),
                "error", Map.of(
                        "code", code,
                        "message", message,
                        "data", data
                )
        );
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castMap(Map<?, ?> map) {
        return (Map<String, Object>) map;
    }

    private Object normalizeJsonRpcId(Object id) {
        if (id instanceof Double value && Double.isFinite(value) && Math.rint(value) == value) {
            return value.longValue();
        }
        if (id instanceof Float value && Float.isFinite(value) && Math.rint(value) == value) {
            return value.longValue();
        }
        return id;
    }
}
