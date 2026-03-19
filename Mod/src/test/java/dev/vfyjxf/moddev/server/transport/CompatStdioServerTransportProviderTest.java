package dev.vfyjxf.moddev.server.transport;

import dev.vfyjxf.moddev.server.bootstrap.ModDevMcpServerFactory;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompatStdioServerTransportProviderTest {

    @Test
    void compatProviderAdvertisesCodexRelevantProtocolVersions() {
        var transport = new CompatStdioServerTransportProvider(
                ModDevMcpServerFactory.JSON_MAPPER,
                new ByteArrayInputStream(new byte[0]),
                new ByteArrayOutputStream()
        );

        assertEquals("2025-11-05", transport.protocolVersions().get(0));
        assertTrue(transport.protocolVersions().contains("2025-06-18"));
        assertTrue(transport.protocolVersions().contains("2024-11-05"));
    }

    @Test
    void compatProviderAcceptsJsonLineInputStreams() {
        var transport = new CompatStdioServerTransportProvider(
                ModDevMcpServerFactory.JSON_MAPPER,
                new ByteArrayInputStream("""
                        {"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-06-18"}}
                        """.getBytes(StandardCharsets.UTF_8)),
                new ByteArrayOutputStream()
        );

        assertTrue(transport.protocolVersions().contains("2025-06-18"));
    }
}

