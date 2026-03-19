package dev.vfyjxf.moddev.runtime;

import dev.vfyjxf.moddev.api.operation.OperationRegistration;
import dev.vfyjxf.moddev.api.runtime.InputController;
import dev.vfyjxf.moddev.runtime.event.RuntimeEventPublisher;
import dev.vfyjxf.moddev.runtime.tool.UiAutomationSessionManager;
import dev.vfyjxf.moddev.runtime.ui.*;
import dev.vfyjxf.moddev.server.api.McpToolProvider;

import java.util.ArrayList;
import java.util.List;

public final class RuntimeRegistries {

    private final UiDriverRegistry uiDrivers = new UiDriverRegistry();
    private final List<InputController> inputControllers = new ArrayList<>();
    private final List<McpToolProvider> toolProviders = new ArrayList<>();
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
    private final UiAutomationSessionManager uiAutomationSessions = new UiAutomationSessionManager();

    public UiDriverRegistry uiDrivers() {
        return uiDrivers;
    }

    public List<InputController> inputControllers() {
        return inputControllers;
    }

    public List<McpToolProvider> toolProviders() {
        return toolProviders;
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

    public UiAutomationSessionManager uiAutomationSessions() {
        return uiAutomationSessions;
    }
}