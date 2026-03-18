package dev.vfyjxf.mcp.server.transport;

import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

public final class CompatStdioServerTransportProvider extends StdioServerTransportProvider {

    private static final List<String> SUPPORTED_PROTOCOL_VERSIONS = List.of(
            "2025-11-05",
            "2025-06-18",
            "2024-11-05"
    );

    public CompatStdioServerTransportProvider(McpJsonMapper jsonMapper, InputStream input, OutputStream output) {
        super(jsonMapper, input, output);
    }

    @Override
    public List<String> protocolVersions() {
        return SUPPORTED_PROTOCOL_VERSIONS;
    }
}
