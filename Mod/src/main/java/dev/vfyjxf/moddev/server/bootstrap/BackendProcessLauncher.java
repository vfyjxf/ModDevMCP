package dev.vfyjxf.moddev.server.bootstrap;

import java.io.IOException;
import java.nio.file.Files;

public class BackendProcessLauncher {

    public Process launch(GatewayBootstrapConfig config) throws IOException {
        var argsFile = config.backendArgsFile().toAbsolutePath().normalize();
        var logDir = argsFile.getParent() == null ? argsFile.toAbsolutePath().getParent() : argsFile.getParent();
        Files.createDirectories(logDir);
        var stdout = logDir.resolve("backend-stdout.log").toFile();
        var stderr = logDir.resolve("backend-stderr.log").toFile();
        Files.writeString(stdout.toPath(), "", java.nio.charset.StandardCharsets.UTF_8,
                java.nio.file.StandardOpenOption.CREATE,
                java.nio.file.StandardOpenOption.TRUNCATE_EXISTING);
        Files.writeString(stderr.toPath(), "", java.nio.charset.StandardCharsets.UTF_8,
                java.nio.file.StandardOpenOption.CREATE,
                java.nio.file.StandardOpenOption.TRUNCATE_EXISTING);
        if (config.backendLauncher() != null && Files.exists(config.backendLauncher())) {
            var launcher = config.backendLauncher().toAbsolutePath().normalize();
            var command = isWindows()
                    ? java.util.List.of("cmd.exe", "/c", launcher.toString())
                    : java.util.List.of("sh", launcher.toString());
            new ProcessBuilder(command)
                    .redirectOutput(ProcessBuilder.Redirect.appendTo(stdout))
                    .redirectError(ProcessBuilder.Redirect.appendTo(stderr))
                    .start();
            return null;
        }
        return new ProcessBuilder(config.javaCommand(), "@" + argsFile)
                .redirectOutput(ProcessBuilder.Redirect.appendTo(stdout))
                .redirectError(ProcessBuilder.Redirect.appendTo(stderr))
                .start();
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase(java.util.Locale.ROOT).contains("win");
    }
}

