package dev.vfyjxf.mcp.gradle;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

public final class ModDevMcpHotswapPlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        ModDevMcpHotswapExtension extension =
                project.getExtensions().create("modDevMcpHotswap", ModDevMcpHotswapExtension.class);
        extension.getProjectRoot().convention(project.getRootProject().getProjectDir());

        project.getPluginManager().withPlugin("net.neoforged.moddev",
                ignored -> new dev.vfyjxf.mcp.gradle.neoforge.NeoForgeRunInjector().inject(project, extension));
    }
}
