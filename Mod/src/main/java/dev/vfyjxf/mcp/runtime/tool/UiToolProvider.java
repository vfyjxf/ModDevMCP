package dev.vfyjxf.mcp.runtime.tool;

import dev.vfyjxf.mcp.api.model.OperationResult;
import dev.vfyjxf.mcp.api.runtime.*;
import dev.vfyjxf.mcp.api.ui.*;
import dev.vfyjxf.mcp.runtime.RuntimeRegistries;
import dev.vfyjxf.mcp.runtime.ui.UiCapturePostProcessor;
import dev.vfyjxf.mcp.runtime.ui.UiDriverComposition;
import dev.vfyjxf.mcp.runtime.ui.UiDriverCompositionResolver;
import dev.vfyjxf.mcp.server.api.McpToolDefinition;
import dev.vfyjxf.mcp.server.api.McpToolProvider;
import dev.vfyjxf.mcp.server.api.ToolResult;
import dev.vfyjxf.mcp.server.runtime.McpToolRegistry;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

public final class UiToolProvider implements McpToolProvider {

    private final RuntimeRegistries registries;
    private final ClientScreenProbe screenProbe;
    private final InputActionDispatcher inputActionDispatcher;
    private final Supplier<Object> currentScreenSupplier;
    private final UiDriverCompositionResolver compositionResolver;

    public UiToolProvider(RuntimeRegistries registries) {
        this(registries, () -> new ClientScreenMetrics(null, 0, 0, 0, 0), UiToolProvider::currentClientScreen);
    }

    public UiToolProvider(RuntimeRegistries registries, ClientScreenProbe screenProbe) {
        this(registries, screenProbe, UiToolProvider::currentClientScreen);
    }

    UiToolProvider(RuntimeRegistries registries, ClientScreenProbe screenProbe, Supplier<Object> currentScreenSupplier) {
        this.registries = registries;
        this.screenProbe = screenProbe;
        this.inputActionDispatcher = new InputActionDispatcher(registries);
        this.currentScreenSupplier = currentScreenSupplier;
        this.compositionResolver = new UiDriverCompositionResolver(registries.uiDrivers());
    }

    @Override
    public void register(McpToolRegistry registry) {
        registry.registerTool(definition("moddev.ui_session_open"), (context, arguments) -> {
            var unavailable = unavailableScreenResult(arguments);
            if (unavailable != null) {
                return unavailable;
            }
            var uiContext = uiContext(arguments);
            return withDriver(uiContext, driver -> {
                var snapshot = driver.snapshot(uiContext, SnapshotOptions.DEFAULT);
                var session = registries.uiAutomationSessions().open(snapshot);
                return ToolResult.success(sessionPayload(session));
            });
        });
        registry.registerTool(definition("moddev.ui_session_refresh"), (context, arguments) -> {
            var sessionId = stringArgument(arguments.get("sessionId"));
            if (sessionId == null || sessionId.isBlank()) {
                return ToolResult.failure("invalid_input: missing sessionId");
            }
            if (registries.uiAutomationSessions().find(sessionId).isEmpty()) {
                return ToolResult.failure("session_not_found");
            }
            var refreshResult = refreshSessionNow(sessionId, arguments);
            if (!refreshResult.accepted()) {
                return ToolResult.failure(refreshResult.reason());
            }
            return ToolResult.success(sessionPayload(refreshResult.value().session(), refreshResult.value().screenChanged()));
        });
        registry.registerTool(definition("moddev.ui_click_ref"), (context, arguments) -> sessionRefActionResult(arguments, "click"));
        registry.registerTool(definition("moddev.ui_hover_ref"), (context, arguments) -> sessionRefActionResult(arguments, "hover"));
        registry.registerTool(definition("moddev.ui_press_key"), (context, arguments) -> inputActionResult("key_press", arguments));
        registry.registerTool(definition("moddev.ui_type_text"), (context, arguments) -> inputActionResult("type_text", arguments));
        registry.registerTool(definition("moddev.ui_wait_for"), (context, arguments) -> sessionWaitResult(arguments));
        registry.registerTool(definition("moddev.ui_screenshot"), (context, arguments) -> screenshotResult(arguments));
        registry.registerTool(definition("moddev.ui_batch"), (context, arguments) -> sessionBatchResult(arguments));
        registry.registerTool(definition("moddev.ui_trace_get"), (context, arguments) -> traceGetResult(arguments));
        registry.registerTool(definition("moddev.ui_trace_recent"), (context, arguments) -> traceRecentResult(arguments));
        registry.registerTool(definition("moddev.ui_inspect"), (context, arguments) -> {
            var uiContext = uiContext(arguments);
            return withDriver(uiContext, driver -> ToolResult.success(
                    withUiContextState(inspectResultToMap(driver.inspect(uiContext, SnapshotOptions.DEFAULT)), uiContext)
            ));
        });
        registry.registerTool(definition("moddev.ui_act"), (context, arguments) -> {
            var action = stringArgument(arguments.get("action"));
            if (action == null || action.isBlank()) {
                return ToolResult.failure("invalid_input: missing action");
            }
            var reference = targetReferenceFrom(arguments);
            if (reference == null) {
                return ToolResult.failure("invalid_input: missing locator/ref");
            }
            var uiContext = uiContext(arguments);
            return withDriver(uiContext, driver -> {
                var resolved = driver.resolve(uiContext, new UiResolveRequest(reference, false, true, true));
                if (!"resolved".equals(resolved.status())) {
                    return ToolResult.failure(resolved.errorCode());
                }
                var actionability = driver.checkActionability(uiContext, resolved.primary(), action);
                if (!actionability.actionable()) {
                    return ToolResult.failure(actionability.errorCode());
                }
                var selector = TargetSelector.builder().id(resolved.primary().targetId()).build();
                var preSnapshot = driver.snapshot(uiContext, SnapshotOptions.DEFAULT);
                var preSnapshotRef = registries.uiSnapshotJournal().record(uiContext, preSnapshot);
                var actionResult = driver.action(uiContext, new UiActionRequest(selector, action, arguments));
                if (!actionResult.accepted()) {
                    return ToolResult.failure(actionResult.reason() == null || actionResult.reason().isBlank()
                            ? "unsupported_action"
                            : actionResult.reason());
                }
                var postSnapshot = driver.snapshot(uiContext, SnapshotOptions.DEFAULT);
                var postSnapshotRef = registries.uiSnapshotJournal().record(uiContext, postSnapshot);
                return ToolResult.success(Map.of(
                        "driverId", driver.descriptor().id(),
                        "action", action,
                        "performed", actionResult.performed(),
                        "resolvedTarget", targetToMap(resolved.primary()),
                        "preSnapshotRef", preSnapshotRef,
                        "postSnapshotRef", postSnapshotRef,
                        "postActionSnapshot", snapshotToMap(postSnapshot),
                        "needsReinspect", true
                ));
            });
        });
        registry.registerTool(definition("moddev.ui_snapshot"), (context, arguments) -> {
            var uiContext = uiContext(arguments);
            return withComposition(uiContext, arguments, composition -> {
                var snapshot = aggregateSnapshot(uiContext, composition);
                var snapshotRef = registries.uiSnapshotJournal().record(uiContext, snapshot);
                return ToolResult.success(withUiContextState(withSnapshotRef(snapshotToMap(snapshot), snapshotRef), uiContext));
            });
        });
        registry.registerTool(definition("moddev.ui_query"), (context, arguments) -> {
            var uiContext = uiContext(arguments);
            var selector = selectorFrom(arguments.get("selector"));
            return withComposition(uiContext, arguments, composition -> ToolResult.success(withUiContextState(Map.of(
                    "driverId", composition.defaultDriverId(),
                    "drivers", driverDetails(composition.drivers()),
                    "targets", aggregateQueryTargets(uiContext, composition, selector).stream().map(this::targetToMap).toList()
            ), uiContext)));
        });
        registry.registerTool(definition("moddev.ui_capture"), (context, arguments) -> {
            var uiContext = uiContext(arguments);
            var targets = arguments.containsKey("targets") || arguments.containsKey("target")
                    ? selectorsFrom(arguments.containsKey("targets") ? arguments.get("targets") : arguments.get("target"))
                    : null;
            var exclude = arguments.containsKey("exclude")
                    ? selectorsFrom(arguments.get("exclude"))
                    : List.<TargetSelector>of();
            var request = new CaptureRequest(
                    (String) arguments.getOrDefault("mode", "full"),
                    targets,
                    exclude,
                    (Boolean) arguments.getOrDefault("withOverlays", true)
            );
            return withDriver(uiContext, driver -> captureResult(
                    driver,
                    uiContext,
                    request,
                    targets,
                    exclude,
                    (String) arguments.getOrDefault("source", "auto")
            ));
        });
        registry.registerTool(definition("moddev.ui_action"), (context, arguments) -> {
            var uiContext = uiContext(arguments);
            var targetSelector = selectorFrom(arguments.get("target"));
            var request = new UiActionRequest(
                    targetSelector,
                    (String) arguments.getOrDefault("action", "click"),
                    arguments
            );
            if (selectorSpecified(targetSelector)) {
                var composition = filteredComposition(uiContext, arguments);
                if (composition.drivers().isEmpty()) {
                    return ToolResult.failure("unsupported: no ui driver matched screenClass=" + uiContext.screenClass() + ", modId=" + uiContext.modId());
                }
                var matchingDrivers = composition.drivers().stream()
                        .filter(driver -> !driver.query(uiContext, targetSelector).isEmpty())
                        .toList();
                if (matchingDrivers.isEmpty()) {
                    return ToolResult.failure("target_not_found: selector did not match any target");
                }
                if (matchingDrivers.size() > 1) {
                    return ToolResult.failure("target_ambiguous");
                }
                return runUiAction(uiContext, arguments, targetSelector, request, matchingDrivers.getFirst());
            }
            return withDriver(uiContext, driver -> runUiAction(uiContext, arguments, targetSelector, request, driver));
        });
        registry.registerTool(definition("moddev.ui_wait"), (context, arguments) -> {
            var uiContext = uiContext(arguments);
            var condition = (String) arguments.getOrDefault("condition", "appeared");
            var timeoutMs = longArgument(arguments, "timeoutMs", 0L);
            var pollIntervalMs = Math.max(1L, longArgument(arguments, "pollIntervalMs", 50L));
            var stableForMs = longArgument(arguments, "stableForMs", 100L);
            return withDriver(uiContext, driver -> {
                var reference = targetReferenceFrom(arguments);
                if (reference != null) {
                    var waitResult = driver.waitFor(uiContext, new dev.vfyjxf.mcp.api.runtime.UiWaitRequest(
                            reference,
                            condition,
                            timeoutMs,
                            pollIntervalMs,
                            stableForMs
                    ));
                    return ToolResult.success(waitResultToMap(driver, condition, waitResult));
                }
                var selector = selectorFrom(arguments.get("selector"));
                var waitResult = waitForCondition(driver, uiContext, selector, condition, timeoutMs, pollIntervalMs, stableForMs);
                return ToolResult.success(Map.of(
                        "driverId", driver.descriptor().id(),
                        "condition", condition,
                        "matched", waitResult.matched(),
                        "timedOut", waitResult.timedOut(),
                        "elapsedMs", waitResult.elapsedMs(),
                        "targets", waitResult.targets().stream().map(this::targetToMap).toList()
                ));
            });
        });
        registry.registerTool(definition("moddev.ui_inspect_at"), (context, arguments) -> {
            var uiContext = uiContext(arguments);
            var x = ((Number) arguments.getOrDefault("x", 0)).intValue();
            var y = ((Number) arguments.getOrDefault("y", 0)).intValue();
            return withComposition(uiContext, arguments, composition -> {
                var mergedMatches = new LinkedHashMap<String, UiTarget>();
                for (var driver : composition.drivers()) {
                    var inspectResult = driver.inspectAt(uiContext, x, y);
                    if (!inspectResult.accepted()) {
                        return operationRejected("inspect", driver, inspectResult);
                    }
                    for (var target : inspectResult.value()) {
                        mergedMatches.put(target.driverId() + ":" + target.targetId(), target);
                    }
                }
                var matches = List.copyOf(mergedMatches.values());
                var topmost = matches.stream()
                        .min((left, right) -> Integer.compare(area(left), area(right)))
                        .orElse(null);
                return ToolResult.success(withInspectTopmost(composition.defaultDriverId(), matches, topmost));
            });
        });
        registry.registerTool(definition("moddev.ui_get_tooltip"), (context, arguments) -> {
            var uiContext = uiContext(arguments);
            var selector = selectorFrom(arguments.get("target"));
            return withDriver(uiContext, driver -> {
                var target = driver.query(uiContext, selector).stream().findFirst().orElse(null);
                if (target == null) {
                    return ToolResult.failure("target_not_found: selector did not match any target");
                }
                var tooltipResult = driver.tooltip(uiContext, selector);
                if (!tooltipResult.accepted()) {
                    return operationRejected("tooltip", driver, tooltipResult);
                }
                var tooltip = tooltipResult.value();
                return ToolResult.success(withTooltipDetails(driver.descriptor().id(), tooltip, target));
            });
        });
        registry.registerTool(definition("moddev.ui_get_interaction_state"), (context, arguments) -> {
            var uiContext = uiContext(arguments);
            return withDriver(uiContext, driver -> ToolResult.success(interactionStateToMap(driver.interactionState(uiContext))));
        });
        registry.registerTool(definition("moddev.ui_get_live_screen"), (context, arguments) -> ToolResult.success(liveScreenToMap(screenProbe.metrics(), arguments)));
        registry.registerTool(definition("moddev.ui_get_target_details"), (context, arguments) -> {
            var uiContext = uiContext(arguments);
            var selector = selectorFrom(arguments.get("target"));
            return withDriver(uiContext, driver -> {
                var missingTarget = missingTargetResult(driver, uiContext, selector);
                if (missingTarget != null) {
                    return missingTarget;
                }
                return ToolResult.success(targetDetails(driver, uiContext, selector));
            });
        });
        registry.registerTool(definition("moddev.ui_run_intent"), (context, arguments) -> {
            var intent = stringArgument(arguments.get("intent"));
            if (intent == null || intent.isBlank()) {
                return ToolResult.failure("invalid_input: missing intent");
            }
            var uiContext = uiContext(arguments);
            return withDriver(uiContext, driver -> runIntentResult(uiContext, driver, intent, arguments));
        });
        registry.registerTool(definition("moddev.ui_close"), (context, arguments) -> {
            var uiContext = uiContext(arguments);
            return withDriver(uiContext, driver -> semanticActionResult(uiContext, driver, "close", TargetSelector.builder().build(), arguments));
        });
        registry.registerTool(definition("moddev.ui_switch"), (context, arguments) -> {
            var uiContext = uiContext(arguments);
            return withDriver(uiContext, driver -> semanticActionResult(uiContext, driver, "switch", selectorFrom(arguments.get("target")), arguments));
        });
    }

    private McpToolDefinition definition(String name) {
        return switch (name) {
            case "moddev.ui_session_open" -> new McpToolDefinition(
                    name,
                    name,
                    "Built-in UI automation tool",
                    objectSchema(
                            Map.of(
                                    "screenClass", stringSchema(),
                                    "modId", stringSchema(),
                                    "mouseX", integerSchema(),
                                    "mouseY", integerSchema()
                            ),
                            List.of()
                    ),
                    objectSchema(
                            Map.of(
                                    "sessionId", stringSchema(),
                                    "screenId", stringSchema(),
                                    "screenClass", stringSchema(),
                                    "driverId", stringSchema(),
                                    "refs", arraySchema(objectSchema())
                            ),
                            List.of("sessionId", "screenClass", "driverId", "refs")
                    ),
                    List.of("ui"),
                    "either",
                    false,
                    false,
                    "public",
                    "public"
            );
            case "moddev.ui_session_refresh" -> new McpToolDefinition(
                    name,
                    name,
                    "Built-in UI automation tool",
                    objectSchema(
                            Map.of(
                                    "sessionId", stringSchema(),
                                    "screenClass", stringSchema(),
                                    "modId", stringSchema(),
                                    "mouseX", integerSchema(),
                                    "mouseY", integerSchema()
                            ),
                            List.of("sessionId")
                    ),
                    objectSchema(
                            Map.of(
                                    "sessionId", stringSchema(),
                                    "screenId", stringSchema(),
                                    "screenClass", stringSchema(),
                                    "driverId", stringSchema(),
                                    "screenChanged", booleanSchema(),
                                    "refs", arraySchema(objectSchema())
                            ),
                            List.of("sessionId", "screenClass", "driverId", "screenChanged", "refs")
                    ),
                    List.of("ui"),
                    "either",
                    false,
                    false,
                    "public",
                    "public"
            );
            case "moddev.ui_click_ref" -> new McpToolDefinition(
                    name,
                    name,
                    "Built-in UI automation tool",
                    objectSchema(
                            Map.of(
                                    "sessionId", stringSchema(),
                                    "refId", stringSchema(),
                                    "waitCondition", stringSchema(),
                                    "waitTarget", objectSchema(),
                                    "waitTimeoutMs", integerSchema(),
                                    "waitPollIntervalMs", integerSchema(),
                                    "waitStableForMs", integerSchema()
                            ),
                            List.of("sessionId", "refId")
                    ),
                    objectSchema(
                            Map.of(
                                    "driverId", stringSchema(),
                                    "action", stringSchema(),
                                    "performed", booleanSchema(),
                                    "preSnapshotRef", stringSchema(),
                                    "postSnapshotRef", stringSchema(),
                                    "postActionSnapshot", objectSchema(),
                                    "wait", objectSchema()
                            ),
                            List.of("driverId", "action", "preSnapshotRef", "postSnapshotRef", "postActionSnapshot")
                    ),
                    List.of("ui"),
                    "either",
                    false,
                    false,
                    "public",
                    "public"
            );
            case "moddev.ui_hover_ref" -> new McpToolDefinition(
                    name,
                    name,
                    "Built-in UI automation tool",
                    objectSchema(
                            Map.of(
                                    "sessionId", stringSchema(),
                                    "refId", stringSchema(),
                                    "hoverDelayMs", integerSchema(),
                                    "waitCondition", stringSchema(),
                                    "waitTarget", objectSchema(),
                                    "waitTimeoutMs", integerSchema(),
                                    "waitPollIntervalMs", integerSchema(),
                                    "waitStableForMs", integerSchema()
                            ),
                            List.of("sessionId", "refId")
                    ),
                    objectSchema(
                            Map.of(
                                    "driverId", stringSchema(),
                                    "action", stringSchema(),
                                    "performed", booleanSchema(),
                                    "preSnapshotRef", stringSchema(),
                                    "postSnapshotRef", stringSchema(),
                                    "postActionSnapshot", objectSchema(),
                                    "wait", objectSchema()
                            ),
                            List.of("driverId", "action", "preSnapshotRef", "postSnapshotRef", "postActionSnapshot")
                    ),
                    List.of("ui"),
                    "either",
                    false,
                    false,
                    "public",
                    "public"
            );
            case "moddev.ui_press_key" -> new McpToolDefinition(
                    name,
                    name,
                    "Built-in UI automation tool",
                    objectSchema(
                            Map.of(
                                    "keyCode", integerSchema(),
                                    "scanCode", integerSchema(),
                                    "modifiers", integerSchema()
                            ),
                            List.of("keyCode")
                    ),
                    objectSchema(
                            Map.of(
                                    "action", stringSchema(),
                                    "performed", booleanSchema(),
                                    "controller", stringSchema()
                            ),
                            List.of("action", "performed", "controller")
                    ),
                    List.of("ui"),
                    "client",
                    false,
                    false,
                    "public",
                    "public"
            );
            case "moddev.ui_type_text" -> new McpToolDefinition(
                    name,
                    name,
                    "Built-in UI automation tool",
                    objectSchema(
                            Map.of("text", stringSchema()),
                            List.of("text")
                    ),
                    objectSchema(
                            Map.of(
                                    "action", stringSchema(),
                                    "performed", booleanSchema(),
                                    "controller", stringSchema()
                            ),
                            List.of("action", "performed", "controller")
                    ),
                    List.of("ui"),
                    "client",
                    false,
                    false,
                    "public",
                    "public"
            );
            case "moddev.ui_wait_for" -> new McpToolDefinition(
                    name,
                    name,
                    "Built-in UI automation tool",
                    objectSchema(
                            Map.of(
                                    "sessionId", stringSchema(),
                                    "refId", stringSchema(),
                                    "condition", stringSchema(),
                                    "timeoutMs", integerSchema(),
                                    "pollIntervalMs", integerSchema(),
                                    "stableForMs", integerSchema()
                            ),
                            List.of("sessionId")
                    ),
                    objectSchema(
                            Map.of(
                                    "driverId", stringSchema(),
                                    "condition", stringSchema(),
                                    "matched", booleanSchema(),
                                    "timedOut", booleanSchema(),
                                    "elapsedMs", integerSchema(),
                                    "targets", arraySchema(objectSchema())
                            ),
                            List.of("driverId", "matched", "timedOut", "elapsedMs", "targets")
                    ),
                    List.of("ui"),
                    "either",
                    false,
                    false,
                    "public",
                    "public"
            );
            case "moddev.ui_screenshot" -> new McpToolDefinition(
                    name,
                    name,
                    "Built-in UI automation tool",
                    objectSchema(
                            Map.ofEntries(
                                    Map.entry("sessionId", stringSchema()),
                                    Map.entry("refId", stringSchema()),
                                    Map.entry("ref", stringSchema()),
                                    Map.entry("locator", objectSchema()),
                                    Map.entry("x", integerSchema()),
                                    Map.entry("y", integerSchema()),
                                    Map.entry("mode", stringSchema()),
                                    Map.entry("source", stringSchema()),
                                    Map.entry("withOverlays", booleanSchema())
                            ),
                            List.of()
                    ),
                    objectSchema(
                            Map.ofEntries(
                                    Map.entry("driverId", stringSchema()),
                                    Map.entry("mode", stringSchema()),
                                    Map.entry("resolvedTarget", objectSchema()),
                                    Map.entry("capturedTargets", arraySchema(objectSchema())),
                                    Map.entry("excludedTargets", arraySchema(objectSchema())),
                                    Map.entry("snapshotRef", stringSchema()),
                                    Map.entry("capturedSnapshot", objectSchema()),
                                    Map.entry("imageRef", stringSchema()),
                                    Map.entry("imagePath", stringSchema()),
                                    Map.entry("imageResourceUri", stringSchema()),
                                    Map.entry("imageMeta", objectSchema()),
                                    Map.entry("needsReinspect", booleanSchema())
                            ),
                            List.of("driverId", "snapshotRef", "imageRef", "imageMeta")
                    ),
                    List.of("ui"),
                    "either",
                    false,
                    false,
                    "public",
                    "public"
            );
            case "moddev.ui_batch" -> new McpToolDefinition(
                    name,
                    name,
                    "Built-in UI automation tool",
                    objectSchema(
                            Map.of(
                                    "sessionId", stringSchema(),
                                    "stopOnError", booleanSchema(),
                                    "steps", arraySchema(objectSchema())
                            ),
                            List.of("sessionId", "steps")
                    ),
                    objectSchema(
                            Map.of(
                                    "sessionId", stringSchema(),
                                    "success", booleanSchema(),
                                    "steps", arraySchema(objectSchema())
                            ),
                            List.of("sessionId", "success", "steps")
                    ),
                    List.of("ui"),
                    "either",
                    false,
                    false,
                    "public",
                    "public"
            );
            case "moddev.ui_trace_get" -> new McpToolDefinition(
                    name,
                    name,
                    "Built-in UI automation tool",
                    objectSchema(
                            Map.of("sessionId", stringSchema()),
                            List.of("sessionId")
                    ),
                    objectSchema(
                            Map.of(
                                    "sessionId", stringSchema(),
                                    "traces", arraySchema(objectSchema())
                            ),
                            List.of("sessionId", "traces")
                    ),
                    List.of("ui"),
                    "either",
                    false,
                    false,
                    "public",
                    "public"
            );
            case "moddev.ui_trace_recent" -> new McpToolDefinition(
                    name,
                    name,
                    "Built-in recent UI trace tool",
                    objectSchema(
                            Map.of(
                                    "sessionId", stringSchema(),
                                    "limit", integerSchema()
                            ),
                            List.of("sessionId")
                    ),
                    objectSchema(
                            Map.of(
                                    "sessionId", stringSchema(),
                                    "traces", arraySchema(objectSchema())
                            ),
                            List.of("sessionId", "traces")
                    ),
                    List.of("ui"),
                    "either",
                    false,
                    false,
                    "public",
                    "public"
            );
            case "moddev.ui_inspect" -> new McpToolDefinition(
                    name,
                    name,
                    "Built-in high-level UI inspect tool",
                    objectSchema(
                            Map.of(
                                    "screenClass", stringSchema(),
                                    "modId", stringSchema(),
                                    "mouseX", integerSchema(),
                                    "mouseY", integerSchema()
                            ),
                            List.of()
                    ),
                    objectSchema(
                            Map.of(
                                    "screen", stringSchema(),
                                    "screenId", stringSchema(),
                                    "driverId", stringSchema(),
                                    "summary", objectSchema(),
                                    "targets", arraySchema(objectSchema()),
                                    "interaction", objectSchema()
                            ),
                            List.of("screen", "screenId", "driverId", "summary", "targets", "interaction")
                    ),
                    List.of("ui"),
                    "either",
                    false,
                    false,
                    "public",
                    "public"
            );
            case "moddev.ui_act" -> new McpToolDefinition(
                    name,
                    name,
                    "Built-in high-level UI action tool",
                    objectSchema(
                            Map.of(
                                    "screenClass", stringSchema(),
                                    "modId", stringSchema(),
                                    "action", stringSchema(),
                                    "ref", stringSchema(),
                                    "locator", objectSchema(),
                                    "x", integerSchema(),
                                    "y", integerSchema()
                            ),
                            List.of("action")
                    ),
                    objectSchema(
                            Map.of(
                                    "driverId", stringSchema(),
                                    "action", stringSchema(),
                                    "performed", booleanSchema(),
                                    "resolvedTarget", objectSchema(),
                                    "preSnapshotRef", stringSchema(),
                                    "postSnapshotRef", stringSchema(),
                                    "postActionSnapshot", objectSchema(),
                                    "needsReinspect", booleanSchema()
                            ),
                            List.of("driverId", "action", "performed", "resolvedTarget", "preSnapshotRef", "postSnapshotRef", "postActionSnapshot", "needsReinspect")
                    ),
                    List.of("ui"),
                    "either",
                    false,
                    false,
                    "public",
                    "public"
            );
            case "moddev.ui_snapshot" -> new McpToolDefinition(
                    name,
                    name,
                    "Built-in UI tool",
                    objectSchema(
                            Map.of(
                                    "driverId", stringSchema(),
                                    "includeDrivers", arraySchema(stringSchema()),
                                    "excludeDrivers", arraySchema(stringSchema()),
                                    "screenClass", stringSchema(),
                                    "modId", stringSchema(),
                                    "mouseX", integerSchema(),
                                    "mouseY", integerSchema()
                            ),
                            List.of()
                    ),
                    objectSchema(
                            Map.of(
                                    "screenId", stringSchema(),
                                    "screenClass", stringSchema(),
                                    "driverId", stringSchema(),
                                    "drivers", arraySchema(objectSchema()),
                                    "targets", arraySchema(objectSchema()),
                                    "focusedTargetId", stringSchema(),
                                    "snapshotRef", stringSchema()
                            ),
                            List.of("driverId", "drivers", "targets", "snapshotRef")
                    ),
                    List.of("ui"),
                    "either",
                    false,
                    false,
                    "public",
                    "public"
            );
            case "moddev.ui_query" -> new McpToolDefinition(
                    name,
                    name,
                    "Built-in UI tool",
                    objectSchema(
                            Map.of(
                                    "driverId", stringSchema(),
                                    "includeDrivers", arraySchema(stringSchema()),
                                    "excludeDrivers", arraySchema(stringSchema()),
                                    "screenClass", stringSchema(),
                                    "modId", stringSchema(),
                                    "selector", objectSchema()
                            ),
                            List.of()
                    ),
                    objectSchema(
                            Map.of(
                                    "driverId", stringSchema(),
                                    "drivers", arraySchema(objectSchema()),
                                    "targets", arraySchema(objectSchema())
                            ),
                            List.of("driverId", "drivers", "targets")
                    ),
                    List.of("ui"),
                    "either",
                    false,
                    false,
                    "public",
                    "public"
            );
            case "moddev.ui_capture" -> new McpToolDefinition(
                    name,
                    name,
                    "Built-in UI tool",
                    objectSchema(
                            Map.of(
                                    "screenClass", stringSchema(),
                                    "modId", stringSchema(),
                                    "mode", stringSchema(),
                                    "target", objectSchema(),
                                    "targets", arraySchema(objectSchema()),
                                    "exclude", arraySchema(objectSchema()),
                                    "source", stringSchema(),
                                    "withOverlays", booleanSchema()
                            ),
                            List.of()
                    ),
                    objectSchema(
                            Map.of(
                                    "driverId", stringSchema(),
                                    "mode", stringSchema(),
                                    "capturedTargets", arraySchema(objectSchema()),
                                    "excludedTargets", arraySchema(objectSchema()),
                                    "snapshotRef", stringSchema(),
                                    "capturedSnapshot", objectSchema(),
                                    "imageRef", stringSchema(),
                                    "imagePath", stringSchema(),
                                    "imageResourceUri", stringSchema(),
                                    "imageMeta", objectSchema()
                            ),
                            List.of("driverId", "capturedTargets", "excludedTargets", "snapshotRef", "imageRef", "imageMeta")
                    ),
                    List.of("ui"),
                    "either",
                    false,
                    false,
                    "public",
                    "public"
            );
            case "moddev.ui_action" -> new McpToolDefinition(
                    name,
                    name,
                    "Built-in UI tool",
                    objectSchema(
                            Map.of(
                                    "screenClass", stringSchema(),
                                    "modId", stringSchema(),
                                    "target", objectSchema(),
                                    "action", stringSchema(),
                                    "waitCondition", stringSchema(),
                                    "waitTarget", objectSchema(),
                                    "waitTimeoutMs", integerSchema(),
                                    "waitPollIntervalMs", integerSchema(),
                                    "waitStableForMs", integerSchema()
                            ),
                            List.of()
                    ),
                    objectSchema(
                            Map.of(
                                    "driverId", stringSchema(),
                                    "action", stringSchema(),
                                    "performed", booleanSchema(),
                                    "preSnapshotRef", stringSchema(),
                                    "postSnapshotRef", stringSchema(),
                                    "postActionSnapshot", objectSchema(),
                                    "wait", objectSchema()
                            ),
                            List.of("driverId", "action", "preSnapshotRef", "postSnapshotRef", "postActionSnapshot")
                    ),
                    List.of("ui"),
                    "either",
                    false,
                    false,
                    "public",
                    "public"
            );
            case "moddev.ui_wait" -> new McpToolDefinition(
                    name,
                    name,
                    "Built-in UI tool",
                    objectSchema(
                            Map.of(
                                    "screenClass", stringSchema(),
                                    "modId", stringSchema(),
                                    "selector", objectSchema(),
                                    "condition", stringSchema(),
                                    "timeoutMs", integerSchema(),
                                    "pollIntervalMs", integerSchema(),
                                    "stableForMs", integerSchema()
                            ),
                            List.of()
                    ),
                    objectSchema(
                            Map.of(
                                    "driverId", stringSchema(),
                                    "condition", stringSchema(),
                                    "matched", booleanSchema(),
                                    "timedOut", booleanSchema(),
                                    "elapsedMs", integerSchema(),
                                    "targets", arraySchema(objectSchema())
                            ),
                            List.of("driverId", "matched", "timedOut", "elapsedMs", "targets")
                    ),
                    List.of("ui"),
                    "either",
                    false,
                    false,
                    "public",
                    "public"
            );
            case "moddev.ui_inspect_at" -> new McpToolDefinition(
                    name,
                    name,
                    "Built-in UI tool",
                    objectSchema(
                            Map.of(
                                    "screenClass", stringSchema(),
                                    "modId", stringSchema(),
                                    "driverId", stringSchema(),
                                    "includeDrivers", arraySchema(stringSchema()),
                                    "excludeDrivers", arraySchema(stringSchema()),
                                    "x", integerSchema(),
                                    "y", integerSchema()
                            ),
                            List.of()
                    ),
                    objectSchema(
                            Map.of(
                                    "driverId", stringSchema(),
                                    "targets", arraySchema(objectSchema()),
                                    "topmostTarget", objectSchema(),
                                    "modId", stringSchema(),
                                    "role", stringSchema(),
                                    "text", stringSchema(),
                                    "bounds", objectSchema(),
                                    "extensions", objectSchema()
                            ),
                            List.of("driverId", "targets", "topmostTarget")
                    ),
                    List.of("ui"),
                    "either",
                    false,
                    false,
                    "public",
                    "public"
            );
            case "moddev.ui_get_tooltip" -> new McpToolDefinition(
                    name,
                    name,
                    "Built-in UI tool",
                    objectSchema(
                            Map.of(
                                    "screenClass", stringSchema(),
                                    "modId", stringSchema(),
                                    "target", objectSchema()
                            ),
                            List.of()
                    ),
                    objectSchema(
                            Map.of(
                                    "driverId", stringSchema(),
                                    "targetId", stringSchema(),
                                    "lines", arraySchema(stringSchema()),
                                    "bounds", objectSchema(),
                                    "modId", stringSchema(),
                                    "role", stringSchema(),
                                    "text", stringSchema(),
                                    "extensions", objectSchema()
                            ),
                            List.of("driverId", "targetId", "lines", "bounds")
                    ),
                    List.of("ui"),
                    "either",
                    false,
                    false,
                    "public",
                    "public"
            );
            case "moddev.ui_get_interaction_state" -> new McpToolDefinition(
                    name,
                    name,
                    "Built-in UI tool",
                    objectSchema(
                            Map.of(
                                    "screenClass", stringSchema(),
                                    "modId", stringSchema(),
                                    "mouseX", integerSchema(),
                                    "mouseY", integerSchema()
                            ),
                            List.of()
                    ),
                    objectSchema(
                            Map.of(
                                    "driverId", stringSchema(),
                                    "focusedTarget", objectSchema(),
                                    "selectedTarget", objectSchema(),
                                    "hoveredTarget", objectSchema(),
                                    "activeTarget", objectSchema(),
                                    "cursorX", integerSchema(),
                                    "cursorY", integerSchema(),
                                    "textInputActive", booleanSchema(),
                                    "selectionSource", stringSchema()
                            ),
                            List.of("driverId", "focusedTarget", "selectedTarget", "hoveredTarget", "activeTarget", "selectionSource")
                    ),
                    List.of("ui"),
                    "either",
                    false,
                    false,
                    "public",
                    "public"
            );
            case "moddev.ui_get_live_screen" -> new McpToolDefinition(
                    name,
                    name,
                    "Built-in live screen probe tool",
                    objectSchema(
                            Map.of(
                                    "driverId", stringSchema(),
                                    "includeDrivers", arraySchema(stringSchema()),
                                    "excludeDrivers", arraySchema(stringSchema())
                            ),
                            List.of()
                    ),
                    objectSchema(
                            Map.of(
                                    "active", booleanSchema(),
                                    "screenClass", stringSchema(),
                                    "modId", stringSchema(),
                                    "driverId", stringSchema(),
                                    "drivers", arraySchema(objectSchema()),
                                    "guiWidth", integerSchema(),
                                    "guiHeight", integerSchema(),
                                    "framebufferWidth", integerSchema(),
                                    "framebufferHeight", integerSchema()
                            ),
                            List.of("active", "screenClass", "modId", "driverId", "drivers", "guiWidth", "guiHeight", "framebufferWidth", "framebufferHeight")
                    ),
                    List.of("ui"),
                    "client",
                    false,
                    false,
                    "public",
                    "public"
            );
            case "moddev.ui_get_target_details" -> new McpToolDefinition(
                    name,
                    name,
                    "Built-in UI tool",
                    objectSchema(
                            Map.of(
                                    "screenClass", stringSchema(),
                                    "modId", stringSchema(),
                                    "target", objectSchema()
                            ),
                            List.of()
                    ),
                    objectSchema(
                            Map.of(
                                    "driverId", stringSchema(),
                                    "target", objectSchema(),
                                    "captureRegion", objectSchema(),
                                    "hierarchyPath", arraySchema(stringSchema()),
                                    "interactionState", objectSchema(),
                                    "actions", arraySchema(stringSchema()),
                                    "overlay", booleanSchema(),
                                    "metadata", objectSchema(),
                                    "extensions", objectSchema()
                            ),
                            List.of("driverId", "target", "captureRegion", "hierarchyPath", "interactionState", "actions", "overlay", "metadata", "extensions")
                    ),
                    List.of("ui"),
                    "either",
                    false,
                    false,
                    "public",
                    "public"
            );
            case "moddev.ui_run_intent" -> new McpToolDefinition(
                    name,
                    name,
                    "Built-in UI tool",
                    objectSchema(
                            Map.of(
                                    "screenClass", stringSchema(),
                                    "modId", stringSchema(),
                                    "intent", stringSchema(),
                                    "waitCondition", stringSchema(),
                                    "waitTarget", objectSchema(),
                                    "waitTimeoutMs", integerSchema(),
                                    "waitPollIntervalMs", integerSchema(),
                                    "waitStableForMs", integerSchema()
                            ),
                            List.of("intent")
                    ),
                    objectSchema(
                            Map.of(
                                    "driverId", stringSchema(),
                                    "action", stringSchema(),
                                    "intent", stringSchema(),
                                    "performed", booleanSchema(),
                                    "preSnapshotRef", stringSchema(),
                                    "postSnapshotRef", stringSchema(),
                                    "postActionSnapshot", objectSchema(),
                                    "wait", objectSchema()
                            ),
                            List.of("driverId", "action", "intent", "performed", "preSnapshotRef", "postSnapshotRef", "postActionSnapshot")
                    ),
                    List.of("ui"),
                    "either",
                    false,
                    false,
                    "public",
                    "public"
            );
            case "moddev.ui_close", "moddev.ui_switch" -> new McpToolDefinition(
                    name,
                    name,
                    "Built-in UI tool",
                    objectSchema(
                            Map.of(
                                    "screenClass", stringSchema(),
                                    "modId", stringSchema(),
                                    "target", objectSchema(),
                                    "waitCondition", stringSchema(),
                                    "waitTarget", objectSchema(),
                                    "waitTimeoutMs", integerSchema(),
                                    "waitPollIntervalMs", integerSchema(),
                                    "waitStableForMs", integerSchema()
                            ),
                            List.of()
                    ),
                    objectSchema(
                            Map.of(
                                    "driverId", stringSchema(),
                                    "action", stringSchema(),
                                    "performed", booleanSchema(),
                                    "preSnapshotRef", stringSchema(),
                                    "postSnapshotRef", stringSchema(),
                                    "postActionSnapshot", objectSchema(),
                                    "wait", objectSchema()
                            ),
                            List.of("driverId", "action", "postSnapshotRef", "postActionSnapshot")
                    ),
                    List.of("ui"),
                    "either",
                    false,
                    false,
                    "public",
                    "public"
            );
            default -> new McpToolDefinition(name, name, "Built-in UI tool", Map.of(), Map.of(), List.of("ui"), "either", false, false, "public", "public");
        };
    }

    private Map<String, Object> objectSchema() {
        return objectSchema(Map.of(), List.of());
    }

    private Map<String, Object> objectSchema(Map<String, Object> properties, List<String> required) {
        return Map.of(
                "type", "object",
                "properties", properties,
                "required", required
        );
    }

    private Map<String, Object> stringSchema() {
        return Map.of("type", "string");
    }

    private Map<String, Object> integerSchema() {
        return Map.of("type", "integer");
    }

    private Map<String, Object> booleanSchema() {
        return Map.of("type", "boolean");
    }

    private Map<String, Object> arraySchema(Map<String, Object> items) {
        return Map.of(
                "type", "array",
                "items", items
        );
    }

    private ToolResult withDriver(UiContext context, java.util.function.Function<UiDriver, ToolResult> handler) {
        return withDriver(context, Map.of(), handler);
    }

    private ToolResult withDriver(UiContext context, Map<String, Object> arguments, java.util.function.Function<UiDriver, ToolResult> handler) {
        return withComposition(context, arguments, composition -> handler.apply(composition.drivers().getFirst()));
    }

    private ToolResult withComposition(UiContext context, Map<String, Object> arguments, java.util.function.Function<UiDriverComposition, ToolResult> handler) {
        var composition = filteredComposition(context, arguments);
        if (composition.drivers().isEmpty()) {
            return ToolResult.failure("unsupported: no ui driver matched screenClass=" + context.screenClass() + ", modId=" + context.modId());
        }
        return handler.apply(composition);
    }

    private UiDriverComposition filteredComposition(UiContext context, Map<String, Object> arguments) {
        var composition = compositionResolver.resolve(context);
        var requestedDriverId = stringArgument(arguments.get("driverId"));
        var includeDrivers = stringSetArgument(arguments.get("includeDrivers"));
        var excludeDrivers = stringSetArgument(arguments.get("excludeDrivers"));
        var filteredDrivers = composition.drivers().stream()
                .filter(driver -> driverMatchesFilter(driver, requestedDriverId, includeDrivers, excludeDrivers))
                .toList();
        var defaultDriverId = filteredDrivers.isEmpty() ? "" : filteredDrivers.getFirst().descriptor().id();
        return new UiDriverComposition(context, filteredDrivers, defaultDriverId);
    }

    private boolean driverMatchesFilter(UiDriver driver, String requestedDriverId, Set<String> includeDrivers, Set<String> excludeDrivers) {
        var driverId = driver.descriptor().id();
        if (requestedDriverId != null && !requestedDriverId.isBlank()) {
            return requestedDriverId.equals(driverId);
        }
        if (!includeDrivers.isEmpty() && !includeDrivers.contains(driverId)) {
            return false;
        }
        return !excludeDrivers.contains(driverId);
    }

    private Set<String> stringSetArgument(Object value) {
        if (value instanceof Iterable<?> iterable) {
            var result = new java.util.LinkedHashSet<String>();
            for (var entry : iterable) {
                if (entry != null) {
                    result.add(String.valueOf(entry));
                }
            }
            return Set.copyOf(result);
        }
        if (value == null) {
            return Set.of();
        }
        return Set.of(String.valueOf(value));
    }

    private UiSnapshot aggregateSnapshot(UiContext context, UiDriverComposition composition) {
        var snapshots = composition.drivers().stream()
                .map(driver -> driver.snapshot(context, SnapshotOptions.DEFAULT))
                .toList();
        var primary = snapshots.isEmpty() ? null : snapshots.getFirst();
        var targets = new LinkedHashMap<String, UiTarget>();
        for (var snapshot : snapshots) {
            for (var target : snapshot.targets()) {
                targets.put(target.driverId() + ":" + target.targetId(), target);
            }
        }
        var overlays = snapshots.stream()
                .flatMap(snapshot -> snapshot.overlays().stream())
                .toList();
        var extensions = new LinkedHashMap<String, Object>();
        if (primary != null) {
            extensions.putAll(primary.extensions());
        }
        extensions.put("drivers", driverDetails(composition.drivers()));
        // Interaction ids are only safe to expose when the composed view still has one
        // unambiguous driver-scoped answer for the field. Otherwise the aggregated snapshot
        // would look more precise than the runtime actually is.
        var focusedTargetId = mergeInteractionId(snapshots, UiSnapshot::focusedTargetId);
        var selectedTargetId = mergeInteractionId(snapshots, UiSnapshot::selectedTargetId);
        var hoveredTargetId = mergeInteractionId(snapshots, UiSnapshot::hoveredTargetId);
        var activeTargetId = mergeInteractionId(snapshots, UiSnapshot::activeTargetId);
        return new UiSnapshot(
                primary == null ? "screen" : primary.screenId(),
                context.screenClass(),
                composition.defaultDriverId(),
                List.copyOf(targets.values()),
                overlays,
                focusedTargetId,
                selectedTargetId,
                hoveredTargetId,
                activeTargetId,
                Map.copyOf(extensions)
        );
    }

    private String mergeInteractionId(List<UiSnapshot> snapshots, java.util.function.Function<UiSnapshot, String> accessor) {
        // Snapshot interaction ids are local to a driver. Two drivers may both report "button-0",
        // which is still ambiguous in a composed snapshot, so uniqueness has to be checked against
        // driver-scoped candidates instead of raw ids.
        var scopedCandidates = snapshots.stream()
                .map(snapshot -> {
                    var interactionId = accessor.apply(snapshot);
                    if (interactionId == null || interactionId.isBlank()) {
                        return null;
                    }
                    return snapshot.driverId() + ":" + interactionId;
                })
                .filter(candidate -> candidate != null)
                .distinct()
                .toList();
        if (scopedCandidates.size() != 1) {
            return null;
        }
        var scoped = scopedCandidates.getFirst();
        var separator = scoped.indexOf(':');
        return separator < 0 ? scoped : scoped.substring(separator + 1);
    }

    private List<UiTarget> aggregateQueryTargets(UiContext context, UiDriverComposition composition, TargetSelector selector) {
        var targets = new LinkedHashMap<String, UiTarget>();
        for (var driver : composition.drivers()) {
            for (var target : driver.query(context, selector)) {
                targets.put(target.driverId() + ":" + target.targetId(), target);
            }
        }
        return List.copyOf(targets.values());
    }

    private List<Map<String, Object>> driverDetails(List<UiDriver> drivers) {
        return drivers.stream()
                .map(driver -> Map.<String, Object>of(
                        "driverId", driver.descriptor().id(),
                        "modId", driver.descriptor().modId(),
                        "priority", driver.descriptor().priority(),
                        "capabilities", driver.descriptor().capabilities().stream().sorted().toList()
                ))
                .toList();
    }

    private <T> ToolResult operationRejected(String operation, UiDriver driver, OperationResult<T> result) {
        var reason = result.reason() == null || result.reason().isBlank()
                ? "Operation rejected"
                : result.reason();
        return ToolResult.failure("unsupported: " + operation + " rejected by driver " + driver.descriptor().id() + ": " + reason);
    }

    private ToolResult missingTargetResult(UiDriver driver, UiContext context, TargetSelector selector) {
        if (!selectorSpecified(selector)) {
            return null;
        }
        return driver.query(context, selector).isEmpty()
                ? ToolResult.failure("target_not_found: selector did not match any target")
                : null;
    }

    private ToolResult runUiAction(
            UiContext uiContext,
            Map<String, Object> arguments,
            TargetSelector targetSelector,
            UiActionRequest request,
            UiDriver driver
    ) {
        var missingTarget = missingTargetResult(driver, uiContext, targetSelector);
        if (missingTarget != null) {
            return missingTarget;
        }
        var preSnapshot = driver.snapshot(uiContext, SnapshotOptions.DEFAULT);
        var preSnapshotRef = registries.uiSnapshotJournal().record(uiContext, preSnapshot);
        var actionResult = driver.action(uiContext, request);
        if (!actionResult.accepted()) {
            return operationRejected("action", driver, actionResult);
        }
        var result = actionResult.value();
        var postSnapshot = driver.snapshot(uiContext, SnapshotOptions.DEFAULT);
        var postSnapshotRef = registries.uiSnapshotJournal().record(uiContext, postSnapshot);
        return ToolResult.success(withOptionalWait(
                uiContext,
                driver,
                arguments,
                targetSelector,
                withActionSnapshots(result, preSnapshotRef, postSnapshotRef, postSnapshot)
        ));
    }

    private boolean selectorSpecified(TargetSelector selector) {
        return selector.scope() != null
                || selector.screen() != null
                || selector.modId() != null
                || selector.text() != null
                || selector.role() != null
                || selector.id() != null
                || selector.bounds() != null;
    }

    private boolean explicitSelectorsRequested(List<TargetSelector> selectors) {
        return selectors != null && !selectors.isEmpty();
    }

    private UiContext uiContext(Map<String, Object> arguments) {
        var screenClass = stringArgument(arguments.get("screenClass"));
        var modId = stringArgument(arguments.get("modId"));
        ClientScreenMetrics metrics = null;
        Object screenHandle = null;
        if (screenClass == null || screenClass.isBlank()) {
            metrics = screenProbe.metrics();
        }
        if ((screenClass == null || screenClass.isBlank()) && metrics != null) {
            screenClass = metrics.screenClass();
            screenHandle = liveScreenHandle(metrics);
        }
        if (modId == null || modId.isBlank()) {
            modId = "minecraft";
        }
        var pointerState = registries.uiPointerStates().stateFor(screenClass, modId);
        var mouseX = arguments.containsKey("mouseX")
                ? ((Number) arguments.getOrDefault("mouseX", 0)).intValue()
                : pointerState.mouseX();
        var mouseY = arguments.containsKey("mouseY")
                ? ((Number) arguments.getOrDefault("mouseY", 0)).intValue()
                : pointerState.mouseY();
        return new MapBackedUiContext(
                screenClass,
                modId,
                mouseX,
                mouseY,
                screenHandle
        );
    }

    private Object liveScreenHandle(ClientScreenMetrics metrics) {
        if (metrics == null || metrics.screenClass() == null || metrics.screenClass().isBlank()) {
            return null;
        }
        var liveScreen = currentScreenSupplier.get();
        if (liveScreen != null && metrics.screenClass().equals(liveScreen.getClass().getName())) {
            return liveScreen;
        }
        return null;
    }

    private static Object currentClientScreen() {
        try {
            var minecraftClass = Class.forName("net.minecraft.client.Minecraft");
            var minecraft = minecraftClass.getMethod("getInstance").invoke(null);
            if (minecraft == null) {
                return null;
            }
            return minecraftClass.getField("screen").get(minecraft);
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return null;
        }
    }

    private ToolResult unavailableScreenResult(Map<String, Object> arguments) {
        var screenClass = stringArgument(arguments.get("screenClass"));
        if (screenClass != null && !screenClass.isBlank()) {
            return null;
        }
        var metricsResult = liveScreenMetrics();
        if (!metricsResult.accepted()) {
            return ToolResult.failure(metricsResult.reason());
        }
        var metrics = metricsResult.value();
        if (metrics.screenClass() != null && !metrics.screenClass().isBlank()) {
            return null;
        }
        return ToolResult.failure("screen_unavailable");
    }

    private OperationResult<ClientScreenMetrics> liveScreenMetrics() {
        try {
            return OperationResult.success(screenProbe.metrics());
        } catch (RuntimeException exception) {
            return OperationResult.rejected("runtime_unavailable");
        }
    }

    private OperationResult<UiAutomationSession> refreshSessionOnDemand(String sessionId, UiAutomationSession session) {
        var metricsResult = liveScreenMetrics();
        if (!metricsResult.accepted()) {
            registries.uiAutomationSessions().markStale(sessionId);
            return OperationResult.rejected(metricsResult.reason());
        }
        var metrics = metricsResult.value();
        if (metrics.screenClass() == null || metrics.screenClass().isBlank()) {
            return OperationResult.rejected("screen_unavailable");
        }
        if (!session.stale() && java.util.Objects.equals(session.snapshot().screenClass(), metrics.screenClass())) {
            return OperationResult.success(session);
        }
        var modId = session.snapshot().targets().stream()
                .map(UiTarget::modId)
                .filter(candidate -> candidate != null && !candidate.isBlank())
                .findFirst()
                .orElse("minecraft");
        var uiContext = uiContext(Map.of(
                "screenClass", metrics.screenClass(),
                "modId", modId
        ));
        var refreshResult = refreshSessionWithContext(sessionId, uiContext);
        if (!refreshResult.accepted()) {
            return OperationResult.rejected(refreshResult.reason());
        }
        return OperationResult.success(refreshResult.value().session());
    }

    private OperationResult<UiAutomationSessionManager.RefreshResult> refreshSessionNow(String sessionId, Map<String, Object> arguments) {
        var unavailable = unavailableScreenResult(arguments);
        if (unavailable != null) {
            return OperationResult.rejected(unavailable.error());
        }
        return refreshSessionWithContext(sessionId, uiContext(arguments));
    }

    private OperationResult<UiAutomationSessionManager.RefreshResult> refreshSessionWithContext(String sessionId, UiContext uiContext) {
        var driver = registries.uiDrivers().select(uiContext);
        if (driver.isEmpty()) {
            registries.uiAutomationSessions().markStale(sessionId);
            return OperationResult.rejected("session_stale");
        }
        var refresh = registries.uiAutomationSessions().refresh(sessionId, driver.get().snapshot(uiContext, SnapshotOptions.DEFAULT));
        if (refresh.isEmpty()) {
            return OperationResult.rejected("session_not_found");
        }
        return OperationResult.success(refresh.get());
    }

    private record ErrorInfo(String code, String message) {
    }

    private ErrorInfo errorInfo(String rawError) {
        if (rawError == null || rawError.isBlank()) {
            return new ErrorInfo("", "");
        }
        var separator = rawError.indexOf(':');
        if (separator < 0) {
            return new ErrorInfo(rawError.trim(), "");
        }
        return new ErrorInfo(
                rawError.substring(0, separator).trim(),
                rawError.substring(separator + 1).trim()
        );
    }

    private void recordSessionTrace(String sessionId, String type, long startedAt, ToolResult result) {
        if (sessionId == null || sessionId.isBlank()) {
            return;
        }
        var errorInfo = errorInfo(result.error());
        registries.uiAutomationSessions().recordTrace(
                sessionId,
                type,
                elapsedMillis(startedAt),
                result.success(),
                errorInfo.code(),
                errorInfo.message()
        );
    }

    private ToolResult sessionRefActionResult(Map<String, Object> arguments, String action) {
        var sessionId = stringArgument(arguments.get("sessionId"));
        if (sessionId == null || sessionId.isBlank()) {
            return ToolResult.failure("invalid_input: missing sessionId");
        }
        var refId = stringArgument(arguments.get("refId"));
        if (refId == null || refId.isBlank()) {
            return ToolResult.failure("invalid_input: missing refId");
        }
        var startedAt = System.nanoTime();
        var session = registries.uiAutomationSessions().find(sessionId).orElse(null);
        if (session == null) {
            var result = ToolResult.failure("session_not_found");
            recordSessionTrace(sessionId, action + "Ref", startedAt, result);
            return result;
        }
        var refreshedSessionResult = refreshSessionOnDemand(sessionId, session);
        if (!refreshedSessionResult.accepted()) {
            var result = ToolResult.failure(refreshedSessionResult.reason());
            recordSessionTrace(sessionId, action + "Ref", startedAt, result);
            return result;
        }
        session = refreshedSessionResult.value();
        var targetResult = registries.uiAutomationSessions().resolveTarget(sessionId, refId);
        if (!targetResult.accepted()) {
            var result = ToolResult.failure(targetResult.reason());
            recordSessionTrace(sessionId, action + "Ref", startedAt, result);
            return result;
        }
        var target = targetResult.value();
        var resolvedTarget = target;
        var actionSession = session;
        var uiContext = automationContext(arguments, actionSession, resolvedTarget);
        var selector = TargetSelector.builder().id(resolvedTarget.targetId()).build();
        var driver = registries.uiDrivers().select(uiContext).orElse(null);
        if (driver == null) {
            var result = ToolResult.failure("unsupported: no ui driver matched screenClass=" + uiContext.screenClass() + ", modId=" + uiContext.modId());
            recordSessionTrace(sessionId, action + "Ref", startedAt, result);
            return result;
        }
        var missingTarget = missingTargetResult(driver, uiContext, selector);
        if (missingTarget != null) {
            var forcedRefresh = refreshSessionNow(sessionId, arguments);
            if (!forcedRefresh.accepted()) {
                var result = ToolResult.failure(forcedRefresh.reason());
                recordSessionTrace(sessionId, action + "Ref", startedAt, result);
                return result;
            }
            actionSession = forcedRefresh.value().session();
            var refreshedTargetResult = registries.uiAutomationSessions().resolveTarget(sessionId, refId);
            if (!refreshedTargetResult.accepted()) {
                var result = ToolResult.failure(refreshedTargetResult.reason());
                recordSessionTrace(sessionId, action + "Ref", startedAt, result);
                return result;
            }
            resolvedTarget = refreshedTargetResult.value();
            uiContext = automationContext(arguments, actionSession, resolvedTarget);
            selector = TargetSelector.builder().id(resolvedTarget.targetId()).build();
            driver = registries.uiDrivers().select(uiContext).orElse(null);
            if (driver == null) {
                var result = ToolResult.failure("unsupported: no ui driver matched screenClass=" + uiContext.screenClass() + ", modId=" + uiContext.modId());
                recordSessionTrace(sessionId, action + "Ref", startedAt, result);
                return result;
            }
            var refreshedMissingTarget = missingTargetResult(driver, uiContext, selector);
            if (refreshedMissingTarget != null) {
                recordSessionTrace(sessionId, action + "Ref", startedAt, refreshedMissingTarget);
                return refreshedMissingTarget;
            }
        }
        var preSnapshot = driver.snapshot(uiContext, SnapshotOptions.DEFAULT);
        var preSnapshotRef = registries.uiSnapshotJournal().record(uiContext, preSnapshot);
        var actionArguments = new java.util.LinkedHashMap<String, Object>();
        actionArguments.putAll(arguments);
        actionArguments.put("action", action);
        var actionResult = driver.action(uiContext, new UiActionRequest(selector, action, Map.copyOf(actionArguments)));
        ToolResult result;
        if (!actionResult.accepted()) {
            result = operationRejected("action", driver, actionResult);
        } else {
            var postSnapshot = driver.snapshot(uiContext, SnapshotOptions.DEFAULT);
            registries.uiAutomationSessions().refresh(sessionId, postSnapshot);
            var postSnapshotRef = registries.uiSnapshotJournal().record(uiContext, postSnapshot);
            result = ToolResult.success(withOptionalWait(
                    uiContext,
                    driver,
                    arguments,
                    selector,
                    withActionSnapshots(actionResult.value(), preSnapshotRef, postSnapshotRef, postSnapshot)
            ));
        }
        recordSessionTrace(sessionId, action + "Ref", startedAt, result);
        return result;
    }

    private ToolResult inputActionResult(String action, Map<String, Object> arguments) {
        var delegatedArguments = new java.util.LinkedHashMap<String, Object>();
        delegatedArguments.putAll(arguments);
        delegatedArguments.put("action", action);
        var result = inputActionDispatcher.dispatch(action, Map.copyOf(delegatedArguments));
        if (!result.success()) {
            return ToolResult.failure(result.error());
        }
        return ToolResult.success(withLiveScreenState(result.payload(action), safeLiveMetrics(), List.of(), ""));
    }

    private ToolResult sessionWaitResult(Map<String, Object> arguments) {
        var sessionId = stringArgument(arguments.get("sessionId"));
        if (sessionId == null || sessionId.isBlank()) {
            return ToolResult.failure("invalid_input: missing sessionId");
        }
        var startedAt = System.nanoTime();
        var session = registries.uiAutomationSessions().find(sessionId).orElse(null);
        if (session == null) {
            var result = ToolResult.failure("session_not_found");
            recordSessionTrace(sessionId, "waitFor", startedAt, result);
            return result;
        }
        TargetSelector selector = TargetSelector.builder().build();
        var refId = stringArgument(arguments.get("refId"));
        UiTarget target = null;
        if (refId != null && !refId.isBlank()) {
            var refreshedSessionResult = refreshSessionOnDemand(sessionId, session);
            if (!refreshedSessionResult.accepted()) {
                var result = ToolResult.failure(refreshedSessionResult.reason());
                recordSessionTrace(sessionId, "waitFor", startedAt, result);
                return result;
            }
            session = refreshedSessionResult.value();
            var targetResult = registries.uiAutomationSessions().resolveTarget(sessionId, refId);
            if (!targetResult.accepted()) {
                var result = ToolResult.failure(targetResult.reason());
                recordSessionTrace(sessionId, "waitFor", startedAt, result);
                return result;
            }
            target = targetResult.value();
            selector = TargetSelector.builder().id(target.targetId()).build();
        }
        var uiContext = automationContext(arguments, session, target);
        var condition = (String) arguments.getOrDefault("condition", "appeared");
        var timeoutMs = longArgument(arguments, "timeoutMs", 0L);
        var pollIntervalMs = Math.max(1L, longArgument(arguments, "pollIntervalMs", 50L));
        var stableForMs = longArgument(arguments, "stableForMs", 100L);
        var finalSelector = selector;
        var result = withDriver(uiContext, driver -> {
            var waitResult = waitForCondition(driver, uiContext, finalSelector, condition, timeoutMs, pollIntervalMs, stableForMs);
            return ToolResult.success(Map.of(
                    "driverId", driver.descriptor().id(),
                    "condition", condition,
                    "matched", waitResult.matched(),
                    "timedOut", waitResult.timedOut(),
                    "elapsedMs", waitResult.elapsedMs(),
                    "targets", waitResult.targets().stream().map(this::targetToMap).toList()
            ));
        });
        recordSessionTrace(sessionId, "waitFor", startedAt, result);
        return result;
    }

    private ToolResult screenshotResult(Map<String, Object> arguments) {
        var sessionId = stringArgument(arguments.get("sessionId"));
        if (sessionId != null && !sessionId.isBlank()) {
            return sessionScreenshotResult(arguments);
        }
        return liveScreenshotResult(arguments);
    }

    private ToolResult sessionScreenshotResult(Map<String, Object> arguments) {
        var sessionId = stringArgument(arguments.get("sessionId"));
        if (sessionId == null || sessionId.isBlank()) {
            return ToolResult.failure("invalid_input: missing sessionId");
        }
        var startedAt = System.nanoTime();
        var session = registries.uiAutomationSessions().find(sessionId).orElse(null);
        if (session == null) {
            var result = ToolResult.failure("session_not_found");
            recordSessionTrace(sessionId, "screenshot", startedAt, result);
            return result;
        }
        List<TargetSelector> selectors = null;
        UiTarget target = null;
        var refId = stringArgument(arguments.get("refId"));
        if (refId != null && !refId.isBlank()) {
            var refreshedSessionResult = refreshSessionOnDemand(sessionId, session);
            if (!refreshedSessionResult.accepted()) {
                var result = ToolResult.failure(refreshedSessionResult.reason());
                recordSessionTrace(sessionId, "screenshot", startedAt, result);
                return result;
            }
            session = refreshedSessionResult.value();
            var targetResult = registries.uiAutomationSessions().resolveTarget(sessionId, refId);
            if (!targetResult.accepted()) {
                var result = ToolResult.failure(targetResult.reason());
                recordSessionTrace(sessionId, "screenshot", startedAt, result);
                return result;
            }
            target = targetResult.value();
            selectors = List.of(TargetSelector.builder().id(target.targetId()).build());
        }
        var uiContext = automationContext(arguments, session, target);
        var request = new CaptureRequest(
                (String) arguments.getOrDefault("mode", selectors == null ? "full" : "crop"),
                selectors,
                List.of(),
                (Boolean) arguments.getOrDefault("withOverlays", true)
        );
        var finalSelectors = selectors;
        var result = withDriver(uiContext, driver -> captureResult(
                driver,
                uiContext,
                request,
                finalSelectors,
                List.of(),
                (String) arguments.getOrDefault("source", "auto")
        ));
        recordSessionTrace(sessionId, "screenshot", startedAt, result);
        return result;
    }

    private ToolResult liveScreenshotResult(Map<String, Object> arguments) {
        var uiContext = uiContext(arguments);
        return withDriver(uiContext, driver -> {
            // Live screenshots must also work in-world when there is no active Screen instance.
            // In that case the fallback-region driver still provides a capture-capable surface,
            // so we let driver selection and capture provider matching decide availability.
            var reference = targetReferenceFrom(arguments);
            List<TargetSelector> selectors = null;
            UiTarget resolvedTarget = null;
            if (reference != null) {
                var resolved = driver.resolve(uiContext, new UiResolveRequest(reference, false, true, true));
                if (!"resolved".equals(resolved.status())) {
                    return ToolResult.failure(resolved.errorCode());
                }
                resolvedTarget = resolved.primary();
                selectors = List.of(TargetSelector.builder().id(resolvedTarget.targetId()).build());
            }
            var request = new CaptureRequest(
                    (String) arguments.getOrDefault("mode", selectors == null ? "full" : "crop"),
                    selectors,
                    List.of(),
                    (Boolean) arguments.getOrDefault("withOverlays", true)
            );
            var result = captureResult(
                    driver,
                    uiContext,
                    request,
                    selectors,
                    List.of(),
                    (String) arguments.getOrDefault("source", "auto")
            );
            if (!result.success()) {
                return result;
            }
            @SuppressWarnings("unchecked")
            var payload = (Map<String, Object>) result.value();
            return ToolResult.success(conciseScreenshotPayload(payload, resolvedTarget));
        });
    }

    @SuppressWarnings("unchecked")
    private ToolResult sessionBatchResult(Map<String, Object> arguments) {
        var sessionId = stringArgument(arguments.get("sessionId"));
        if (sessionId == null || sessionId.isBlank()) {
            return ToolResult.failure("invalid_input: missing sessionId");
        }
        if (registries.uiAutomationSessions().find(sessionId).isEmpty()) {
            return ToolResult.failure("session_not_found");
        }
        if (!(arguments.get("steps") instanceof List<?> rawSteps)) {
            return ToolResult.failure("invalid_input: missing steps");
        }
        var stopOnError = !(arguments.get("stopOnError") instanceof Boolean stop) || stop;
        var stepResults = new java.util.ArrayList<Map<String, Object>>();
        Integer failureStepIndex = null;
        for (int index = 0; index < rawSteps.size(); index++) {
            if (!(rawSteps.get(index) instanceof Map<?, ?> rawStep)) {
                return ToolResult.failure("invalid_input: batch step must be an object");
            }
            var step = new java.util.LinkedHashMap<String, Object>();
            step.putAll((Map<String, Object>) rawStep);
            step.putIfAbsent("sessionId", sessionId);
            var type = stringArgument(step.get("type"));
            var startedAt = System.nanoTime();
            var stepResult = executeBatchStep(type, Map.copyOf(step));
            var elapsedMs = elapsedMillis(startedAt);
            var errorInfo = errorInfo(stepResult.error());
            registries.uiAutomationSessions().recordTrace(
                    sessionId,
                    type == null ? "" : type,
                    elapsedMs,
                    stepResult.success(),
                    stepResult.success() ? "" : errorInfo.code(),
                    stepResult.success() ? "" : errorInfo.message()
            );
            var stepSummary = new java.util.LinkedHashMap<String, Object>();
            stepSummary.put("index", index);
            stepSummary.put("type", type == null ? "" : type);
            stepSummary.put("success", stepResult.success());
            stepSummary.put("elapsedMs", elapsedMs);
            if (stepResult.success()) {
                stepSummary.put("result", stepResult.value());
            } else {
                stepSummary.put("errorCode", errorInfo.code());
                if (!errorInfo.message().isBlank()) {
                    stepSummary.put("errorMessage", errorInfo.message());
                }
            }
            stepResults.add(Map.copyOf(stepSummary));
            if (!stepResult.success() && stopOnError) {
                failureStepIndex = index;
                break;
            }
            if (!stepResult.success() && failureStepIndex == null) {
                failureStepIndex = index;
            }
        }
        var payload = new java.util.LinkedHashMap<String, Object>();
        payload.put("sessionId", sessionId);
        payload.put("success", failureStepIndex == null);
        payload.put("steps", List.copyOf(stepResults));
        if (failureStepIndex != null) {
            payload.put("errorCode", "batch_step_failed");
            payload.put("failureStepIndex", failureStepIndex);
        }
        return ToolResult.success(Map.copyOf(payload));
    }

    private ToolResult executeBatchStep(String type, Map<String, Object> step) {
        if (type == null || type.isBlank()) {
            return ToolResult.failure("invalid_input: batch step missing type");
        }
        return switch (type) {
            case "clickRef" -> sessionRefActionResult(step, "click");
            case "hoverRef" -> sessionRefActionResult(step, "hover");
            case "pressKey" -> inputActionResult("key_press", step);
            case "typeText" -> inputActionResult("type_text", step);
            case "waitFor" -> sessionWaitResult(step);
            case "screenshot" -> sessionScreenshotResult(step);
            case "refresh" -> refreshSessionStep(step);
            default -> ToolResult.failure("unsupported_action");
        };
    }

    private ToolResult refreshSessionStep(Map<String, Object> step) {
        var sessionId = stringArgument(step.get("sessionId"));
        if (sessionId == null || sessionId.isBlank()) {
            return ToolResult.failure("invalid_input: missing sessionId");
        }
        var startedAt = System.nanoTime();
        if (registries.uiAutomationSessions().find(sessionId).isEmpty()) {
            var result = ToolResult.failure("session_not_found");
            recordSessionTrace(sessionId, "refresh", startedAt, result);
            return result;
        }
        var refreshResult = refreshSessionNow(sessionId, step);
        if (!refreshResult.accepted()) {
            var result = ToolResult.failure(refreshResult.reason());
            recordSessionTrace(sessionId, "refresh", startedAt, result);
            return result;
        }
        var result = ToolResult.success(sessionPayload(refreshResult.value().session(), refreshResult.value().screenChanged()));
        recordSessionTrace(sessionId, "refresh", startedAt, result);
        return result;
    }

    private ToolResult traceGetResult(Map<String, Object> arguments) {
        var sessionId = stringArgument(arguments.get("sessionId"));
        if (sessionId == null || sessionId.isBlank()) {
            return ToolResult.failure("invalid_input: missing sessionId");
        }
        var trace = registries.uiAutomationSessions().trace(sessionId).orElse(null);
        if (trace == null) {
            return ToolResult.failure("session_not_found");
        }
        return ToolResult.success(tracePayload(sessionId, trace));
    }

    private ToolResult traceRecentResult(Map<String, Object> arguments) {
        var sessionId = stringArgument(arguments.get("sessionId"));
        if (sessionId == null || sessionId.isBlank()) {
            return ToolResult.failure("invalid_input: missing sessionId");
        }
        var trace = registries.uiAutomationSessions().trace(sessionId).orElse(null);
        if (trace == null) {
            return ToolResult.failure("session_not_found");
        }
        var limit = Math.max(1, (int) longArgument(arguments, "limit", 10L));
        var fromIndex = Math.max(0, trace.size() - limit);
        return ToolResult.success(tracePayload(sessionId, trace.subList(fromIndex, trace.size())));
    }

    private ToolResult captureResult(
            UiDriver driver,
            UiContext uiContext,
            CaptureRequest request,
            List<TargetSelector> targets,
            List<TargetSelector> exclude,
            String requestedSource
    ) {
        var captureHookResult = optionalCaptureHookResult(driver, uiContext, request);
        if (captureHookResult != null) {
            return captureHookResult;
        }
        var snapshot = driver.snapshot(uiContext, SnapshotOptions.DEFAULT);
        var snapshotRef = registries.uiSnapshotJournal().record(uiContext, snapshot);
        var captured = resolveTargets(driver, uiContext, targets, true);
        if (explicitSelectorsRequested(targets) && captured.isEmpty()) {
            return ToolResult.failure("target_not_found: selector did not match any target");
        }
        var excluded = resolveTargets(driver, uiContext, exclude, false);
        var filtered = captured.stream()
                .filter(target -> excluded.stream().noneMatch(excludedTarget -> excludedTarget.targetId().equals(target.targetId())))
                .toList();
        if (explicitSelectorsRequested(targets) && filtered.isEmpty()) {
            return ToolResult.failure("target_not_found: explicit targets were excluded or resolved to no capturable targets");
        }
        var captureImage = selectCaptureImage(
                requestedSource,
                uiContext,
                snapshot,
                request,
                filtered,
                excluded
        );
        if (captureImage.isEmpty()) {
            return ToolResult.failure(captureUnavailableMessage(requestedSource, driver.descriptor().id(), snapshot.screenClass()));
        }
        var processedCaptureImage = UiCapturePostProcessor.process(
                captureImage.get(),
                request,
                filtered,
                excluded
        );
        var artifact = registries.uiCaptureArtifactStore().store(
                driver.descriptor().id(),
                processedCaptureImage.pngBytes(),
                processedCaptureImage.width(),
                processedCaptureImage.height(),
                Map.of(
                        "source", processedCaptureImage.source(),
                        "providerId", processedCaptureImage.providerId()
                )
        );
        return ToolResult.success(Map.of(
                "driverId", driver.descriptor().id(),
                "mode", request.mode(),
                "capturedTargets", filtered.stream().map(this::targetToMap).toList(),
                "excludedTargets", excluded.stream().map(this::targetToMap).toList(),
                "snapshotRef", snapshotRef,
                "capturedSnapshot", snapshotToMap(snapshot),
                "imageRef", artifact.imageRef(),
                "imagePath", artifact.path(),
                "imageResourceUri", artifact.resourceUri(),
                "imageMeta", artifact.metadata()
        ));
    }

    private ToolResult optionalCaptureHookResult(UiDriver driver, UiContext uiContext, CaptureRequest request) {
        var captureResult = driver.capture(uiContext, request);
        if (captureResult.accepted()) {
            return null;
        }
        return "Capture not supported".equals(captureResult.reason())
                ? null
                : operationRejected("capture", driver, captureResult);
    }

    private UiContext automationContext(Map<String, Object> arguments, UiAutomationSession session, UiTarget target) {
        var delegatedArguments = new java.util.LinkedHashMap<String, Object>();
        delegatedArguments.putAll(arguments);
        delegatedArguments.put("screenClass", session.snapshot().screenClass());
        delegatedArguments.put("modId", target == null
                ? session.snapshot().targets().stream()
                        .map(UiTarget::modId)
                        .filter(modId -> modId != null && !modId.isBlank())
                        .findFirst()
                        .orElse("minecraft")
                : target.modId());
        return uiContext(Map.copyOf(delegatedArguments));
    }

    private String stringArgument(Object value) {
        return value instanceof String string ? string : null;
    }

    @SuppressWarnings("unchecked")
    private UiTargetReference targetReferenceFrom(Map<String, Object> arguments) {
        var ref = stringArgument(arguments.get("ref"));
        if (ref != null && !ref.isBlank()) {
            return UiTargetReference.ref(ref);
        }
        if (arguments.get("locator") instanceof Map<?, ?> rawLocator) {
            var locator = locatorFrom((Map<String, Object>) rawLocator);
            return UiTargetReference.locator(locator);
        }
        if (arguments.get("x") instanceof Number x && arguments.get("y") instanceof Number y) {
            return UiTargetReference.point(x.intValue(), y.intValue());
        }
        return null;
    }

    private UiLocator locatorFrom(Map<String, Object> rawLocator) {
        return new UiLocator(
                stringArgument(rawLocator.get("role")),
                stringArgument(rawLocator.get("text")),
                stringArgument(rawLocator.get("containsText")),
                stringArgument(rawLocator.get("id")),
                rawLocator.get("index") instanceof Number number ? number.intValue() : null,
                stringArgument(rawLocator.get("scopeRef"))
        );
    }

    @SuppressWarnings("unchecked")
    private TargetSelector selectorFrom(Object rawSelector) {
        if (!(rawSelector instanceof Map<?, ?> selectorMap)) {
            return TargetSelector.builder().build();
        }
        var map = (Map<String, Object>) selectorMap;
        var builder = TargetSelector.builder();
        if (map.get("scope") instanceof String scope) {
            builder.scope(scope);
        }
        if (map.get("screen") instanceof String screen) {
            builder.screen(screen);
        }
        if (map.get("modId") instanceof String modId) {
            builder.modId(modId);
        }
        if (map.get("text") instanceof String text) {
            builder.text(text);
        }
        if (map.get("role") instanceof String role) {
            builder.role(role);
        }
        if (map.get("id") instanceof String id) {
            builder.id(id);
        } else if (map.get("targetId") instanceof String targetId) {
            builder.id(targetId);
        }
        if (map.get("bounds") instanceof Map<?, ?> rawBounds) {
            var bounds = (Map<String, Object>) rawBounds;
            builder.bounds(new Bounds(
                    ((Number) bounds.getOrDefault("x", 0)).intValue(),
                    ((Number) bounds.getOrDefault("y", 0)).intValue(),
                    ((Number) bounds.getOrDefault("width", 0)).intValue(),
                    ((Number) bounds.getOrDefault("height", 0)).intValue()
            ));
        }
        return builder.build();
    }

    private List<TargetSelector> selectorsFrom(Object rawSelector) {
        if (rawSelector instanceof List<?> list) {
            return list.stream().map(this::selectorFrom).toList();
        }
        if (rawSelector == null) {
            return List.of();
        }
        return List.of(selectorFrom(rawSelector));
    }

    private WaitResult waitForCondition(
            UiDriver driver,
            UiContext context,
            TargetSelector selector,
            String condition,
            long timeoutMs,
            long pollIntervalMs,
            long stableForMs
    ) {
        var startedAt = System.nanoTime();
        var baselineSnapshot = driver.snapshot(context, SnapshotOptions.DEFAULT);
        var baselineProbe = new WaitProbe(baselineSnapshot, driver.query(context, selector));
        var lastSignature = snapshotSignature(baselineProbe.snapshot());
        var stableSinceAt = startedAt;
        do {
            var snapshot = driver.snapshot(context, SnapshotOptions.DEFAULT);
            var probe = new WaitProbe(snapshot, driver.query(context, selector));
            var signature = snapshotSignature(snapshot);
            var now = System.nanoTime();
            if (!signature.equals(lastSignature)) {
                lastSignature = signature;
                stableSinceAt = now;
            }
            var matched = conditionSatisfied(condition, baselineProbe, probe, stableSinceAt, now, stableForMs);
            var elapsedMs = elapsedMillis(startedAt, now);
            if (matched) {
                return new WaitResult(true, false, elapsedMs, probe.targets());
            }
            if (elapsedMs >= timeoutMs) {
                return new WaitResult(false, timeoutMs > 0, elapsedMs, probe.targets());
            }
            sleep(Math.min(pollIntervalMs, Math.max(1L, timeoutMs - elapsedMs)));
        } while (true);
    }

    private boolean conditionSatisfied(
            String condition,
            WaitProbe baseline,
            WaitProbe current,
            long stableSinceAt,
            long now,
            long stableForMs
    ) {
        return switch (condition) {
            case "disappeared" -> current.targets().isEmpty();
            case "screen_changed" -> !sameScreen(baseline.snapshot(), current.snapshot());
            case "focus_changed" -> !java.util.Objects.equals(baseline.snapshot().focusedTargetId(), current.snapshot().focusedTargetId());
            case "text_changed" -> !targetTexts(baseline.targets()).equals(targetTexts(current.targets()));
            case "screen_stable" -> elapsedMillis(stableSinceAt, now) >= stableForMs;
            case "appeared" -> !current.targets().isEmpty();
            default -> !current.targets().isEmpty();
        };
    }

    private long longArgument(Map<String, Object> arguments, String key, long defaultValue) {
        var value = arguments.get(key);
        return value instanceof Number number ? Math.max(0L, number.longValue()) : defaultValue;
    }

    private long elapsedMillis(long startedAt) {
        return elapsedMillis(startedAt, System.nanoTime());
    }

    private long elapsedMillis(long startedAt, long finishedAt) {
        return java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(finishedAt - startedAt);
    }

    private void sleep(long durationMs) {
        try {
            Thread.sleep(durationMs);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for UI condition", exception);
        }
    }

    private boolean sameScreen(UiSnapshot left, UiSnapshot right) {
        return java.util.Objects.equals(left.screenId(), right.screenId())
                && java.util.Objects.equals(left.screenClass(), right.screenClass());
    }

    private WaitSnapshotSignature snapshotSignature(UiSnapshot snapshot) {
        return new WaitSnapshotSignature(
                snapshot.screenId(),
                snapshot.screenClass(),
                snapshot.focusedTargetId(),
                snapshot.selectedTargetId(),
                snapshot.hoveredTargetId(),
                snapshot.activeTargetId(),
                snapshot.targets().stream()
                        .map(target -> target.targetId() + "|" + target.state().focused() + "|" + target.state().selected() + "|" + target.state().hovered() + "|" + target.state().active())
                        .toList()
        );
    }

    private List<String> targetTexts(List<UiTarget> targets) {
        return targets.stream()
                .map(target -> target.targetId() + "|" + target.text())
                .toList();
    }

    private Map<String, Object> targetDetails(UiDriver driver, UiContext context, TargetSelector selector) {
        var snapshot = driver.snapshot(context, SnapshotOptions.DEFAULT);
        var target = driver.query(context, selector).stream().findFirst().orElse(null);
        if (target == null) {
            return Map.of(
                    "driverId", driver.descriptor().id(),
                    "target", Map.of(),
                    "captureRegion", Map.of(),
                    "hierarchyPath", List.of(),
                    "interactionState", Map.of(),
                    "actions", List.of(),
                    "overlay", false,
                    "metadata", Map.of(),
                    "extensions", Map.of()
            );
        }
        return Map.of(
                "driverId", driver.descriptor().id(),
                "target", targetToMap(target),
                "captureRegion", targetToMap(target),
                "hierarchyPath", hierarchyPath(snapshot, target),
                "interactionState", targetStateToMap(target),
                "actions", target.actions(),
                "overlay", overlayFlag(target),
                "metadata", targetMetadata(target),
                "extensions", target.extensions()
        );
    }

    private List<String> hierarchyPath(UiSnapshot snapshot, UiTarget target) {
        var path = new java.util.ArrayList<String>();
        snapshot.targets().stream()
                .filter(candidate -> "screen".equals(candidate.role()))
                .map(UiTarget::targetId)
                .filter(candidateId -> !candidateId.equals(target.targetId()))
                .findFirst()
                .ifPresent(path::add);
        path.add(target.targetId());
        return List.copyOf(path);
    }

    private Map<String, Object> targetStateToMap(UiTarget target) {
        return Map.of(
                "visible", target.state().visible(),
                "enabled", target.state().enabled(),
                "focused", target.state().focused(),
                "hovered", target.state().hovered(),
                "selected", target.state().selected(),
                "active", target.state().active()
        );
    }

    private boolean overlayFlag(UiTarget target) {
        return "overlay".equals(target.role()) || Boolean.TRUE.equals(target.extensions().get("overlay"));
    }

    private Map<String, Object> targetMetadata(UiTarget target) {
        var metadata = new java.util.LinkedHashMap<String, Object>();
        if (target.extensions().containsKey("slotIndex")) {
            metadata.put("slotIndex", target.extensions().get("slotIndex"));
        }
        if (target.extensions().containsKey("textInput")) {
            metadata.put("textInput", target.extensions().get("textInput"));
        }
        return Map.copyOf(metadata);
    }

    private List<UiTarget> resolveTargets(UiDriver driver, UiContext context, List<TargetSelector> selectors, boolean defaultToAll) {
        if (selectors == null) {
            return defaultToAll ? driver.snapshot(context, SnapshotOptions.DEFAULT).targets() : List.of();
        }
        if (selectors.isEmpty()) {
            if (!defaultToAll) {
                return List.of();
            }
            return driver.snapshot(context, SnapshotOptions.DEFAULT).targets();
        }
        return selectors.stream()
                .flatMap(selector -> driver.query(context, selector).stream())
                .distinct()
                .toList();
    }

    private int area(UiTarget target) {
        return target.bounds().width() * target.bounds().height();
    }

    private int captureWidth(UiSnapshot snapshot) {
        return snapshot.targets().stream()
                .mapToInt(target -> target.bounds().x() + target.bounds().width())
                .max()
                .orElse(320);
    }

    private int captureHeight(UiSnapshot snapshot) {
        return snapshot.targets().stream()
                .mapToInt(target -> target.bounds().y() + target.bounds().height())
                .max()
                .orElse(240);
    }

    private java.util.Optional<UiCaptureImage> selectCaptureImage(
            String requestedSource,
            UiContext uiContext,
            UiSnapshot snapshot,
            CaptureRequest request,
            List<UiTarget> capturedTargets,
            List<UiTarget> excludedTargets
    ) {
        return switch (requestedSource) {
            case "offscreen" -> registries.uiOffscreenCaptureProviders().select(uiContext, snapshot)
                    .map(provider -> provider.capture(uiContext, snapshot, request, capturedTargets, excludedTargets));
            case "framebuffer" -> registries.uiFramebufferCaptureProviders().select(uiContext, snapshot)
                    .map(provider -> provider.capture(uiContext, snapshot, request, capturedTargets, excludedTargets));
            case "placeholder" -> java.util.Optional.empty();
            case "auto" -> registries.uiOffscreenCaptureProviders().select(uiContext, snapshot)
                    .map(provider -> provider.capture(uiContext, snapshot, request, capturedTargets, excludedTargets))
                    .or(() -> registries.uiFramebufferCaptureProviders().select(uiContext, snapshot)
                            .map(provider -> provider.capture(uiContext, snapshot, request, capturedTargets, excludedTargets)));
            default -> java.util.Optional.empty();
        };
    }

    private String captureUnavailableMessage(String requestedSource, String driverId, String screenClass) {
        return "capture_unavailable: no real UI capture provider matched source=" + requestedSource
                + ", driver=" + driverId
                + ", screenClass=" + screenClass;
    }

    private ToolResult semanticActionResult(UiContext uiContext, UiDriver driver, String action, TargetSelector selector, Map<String, Object> arguments) {
        var missingTarget = missingTargetResult(driver, uiContext, selector);
        if (missingTarget != null) {
            return missingTarget;
        }
        var preSnapshot = driver.snapshot(uiContext, SnapshotOptions.DEFAULT);
        var preSnapshotRef = registries.uiSnapshotJournal().record(uiContext, preSnapshot);
        var actionResult = driver.action(uiContext, new UiActionRequest(selector, action, arguments));
        if (!actionResult.accepted()) {
            return operationRejected("action", driver, actionResult);
        }
        var result = actionResult.value();
        var postSnapshot = driver.snapshot(uiContext, SnapshotOptions.DEFAULT);
        var postSnapshotRef = registries.uiSnapshotJournal().record(uiContext, postSnapshot);
        return ToolResult.success(withOptionalWait(
                uiContext,
                driver,
                arguments,
                selector,
                withActionSnapshots(result, preSnapshotRef, postSnapshotRef, postSnapshot)
        ));
    }

    private ToolResult runIntentResult(UiContext uiContext, UiDriver driver, String intent, Map<String, Object> arguments) {
        var preSnapshot = driver.snapshot(uiContext, SnapshotOptions.DEFAULT);
        var preSnapshotRef = registries.uiSnapshotJournal().record(uiContext, preSnapshot);
        var delegatedArguments = new java.util.LinkedHashMap<String, Object>();
        delegatedArguments.putAll(arguments);
        delegatedArguments.put("intent", intent);
        var dispatchResult = inputActionDispatcher.dispatch("ui_intent", Map.copyOf(delegatedArguments));
        if (!dispatchResult.success()) {
            return ToolResult.failure(dispatchResult.error());
        }
        var postSnapshot = driver.snapshot(uiContext, SnapshotOptions.DEFAULT);
        var postSnapshotRef = registries.uiSnapshotJournal().record(uiContext, postSnapshot);
        var payload = new java.util.LinkedHashMap<String, Object>();
        payload.put("driverId", driver.descriptor().id());
        payload.put("action", "run_intent");
        payload.put("intent", intent);
        payload.put("performed", dispatchResult.performed());
        payload.put("preSnapshotRef", preSnapshotRef);
        payload.put("postSnapshotRef", postSnapshotRef);
        payload.put("postActionSnapshot", snapshotToMap(postSnapshot));
        return ToolResult.success(withOptionalWait(
                uiContext,
                driver,
                arguments,
                TargetSelector.builder().build(),
                Map.copyOf(payload)
        ));
    }

    private Map<String, Object> withOptionalWait(
            UiContext context,
            UiDriver driver,
            Map<String, Object> arguments,
            TargetSelector defaultSelector,
            Map<String, Object> payload
    ) {
        if (!(arguments.get("waitCondition") instanceof String waitCondition) || waitCondition.isBlank()) {
            return payload;
        }
        var waitSelector = arguments.containsKey("waitTarget")
                ? selectorFrom(arguments.get("waitTarget"))
                : defaultSelector;
        var waitTimeoutMs = longArgument(arguments, "waitTimeoutMs", 0L);
        var waitPollIntervalMs = Math.max(1L, longArgument(arguments, "waitPollIntervalMs", 50L));
        var waitStableForMs = longArgument(arguments, "waitStableForMs", 100L);
        var waitResult = waitForCondition(driver, context, waitSelector, waitCondition, waitTimeoutMs, waitPollIntervalMs, waitStableForMs);
        var result = new java.util.LinkedHashMap<String, Object>();
        result.putAll(payload);
        result.put("wait", Map.of(
                "condition", waitCondition,
                "matched", waitResult.matched(),
                "timedOut", waitResult.timedOut(),
                "elapsedMs", waitResult.elapsedMs(),
                "targets", waitResult.targets().stream().map(this::targetToMap).toList()
        ));
        return Map.copyOf(result);
    }

    private Map<String, Object> withSnapshotRef(Map<String, Object> payload, String snapshotRef) {
        var result = new java.util.LinkedHashMap<String, Object>();
        result.putAll(payload);
        result.put("snapshotRef", snapshotRef);
        return Map.copyOf(result);
    }

    private Map<String, Object> sessionPayload(UiAutomationSession session) {
        return sessionPayload(session, false);
    }

    private Map<String, Object> sessionPayload(UiAutomationSession session, boolean screenChanged) {
        return Map.of(
                "sessionId", session.sessionId(),
                "screenId", session.snapshot().screenId(),
                "screenClass", session.snapshot().screenClass(),
                "driverId", session.snapshot().driverId(),
                "screenChanged", screenChanged,
                "refs", session.refs().stream().map(ref -> {
                    var target = registries.uiAutomationSessions()
                            .resolveTarget(session.sessionId(), ref.refId())
                            .value();
                    return Map.of(
                            "refId", ref.refId(),
                            "driverId", ref.driverId(),
                            "targetId", ref.targetId(),
                            "screenId", ref.screenId(),
                            "role", target == null ? "" : target.role(),
                            "text", target == null || target.text() == null ? "" : target.text()
                    );
                }).toList()
        );
    }

    private Map<String, Object> withActionSnapshots(
            Map<String, Object> payload,
            String preSnapshotRef,
            String postSnapshotRef,
            UiSnapshot postSnapshot
    ) {
        var result = new java.util.LinkedHashMap<String, Object>();
        result.putAll(payload);
        result.put("preSnapshotRef", preSnapshotRef);
        result.put("postSnapshotRef", postSnapshotRef);
        result.put("postActionSnapshot", snapshotToMap(postSnapshot));
        return Map.copyOf(result);
    }

    private Map<String, Object> snapshotToMap(UiSnapshot snapshot) {
        var result = new LinkedHashMap<String, Object>();
        result.put("screenId", blankIfNull(snapshot.screenId()));
        result.put("screenClass", blankIfNull(snapshot.screenClass()));
        result.put("driverId", blankIfNull(snapshot.driverId()));
        result.put("targets", snapshot.targets().stream().map(this::targetToMap).toList());
        result.put("focusedTargetId", snapshot.focusedTargetId() == null ? "" : snapshot.focusedTargetId());
        var drivers = snapshot.extensions().get("drivers");
        result.put("drivers", drivers instanceof List<?> list ? list : List.of());
        return Map.copyOf(result);
    }

    private Map<String, Object> inspectResultToMap(UiInspectResult inspectResult) {
        var result = new java.util.LinkedHashMap<String, Object>();
        result.put("screen", blankIfNull(inspectResult.screen()));
        result.put("screenId", blankIfNull(inspectResult.screenId()));
        result.put("driverId", blankIfNull(inspectResult.driverId()));
        result.put("summary", inspectResult.summary());
        result.put("targets", inspectResult.targets().stream().map(this::targetToMap).toList());
        result.put("interaction", inspectResult.interaction());
        if (inspectResult.tooltip() != null) {
            result.put("tooltip", Map.of(
                    "targetId", inspectResult.tooltip().targetId(),
                    "lines", inspectResult.tooltip().lines(),
                    "bounds", boundsToMap(inspectResult.tooltip().bounds())
            ));
        }
        return Map.copyOf(result);
    }

    private Map<String, Object> conciseScreenshotPayload(Map<String, Object> payload, UiTarget resolvedTarget) {
        var result = new java.util.LinkedHashMap<String, Object>();
        result.put("driverId", payload.get("driverId"));
        result.put("mode", payload.get("mode"));
        if (resolvedTarget != null) {
            result.put("resolvedTarget", targetToMap(resolvedTarget));
        }
        result.put("snapshotRef", payload.get("snapshotRef"));
        result.put("imageRef", payload.get("imageRef"));
        result.put("imagePath", payload.get("imagePath"));
        result.put("imageResourceUri", payload.get("imageResourceUri"));
        result.put("imageMeta", payload.get("imageMeta"));
        result.put("needsReinspect", true);
        return Map.copyOf(result);
    }

    private Map<String, Object> tracePayload(String sessionId, List<UiAutomationTraceEntry> trace) {
        return Map.of(
                "sessionId", sessionId,
                "traces", trace.stream().map(this::traceEntryToMap).toList()
        );
    }

    private Map<String, Object> traceEntryToMap(UiAutomationTraceEntry entry) {
        return Map.of(
                "stepIndex", entry.stepIndex(),
                "type", entry.type(),
                "elapsedMs", entry.elapsedMs(),
                "success", entry.success(),
                "errorCode", entry.errorCode(),
                "errorMessage", entry.errorMessage()
        );
    }

    private Map<String, Object> waitResultToMap(UiDriver driver, String condition, dev.vfyjxf.mcp.api.runtime.UiWaitResult waitResult) {
        var result = new java.util.LinkedHashMap<String, Object>();
        result.put("driverId", driver.descriptor().id());
        result.put("condition", condition);
        result.put("matched", waitResult.matched());
        result.put("timedOut", "timeout".equals(waitResult.errorCode()));
        result.put("elapsedMs", waitResult.elapsedMs());
        result.put("targets", waitResult.matchedTarget() == null ? List.of() : List.of(targetToMap(waitResult.matchedTarget())));
        if (waitResult.matchedTarget() != null) {
            result.put("matchedTarget", targetToMap(waitResult.matchedTarget()));
        }
        if (waitResult.errorCode() != null && !waitResult.errorCode().isBlank()) {
            result.put("errorCode", waitResult.errorCode());
        }
        return Map.copyOf(result);
    }

    private Map<String, Object> interactionStateToMap(UiInteractionState state) {
        return Map.of(
                "driverId", state.driverId(),
                "focusedTarget", state.focusedTarget() == null ? Map.of() : targetToMap(state.focusedTarget()),
                "selectedTarget", state.selectedTarget() == null ? Map.of() : targetToMap(state.selectedTarget()),
                "hoveredTarget", state.hoveredTarget() == null ? Map.of() : targetToMap(state.hoveredTarget()),
                "activeTarget", state.activeTarget() == null ? Map.of() : targetToMap(state.activeTarget()),
                "cursorX", state.cursorX(),
                "cursorY", state.cursorY(),
                "textInputActive", state.textInputActive(),
                "selectionSource", state.selectionSource()
        );
    }

    /**
     * Expose the real live-client state explicitly so in-world automation can stay inspectable
     * without inventing a fake screen class.
     */
    private Map<String, Object> withLiveScreenState(
            Map<String, Object> payload,
            ClientScreenMetrics metrics,
            List<Map<String, Object>> drivers,
            String driverId
    ) {
        var result = new java.util.LinkedHashMap<String, Object>();
        result.putAll(payload);
        result.put("screenClass", hasScreen(metrics) ? metrics.screenClass() : "");
        result.put("modId", "minecraft");
        result.put("screenAvailable", hasScreen(metrics));
        result.put("inWorld", !hasScreen(metrics));
        result.put("driverId", blankIfNull(driverId));
        result.put("drivers", drivers);
        result.put("guiWidth", metrics.guiWidth());
        result.put("guiHeight", metrics.guiHeight());
        result.put("framebufferWidth", metrics.framebufferWidth());
        result.put("framebufferHeight", metrics.framebufferHeight());
        return Map.copyOf(result);
    }

    private Map<String, Object> withUiContextState(Map<String, Object> payload, UiContext context) {
        var result = new java.util.LinkedHashMap<String, Object>();
        result.putAll(payload);
        result.put("screenClass", blankIfNull(context.screenClass()));
        result.put("modId", blankIfNull(context.modId()));
        result.put("screenAvailable", context.screenClass() != null && !context.screenClass().isBlank());
        result.put("inWorld", context.screenClass() == null || context.screenClass().isBlank());
        return Map.copyOf(result);
    }

    private ClientScreenMetrics safeLiveMetrics() {
        var metricsResult = liveScreenMetrics();
        return metricsResult.accepted() ? metricsResult.value() : new ClientScreenMetrics(null, 0, 0, 0, 0);
    }

    private boolean hasScreen(ClientScreenMetrics metrics) {
        return metrics.screenClass() != null && !metrics.screenClass().isBlank();
    }

    private String blankIfNull(String value) {
        return value == null ? "" : value;
    }

    private Map<String, Object> liveScreenToMap(ClientScreenMetrics metrics, Map<String, Object> arguments) {
        var composition = filteredComposition(new MapBackedUiContext(
                metrics.screenClass(),
                "minecraft",
                0,
                0,
                liveScreenHandle(metrics)
        ), arguments);
        return withLiveScreenState(
                Map.of("active", hasScreen(metrics)),
                metrics,
                driverDetails(composition.drivers()),
                composition.defaultDriverId()
        );
    }

    private Map<String, Object> targetToMap(UiTarget target) {
        return Map.of(
                "targetId", target.targetId(),
                "driverId", target.driverId(),
                "screenClass", blankIfNull(target.screenClass()),
                "modId", blankIfNull(target.modId()),
                "role", target.role(),
                "text", target.text() == null ? "" : target.text(),
                "bounds", Map.of(
                        "x", target.bounds().x(),
                        "y", target.bounds().y(),
                        "width", target.bounds().width(),
                        "height", target.bounds().height()
                ),
                "state", targetStateToMap(target),
                "actions", target.actions(),
                "extensions", target.extensions()
        );
    }

    private Map<String, Object> boundsToMap(Bounds bounds) {
        return Map.of(
                "x", bounds.x(),
                "y", bounds.y(),
                "width", bounds.width(),
                "height", bounds.height()
        );
    }

    private record MapBackedUiContext(
            String screenClass,
            String modId,
            int mouseX,
            int mouseY,
            Object screenHandle
    ) implements UiContext {
    }

    private record WaitResult(
            boolean matched,
            boolean timedOut,
            long elapsedMs,
            List<UiTarget> targets
    ) {
    }

    private record WaitProbe(
            UiSnapshot snapshot,
            List<UiTarget> targets
    ) {
    }

    private Map<String, Object> withInspectTopmost(String driverId, List<UiTarget> matches, UiTarget topmost) {
        var result = new java.util.LinkedHashMap<String, Object>();
        result.put("driverId", driverId);
        result.put("targets", matches.stream().map(this::targetToMap).toList());
        result.put("topmostTarget", topmost == null ? Map.of() : targetToMap(topmost));
        result.put("modId", topmost == null ? "" : topmost.modId());
        result.put("role", topmost == null ? "" : topmost.role());
        result.put("text", topmost == null ? "" : (topmost.text() == null ? "" : topmost.text()));
        result.put("bounds", topmost == null ? Map.of() : Map.of(
                "x", topmost.bounds().x(),
                "y", topmost.bounds().y(),
                "width", topmost.bounds().width(),
                "height", topmost.bounds().height()
        ));
        result.put("extensions", topmost == null ? Map.of() : topmost.extensions());
        return Map.copyOf(result);
    }

    private Map<String, Object> withTooltipDetails(String driverId, dev.vfyjxf.mcp.api.ui.TooltipSnapshot tooltip, UiTarget target) {
        var result = new java.util.LinkedHashMap<String, Object>();
        result.put("driverId", driverId);
        result.put("targetId", tooltip.targetId());
        result.put("lines", tooltip.lines());
        result.put("bounds", Map.of(
                "x", tooltip.bounds().x(),
                "y", tooltip.bounds().y(),
                "width", tooltip.bounds().width(),
                "height", tooltip.bounds().height()
        ));
        result.put("modId", target == null ? "" : target.modId());
        result.put("role", target == null ? "" : target.role());
        result.put("text", target == null ? "" : (target.text() == null ? "" : target.text()));
        result.put("extensions", target == null ? Map.of() : target.extensions());
        return Map.copyOf(result);
    }

    private record WaitSnapshotSignature(
            String screenId,
            String screenClass,
            String focusedTargetId,
            String selectedTargetId,
            String hoveredTargetId,
            String activeTargetId,
            List<String> targetStates
    ) {
    }
}
