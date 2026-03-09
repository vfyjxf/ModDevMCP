package dev.vfyjxf.mcp.runtime.ui;

import dev.vfyjxf.mcp.api.ui.UiSnapshot;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UiCaptureArtifactStoreTest {

    @Test
    void storePersistsPngArtifactsAndReadsThemBackByImageRef() throws Exception {
        var root = Files.createTempDirectory("ui-capture-store-test");
        var store = new UiCaptureArtifactStore(root);
        var bytes = new UiCaptureRenderer().render(
                new UiSnapshot("screen", "custom.UnknownScreen", "fallback-region", java.util.List.of(), java.util.List.of(), null, null, null, null, Map.of()),
                java.util.List.of(),
                java.util.List.of()
        );

        var artifact = store.store("fallback-region", bytes, 320, 240);

        assertTrue(Files.exists(Path.of(artifact.path())));
        assertEquals("image/png", artifact.mimeType());
        assertEquals("png", artifact.metadata().get("format"));
        assertEquals(artifact.imageRef(), store.read(artifact.imageRef()).orElseThrow().imageRef());
    }
}
