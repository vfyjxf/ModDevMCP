package dev.vfyjxf.mcp.api.runtime;

import dev.vfyjxf.mcp.api.model.OperationResult;

import java.util.Map;

public interface InputController {

    OperationResult<Void> perform(String action, Map<String, Object> arguments);
}
