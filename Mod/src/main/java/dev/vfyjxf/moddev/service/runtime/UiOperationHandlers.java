package dev.vfyjxf.moddev.service.runtime;

import dev.vfyjxf.moddev.api.model.OperationResult;
import dev.vfyjxf.moddev.api.runtime.ClientScreenMetrics;
import dev.vfyjxf.moddev.api.runtime.UiCaptureImage;
import dev.vfyjxf.moddev.api.ui.CaptureRequest;
import dev.vfyjxf.moddev.api.ui.SnapshotOptions;
import dev.vfyjxf.moddev.api.runtime.UiContext;
import dev.vfyjxf.moddev.api.runtime.UiDriver;
import dev.vfyjxf.moddev.api.runtime.UiInspectResult;
import dev.vfyjxf.moddev.api.ui.Bounds;
import dev.vfyjxf.moddev.api.ui.TargetSelector;
import dev.vfyjxf.moddev.api.ui.UiActionRequest;
import dev.vfyjxf.moddev.api.ui.UiSnapshot;
import dev.vfyjxf.moddev.api.ui.UiTarget;
import dev.vfyjxf.moddev.api.ui.UiTargetState;
import dev.vfyjxf.moddev.runtime.RuntimeRegistries;
import dev.vfyjxf.moddev.runtime.ui.UiCaptureArtifact;
import dev.vfyjxf.moddev.runtime.ui.UiCapturePostProcessor;
import dev.vfyjxf.moddev.service.operation.OperationDefinition;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class UiOperationHandlers {

    private UiOperationHandlers() {
    }

    static List<RuntimeOperationBindings.OperationBinding> operations(RuntimeRegistries registries) {
        Objects.requireNonNull(registries, "registries");
        return List.of(
                RuntimeOperationBindings.binding(
                        new OperationDefinition(
                                "ui.inspect",
                                "ui",
                                "Inspect UI",
                                "Inspects the active screen and returns structured UI targets.",
                                true,
                                Set.of("client"),
                                RuntimeOperationBindings.objectSchema(Map.of(), List.of()),
                                Map.of("operationId", "ui.inspect", "targetSide", "client", "input", Map.of())
                        ),
                        (input, resolvedTargetSide) -> {
                            var context = uiContext(registries, input);
                            var driver = selectDriver(registries, context);
                            return inspectResultToMap(driver.inspect(context, SnapshotOptions.DEFAULT));
                        }
                ),
                RuntimeOperationBindings.binding(
                        new OperationDefinition(
                                "ui.snapshot",
                                "ui",
                                "Snapshot UI",
                                "Captures the current UI snapshot and snapshot reference.",
                                true,
                                Set.of("client"),
                                RuntimeOperationBindings.objectSchema(Map.of(), List.of()),
                                Map.of("operationId", "ui.snapshot", "targetSide", "client", "input", Map.of())
                        ),
                        (input, resolvedTargetSide) -> {
                            var context = uiContext(registries, input);
                            var driver = selectDriver(registries, context);
                            var snapshot = driver.snapshot(context, SnapshotOptions.DEFAULT);
                            var snapshotRef = registries.uiSnapshotJournal().record(context, snapshot);
                            var payload = new LinkedHashMap<String, Object>(snapshotToMap(snapshot));
                            payload.put("snapshotRef", snapshotRef);
                            return Map.copyOf(payload);
                        }
                ),
                RuntimeOperationBindings.binding(
                        new OperationDefinition(
                                "ui.capture",
                                "ui",
                                "Capture UI",
                                "Captures a UI rendering export and stores a capture artifact.",
                                true,
                                Set.of("client"),
                                RuntimeOperationBindings.objectSchema(
                                        Map.of(
                                                "source", Map.of("type", "string"),
                                                "mode", Map.of("type", "string"),
                                                "target", Map.of("type", "array"),
                                                "exclude", Map.of("type", "array"),
                                                "withOverlays", Map.of("type", "boolean"),
                                                "screenClass", Map.of("type", "string"),
                                                "modId", Map.of("type", "string"),
                                                "mouseX", Map.of("type", "number"),
                                                "mouseY", Map.of("type", "number")
                                        ),
                                        List.of()
                                ),
                                Map.of(
                                        "operationId", "ui.capture",
                                        "targetSide", "client",
                                        "input", Map.of(
                                                "source", "auto",
                                                "mode", "full"
                                        )
                                )
                        ),
                        (input, resolvedTargetSide) -> {
                            var context = uiContext(registries, input);
                            var driver = selectDriver(registries, context);
                            var snapshot = driver.snapshot(context, SnapshotOptions.DEFAULT);
                            var targetSelectors = selectorListFrom(input.get("target"));
                            var excludeSelectors = selectorListFrom(input.get("exclude"));
                            var capturedTargets = resolveTargets(driver, context, snapshot, targetSelectors, true);
                            var excludedTargets = resolveTargets(driver, context, snapshot, excludeSelectors, false);
                            var filteredTargets = capturedTargets.stream()
                                    .filter(target -> excludedTargets.stream().noneMatch(excluded -> excluded.targetId().equals(target.targetId())))
                                    .toList();
                            var source = normalizeCaptureSource(stringArg(input.get("source")));
                            var mode = normalizeCaptureMode(stringArg(input.get("mode")));
                            var withOverlays = !(input.get("withOverlays") instanceof Boolean enabled) || enabled;
                            var request = new CaptureRequest(mode, targetSelectors, excludeSelectors, withOverlays);
                            var captured = captureImage(registries, driver, context, snapshot, request, filteredTargets, excludedTargets, source);
                            var processed = UiCapturePostProcessor.process(captured, request, filteredTargets, excludedTargets);
                            var snapshotRef = registries.uiSnapshotJournal().record(context, snapshot);
                            var artifact = storeCapture(registries, driver, processed);
                            var payload = new LinkedHashMap<String, Object>();
                            payload.put("driverId", driver.descriptor().id());
                            payload.put("source", processed.source());
                            payload.put("mode", mode);
                            payload.put("snapshotRef", snapshotRef);
                            payload.put("captureRef", artifact.imageRef());
                            payload.put("path", artifact.path());
                            payload.put("resourceUri", artifact.resourceUri());
                            payload.put("mimeType", artifact.mimeType());
                            payload.put("metadata", artifact.metadata());
                            payload.put("capturedTargets", filteredTargets.stream().map(UiOperationHandlers::targetToMap).toList());
                            payload.put("excludedTargets", excludedTargets.stream().map(UiOperationHandlers::targetToMap).toList());
                            return Map.copyOf(payload);
                        }
                ),
                RuntimeOperationBindings.binding(
                        new OperationDefinition(
                                "ui.action",
                                "ui",
                                "Run UI Action",
                                "Performs a UI action against a selected target.",
                                true,
                                Set.of("client"),
                                RuntimeOperationBindings.objectSchema(
                                        Map.of(
                                                "action", Map.of("type", "string"),
                                                "target", Map.of("type", "object")
                                        ),
                                        List.of("action")
                                ),
                                Map.of(
                                        "operationId", "ui.action",
                                        "targetSide", "client",
                                        "input", Map.of("action", "click")
                                )
                        ),
                        (input, resolvedTargetSide) -> {
                            var action = stringArg(input.get("action"));
                            if (action == null || action.isBlank()) {
                                throw RuntimeOperationBindings.executionFailure("invalid_input: missing action");
                            }
                            var context = uiContext(registries, input);
                            var driver = selectDriver(registries, context);
                            var selector = selectorFrom(input.get("target"));
                            UiTarget resolvedTarget = null;
                            if (selectorSpecified(selector)) {
                                var matches = driver.query(context, selector);
                                if (matches.isEmpty()) {
                                    throw RuntimeOperationBindings.executionFailure("target_not_found: selector did not match any target");
                                }
                                if (matches.size() > 1) {
                                    throw RuntimeOperationBindings.executionFailure("target_ambiguous");
                                }
                                resolvedTarget = matches.getFirst();
                            }

                            var preSnapshot = driver.snapshot(context, SnapshotOptions.DEFAULT);
                            var preSnapshotRef = registries.uiSnapshotJournal().record(context, preSnapshot);
                            var actionResult = driver.action(context, new UiActionRequest(selector, action, input));
                            if (!actionResult.accepted()) {
                                throw RuntimeOperationBindings.executionFailure(RuntimeOperationBindings.normalizeMessage(actionResult.reason(), "unsupported_action"));
                            }
                            var postSnapshot = driver.snapshot(context, SnapshotOptions.DEFAULT);
                            var postSnapshotRef = registries.uiSnapshotJournal().record(context, postSnapshot);

                            var payload = new LinkedHashMap<String, Object>();
                            payload.put("driverId", driver.descriptor().id());
                            payload.put("action", action);
                            payload.put("performed", actionResult.performed());
                            payload.put("preSnapshotRef", preSnapshotRef);
                            payload.put("postSnapshotRef", postSnapshotRef);
                            payload.put("postActionSnapshot", snapshotToMap(postSnapshot));
                            payload.put("needsReinspect", true);
                            if (resolvedTarget != null) {
                                payload.put("resolvedTarget", targetToMap(resolvedTarget));
                            }
                            if (actionResult.value() != null && !actionResult.value().isEmpty()) {
                                payload.put("result", actionResult.value());
                            }
                            return Map.copyOf(payload);
                        }
                )
        );
    }

    private static UiDriver selectDriver(RuntimeRegistries registries, UiContext context) {
        return registries.uiDrivers().select(context)
                .orElseThrow(() -> RuntimeOperationBindings.executionFailure(
                        "unsupported: no ui driver matched screenClass=" + context.screenClass() + ", modId=" + context.modId()
                ));
    }

    private static UiContext uiContext(RuntimeRegistries registries, Map<String, Object> input) {
        var screenClass = stringArg(input.get("screenClass"));
        var modId = stringArg(input.get("modId"));
        ClientScreenMetrics metrics = null;
        Object screenHandle = null;
        if (screenClass == null || screenClass.isBlank()) {
            metrics = registries.screenProbe("client").map(probe -> probe.metrics()).orElse(null);
        }
        if ((screenClass == null || screenClass.isBlank()) && metrics != null) {
            screenClass = metrics.screenClass();
            screenHandle = liveScreenHandle(metrics);
        }
        if (screenClass == null || screenClass.isBlank()) {
            screenClass = "custom.UnknownScreen";
        }
        if (modId == null || modId.isBlank()) {
            modId = "minecraft";
        }
        var pointerState = registries.uiPointerStates().stateFor(screenClass, modId);
        var mouseX = input.containsKey("mouseX")
                ? numberAsInt(input.get("mouseX"), 0)
                : pointerState.mouseX();
        var mouseY = input.containsKey("mouseY")
                ? numberAsInt(input.get("mouseY"), 0)
                : pointerState.mouseY();
        return new MapBackedUiContext(screenClass, modId, mouseX, mouseY, screenHandle);
    }

    private static Object liveScreenHandle(ClientScreenMetrics metrics) {
        if (metrics == null || metrics.screenClass() == null || metrics.screenClass().isBlank()) {
            return null;
        }
        try {
            var minecraftClass = Class.forName("net.minecraft.client.Minecraft");
            var minecraft = minecraftClass.getMethod("getInstance").invoke(null);
            if (minecraft == null) {
                return null;
            }
            var field = minecraftClass.getField("screen");
            var liveScreen = field.get(minecraft);
            if (liveScreen != null && metrics.screenClass().equals(liveScreen.getClass().getName())) {
                return liveScreen;
            }
            return null;
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return null;
        }
    }

    private static TargetSelector selectorFrom(Object selectorValue) {
        if (!(selectorValue instanceof Map<?, ?> rawSelector)) {
            return TargetSelector.builder().build();
        }
        var selector = TargetSelector.builder();
        selector.scope(stringArg(rawSelector.get("scope")));
        selector.screen(stringArg(rawSelector.get("screen")));
        selector.modId(stringArg(rawSelector.get("modId")));
        selector.text(stringArg(rawSelector.get("text")));
        selector.role(stringArg(rawSelector.get("role")));
        selector.id(stringArg(rawSelector.get("id")));
        if (rawSelector.get("index") instanceof Number index) {
            selector.index(index.intValue());
        }
        if (rawSelector.get("bounds") instanceof Map<?, ?> boundsMap) {
            selector.bounds(new Bounds(
                    numberAsInt(boundsMap.get("x"), 0),
                    numberAsInt(boundsMap.get("y"), 0),
                    numberAsInt(boundsMap.get("width"), 0),
                    numberAsInt(boundsMap.get("height"), 0)
            ));
        }
        return selector.build();
    }

    private static boolean selectorSpecified(TargetSelector selector) {
        return selector.scope() != null
                || selector.screen() != null
                || selector.modId() != null
                || selector.text() != null
                || selector.role() != null
                || selector.id() != null
                || selector.bounds() != null
                || selector.index() != null;
    }

    private static String stringArg(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static int numberAsInt(Object value, int defaultValue) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        return defaultValue;
    }

    private static List<TargetSelector> selectorListFrom(Object selectorValue) {
        if (selectorValue == null) {
            return List.of();
        }
        if (selectorValue instanceof List<?> list) {
            return list.stream()
                    .filter(Map.class::isInstance)
                    .map(UiOperationHandlers::selectorFrom)
                    .toList();
        }
        if (selectorValue instanceof Map<?, ?> map) {
            return List.of(selectorFrom(map));
        }
        return List.of();
    }

    private static List<UiTarget> resolveTargets(
            UiDriver driver,
            UiContext context,
            UiSnapshot snapshot,
            List<TargetSelector> selectors,
            boolean defaultToAll
    ) {
        if (selectors == null || selectors.isEmpty()) {
            return defaultToAll ? snapshot.targets() : List.of();
        }
        return selectors.stream()
                .flatMap(selector -> driver.query(context, selector).stream())
                .distinct()
                .toList();
    }

    private static UiCaptureImage captureImage(
            RuntimeRegistries registries,
            UiDriver driver,
            UiContext context,
            UiSnapshot snapshot,
            CaptureRequest request,
            List<UiTarget> capturedTargets,
            List<UiTarget> excludedTargets,
            String source
    ) {
        return switch (source) {
            case "render" -> renderCapture(registries, snapshot, capturedTargets, excludedTargets);
            case "offscreen" -> captureOffscreen(registries, context, snapshot, request, capturedTargets, excludedTargets);
            case "framebuffer" -> captureFramebuffer(registries, context, snapshot, request, capturedTargets, excludedTargets);
            case "auto" -> {
                var framebuffer = registries.uiFramebufferCaptureProviders().select(context, snapshot);
                if (framebuffer.isPresent()) {
                    yield framebuffer.get().capture(context, snapshot, request, capturedTargets, excludedTargets);
                }
                var offscreen = registries.uiOffscreenCaptureProviders().select(context, snapshot);
                if (offscreen.isPresent()) {
                    yield offscreen.get().capture(context, snapshot, request, capturedTargets, excludedTargets);
                }
                yield renderCapture(registries, snapshot, capturedTargets, excludedTargets);
            }
            default -> throw RuntimeOperationBindings.executionFailure("invalid_input: unsupported capture source " + source);
        };
    }

    private static UiCaptureImage captureOffscreen(
            RuntimeRegistries registries,
            UiContext context,
            UiSnapshot snapshot,
            CaptureRequest request,
            List<UiTarget> capturedTargets,
            List<UiTarget> excludedTargets
    ) {
        var provider = registries.uiOffscreenCaptureProviders().select(context, snapshot)
                .orElseThrow(() -> RuntimeOperationBindings.executionFailure("capture_unavailable: offscreen capture provider unavailable"));
        return provider.capture(context, snapshot, request, capturedTargets, excludedTargets);
    }

    private static UiCaptureImage captureFramebuffer(
            RuntimeRegistries registries,
            UiContext context,
            UiSnapshot snapshot,
            CaptureRequest request,
            List<UiTarget> capturedTargets,
            List<UiTarget> excludedTargets
    ) {
        var provider = registries.uiFramebufferCaptureProviders().select(context, snapshot)
                .orElseThrow(() -> RuntimeOperationBindings.executionFailure("capture_unavailable: framebuffer capture provider unavailable"));
        return provider.capture(context, snapshot, request, capturedTargets, excludedTargets);
    }

    private static UiCaptureImage renderCapture(
            RuntimeRegistries registries,
            UiSnapshot snapshot,
            List<UiTarget> capturedTargets,
            List<UiTarget> excludedTargets
    ) {
        var pngBytes = registries.uiCaptureRenderer().render(snapshot, capturedTargets, excludedTargets);
        var image = decodeImage(pngBytes);
        var metadata = new LinkedHashMap<String, Object>();
        metadata.put("guiWidth", image.getWidth());
        metadata.put("guiHeight", image.getHeight());
        metadata.put("captureMode", "render");
        metadata.put("screenClass", snapshot.screenClass());
        return new UiCaptureImage(
                snapshot.driverId(),
                "render",
                pngBytes,
                image.getWidth(),
                image.getHeight(),
                Map.copyOf(metadata)
        );
    }

    private static BufferedImage decodeImage(byte[] pngBytes) {
        try {
            var image = ImageIO.read(new ByteArrayInputStream(pngBytes));
            if (image == null) {
                throw new IOException("Unable to decode capture image");
            }
            return image;
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    private static UiCaptureArtifact storeCapture(
            RuntimeRegistries registries,
            UiDriver driver,
            UiCaptureImage captureImage
    ) {
        var metadata = new LinkedHashMap<String, Object>(captureImage.metadata());
        metadata.put("source", captureImage.source());
        return registries.uiCaptureArtifactStore().store(
                driver.descriptor().id(),
                captureImage.pngBytes(),
                captureImage.width(),
                captureImage.height(),
                Map.copyOf(metadata)
        );
    }

    private static String normalizeCaptureSource(String source) {
        if (source == null || source.isBlank()) {
            return "auto";
        }
        return source.trim().toLowerCase();
    }

    private static String normalizeCaptureMode(String mode) {
        if (mode == null || mode.isBlank()) {
            return "full";
        }
        return mode.trim().toLowerCase();
    }

    private static Map<String, Object> inspectResultToMap(UiInspectResult inspectResult) {
        var result = new LinkedHashMap<String, Object>();
        result.put("screen", inspectResult.screen());
        result.put("screenId", inspectResult.screenId());
        result.put("driverId", inspectResult.driverId());
        result.put("summary", inspectResult.summary());
        result.put("targets", inspectResult.targets().stream().map(UiOperationHandlers::targetToMap).toList());
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

    private static Map<String, Object> snapshotToMap(UiSnapshot snapshot) {
        var result = new LinkedHashMap<String, Object>();
        result.put("screenId", snapshot.screenId());
        result.put("screenClass", snapshot.screenClass());
        result.put("driverId", snapshot.driverId());
        result.put("targets", snapshot.targets().stream().map(UiOperationHandlers::targetToMap).toList());
        result.put("focusedTargetId", snapshot.focusedTargetId() == null ? "" : snapshot.focusedTargetId());
        result.put("selectedTargetId", snapshot.selectedTargetId() == null ? "" : snapshot.selectedTargetId());
        result.put("hoveredTargetId", snapshot.hoveredTargetId() == null ? "" : snapshot.hoveredTargetId());
        result.put("activeTargetId", snapshot.activeTargetId() == null ? "" : snapshot.activeTargetId());
        result.put("extensions", snapshot.extensions());
        return Map.copyOf(result);
    }

    private static Map<String, Object> targetToMap(UiTarget target) {
        return Map.of(
                "targetId", target.targetId(),
                "driverId", target.driverId(),
                "screenClass", target.screenClass(),
                "modId", target.modId(),
                "role", target.role(),
                "text", target.text() == null ? "" : target.text(),
                "bounds", boundsToMap(target.bounds()),
                "state", targetStateToMap(target.state()),
                "actions", target.actions(),
                "extensions", target.extensions()
        );
    }

    private static Map<String, Object> targetStateToMap(UiTargetState state) {
        if (state == null) {
            return Map.of();
        }
        return Map.of(
                "visible", state.visible(),
                "enabled", state.enabled(),
                "focused", state.focused(),
                "hovered", state.hovered(),
                "selected", state.selected(),
                "active", state.active()
        );
    }

    private static Map<String, Object> boundsToMap(Bounds bounds) {
        if (bounds == null) {
            return Map.of();
        }
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
}



