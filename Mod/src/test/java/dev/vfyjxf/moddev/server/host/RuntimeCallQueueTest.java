package dev.vfyjxf.moddev.server.host;

import dev.vfyjxf.moddev.server.api.McpToolDefinition;
import dev.vfyjxf.moddev.server.api.ToolResult;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class RuntimeCallQueueTest {

    @Test
    void serializesConcurrentRuntimeCalls() throws Exception {
        var registry = connectedRegistry();
        var tool = dynamicTool("moddev.ui.inspect");
        var startedSecond = new CountDownLatch(1);
        var releaseFirst = new CountDownLatch(1);
        var events = new CopyOnWriteArrayList<String>();
        try (var scheduler = new RuntimeCallQueue(registry, (session, descriptor, arguments) -> {
            var call = String.valueOf(arguments.get("call"));
            events.add("start:" + call);
            if ("first".equals(call)) {
                assertTrue(releaseFirst.await(5, TimeUnit.SECONDS));
            } else {
                startedSecond.countDown();
            }
            events.add("finish:" + call);
            return ToolResult.success(Map.of("call", call));
        })) {
            var first = CompletableFuture.supplyAsync(() -> scheduler.call(tool, Map.of("call", "first")));
            Thread.sleep(150L);
            var second = CompletableFuture.supplyAsync(() -> scheduler.call(tool, Map.of("call", "second")));
            Thread.sleep(150L);
            assertEquals(1, scheduler.queueDepth());
            assertEquals(1L, startedSecond.getCount());

            releaseFirst.countDown();

            assertTrue(first.get(5, TimeUnit.SECONDS).success());
            assertTrue(second.get(5, TimeUnit.SECONDS).success());
            assertEquals(List.of("start:first", "finish:first", "start:second", "finish:second"), events);
        }
    }

    @Test
    void returnsExplicitFailureWhenGameIsNotConnected() throws Exception {
        try (var scheduler = new RuntimeCallQueue(new RuntimeRegistry(), (session, descriptor, arguments) -> ToolResult.success(Map.of()))) {
            var result = scheduler.call(dynamicTool("moddev.ui.inspect"), Map.of());
            assertFalse(result.success());
            assertEquals("game_not_connected", result.error());
        }
    }

    @Test
    void disconnectFailsQueuedCallsExplicitly() throws Exception {
        var registry = connectedRegistry();
        var tool = dynamicTool("moddev.ui.inspect");
        var releaseFirst = new CountDownLatch(1);
        var startedSecond = new CountDownLatch(1);
        try (var scheduler = new RuntimeCallQueue(registry, (session, descriptor, arguments) -> {
            var call = String.valueOf(arguments.get("call"));
            if ("first".equals(call)) {
                assertTrue(releaseFirst.await(5, TimeUnit.SECONDS));
                return ToolResult.success(Map.of("call", call));
            }
            startedSecond.countDown();
            return ToolResult.success(Map.of("call", call));
        })) {
            var first = CompletableFuture.supplyAsync(() -> scheduler.call(tool, Map.of("call", "first")));
            Thread.sleep(150L);
            var second = CompletableFuture.supplyAsync(() -> scheduler.call(tool, Map.of("call", "second")));
            Thread.sleep(150L);

            registry.disconnect("runtime-1");
            scheduler.onRuntimeDisconnected("runtime-1");

            var secondResult = second.get(5, TimeUnit.SECONDS);
            assertFalse(secondResult.success());
            assertEquals("game_disconnected", secondResult.error());
            assertEquals(1L, startedSecond.getCount());

            releaseFirst.countDown();
            assertTrue(first.get(5, TimeUnit.SECONDS).success());
        }
    }

    private RuntimeRegistry connectedRegistry() {
        var registry = new RuntimeRegistry();
        registry.connect(
                new RuntimeSession("runtime-1", "client", List.of("common", "client"), List.of("client"), Map.of()),
                List.of(dynamicTool("moddev.ui.inspect"))
        );
        return registry;
    }

    private RuntimeToolDescriptor dynamicTool(String name) {
        return new RuntimeToolDescriptor(
                new McpToolDefinition(
                        name,
                        "Inspect UI",
                        "Dynamic runtime tool",
                        Map.of("type", "object"),
                        Map.of("type", "object"),
                        List.of("ui"),
                        "client",
                        false,
                        false,
                        "runtime",
                        "runtime"
                ),
                "client",
                "client",
                true,
                false
        );
    }
}


