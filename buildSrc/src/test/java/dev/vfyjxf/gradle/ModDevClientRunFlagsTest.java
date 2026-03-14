package dev.vfyjxf.gradle;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ModDevClientRunFlagsTest {

    @Test
    void resolvesOnlyMcpHostAndPortFromSystemPropertiesFirst() {
        var flags = ModDevClientRunFlags.resolveJvmSystemProperties(
                Map.of(
                        "moddevmcp.devUiCapture", "true",
                        "moddevmcp.host", "127.0.0.1",
                        "moddevmcp.port", "47653"
                ),
                Map.of(
                        "moddevmcp.host", "gradle-host",
                        "moddevmcp.port", "41234"
                )
        );

        assertEquals("127.0.0.1", flags.get("moddevmcp.host"));
        assertEquals("47653", flags.get("moddevmcp.port"));
        assertFalse(flags.containsKey("moddevmcp.devUiCapture"));
    }

    @Test
    void fallsBackToGradlePropertiesForMissingHostAndPort() {
        var flags = ModDevClientRunFlags.resolveJvmSystemProperties(
                Map.of(),
                Map.of(
                        "moddevmcp.devUiCapture", "true",
                        "moddevmcp.host", "127.0.0.1",
                        "moddevmcp.port", "47653"
                )
        );

        assertEquals("127.0.0.1", flags.get("moddevmcp.host"));
        assertEquals("47653", flags.get("moddevmcp.port"));
        assertFalse(flags.containsKey("moddevmcp.devUiCapture"));
    }

    @Test
    void ignoresBlankValues() {
        var flags = ModDevClientRunFlags.resolveJvmSystemProperties(
                Map.of(
                        "moddevmcp.devUiCapture", "true",
                        "moddevmcp.host", " ",
                        "moddevmcp.port", ""
                ),
                Map.of()
        );

        assertFalse(flags.containsKey("moddevmcp.devUiCapture"));
        assertFalse(flags.containsKey("moddevmcp.host"));
        assertFalse(flags.containsKey("moddevmcp.port"));
    }
}
