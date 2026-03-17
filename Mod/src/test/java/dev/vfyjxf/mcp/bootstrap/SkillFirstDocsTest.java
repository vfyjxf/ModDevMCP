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

        assertTrue(readme.contains("approved migration direction"));
        assertTrue(readme.contains("moddev-entry"));
        assertTrue(readme.contains("/api/v1/status"));
        assertTrue(readme.toLowerCase().contains("exported skills"));
        assertTrue(readme.toLowerCase().contains("local http service"));
        assertFalse(readme.contains(":Server:runStdioMcp"));
        assertFalse(readme.contains("createMcpClientFiles"));
        assertFalse(readme.contains("docs/guides/2026-03-11-simple-agent-install-guide.md"));
        assertFalse(readme.contains("docs/guides/2026-03-11-game-mcp-guide.md"));
        assertFalse(readme.contains("docs/guides/2026-03-11-testmod-runclient-guide.md"));
        assertFalse(readme.contains("docs/guides/2026-03-11-agent-preflight-checklist.md"));

        assertTrue(readmeZh.contains("已批准的迁移方向"));
        assertTrue(readmeZh.contains("moddev-entry"));
        assertTrue(readmeZh.contains("/api/v1/status"));
        assertTrue(readmeZh.contains("面向最终用户的运行时产品：`:Mod`"));
        assertTrue(readmeZh.contains("迁移期内部模块：`:Server`、`:Plugin`"));
        assertTrue(readmeZh.contains("导出技能"));
        assertTrue(readmeZh.contains("本地 HTTP 服务"));
        assertFalse(readmeZh.contains(":Server:runStdioMcp"));
        assertFalse(readmeZh.contains("createMcpClientFiles"));
        assertFalse(readmeZh.contains("docs/guides/2026-03-11-simple-agent-install-guide.md"));
        assertFalse(readmeZh.contains("docs/guides/2026-03-11-game-mcp-guide.md"));
        assertFalse(readmeZh.contains("docs/guides/2026-03-11-testmod-runclient-guide.md"));
        assertFalse(readmeZh.contains("docs/guides/2026-03-11-agent-preflight-checklist.md"));
    }
}
