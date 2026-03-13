package dev.vfyjxf.mcp.server.bootstrap;

public record HostEndpointConfig(
        String host,
        int port
) {

    public static final String HOST_PROPERTY = "moddevmcp.host";
    public static final String PORT_PROPERTY = "moddevmcp.port";
    public static final String DEFAULT_HOST = "127.0.0.1";
    public static final int DEFAULT_PORT = 47653;

    public HostEndpointConfig {
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("host must not be blank");
        }
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("port must be between 1 and 65535");
        }
    }

    public static HostEndpointConfig loadResolved() {
        var host = System.getProperty(HOST_PROPERTY, DEFAULT_HOST);
        var portValue = System.getProperty(PORT_PROPERTY);
        var port = portValue == null || portValue.isBlank()
                ? DEFAULT_PORT
                : Integer.parseInt(portValue);
        return new HostEndpointConfig(host, port);
    }
}


