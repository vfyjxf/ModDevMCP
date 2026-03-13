# Hotswap Consumer Integration Design

**Date:** 2026-03-12

## Goal

Provide a standard downstream integration path for hotswap so consumer projects can enable ModDevMCP hotswap with a Gradle plugin, while retaining manual JVM argument setup as a fallback.

## Requirements

- Primary integration path is a Gradle plugin.
- Fallback path remains supported through explicit JVM arguments.
- First release supports NeoForge only.
- Plugin architecture must leave room for future Forge/Fabric support.
- Downstream projects should not need to depend on this repository layout.

## Recommended Architecture

Publish three artifacts:

1. `dev.vfyjxf:moddevmcp-mod`
2. `dev.vfyjxf:moddevmcp-agent`
3. `dev.vfyjxf:moddev-hotswap-gradle-plugin`

The plugin is the primary entrypoint for downstream projects. It configures dev runs, resolves the matching agent artifact, and passes runtime configuration into the mod through JVM system properties.

The mod continues to own compile and reload behavior. The plugin does not implement hotswap logic; it only prepares the JVM and runtime configuration.

## Responsibilities

### Gradle Plugin

- Integrate with NeoForge dev runs
- Resolve the correct `moddevmcp-agent` version
- Inject `-javaagent`
- Inject runtime system properties
- Validate preconditions during configuration
- Expose a small extension DSL for overrides

### Mod Runtime

- Register `moddev.compile` and `moddev.hotswap`
- Read runtime configuration from system properties
- Compile downstream sources
- Scan downstream class outputs
- Perform class redefinition via `Instrumentation`

### Agent

- Remain minimal
- Capture `Instrumentation` in `premain`
- Report runtime capabilities such as enhanced hotswap availability

## Runtime Contract

The plugin and manual fallback both use the same runtime protocol:

- `-javaagent:/path/to/moddevmcp-agent.jar`
- `-Dmoddevmcp.project.root=/path/to/consumer-project`
- `-Dmoddevmcp.compile.task=:module:compileJava`
- `-Dmoddevmcp.class.output=/path/to/consumer-project/module/build/classes/java/main`

This removes the current hard dependency on this repository's `:Mod` task name and `Mod/build/classes/java/main` layout.

## Plugin DSL

Default downstream usage:

```gradle
plugins {
    id 'net.neoforged.moddev'
    id 'dev.vfyjxf.moddev-hotswap' version '<version>'
}

dependencies {
    implementation "dev.vfyjxf:moddevmcp-mod:<version>"
}

modDevMcpHotswap {
    enabled = true
}
```

Optional overrides:

```gradle
modDevMcpHotswap {
    enabled = true
    projectRoot = rootProject.projectDir
    compileTask = ":MyMod:compileJava"
    classOutputDir = "MyMod/build/classes/java/main"
    runs = ["client", "gameTestServer"]
    requireEnhancedHotswap = false
}
```

## NeoForge Scope

The first implementation only supports NeoForge run configuration injection.

To preserve future flexibility, the plugin should separate:

- platform-specific run injection
- runtime property computation
- agent resolution

That allows Forge/Fabric support later without redesigning the runtime contract.

## Validation Rules

The plugin should fail fast with clear messages when:

- `net.neoforged.moddev` is not applied
- a targeted run configuration cannot be found
- Java toolchain/runtime is below 21
- the agent artifact cannot be resolved
- required runtime values cannot be determined

Warnings are sufficient when:

- enhanced hotswap is recommended but not required
- fallback values are inferred from defaults

## Manual Fallback

If the plugin cannot be used, downstream users can configure dev runs manually with the same JVM arguments:

```text
-javaagent:/path/to/moddevmcp-agent.jar
-Dmoddevmcp.project.root=/path/to/project
-Dmoddevmcp.compile.task=:mod:compileJava
-Dmoddevmcp.class.output=/path/to/project/build/classes/java/main
```

This keeps plugin and manual setup behavior aligned and minimizes support burden.

## Migration Impact

Runtime code must stop assuming:

- project root from `user.dir`
- compile task `:Mod:compileJava`
- class output `Mod/build/classes/java/main`

Those become configurable runtime inputs, with compatibility defaults preserved only where safe.

## Recommendation

Use the plugin as the productized integration path and keep manual JVM arguments as a documented fallback. This gives downstream projects a low-friction setup while preserving a debuggable escape hatch.
