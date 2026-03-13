package dev.vfyjxf.mcp.runtime.input;

import dev.vfyjxf.mcp.api.model.OperationResult;
import dev.vfyjxf.mcp.api.runtime.ClientScreenProbe;

interface ClientInputBridge extends ClientScreenProbe {

    OperationResult<Void> execute(InputCommand command);
}
