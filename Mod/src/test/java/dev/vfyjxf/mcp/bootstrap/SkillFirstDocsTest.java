package dev.vfyjxf.mcp.bootstrap;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SkillFirstDocsTest {

    @Test
    void readmesDescribeSkillFirstServiceContract() throws Exception {
        var rootDir = Path.of("").toAbsolutePath().normalize().getParent();
        var readme = Files.readString(rootDir.resolve("README.md"));
        var readmeZh = Files.readString(rootDir.resolve("README.zh.md"));

        assertTrue(readme.contains("skill-first service model"));
        assertTrue(readme.contains("moddev-usage"));
        assertTrue(readme.contains("/api/v1/status"));
        assertTrue(readme.contains("build/moddevmcp/game-instances.json"));
        assertTrue(readme.toLowerCase().contains("client and server use separate ports"));
        assertTrue(readme.contains("default probe"));
        assertTrue(readme.contains("project-local fallback"));
        assertTrue(readme.toLowerCase().contains("exported skills"));
        assertTrue(readme.toLowerCase().contains("local http service"));
        assertFalse(readme.contains(":Server:runStdioMcp"));
        assertFalse(readme.contains(":Server"));
        assertFalse(readme.contains(":Plugin"));
        assertFalse(readme.contains("createMcpClientFiles"));
        assertFalse(readme.contains("docs/guides/2026-03-11-simple-agent-install-guide.md"));
        assertFalse(readme.contains("docs/guides/2026-03-11-game-mcp-guide.md"));
        assertFalse(readme.contains("docs/guides/2026-03-11-testmod-runclient-guide.md"));
        assertFalse(readme.contains("docs/guides/2026-03-11-agent-preflight-checklist.md"));

        assertTrue(readmeZh.contains("skill-first 服务模型"));
        assertTrue(readmeZh.contains("moddev-usage"));
        assertTrue(readmeZh.contains("/api/v1/status"));
        assertTrue(readmeZh.contains("build/moddevmcp/game-instances.json"));
        assertTrue(readmeZh.contains("client 和 server 使用独立端口"));
        assertTrue(readmeZh.contains("默认探测"));
        assertTrue(readmeZh.contains("项目级回退"));
        assertTrue(readmeZh.contains("面向最终用户的运行时产品：`:Mod`"));
        assertTrue(readmeZh.contains("导出技能"));
        assertTrue(readmeZh.contains("本地 HTTP 服务"));
        assertFalse(readmeZh.contains(":Server:runStdioMcp"));
        assertFalse(readmeZh.contains(":Server"));
        assertFalse(readmeZh.contains(":Plugin"));
        assertFalse(readmeZh.contains("createMcpClientFiles"));
        assertFalse(readmeZh.contains("docs/guides/2026-03-11-simple-agent-install-guide.md"));
        assertFalse(readmeZh.contains("docs/guides/2026-03-11-game-mcp-guide.md"));
        assertFalse(readmeZh.contains("docs/guides/2026-03-11-testmod-runclient-guide.md"));
        assertFalse(readmeZh.contains("docs/guides/2026-03-11-agent-preflight-checklist.md"));
    }
}

