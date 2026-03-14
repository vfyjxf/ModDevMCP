package dev.vfyjxf.mcp.server.host;

import dev.vfyjxf.mcp.server.api.McpToolDefinition;

import java.util.*;

public final class RuntimeRegistry {

    private RuntimeState state = RuntimeState.disconnected();
    private final Map<String, RuntimeSession> sessions = new LinkedHashMap<>();
    private final Map<String, List<RuntimeToolDescriptor>> dynamicToolsByRuntimeId = new LinkedHashMap<>();

    public synchronized RuntimeState state() {
        return state;
    }

    public synchronized Optional<RuntimeSession> activeSession() {
        if (sessions.size() != 1) {
            return Optional.empty();
        }
        return Optional.of(sessions.values().iterator().next());
    }

    public synchronized Optional<RuntimeSession> findSession(String runtimeId) {
        return Optional.ofNullable(sessions.get(runtimeId));
    }

    public synchronized List<RuntimeSession> listSessions() {
        return List.copyOf(sessions.values());
    }

    public synchronized List<RuntimeToolDescriptor> listDynamicTools() {
        var aggregated = new LinkedHashMap<String, RuntimeToolDescriptor>();
        dynamicToolsByRuntimeId.values().forEach(toolDescriptors -> toolDescriptors.forEach(toolDescriptor ->
                aggregated.merge(toolDescriptor.definition().name(), toolDescriptor, RuntimeRegistry::mergeDescriptor)
        ));
        return aggregated.values().stream()
                .sorted(java.util.Comparator.comparing(tool -> tool.definition().name()))
                .toList();
    }

    public synchronized RuntimeToolSelection resolveDynamicTool(String toolName, Map<String, Object> arguments) {
        Objects.requireNonNull(toolName, "toolName");
        var routes = routesFor(toolName);
        if (routes.isEmpty()) {
            return RuntimeToolSelection.failure("game_not_connected");
        }
        var requestedSide = requestedSide(arguments);
        if (requestedSide != null) {
            return routes.stream()
                    .filter(route -> requestedSide.equalsIgnoreCase(route.session().runtimeSide()))
                    .findFirst()
                    .map(route -> RuntimeToolSelection.resolved(route.session(), route.descriptor()))
                    .orElseGet(() -> RuntimeToolSelection.failure("runtime_not_connected: side=" + requestedSide));
        }
        var runtimeSides = routes.stream().map(route -> route.session().runtimeSide()).distinct().toList();
        if (runtimeSides.size() > 1) {
            return RuntimeToolSelection.failure("ambiguous_runtime_side: specify targetSide");
        }
        var route = routes.getFirst();
        return RuntimeToolSelection.resolved(route.session(), route.descriptor());
    }

    public synchronized void connect(RuntimeSession session, List<RuntimeToolDescriptor> tools) {
        sessions.put(Objects.requireNonNull(session, "session").runtimeId(), session);
        dynamicToolsByRuntimeId.put(session.runtimeId(), List.copyOf(tools));
        state = RuntimeState.fromSessions(sessions.values());
    }

    public synchronized void refreshTools(String runtimeId, List<RuntimeToolDescriptor> tools) {
        if (!sessions.containsKey(runtimeId)) {
            return;
        }
        dynamicToolsByRuntimeId.put(runtimeId, List.copyOf(tools));
    }

    public synchronized void disconnect(String runtimeId) {
        if (sessions.remove(runtimeId) == null) {
            return;
        }
        dynamicToolsByRuntimeId.remove(runtimeId);
        state = RuntimeState.fromSessions(sessions.values());
    }

    private List<Route> routesFor(String toolName) {
        var routes = new ArrayList<Route>();
        sessions.forEach((runtimeId, session) -> dynamicToolsByRuntimeId.getOrDefault(runtimeId, List.of()).stream()
                .filter(tool -> tool.definition().name().equals(toolName))
                .forEach(tool -> routes.add(new Route(session, tool))));
        return List.copyOf(routes);
    }

    private String requestedSide(Map<String, Object> arguments) {
        if (arguments == null) {
            return null;
        }
        var value = arguments.get("targetSide");
        if (!(value instanceof String requestedSide) || requestedSide.isBlank()) {
            return null;
        }
        return requestedSide;
    }

    private static RuntimeToolDescriptor mergeDescriptor(RuntimeToolDescriptor left, RuntimeToolDescriptor right) {
        var definition = mergeDefinition(left.definition(), right.definition());
        var scope = Objects.equals(left.scope(), right.scope()) ? left.scope() : "common";
        var runtimeSide = Objects.equals(left.runtimeSide(), right.runtimeSide()) ? left.runtimeSide() : "either";
        return new RuntimeToolDescriptor(
                definition,
                scope,
                runtimeSide,
                left.requiresGame() || right.requiresGame(),
                left.mutating() || right.mutating()
        );
    }

    private static McpToolDefinition mergeDefinition(McpToolDefinition left, McpToolDefinition right) {
        var side = Objects.equals(left.side(), right.side()) ? left.side() : "common";
        var tags = java.util.stream.Stream.concat(left.tags().stream(), right.tags().stream()).distinct().toList();
        return new McpToolDefinition(
                left.name(),
                left.title(),
                left.description(),
                left.inputSchema(),
                left.outputSchema(),
                tags,
                side,
                left.requiresWorld() || right.requiresWorld(),
                left.requiresPlayer() || right.requiresPlayer(),
                left.availability(),
                left.exposurePolicy()
        );
    }

    private record Route(RuntimeSession session, RuntimeToolDescriptor descriptor) {
    }
}
