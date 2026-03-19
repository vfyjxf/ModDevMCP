package dev.vfyjxf.moddev.runtime.ui;

import dev.vfyjxf.moddev.server.api.McpResource;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public final class UiCaptureArtifactStore {

    private final AtomicLong sequence = new AtomicLong(1);
    private final Path root;
    private final Map<String, UiCaptureArtifact> artifacts = new ConcurrentHashMap<>();

    public UiCaptureArtifactStore() {
        this(Path.of("build", "moddevmcp", "captures"));
    }

    public UiCaptureArtifactStore(Path root) {
        this.root = root.toAbsolutePath().normalize();
    }

    public UiCaptureArtifact store(String driverId, byte[] content, int width, int height) {
        return store(driverId, content, width, height, Map.of());
    }

    public UiCaptureArtifact store(String driverId, byte[] content, int width, int height, Map<String, Object> extraMetadata) {
        ensureRoot();
        var imageRef = "capture-" + sequence.getAndIncrement();
        var path = root.resolve(imageRef + ".png");
        write(path, content);
        var createdAt = System.currentTimeMillis();
        var metadata = new java.util.LinkedHashMap<String, Object>();
        metadata.put("width", width);
        metadata.put("height", height);
        metadata.put("format", "png");
        metadata.put("driverId", driverId);
        metadata.put("createdAt", createdAt);
        metadata.putAll(extraMetadata);
        var artifact = new UiCaptureArtifact(
                imageRef,
                path.toString(),
                "moddev://capture/" + imageRef,
                "image/png",
                Map.copyOf(metadata),
                content
        );
        artifacts.put(imageRef, artifact);
        return artifact;
    }

    public Optional<UiCaptureArtifact> read(String imageRef) {
        return Optional.ofNullable(artifacts.get(imageRef));
    }

    public Optional<McpResource> readResource(String uri) {
        var imageRef = imageRefFromUri(uri);
        if (imageRef == null) {
            return Optional.empty();
        }
        return read(imageRef).map(artifact -> new McpResource(
                artifact.resourceUri(),
                artifact.mimeType(),
                "UI Capture " + artifact.imageRef(),
                artifact.metadata(),
                artifact.content()
        ));
    }

    private String imageRefFromUri(String uri) {
        var prefix = "moddev://capture/";
        return uri != null && uri.startsWith(prefix) ? uri.substring(prefix.length()) : null;
    }

    private void ensureRoot() {
        try {
            Files.createDirectories(root);
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    private void write(Path path, byte[] content) {
        try {
            Files.write(path, content);
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }
}

