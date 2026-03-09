package dev.vfyjxf.mcp.runtime.tool;

import dev.vfyjxf.mcp.runtime.RuntimeRegistries;
import dev.vfyjxf.mcp.api.runtime.UiCaptureImage;
import dev.vfyjxf.mcp.api.runtime.UiContext;
import dev.vfyjxf.mcp.api.runtime.UiDriver;
import dev.vfyjxf.mcp.api.ui.Bounds;
import dev.vfyjxf.mcp.api.ui.CaptureRequest;
import dev.vfyjxf.mcp.api.ui.SnapshotOptions;
import dev.vfyjxf.mcp.api.ui.TargetSelector;
import dev.vfyjxf.mcp.api.ui.UiActionRequest;
import dev.vfyjxf.mcp.api.ui.UiInteractionState;
import dev.vfyjxf.mcp.api.ui.UiSnapshot;
import dev.vfyjxf.mcp.api.ui.UiTarget;
import dev.vfyjxf.mcp.server.api.McpToolDefinition;
import dev.vfyjxf.mcp.server.api.McpToolProvider;
import dev.vfyjxf.mcp.server.api.ToolResult;
import dev.vfyjxf.mcp.server.runtime.McpToolRegistry;

import java.util.List;
import java.util.Map;

public final class UiToolProvider implements McpToolProvider {

    private final RuntimeRegistries registries;

    public UiToolProvider(RuntimeRegistries registries) {
        this.registries = registries;
    }

    @Override
    public void register(McpToolRegistry registry) {
        registry.registerTool(definition("moddev.ui_snapshot"), (context, arguments) -> {
            var uiContext = uiContext(arguments);
            var driver = selectDriver(uiContext);
            var snapshot = driver.snapshot(uiContext, SnapshotOptions.DEFAULT);
            var snapshotRef = registries.uiSnapshotJournal().record(uiContext, snapshot);
            return ToolResult.success(withSnapshotRef(snapshotToMap(snapshot), snapshotRef));
        });
        registry.registerTool(definition("moddev.ui_query"), (context, arguments) -> {
            var uiContext = uiContext(arguments);
            var driver = selectDriver(uiContext);
            var selector = selectorFrom(arguments.get("selector"));
            return ToolResult.success(Map.of(
                    "driverId", driver.descriptor().id(),
                    "targets", driver.query(uiContext, selector).stream().map(this::targetToMap).toList()
            ));
        });
        registry.registerTool(definition("moddev.ui_capture"), (context, arguments) -> {
            var uiContext = uiContext(arguments);
            var driver = selectDriver(uiContext);
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
            driver.capture(uiContext, request);
            var snapshot = driver.snapshot(uiContext, SnapshotOptions.DEFAULT);
            var snapshotRef = registries.uiSnapshotJournal().record(uiContext, snapshot);
            var captured = resolveTargets(driver, uiContext, targets, true);
            var excluded = resolveTargets(driver, uiContext, exclude, false);
            var filtered = captured.stream()
                    .filter(target -> excluded.stream().noneMatch(excludedTarget -> excludedTarget.targetId().equals(target.targetId())))
                    .toList();
            var captureImage = selectCaptureImage(
                    (String) arguments.getOrDefault("source", "auto"),
                    uiContext,
                    snapshot,
                    request,
                    filtered,
                    excluded
            );
            var artifact = registries.uiCaptureArtifactStore().store(
                    driver.descriptor().id(),
                    captureImage.pngBytes(),
                    captureImage.width(),
                    captureImage.height(),
                    Map.of(
                            "source", captureImage.source(),
                            "providerId", captureImage.providerId()
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
        });
        registry.registerTool(definition("moddev.ui_action"), (context, arguments) -> {
            var uiContext = uiContext(arguments);
            var driver = selectDriver(uiContext);
            var preSnapshot = driver.snapshot(uiContext, SnapshotOptions.DEFAULT);
            var preSnapshotRef = registries.uiSnapshotJournal().record(uiContext, preSnapshot);
            var request = new UiActionRequest(
                    selectorFrom(arguments.get("target")),
                    (String) arguments.getOrDefault("action", "click"),
                    arguments
            );
            var result = driver.action(uiContext, request).value();
            var postSnapshot = driver.snapshot(uiContext, SnapshotOptions.DEFAULT);
            var postSnapshotRef = registries.uiSnapshotJournal().record(uiContext, postSnapshot);
            return ToolResult.success(withActionSnapshots(result, preSnapshotRef, postSnapshotRef, postSnapshot));
        });
        registry.registerTool(definition("moddev.ui_wait"), (context, arguments) -> {
            var uiContext = uiContext(arguments);
            var driver = selectDriver(uiContext);
            var selector = selectorFrom(arguments.get("selector"));
            var matches = driver.query(uiContext, selector);
            return ToolResult.success(Map.of(
                    "driverId", driver.descriptor().id(),
                    "matched", !matches.isEmpty(),
                    "targets", matches.stream().map(this::targetToMap).toList()
            ));
        });
        registry.registerTool(definition("moddev.ui_inspect_at"), (context, arguments) -> {
            var uiContext = uiContext(arguments);
            var driver = selectDriver(uiContext);
            var matches = driver.inspectAt(
                    uiContext,
                    ((Number) arguments.getOrDefault("x", 0)).intValue(),
                    ((Number) arguments.getOrDefault("y", 0)).intValue()
            ).value();
            var topmost = matches.stream()
                    .min((left, right) -> Integer.compare(area(left), area(right)))
                    .orElse(null);
            return ToolResult.success(Map.of(
                    "driverId", driver.descriptor().id(),
                    "targets", matches.stream().map(this::targetToMap).toList(),
                    "topmostTarget", topmost == null ? Map.of() : targetToMap(topmost)
            ));
        });
        registry.registerTool(definition("moddev.ui_get_tooltip"), (context, arguments) -> {
            var uiContext = uiContext(arguments);
            var driver = selectDriver(uiContext);
            var selector = selectorFrom(arguments.get("target"));
            var tooltip = driver.tooltip(uiContext, selector).value();
            return ToolResult.success(Map.of(
                    "driverId", driver.descriptor().id(),
                    "targetId", tooltip.targetId(),
                    "lines", tooltip.lines(),
                    "bounds", Map.of(
                            "x", tooltip.bounds().x(),
                            "y", tooltip.bounds().y(),
                            "width", tooltip.bounds().width(),
                            "height", tooltip.bounds().height()
                    )
            ));
        });
        registry.registerTool(definition("moddev.ui_get_interaction_state"), (context, arguments) -> {
            var uiContext = uiContext(arguments);
            var driver = selectDriver(uiContext);
            return ToolResult.success(interactionStateToMap(driver.interactionState(uiContext)));
        });
        registry.registerTool(definition("moddev.ui_get_target_details"), (context, arguments) -> {
            var uiContext = uiContext(arguments);
            var driver = selectDriver(uiContext);
            var selector = selectorFrom(arguments.get("target"));
            var matches = driver.query(uiContext, selector);
            var target = matches.isEmpty() ? null : matches.getFirst();
            return ToolResult.success(Map.of(
                    "driverId", driver.descriptor().id(),
                    "target", target == null ? Map.of() : targetToMap(target),
                    "captureRegion", target == null ? Map.of() : targetToMap(target),
                    "actions", target == null ? List.of() : target.actions()
            ));
        });
        registry.registerTool(definition("moddev.ui_open"), (context, arguments) -> {
            var uiContext = uiContext(arguments);
            var driver = selectDriver(uiContext);
            return semanticActionResult(uiContext, driver, "open", TargetSelector.builder().build(), arguments);
        });
        registry.registerTool(definition("moddev.ui_close"), (context, arguments) -> {
            var uiContext = uiContext(arguments);
            var driver = selectDriver(uiContext);
            return semanticActionResult(uiContext, driver, "close", TargetSelector.builder().build(), arguments);
        });
        registry.registerTool(definition("moddev.ui_switch"), (context, arguments) -> {
            var uiContext = uiContext(arguments);
            var driver = selectDriver(uiContext);
            return semanticActionResult(uiContext, driver, "switch", selectorFrom(arguments.get("target")), arguments);
        });
    }

    private McpToolDefinition definition(String name) {
        return new McpToolDefinition(name, name, "Built-in UI tool", Map.of(), Map.of(), List.of("ui"), "either", false, false, "public", "public");
    }

    private UiDriver selectDriver(UiContext context) {
        return registries.uiDrivers().select(context).orElseThrow();
    }

    private UiContext uiContext(Map<String, Object> arguments) {
        return new MapBackedUiContext(
                (String) arguments.getOrDefault("screenClass", "custom.UnknownScreen"),
                (String) arguments.getOrDefault("modId", "minecraft"),
                ((Number) arguments.getOrDefault("mouseX", 0)).intValue(),
                ((Number) arguments.getOrDefault("mouseY", 0)).intValue()
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

    private UiCaptureImage selectCaptureImage(
            String requestedSource,
            UiContext uiContext,
            UiSnapshot snapshot,
            CaptureRequest request,
            List<UiTarget> capturedTargets,
            List<UiTarget> excludedTargets
    ) {
        return switch (requestedSource) {
            case "offscreen" -> registries.uiOffscreenCaptureProviders().select(uiContext, snapshot)
                    .map(provider -> provider.capture(uiContext, snapshot, request, capturedTargets, excludedTargets))
                    .orElseGet(() -> placeholderCapture(snapshot, capturedTargets, excludedTargets));
            case "framebuffer" -> registries.uiFramebufferCaptureProviders().select(uiContext, snapshot)
                    .map(provider -> provider.capture(uiContext, snapshot, request, capturedTargets, excludedTargets))
                    .orElseGet(() -> placeholderCapture(snapshot, capturedTargets, excludedTargets));
            case "placeholder" -> placeholderCapture(snapshot, capturedTargets, excludedTargets);
            case "auto" -> registries.uiOffscreenCaptureProviders().select(uiContext, snapshot)
                    .map(provider -> provider.capture(uiContext, snapshot, request, capturedTargets, excludedTargets))
                    .or(() -> registries.uiFramebufferCaptureProviders().select(uiContext, snapshot)
                            .map(provider -> provider.capture(uiContext, snapshot, request, capturedTargets, excludedTargets)))
                    .orElseGet(() -> placeholderCapture(snapshot, capturedTargets, excludedTargets));
            default -> placeholderCapture(snapshot, capturedTargets, excludedTargets);
        };
    }

    private UiCaptureImage placeholderCapture(UiSnapshot snapshot, List<UiTarget> capturedTargets, List<UiTarget> excludedTargets) {
        return new UiCaptureImage(
                "placeholder",
                "placeholder",
                registries.uiCaptureRenderer().render(snapshot, capturedTargets, excludedTargets),
                captureWidth(snapshot),
                captureHeight(snapshot),
                Map.of()
        );
    }

    private ToolResult semanticActionResult(UiContext uiContext, UiDriver driver, String action, TargetSelector selector, Map<String, Object> arguments) {
        var preSnapshot = driver.snapshot(uiContext, SnapshotOptions.DEFAULT);
        var preSnapshotRef = registries.uiSnapshotJournal().record(uiContext, preSnapshot);
        var result = driver.action(uiContext, new UiActionRequest(selector, action, arguments)).value();
        var postSnapshot = driver.snapshot(uiContext, SnapshotOptions.DEFAULT);
        var postSnapshotRef = registries.uiSnapshotJournal().record(uiContext, postSnapshot);
        return ToolResult.success(withActionSnapshots(result, preSnapshotRef, postSnapshotRef, postSnapshot));
    }

    private Map<String, Object> withSnapshotRef(Map<String, Object> payload, String snapshotRef) {
        var result = new java.util.LinkedHashMap<String, Object>();
        result.putAll(payload);
        result.put("snapshotRef", snapshotRef);
        return Map.copyOf(result);
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
        return Map.of(
                "screenId", snapshot.screenId(),
                "screenClass", snapshot.screenClass(),
                "driverId", snapshot.driverId(),
                "targets", snapshot.targets().stream().map(this::targetToMap).toList(),
                "focusedTargetId", snapshot.focusedTargetId() == null ? "" : snapshot.focusedTargetId()
        );
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

    private Map<String, Object> targetToMap(UiTarget target) {
        return Map.of(
                "targetId", target.targetId(),
                "driverId", target.driverId(),
                "screenClass", target.screenClass(),
                "modId", target.modId(),
                "role", target.role(),
                "text", target.text() == null ? "" : target.text(),
                "bounds", Map.of(
                        "x", target.bounds().x(),
                        "y", target.bounds().y(),
                        "width", target.bounds().width(),
                        "height", target.bounds().height()
                ),
                "actions", target.actions(),
                "extensions", target.extensions()
        );
    }

    private record MapBackedUiContext(
            String screenClass,
            String modId,
            int mouseX,
            int mouseY
    ) implements UiContext {
    }
}
