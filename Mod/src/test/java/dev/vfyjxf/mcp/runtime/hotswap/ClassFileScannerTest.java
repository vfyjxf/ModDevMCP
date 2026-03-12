package dev.vfyjxf.mcp.runtime.hotswap;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClassFileScannerTest {

    @Test
    void classFileToClassNameConvertsCorrectly() {
        assertEquals("dev.vfyjxf.mcp.Foo", ClassFileScanner.classFileToClassName("dev/vfyjxf/mcp/Foo.class"));
        assertEquals("com.example.Bar", ClassFileScanner.classFileToClassName("com/example/Bar.class"));
        assertEquals("Root", ClassFileScanner.classFileToClassName("Root.class"));
    }

    @Test
    void classFileToClassNameHandlesBackslashes() {
        assertEquals("dev.vfyjxf.mcp.Foo", ClassFileScanner.classFileToClassName("dev\\vfyjxf\\mcp\\Foo.class"));
    }

    @Test
    void scanFindsClassFiles(@TempDir Path tempDir) throws IOException {
        Path classFile = tempDir.resolve("dev/vfyjxf/mcp/Foo.class");
        Files.createDirectories(classFile.getParent());
        Files.write(classFile, new byte[]{(byte) 0xCA, (byte) 0xFE});

        Map<String, Long> result = ClassFileScanner.scan(tempDir);

        assertEquals(1, result.size());
        assertTrue(result.containsKey("dev/vfyjxf/mcp/Foo.class"));
    }

    @Test
    void scanReturnsEmptyForNonexistentDir(@TempDir Path tempDir) {
        Map<String, Long> result = ClassFileScanner.scan(tempDir.resolve("nonexistent"));
        assertTrue(result.isEmpty());
    }

    @Test
    void readChangedFiltersOlderFiles(@TempDir Path tempDir) throws IOException, InterruptedException {
        Path classFile = tempDir.resolve("dev/vfyjxf/mcp/Foo.class");
        Files.createDirectories(classFile.getParent());
        Files.write(classFile, new byte[]{(byte) 0xCA, (byte) 0xFE});

        Map<String, Long> baseline = ClassFileScanner.scan(tempDir);

        // File not modified — should not appear in changed
        Map<String, byte[]> changed = ClassFileScanner.readChanged(tempDir, baseline);
        assertTrue(changed.isEmpty());

        // Now modify the file (ensure timestamp advances)
        Thread.sleep(50);
        Files.write(classFile, new byte[]{(byte) 0xCA, (byte) 0xFE, (byte) 0xBA, (byte) 0xBE});

        changed = ClassFileScanner.readChanged(tempDir, baseline);
        assertEquals(1, changed.size());
        assertTrue(changed.containsKey("dev/vfyjxf/mcp/Foo.class"));
        assertEquals(4, changed.get("dev/vfyjxf/mcp/Foo.class").length);
    }

    @Test
    void readChangedDetectsNewFiles(@TempDir Path tempDir) throws IOException {
        Map<String, Long> baseline = ClassFileScanner.scan(tempDir);

        Path classFile = tempDir.resolve("com/example/New.class");
        Files.createDirectories(classFile.getParent());
        Files.write(classFile, new byte[]{1, 2, 3});

        Map<String, byte[]> changed = ClassFileScanner.readChanged(tempDir, baseline);
        assertFalse(changed.isEmpty());
        assertTrue(changed.containsKey("com/example/New.class"));
    }
}
