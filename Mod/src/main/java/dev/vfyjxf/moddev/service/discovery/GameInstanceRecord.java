package dev.vfyjxf.moddev.service.discovery;

import java.time.Instant;
import java.util.Objects;

public record GameInstanceRecord(
        String baseUrl,
        int port,
        long pid,
        Instant startedAt,
        Instant lastSeen
) {

    public GameInstanceRecord {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalArgumentException("baseUrl must not be blank");
        }
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("port must be between 1 and 65535");
        }
        if (pid < 0) {
            throw new IllegalArgumentException("pid must be non-negative");
        }
        startedAt = Objects.requireNonNull(startedAt, "startedAt");
        lastSeen = Objects.requireNonNull(lastSeen, "lastSeen");
    }
}

