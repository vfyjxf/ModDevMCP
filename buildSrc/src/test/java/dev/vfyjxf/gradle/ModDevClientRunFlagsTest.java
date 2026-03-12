package dev.vfyjxf.gradle;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModDevClientRunFlagsTest {

    @Test
    void forwardsDevUiCaptureFlagFromSystemProperties() {
        var flags = ModDevClientRunFlags.resolveJvmSystemProperties(
                Map.of("moddevmcp.devUiCapture", "true"),
                Map.of()
        );

        assertEquals("true", flags.get("moddevmcp.devUiCapture"));
    }

    @Test
    void fallsBackToGradlePropertiesWhenSystemPropertyMissing() {
        var flags = ModDevClientRunFlags.resolveJvmSystemProperties(
                Map.of(),
                Map.of("moddevmcp.devUiCapture", "true")
        );

        assertEquals("true", flags.get("moddevmcp.devUiCapture"));
    }

    @Test
    void ignoresBlankValues() {
        var flags = ModDevClientRunFlags.resolveJvmSystemProperties(
                Map.of("moddevmcp.devUiCapture", " "),
                Map.of()
        );

        assertTrue(flags.isEmpty());
    }
}
