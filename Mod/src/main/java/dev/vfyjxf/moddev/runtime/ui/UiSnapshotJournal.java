package dev.vfyjxf.moddev.runtime.ui;

import dev.vfyjxf.moddev.api.runtime.UiContext;
import dev.vfyjxf.moddev.api.ui.UiSnapshot;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public final class UiSnapshotJournal {

    private final AtomicLong sequence = new AtomicLong(1);
    private final Map<String, UiSnapshot> snapshots = new ConcurrentHashMap<>();
    private final Map<UiSnapshotKey, String> latestRefs = new ConcurrentHashMap<>();

    public String record(UiContext context, UiSnapshot snapshot) {
        var ref = "ui-" + sequence.getAndIncrement();
        snapshots.put(ref, snapshot);
        latestRefs.put(new UiSnapshotKey(snapshot.driverId(), context.screenClass(), context.modId()), ref);
        return ref;
    }

    public UiSnapshot latest(UiContext context, String driverId) {
        var ref = latestRefs.get(new UiSnapshotKey(driverId, context.screenClass(), context.modId()));
        return ref == null ? null : snapshots.get(ref);
    }

    private record UiSnapshotKey(
            String driverId,
            String screenClass,
            String modId
    ) {
    }
}

