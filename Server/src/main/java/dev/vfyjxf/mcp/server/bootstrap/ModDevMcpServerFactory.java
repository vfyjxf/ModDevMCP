package dev.vfyjxf.mcp.server.bootstrap;

import dev.vfyjxf.mcp.server.ModDevMcpServer;
import dev.vfyjxf.mcp.server.api.McpResource;
import dev.vfyjxf.mcp.server.api.ToolCallContext;
import dev.vfyjxf.mcp.server.protocol.McpProtocolDispatcher;
import dev.vfyjxf.mcp.server.host.transport.RuntimeHost;
import dev.vfyjxf.mcp.server.runtime.McpToolRegistry;
import dev.vfyjxf.mcp.server.transport.GsonMcpJsonMapper;
import dev.vfyjxf.mcp.server.transport.McpServerTransport;
import dev.vfyjxf.mcp.server.transport.PermissiveJsonSchemaValidator;
import dev.vfyjxf.mcp.server.transport.StdioMcpServerHost;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpServerTransportProvider;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ModDevMcpServerFactory {

    public static final GsonMcpJsonMapper JSON_MAPPER = new GsonMcpJsonMapper();

    private ModDevMcpServerFactory() {
    }

    public static ModDevMcpServer createServer() {
        return new ModDevMcpServer();
    }

    public static McpProtocolDispatcher createDispatcher(ModDevMcpServer server) {
        return new McpProtocolDispatcher(server);
    }

    public static McpSyncServer createSdkServer(ModDevMcpServer server, McpServerTransportProvider transportProvider) {
        var registeredTools = server.registry().listTools().stream()
                .map(ModDevMcpServerFactory::toSdkTool)
                .toList();
        var registeredResources = server.resourceRegistry().list().stream()
                .map(ModDevMcpServerFactory::toSdkResource)
                .toList();
        var capabilities = McpSchema.ServerCapabilities.builder().logging();
        if (!registeredTools.isEmpty()) {
            capabilities.tools(false);
        }
        if (!registeredResources.isEmpty()) {
            capabilities.resources(false, false);
        }
        return McpServer.sync(transportProvider)
                .serverInfo("moddev-mcp", "0.1.0")
                .jsonMapper(JSON_MAPPER)
                .jsonSchemaValidator(PermissiveJsonSchemaValidator.INSTANCE)
                .capabilities(capabilities.build())
                .tools(registeredTools)
                .resources(registeredResources)
                .build();
    }

    public static McpServerTransport createStdioHost(ModDevMcpServer server, InputStream input, OutputStream output) {
        return new StdioMcpServerHost(input, output, createDispatcher(server));
    }

    public static McpServerTransport createHostAttachedStdioHost(ModDevMcpServer server, InputStream input, OutputStream output, HostEndpointConfig config) {
        try {
            return new HostAttachedStdioMcpServerHost(
                    createStdioHost(server, input, output),
                    startRuntimeHost(server, config)
            );
        } catch (java.io.IOException exception) {
            throw new IllegalStateException("Failed to start runtime host", exception);
        }
    }

    public static RuntimeHost startRuntimeHost(ModDevMcpServer server, HostEndpointConfig config) throws java.io.IOException {
        return RuntimeHost.start(server.runtimeRegistry(), config.host(), config.port(), server.callScheduler());
    }

    static McpServerFeatures.SyncToolSpecification toSdkTool(McpToolRegistry.RegisteredTool registeredTool) {
        return McpServerFeatures.SyncToolSpecification.builder()
                .tool(toSdkToolDefinition(registeredTool.definition()))
                .callHandler((exchange, request) -> {
                    var result = registeredTool.handler().handle(
                            ToolCallContext.empty(),
                            request.arguments() == null ? Map.of() : request.arguments()
                    );
                    if (result.success()) {
                        var value = result.value();
                        return new McpSchema.CallToolResult(
                                List.of(new McpSchema.TextContent(renderToolResult(value))),
                                false,
                                value,
                                Map.of()
                        );
                    }
                    return new McpSchema.CallToolResult(
                            List.of(new McpSchema.TextContent(result.error() == null ? "" : result.error())),
                            true,
                            null,
                            Map.of()
                    );
                })
                .build();
    }

    static McpServerFeatures.SyncResourceSpecification toSdkResource(McpResource resource) {
        return new McpServerFeatures.SyncResourceSpecification(
                new McpSchema.Resource(
                        resource.uri(),
                        resource.displayName(),
                        null,
                        null,
                        resource.mimeType(),
                        (long) resource.content().length,
                        null,
                        resource.metadata()
                ),
                (exchange, request) -> new McpSchema.ReadResourceResult(List.of(toSdkResourceContents(resource)))
        );
    }

    private static McpSchema.Tool toSdkToolDefinition(dev.vfyjxf.mcp.server.api.McpToolDefinition definition) {
        return McpSchema.Tool.builder()
                .name(definition.name())
                .title(definition.title())
                .description(definition.description())
                .inputSchema(toJsonSchema(definition.inputSchema()))
                .outputSchema(definition.outputSchema())
                .annotations(toToolAnnotations(definition))
                .meta(toToolMeta(definition))
                .build();
    }

    private static McpSchema.ToolAnnotations toToolAnnotations(dev.vfyjxf.mcp.server.api.McpToolDefinition definition) {
        return new McpSchema.ToolAnnotations(
                definition.title(),
                false,
                false,
                true,
                !definition.requiresWorld(),
                false
        );
    }

    private static Map<String, Object> toToolMeta(dev.vfyjxf.mcp.server.api.McpToolDefinition definition) {
        var meta = new LinkedHashMap<String, Object>();
        if (!definition.tags().isEmpty()) {
            meta.put("tags", definition.tags());
        }
        meta.put("side", definition.side());
        meta.put("requiresWorld", definition.requiresWorld());
        meta.put("requiresPlayer", definition.requiresPlayer());
        meta.put("availability", definition.availability());
        meta.put("exposurePolicy", definition.exposurePolicy());
        return Map.copyOf(meta);
    }

    @SuppressWarnings("unchecked")
    private static McpSchema.JsonSchema toJsonSchema(Map<String, Object> schema) {
        if (schema == null || schema.isEmpty()) {
            return new McpSchema.JsonSchema("object", Map.of(), List.of(), true, Map.of(), Map.of());
        }
        return new McpSchema.JsonSchema(
                (String) schema.getOrDefault("type", "object"),
                (Map<String, Object>) schema.getOrDefault("properties", Map.of()),
                (List<String>) schema.getOrDefault("required", List.of()),
                (Boolean) schema.getOrDefault("additionalProperties", Boolean.TRUE),
                (Map<String, Object>) schema.getOrDefault("$defs", Map.of()),
                (Map<String, Object>) schema.getOrDefault("definitions", Map.of())
        );
    }

    private static McpSchema.ResourceContents toSdkResourceContents(McpResource resource) {
        if (isTextResource(resource.mimeType())) {
            return new McpSchema.TextResourceContents(
                    resource.uri(),
                    resource.mimeType(),
                    new String(resource.content(), StandardCharsets.UTF_8),
                    resource.metadata()
            );
        }
        return new McpSchema.BlobResourceContents(
                resource.uri(),
                resource.mimeType(),
                Base64.getEncoder().encodeToString(resource.content()),
                resource.metadata()
        );
    }

    private static boolean isTextResource(String mimeType) {
        return mimeType != null && (mimeType.startsWith("text/") || "application/json".equals(mimeType));
    }

    private static String renderToolResult(Object value) {
        try {
            return JSON_MAPPER.writeValueAsString(value);
        } catch (java.io.IOException exception) {
            throw new IllegalStateException("Failed to render tool result", exception);
        }
    }
}





