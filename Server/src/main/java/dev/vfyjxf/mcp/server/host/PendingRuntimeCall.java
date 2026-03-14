package dev.vfyjxf.mcp.server.host;

import dev.vfyjxf.mcp.server.api.ToolResult;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

final class PendingRuntimeCall {

    private final String runtimeId;
    private final RuntimeToolDescriptor descriptor;
    private final Map<String, Object> arguments;
    private final CompletableFuture<ToolResult> future;
    private final AtomicBoolean finished;

    PendingRuntimeCall(String runtimeId, RuntimeToolDescriptor descriptor, Map<String, Object> arguments) {
        this.runtimeId = runtimeId;
        this.descriptor = descriptor;
        this.arguments = Map.copyOf(arguments);
        this.future = new CompletableFuture<>();
        this.finished = new AtomicBoolean();
    }

    String runtimeId() {
        return runtimeId;
    }

    RuntimeToolDescriptor descriptor() {
        return descriptor;
    }

    Map<String, Object> arguments() {
        return arguments;
    }

    CompletableFuture<ToolResult> future() {
        return future;
    }

    boolean complete(ToolResult result) {
        if (!finished.compareAndSet(false, true)) {
            return false;
        }
        future.complete(result);
        return true;
    }

    boolean isFinished() {
        return future.isDone();
    }
}

