package dev.vfyjxf.moddev.runtime;

import dev.vfyjxf.moddev.api.operation.OperationRegistration;
import dev.vfyjxf.moddev.api.runtime.ClientScreenProbe;
import dev.vfyjxf.moddev.api.runtime.InputController;
import dev.vfyjxf.moddev.runtime.command.CommandService;
import dev.vfyjxf.moddev.runtime.event.RuntimeEventPublisher;
import dev.vfyjxf.moddev.runtime.game.GameCloser;
import dev.vfyjxf.moddev.runtime.game.PauseOnLostFocusService;
import dev.vfyjxf.moddev.runtime.hotswap.HotswapService;
import dev.vfyjxf.moddev.runtime.ui.*;
import dev.vfyjxf.moddev.runtime.world.WorldService;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class RuntimeRegistries {

    private final UiDriverRegistry uiDrivers = new UiDriverRegistry();
    private final List<InputController> inputControllers = new ArrayList<>();
    private final List<OperationRegistration> operationRegistrations = new ArrayList<>();
    private final RuntimeEventPublisher eventPublisher = new RuntimeEventPublisher();
    private final UiSessionStateRegistry uiSessionStates = new UiSessionStateRegistry();
    private final UiPointerStateRegistry uiPointerStates = new UiPointerStateRegistry();
    private final UiSnapshotJournal uiSnapshotJournal = new UiSnapshotJournal();
    private final UiInteractionStateResolverRegistry uiInteractionResolvers = new UiInteractionStateResolverRegistry();
    private final UiOffscreenCaptureProviderRegistry uiOffscreenCaptureProviders = new UiOffscreenCaptureProviderRegistry();
    private final UiFramebufferCaptureProviderRegistry uiFramebufferCaptureProviders = new UiFramebufferCaptureProviderRegistry();
    private final UiCaptureRenderer uiCaptureRenderer = new UiCaptureRenderer();
    private final UiCaptureArtifactStore uiCaptureArtifactStore = new UiCaptureArtifactStore();
    private final Map<String, GameCloser> gameClosers = new LinkedHashMap<>();
    private final Map<String, CommandService> commandServices = new LinkedHashMap<>();
    private final Map<String, WorldService> worldServices = new LinkedHashMap<>();
    private final Map<String, PauseOnLostFocusService> pauseOnLostFocusServices = new LinkedHashMap<>();
    private final Map<String, ClientScreenProbe> screenProbes = new LinkedHashMap<>();
    private HotswapService hotswapService;

    public UiDriverRegistry uiDrivers() {
        return uiDrivers;
    }

    public List<InputController> inputControllers() {
        return inputControllers;
    }

    public List<OperationRegistration> operationRegistrations() {
        return operationRegistrations;
    }

    public RuntimeEventPublisher eventPublisher() {
        return eventPublisher;
    }

    public UiSessionStateRegistry uiSessionStates() {
        return uiSessionStates;
    }

    public UiPointerStateRegistry uiPointerStates() {
        return uiPointerStates;
    }

    public UiSnapshotJournal uiSnapshotJournal() {
        return uiSnapshotJournal;
    }

    public UiInteractionStateResolverRegistry uiInteractionResolvers() {
        return uiInteractionResolvers;
    }

    public UiOffscreenCaptureProviderRegistry uiOffscreenCaptureProviders() {
        return uiOffscreenCaptureProviders;
    }

    public UiFramebufferCaptureProviderRegistry uiFramebufferCaptureProviders() {
        return uiFramebufferCaptureProviders;
    }

    public UiCaptureRenderer uiCaptureRenderer() {
        return uiCaptureRenderer;
    }

    public UiCaptureArtifactStore uiCaptureArtifactStore() {
        return uiCaptureArtifactStore;
    }

    public void registerGameCloser(String side, GameCloser gameCloser) {
        gameClosers.put(normalizeSide(side), Objects.requireNonNull(gameCloser, "gameCloser"));
    }

    public Optional<GameCloser> gameCloser(String side) {
        return Optional.ofNullable(gameClosers.get(normalizeSide(side)));
    }

    public void registerCommandService(String side, CommandService commandService) {
        commandServices.put(normalizeSide(side), Objects.requireNonNull(commandService, "commandService"));
    }

    public Optional<CommandService> commandService(String side) {
        return Optional.ofNullable(commandServices.get(normalizeSide(side)));
    }

    public void registerWorldService(String side, WorldService worldService) {
        worldServices.put(normalizeSide(side), Objects.requireNonNull(worldService, "worldService"));
    }

    public Optional<WorldService> worldService(String side) {
        return Optional.ofNullable(worldServices.get(normalizeSide(side)));
    }

    public void registerPauseOnLostFocusService(String side, PauseOnLostFocusService service) {
        pauseOnLostFocusServices.put(normalizeSide(side), Objects.requireNonNull(service, "service"));
    }

    public Optional<PauseOnLostFocusService> pauseOnLostFocusService(String side) {
        return Optional.ofNullable(pauseOnLostFocusServices.get(normalizeSide(side)));
    }

    public void registerScreenProbe(String side, ClientScreenProbe screenProbe) {
        screenProbes.put(normalizeSide(side), Objects.requireNonNull(screenProbe, "screenProbe"));
    }

    public Optional<ClientScreenProbe> screenProbe(String side) {
        return Optional.ofNullable(screenProbes.get(normalizeSide(side)));
    }

    public void registerHotswapService(HotswapService service) {
        this.hotswapService = Objects.requireNonNull(service, "service");
    }

    public Optional<HotswapService> hotswapService() {
        return Optional.ofNullable(hotswapService);
    }

    private static String normalizeSide(String side) {
        if ("client".equals(side) || "server".equals(side)) {
            return side;
        }
        throw new IllegalArgumentException("side must be client or server");
    }
}
