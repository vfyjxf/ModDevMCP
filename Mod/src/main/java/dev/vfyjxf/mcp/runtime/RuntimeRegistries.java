package dev.vfyjxf.mcp.runtime;

import dev.vfyjxf.mcp.api.runtime.InputController;
import dev.vfyjxf.mcp.runtime.event.RuntimeEventPublisher;
import dev.vfyjxf.mcp.runtime.tool.UiAutomationSessionManager;
import dev.vfyjxf.mcp.runtime.ui.UiCaptureArtifactStore;
import dev.vfyjxf.mcp.runtime.ui.UiCaptureRenderer;
import dev.vfyjxf.mcp.runtime.ui.UiPointerStateRegistry;
import dev.vfyjxf.mcp.runtime.ui.UiSnapshotJournal;
import dev.vfyjxf.mcp.runtime.ui.UiSessionStateRegistry;
import dev.vfyjxf.mcp.server.api.McpToolProvider;

import java.util.ArrayList;
import java.util.List;

public final class RuntimeRegistries {

    private final UiDriverRegistry uiDrivers = new UiDriverRegistry();
    private final InventoryDriverRegistry inventoryDrivers = new InventoryDriverRegistry();
    private final List<InputController> inputControllers = new ArrayList<>();
    private final List<McpToolProvider> toolProviders = new ArrayList<>();
    private final RuntimeEventPublisher eventPublisher = new RuntimeEventPublisher();
    private final UiSessionStateRegistry uiSessionStates = new UiSessionStateRegistry();
    private final UiPointerStateRegistry uiPointerStates = new UiPointerStateRegistry();
    private final UiSnapshotJournal uiSnapshotJournal = new UiSnapshotJournal();
    private final UiInteractionStateResolverRegistry uiInteractionResolvers = new UiInteractionStateResolverRegistry();
    private final UiOffscreenCaptureProviderRegistry uiOffscreenCaptureProviders = new UiOffscreenCaptureProviderRegistry();
    private final UiFramebufferCaptureProviderRegistry uiFramebufferCaptureProviders = new UiFramebufferCaptureProviderRegistry();
    private final UiCaptureRenderer uiCaptureRenderer = new UiCaptureRenderer();
    private final UiCaptureArtifactStore uiCaptureArtifactStore = new UiCaptureArtifactStore();
    private final UiAutomationSessionManager uiAutomationSessions = new UiAutomationSessionManager();

    public UiDriverRegistry uiDrivers() {
        return uiDrivers;
    }

    public InventoryDriverRegistry inventoryDrivers() {
        return inventoryDrivers;
    }

    public List<InputController> inputControllers() {
        return inputControllers;
    }

    public List<McpToolProvider> toolProviders() {
        return toolProviders;
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

    public UiAutomationSessionManager uiAutomationSessions() {
        return uiAutomationSessions;
    }
}
