# Hotswap Implementation Details

**Date:** 2026-03-10

## HotswapAgent

Minimal Java agent using `premain` to capture `Instrumentation`:

```java
public static void premain(String args, Instrumentation inst) {
    instrumentation = inst;
}
```

The `Instrumentation` instance is stored in a `volatile` static field for thread-safe access from any class loader.

## HotswapCapabilities

Detects runtime capabilities:

1. **JBR detection**: `System.getProperty("java.vm.name")` contains "JBR"
2. **DCEVM detection**: Check `dcevm.version` system property
3. **Capability probing**: Uses `Instrumentation.isRedefineClassesSupported()` and related methods
4. **Serialization**: `toMap()` produces a Map for inclusion in tool responses

## HotswapService Reload Logic

1. Get `Instrumentation` from `HotswapAgent.instrumentation()`
2. Scan class output directory for `.class` files with timestamps newer than baseline
3. Read changed class file bytes
4. Match against `inst.getAllLoadedClasses()` — filter by mod package prefix
5. Build `ClassDefinition[]` array for matched classes
6. Call `inst.redefineClasses(definitions)`
7. Report: reloaded classes, classes not yet loaded (will use new version), errors
8. Update timestamp baseline

## ProcessBuilder Compilation

```java
ProcessBuilder pb = new ProcessBuilder(gradleCommand, ":Mod:compileJava");
pb.directory(gradleRoot.toFile());
pb.redirectErrorStream(false);
Process process = pb.start();
```

- Captures stdout/stderr separately for reporting
- Returns exit code for success/failure detection
- Uses platform-appropriate gradle wrapper (`gradlew` vs `gradlew.bat`)
- Uses the real Gradle root as the process working directory, which is required when the game mod lives in a subproject

## DCEVM/JBR Behavior Differences

| Capability | Standard JVM | DCEVM/JBR |
|-----------|-------------|-----------|
| Method body changes | Yes | Yes |
| Add/remove methods | No | Yes |
| Add/remove fields | No | Yes |
| Change class hierarchy | No | Yes |
| Add new classes | N/A (first load) | N/A (first load) |

## Class Output Directory

Default location: `Mod/build/classes/java/main/`

This is where Gradle outputs compiled `.class` files from `:Mod:compileJava`.
