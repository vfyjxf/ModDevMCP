package dev.vfyjxf.moddev.service.config;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Path;

public record ServiceConfig(
        String host,
        int port,
        Path exportRoot,
        Path projectRoot
) {

    public static final String DEFAULT_HOST = "127.0.0.1";
    public static final int DEFAULT_PORT = 47812;
    public static final String HOST_PROPERTY = "moddev.service.host";
    public static final String PORT_PROPERTY = "moddev.service.port";
    public static final String EXPORT_ROOT_PROPERTY = "moddev.skill.exportRoot";
    public static final String PROJECT_ROOT_PROPERTY = "moddevmcp.project.root";

    public ServiceConfig {
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("host must not be blank");
        }
        ensureLoopbackHost(host);
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("port must be between 1 and 65535");
        }
        if (exportRoot == null) {
            throw new IllegalArgumentException("exportRoot must not be null");
        }
        if (projectRoot == null) {
            throw new IllegalArgumentException("projectRoot must not be null");
        }
        exportRoot = exportRoot.toAbsolutePath().normalize();
        projectRoot = projectRoot.toAbsolutePath().normalize();
    }

    public static ServiceConfig loadResolved() {
        var host = defaultIfBlank(System.getProperty(HOST_PROPERTY), DEFAULT_HOST);
        var portText = defaultIfBlank(System.getProperty(PORT_PROPERTY), String.valueOf(DEFAULT_PORT));
        var port = parsePort(portText);
        var exportRootText = defaultIfBlank(System.getProperty(EXPORT_ROOT_PROPERTY), defaultExportRoot().toString());
        var exportRoot = Path.of(exportRootText);
        var projectRootText = defaultIfBlank(System.getProperty(PROJECT_ROOT_PROPERTY), System.getProperty("user.dir"));
        var projectRoot = Path.of(projectRootText);
        return new ServiceConfig(host, port, exportRoot, projectRoot);
    }

    public ServiceConfig(String host, int port, Path exportRoot) {
        this(host, port, exportRoot, Path.of(defaultIfBlank(System.getProperty(PROJECT_ROOT_PROPERTY), System.getProperty("user.dir"))));
    }

    public Path gameInstancesPath() {
        return projectRoot().resolve("build").resolve("moddevmcp").resolve("game-instances.json").toAbsolutePath().normalize();
    }

    private static String defaultIfBlank(String value, String defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return value.trim();
    }

    private static int parsePort(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("port must be a valid integer", exception);
        }
    }

    private static Path defaultExportRoot() {
        return Path.of(System.getProperty("user.home"), ".moddev", "skills");
    }

    private static void ensureLoopbackHost(String host) {
        try {
            if (!InetAddress.getByName(host).isLoopbackAddress()) {
                throw new IllegalArgumentException("host must be loopback-only");
            }
        } catch (UnknownHostException exception) {
            throw new IllegalArgumentException("host must resolve to a loopback address", exception);
        }
    }
}

