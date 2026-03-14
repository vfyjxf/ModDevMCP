package dev.vfyjxf.mcp.server.host;

import dev.vfyjxf.mcp.server.api.ToolResult;

import java.util.Map;

@FunctionalInterface
public interface RuntimeInvoker {

    RuntimeInvoker UNAVAILABLE = (session, descriptor, arguments) -> ToolResult.failure("runtime_dispatch_unavailable");

    ToolResult invoke(RuntimeSession session, RuntimeToolDescriptor descriptor, Map<String, Object> arguments) throws Exception;
}

