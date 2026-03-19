package dev.vfyjxf.moddev.server.bootstrap;

public record BackendMcpEndpointConfig(
        String host,
        int port
) {

    public static final String MCP_PORT_PROPERTY = "moddevmcp.mcpPort";
    public static final int DEFAULT_MCP_PORT = 47654;

    public BackendMcpEndpointConfig {
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("host must not be blank");
        }
        if (port < 0 || port > 65535) {
            throw new IllegalArgumentException("port must be between 0 and 65535");
        }
    }

    public static BackendMcpEndpointConfig loadResolved() {
        var host = System.getProperty(HostEndpointConfig.HOST_PROPERTY, HostEndpointConfig.DEFAULT_HOST);
        var portValue = System.getProperty(MCP_PORT_PROPERTY);
        var port = portValue == null || portValue.isBlank()
                ? DEFAULT_MCP_PORT
                : Integer.parseInt(portValue);
        return new BackendMcpEndpointConfig(host, port);
    }
}

