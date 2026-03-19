package dev.vfyjxf.moddev.service.discovery;

import dev.vfyjxf.moddev.server.transport.JsonCodec;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public final class GameInstanceRegistry {

    private static final Set<String> ALLOWED_SIDES = Set.of("client", "server");

    private final Path registryPath;
    private final JsonCodec jsonCodec;

    public GameInstanceRegistry(Path registryPath) {
        this(registryPath, new JsonCodec());
    }

    GameInstanceRegistry(Path registryPath, JsonCodec jsonCodec) {
        this.registryPath = Objects.requireNonNull(registryPath, "registryPath").toAbsolutePath().normalize();
        this.jsonCodec = Objects.requireNonNull(jsonCodec, "jsonCodec");
    }

    public synchronized void upsert(String side, GameInstanceRecord record) {
        var normalizedSide = normalizeSide(side);
        Objects.requireNonNull(record, "record");
        withRegistryLock(() -> {
            var root = readRoot();
            var instances = mutableInstances(root);
            instances.put(normalizedSide, toPayload(record));
            root.put("updatedAt", Instant.now().toString());
            root.put("projectPath", projectPath().toString());
            root.put("instances", instances);
            writeRoot(root);
            return null;
        });
    }

    public synchronized void remove(String side) {
        var normalizedSide = normalizeSide(side);
        if (!Files.exists(registryPath) && !Files.exists(lockPath())) {
            return;
        }
        withRegistryLock(() -> {
            removeSideEntry(normalizedSide);
            return null;
        });
    }

    public synchronized boolean removeIfSame(String side, GameInstanceRecord expected) {
        var normalizedSide = normalizeSide(side);
        Objects.requireNonNull(expected, "expected");
        if (!Files.exists(registryPath) && !Files.exists(lockPath())) {
            return false;
        }
        return withRegistryLock(() -> {
            var current = findInternal(normalizedSide);
            if (current.isEmpty() || !sameIdentity(current.get(), expected)) {
                return false;
            }
            removeSideEntry(normalizedSide);
            return true;
        });
    }

    public synchronized Optional<GameInstanceRecord> find(String side) {
        var normalizedSide = normalizeSide(side);
        if (!Files.exists(registryPath) && !Files.exists(lockPath())) {
            return Optional.empty();
        }
        return withRegistryLock(() -> findInternal(normalizedSide));
    }

    private String normalizeSide(String side) {
        if (side == null) {
            throw new IllegalArgumentException("side must not be null");
        }
        var trimmed = side.trim();
        if (!ALLOWED_SIDES.contains(trimmed)) {
            throw new IllegalArgumentException("side must be one of " + ALLOWED_SIDES);
        }
        return trimmed;
    }

    private Path projectPath() {
        if (!"game-instances.json".equals(registryPath.getFileName().toString())) {
            throw new IllegalStateException("registryPath must end with build/moddevmcp/game-instances.json");
        }
        var moddevmcpDir = registryPath.getParent();
        if (moddevmcpDir == null || !"moddevmcp".equals(moddevmcpDir.getFileName().toString())) {
            throw new IllegalStateException("registryPath must end with build/moddevmcp/game-instances.json");
        }
        var buildDir = moddevmcpDir.getParent();
        if (buildDir == null || !"build".equals(buildDir.getFileName().toString())) {
            throw new IllegalStateException("registryPath must end with build/moddevmcp/game-instances.json");
        }
        var projectRoot = buildDir.getParent();
        if (projectRoot == null) {
            throw new IllegalStateException("registryPath must end with build/moddevmcp/game-instances.json");
        }
        return projectRoot.toAbsolutePath().normalize();
    }

    private Path lockPath() {
        return registryPath.resolveSibling("game-instances.lock");
    }

    private Map<String, Object> readRoot() {
        if (!Files.exists(registryPath)) {
            return baseRoot();
        }
        try {
            var bytes = Files.readAllBytes(registryPath);
            return new LinkedHashMap<>(jsonCodec.parseObject(bytes));
        } catch (IOException exception) {
            throw new IllegalStateException("failed to read game instance registry: " + registryPath, exception);
        }
    }

    private Map<String, Object> baseRoot() {
        var root = new LinkedHashMap<String, Object>();
        root.put("projectPath", projectPath().toString());
        root.put("updatedAt", Instant.now().toString());
        root.put("instances", new LinkedHashMap<String, Object>());
        return root;
    }

    private Optional<GameInstanceRecord> findInternal(String normalizedSide) {
        if (!Files.exists(registryPath)) {
            return Optional.empty();
        }
        var root = readRoot();
        var instances = root.get("instances");
        if (!(instances instanceof Map<?, ?> instancesMap)) {
            return Optional.empty();
        }
        var entry = instancesMap.get(normalizedSide);
        if (!(entry instanceof Map<?, ?> entryMap)) {
            return Optional.empty();
        }
        try {
            return Optional.of(fromPayload(entryMap));
        } catch (RuntimeException ignored) {
            return Optional.empty();
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> mutableInstances(Map<String, Object> root) {
        var existing = root.get("instances");
        if (!(existing instanceof Map<?, ?> existingMap)) {
            return new LinkedHashMap<>();
        }
        var copied = new LinkedHashMap<String, Object>();
        for (var entry : existingMap.entrySet()) {
            if (entry.getKey() instanceof String key) {
                copied.put(key, entry.getValue());
            }
        }
        return copied;
    }

    private static Map<String, Object> toPayload(GameInstanceRecord record) {
        var payload = new LinkedHashMap<String, Object>();
        payload.put("baseUrl", record.baseUrl());
        payload.put("port", record.port());
        payload.put("pid", record.pid());
        payload.put("startedAt", record.startedAt().toString());
        payload.put("lastSeen", record.lastSeen().toString());
        return payload;
    }

    private static boolean sameIdentity(GameInstanceRecord left, GameInstanceRecord right) {
        return left.pid() == right.pid()
                && left.port() == right.port()
                && left.baseUrl().equals(right.baseUrl())
                && left.startedAt().equals(right.startedAt());
    }

    private static GameInstanceRecord fromPayload(Map<?, ?> payload) {
        var baseUrl = requiredString(payload, "baseUrl");
        var port = requiredNumber(payload, "port").intValue();
        var pid = requiredNumber(payload, "pid").longValue();
        var startedAt = requiredInstant(payload, "startedAt");
        var lastSeen = requiredInstant(payload, "lastSeen");
        return new GameInstanceRecord(baseUrl, port, pid, startedAt, lastSeen);
    }

    private static String requiredString(Map<?, ?> source, String key) {
        var value = source.get(key);
        if (!(value instanceof String stringValue) || stringValue.isBlank()) {
            throw new IllegalStateException("registry entry missing non-blank string field: " + key);
        }
        return stringValue;
    }

    private static Number requiredNumber(Map<?, ?> source, String key) {
        var value = source.get(key);
        if (!(value instanceof Number numberValue)) {
            throw new IllegalStateException("registry entry missing numeric field: " + key);
        }
        return numberValue;
    }

    private static Instant requiredInstant(Map<?, ?> source, String key) {
        var text = requiredString(source, key);
        try {
            return Instant.parse(text);
        } catch (DateTimeParseException exception) {
            throw new IllegalStateException("registry entry has invalid instant field: " + key, exception);
        }
    }

    private void writeRoot(Map<String, Object> root) {
        var parent = registryPath.getParent();
        if (parent == null) {
            throw new IllegalStateException("registryPath must include parent directory");
        }
        try {
            Files.createDirectories(parent);
            var temp = Files.createTempFile(parent, "game-instances-", ".tmp");
            try {
                Files.writeString(temp, jsonCodec.writeString(root), StandardCharsets.UTF_8);
                try {
                    Files.move(temp, registryPath, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
                } catch (AtomicMoveNotSupportedException ignored) {
                    Files.move(temp, registryPath, StandardCopyOption.REPLACE_EXISTING);
                }
            } finally {
                Files.deleteIfExists(temp);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("failed to write game instance registry: " + registryPath, exception);
        }
    }

    private void removeSideEntry(String normalizedSide) {
        if (!Files.exists(registryPath)) {
            return;
        }
        var root = readRoot();
        var instances = mutableInstances(root);
        instances.remove(normalizedSide);
        if (instances.isEmpty()) {
            try {
                Files.deleteIfExists(registryPath);
                return;
            } catch (IOException exception) {
                throw new IllegalStateException("failed to delete game instance registry: " + registryPath, exception);
            }
        }
        root.put("updatedAt", Instant.now().toString());
        root.put("projectPath", projectPath().toString());
        root.put("instances", instances);
        writeRoot(root);
    }

    private <T> T withRegistryLock(LockedOperation<T> operation) {
        var parent = registryPath.getParent();
        if (parent == null) {
            throw new IllegalStateException("registryPath must include parent directory");
        }
        try {
            Files.createDirectories(parent);
            try (FileChannel channel = FileChannel.open(lockPath(), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
                 FileLock ignored = channel.lock()) {
                return operation.run();
            }
        } catch (IOException exception) {
            throw new IllegalStateException("failed to lock game instance registry: " + registryPath, exception);
        }
    }

    @FunctionalInterface
    private interface LockedOperation<T> {
        T run();
    }
}

