package dev.vfyjxf.mcp.runtime.tool;

import dev.vfyjxf.mcp.api.model.OperationResult;
import dev.vfyjxf.mcp.api.ui.UiSnapshot;
import dev.vfyjxf.mcp.api.ui.UiTarget;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public final class UiAutomationSessionManager {

    private final Map<String, SessionState> sessions = new ConcurrentHashMap<>();

    public UiAutomationSession open(UiSnapshot snapshot) {
        var sessionId = UUID.randomUUID().toString();
        var state = new SessionState(sessionId, snapshot, new LinkedHashMap<>(), new AtomicLong(0L), false);
        state.rebuildFor(snapshot, false);
        sessions.put(sessionId, state);
        return state.view();
    }

    public Optional<UiAutomationSession> find(String sessionId) {
        var state = sessions.get(sessionId);
        return state == null ? Optional.empty() : Optional.of(state.view());
    }

    public Optional<RefreshResult> refresh(String sessionId, UiSnapshot snapshot) {
        var state = sessions.get(sessionId);
        if (state == null) {
            return Optional.empty();
        }
        var previous = state.snapshot();
        var screenChanged = !sameScreen(previous, snapshot);
        state.rebuildFor(snapshot, screenChanged);
        return Optional.of(new RefreshResult(state.view(), screenChanged));
    }

    public OperationResult<UiTarget> resolveTarget(String sessionId, String refId) {
        var state = sessions.get(sessionId);
        if (state == null) {
            return OperationResult.rejected("session_not_found");
        }
        if (state.stale()) {
            return OperationResult.rejected("session_stale");
        }
        var refState = state.refsById().get(refId);
        if (refState == null) {
            return OperationResult.rejected("target_not_found");
        }
        if (refState.stale()) {
            return OperationResult.rejected("target_stale");
        }
        return state.snapshot().targets().stream()
                .filter(target -> target.targetId().equals(refState.ref().targetId()))
                .findFirst()
                .map(OperationResult::success)
                .orElseGet(() -> {
                    refState.markStale();
                    return OperationResult.rejected("target_stale");
                });
    }

    public boolean markStale(String sessionId) {
        var state = sessions.get(sessionId);
        if (state == null) {
            return false;
        }
        state.markStale();
        return true;
    }

    public boolean recordTrace(String sessionId, String type, long elapsedMs, boolean success, String errorCode, String errorMessage) {
        var state = sessions.get(sessionId);
        if (state == null) {
            return false;
        }
        state.appendTrace(type, elapsedMs, success, errorCode, errorMessage);
        return true;
    }

    public Optional<List<UiAutomationTraceEntry>> trace(String sessionId) {
        var state = sessions.get(sessionId);
        return state == null ? Optional.empty() : Optional.of(state.trace());
    }

    private boolean sameScreen(UiSnapshot left, UiSnapshot right) {
        return java.util.Objects.equals(left.screenId(), right.screenId())
                && java.util.Objects.equals(left.screenClass(), right.screenClass());
    }

    public record RefreshResult(
            UiAutomationSession session,
            boolean screenChanged
    ) {
    }

    private static final class SessionState {

        private final String sessionId;
        private final Map<String, RefState> refsById;
        private final AtomicLong refSequence;
        private final List<UiAutomationTraceEntry> trace = new java.util.ArrayList<>();
        private volatile UiSnapshot snapshot;
        private volatile boolean stale;

        private SessionState(
                String sessionId,
                UiSnapshot snapshot,
                Map<String, RefState> refsById,
                AtomicLong refSequence,
                boolean stale
        ) {
            this.sessionId = sessionId;
            this.snapshot = snapshot;
            this.refsById = refsById;
            this.refSequence = refSequence;
            this.stale = stale;
        }

        private synchronized void rebuildFor(UiSnapshot snapshot, boolean invalidateExistingRefs) {
            if (invalidateExistingRefs) {
                refsById.values().forEach(RefState::markStale);
            }
            var activeTargetIds = snapshot.targets().stream()
                    .map(UiTarget::targetId)
                    .collect(java.util.stream.Collectors.toSet());
            refsById.values().stream()
                    .filter(refState -> !refState.stale() && !activeTargetIds.contains(refState.ref().targetId()))
                    .forEach(RefState::markStale);
            for (var target : snapshot.targets()) {
                var existing = refsById.values().stream()
                        .filter(refState -> !refState.stale() && refState.ref().targetId().equals(target.targetId()))
                        .findFirst()
                        .orElse(null);
                if (existing == null) {
                    var refId = "ref-" + refSequence.incrementAndGet();
                    refsById.put(refId, new RefState(new UiAutomationRef(refId, target.targetId(), snapshot.screenId())));
                }
            }
            this.snapshot = snapshot;
            this.stale = false;
        }

        private UiSnapshot snapshot() {
            return snapshot;
        }

        private Map<String, RefState> refsById() {
            return refsById;
        }

        private boolean stale() {
            return stale;
        }

        private void markStale() {
            stale = true;
        }

        private UiAutomationSession view() {
            List<UiAutomationRef> refs = refsById.values().stream()
                    .filter(refState -> !refState.stale())
                    .map(RefState::ref)
                    .toList();
            return new UiAutomationSession(sessionId, snapshot, refs, stale);
        }

        private synchronized void appendTrace(String type, long elapsedMs, boolean success, String errorCode, String errorMessage) {
            trace.add(new UiAutomationTraceEntry(
                    trace.size(),
                    type,
                    elapsedMs,
                    success,
                    errorCode,
                    errorMessage
            ));
        }

        private synchronized List<UiAutomationTraceEntry> trace() {
            return List.copyOf(trace);
        }
    }

    private static final class RefState {

        private final UiAutomationRef ref;
        private volatile boolean stale;

        private RefState(UiAutomationRef ref) {
            this.ref = ref;
        }

        private UiAutomationRef ref() {
            return ref;
        }

        private boolean stale() {
            return stale;
        }

        private void markStale() {
            stale = true;
        }
    }
}
