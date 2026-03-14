package dev.vfyjxf.mcp.gradle;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class McpGatewayLaunchFilesTest {

    @Test
    void gatewayJavaArgsIncludeGatewayMainClass() {
        var content = McpLaunchFiles.javaArgs(
                "C:\\libs\\alpha.jar",
                "dev.vfyjxf.mcp.server.bootstrap.ModDevMcpGatewayMain"
        );

        assertTrue(content.contains("dev.vfyjxf.mcp.server.bootstrap.ModDevMcpGatewayMain"));
    }

    @Test
    void backendJavaArgsIncludeBackendMainClass() {
        var content = McpLaunchFiles.javaArgs(
                "C:\\libs\\alpha.jar",
                "dev.vfyjxf.mcp.server.bootstrap.ModDevMcpBackendMain"
        );

        assertTrue(content.contains("dev.vfyjxf.mcp.server.bootstrap.ModDevMcpBackendMain"));
    }

    @Test
    void codexSnippetCanPointAtGatewayArgumentFile() {
        var snippet = McpLaunchFiles.mcpClientTomlSnippet(
                "moddevmcp",
                "java",
                List.of(
                        "-Dmoddevmcp.host=127.0.0.1",
                        "-Dmoddevmcp.port=47653",
                        "-Dmoddevmcp.mcpPort=47654",
                        "@D:\\build\\moddevmcp\\mcp-gateway-java.args"
                )
        );

        assertTrue(snippet.contains("'-Dmoddevmcp.mcpPort=47654'"));
        assertTrue(snippet.contains("'@D:\\build\\moddevmcp\\mcp-gateway-java.args'"));
    }
}
