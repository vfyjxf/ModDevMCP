package dev.vfyjxf.mcp.runtime.input;

import dev.vfyjxf.mcp.api.model.OperationResult;
import dev.vfyjxf.mcp.api.runtime.InputController;

import java.util.Map;

public final class MinecraftInputController implements InputController {

    @Override
    public OperationResult<Void> perform(String action, Map<String, Object> arguments) {
        return OperationResult.success(null);
    }
}
