package dev.vfyjxf.mcp.server.host;

import dev.vfyjxf.mcp.server.api.ToolResult;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

public final class RuntimeCallQueue implements AutoCloseable {

    private final RuntimeRegistry runtimeRegistry;
    private final ExecutorService executor;
    private final Set<PendingRuntimeCall> pendingCalls;
    private final AtomicReference<PendingRuntimeCall> activeCall;
    private final Duration callTimeout;
    private volatile RuntimeInvoker invoker;

    public RuntimeCallQueue(RuntimeRegistry runtimeRegistry) {
        this(runtimeRegistry, RuntimeInvoker.UNAVAILABLE);
    }

    public RuntimeCallQueue(RuntimeRegistry runtimeRegistry, RuntimeInvoker invoker) {
        this(runtimeRegistry, invoker, Duration.ofSeconds(10));
    }

    RuntimeCallQueue(RuntimeRegistry runtimeRegistry, RuntimeInvoker invoker, Duration callTimeout) {
        this.runtimeRegistry = Objects.requireNonNull(runtimeRegistry, "runtimeRegistry");
        this.invoker = Objects.requireNonNull(invoker, "invoker");
        this.callTimeout = Objects.requireNonNull(callTimeout, "callTimeout");
        this.pendingCalls = ConcurrentHashMap.newKeySet();
        this.activeCall = new AtomicReference<>();
        this.executor = Executors.newSingleThreadExecutor(new SchedulerThreadFactory());
    }

    public ToolResult call(RuntimeToolDescriptor descriptor, Map<String, Object> arguments) {
        Objects.requireNonNull(descriptor, "descriptor");
        Objects.requireNonNull(arguments, "arguments");
        var session = runtimeRegistry.activeSession().orElse(null);
        if (session == null || !runtimeRegistry.state().gameConnected()) {
            return ToolResult.failure("game_not_connected");
        }
        return call(session, descriptor, arguments);
    }

    public ToolResult call(RuntimeSession session, RuntimeToolDescriptor descriptor, Map<String, Object> arguments) {
        Objects.requireNonNull(session, "session");
        Objects.requireNonNull(descriptor, "descriptor");
        Objects.requireNonNull(arguments, "arguments");
        if (!runtimeRegistry.state().gameConnected() || runtimeRegistry.findSession(session.runtimeId()).isEmpty()) {
            return ToolResult.failure("game_not_connected");
        }
        var pending = new PendingRuntimeCall(session.runtimeId(), descriptor, arguments);
        pendingCalls.add(pending);
        executor.execute(() -> executePending(session, pending));
        try {
            return pending.future().get(callTimeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException exception) {
            completePending(pending, ToolResult.failure("game_call_timeout"));
            return pending.future().getNow(ToolResult.failure("game_call_timeout"));
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            completePending(pending, ToolResult.failure("game_call_timeout"));
            return pending.future().getNow(ToolResult.failure("game_call_timeout"));
        } catch (java.util.concurrent.ExecutionException exception) {
            completePending(pending, ToolResult.failure("runtime_protocol_error"));
            return pending.future().getNow(ToolResult.failure("runtime_protocol_error"));
        }
    }

    public int queueDepth() {
        var depth = pendingCalls.size();
        var active = activeCall.get();
        if (active != null && pendingCalls.contains(active)) {
            depth -= 1;
        }
        return Math.max(depth, 0);
    }

    public void setInvoker(RuntimeInvoker invoker) {
        this.invoker = Objects.requireNonNull(invoker, "invoker");
    }

    public void onRuntimeDisconnected(String runtimeId) {
        var active = activeCall.get();
        pendingCalls.stream()
                .filter(pending -> pending.runtimeId().equals(runtimeId))
                .filter(pending -> pending != active)
                .forEach(pending -> completePending(pending, ToolResult.failure("game_disconnected")));
    }

    @Override
    public void close() {
        executor.shutdownNow();
    }

    private void executePending(RuntimeSession expectedSession, PendingRuntimeCall pending) {
        if (pending.isFinished()) {
            return;
        }
        activeCall.set(pending);
        try {
            if (pending.isFinished()) {
                return;
            }
            var session = runtimeRegistry.findSession(expectedSession.runtimeId()).orElse(null);
            if (session == null || !runtimeRegistry.state().gameConnected()) {
                completePending(pending, ToolResult.failure("game_disconnected"));
                return;
            }
            var result = invoker.invoke(session, pending.descriptor(), pending.arguments());
            completePending(pending, result == null ? ToolResult.failure("runtime_protocol_error") : result);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            completePending(pending, ToolResult.failure("game_disconnected"));
        } catch (Exception exception) {
            completePending(pending, ToolResult.failure("runtime_protocol_error"));
        } finally {
            activeCall.compareAndSet(pending, null);
        }
    }

    private void completePending(PendingRuntimeCall pending, ToolResult result) {
        if (pending.complete(result)) {
            pendingCalls.remove(pending);
        }
    }

    private static final class SchedulerThreadFactory implements ThreadFactory {

        @Override
        public Thread newThread(Runnable runnable) {
            var thread = new Thread(runnable, "moddev-host-call-queue");
            thread.setDaemon(true);
            return thread;
        }
    }
}
