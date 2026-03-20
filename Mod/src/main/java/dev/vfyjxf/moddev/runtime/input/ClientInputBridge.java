package dev.vfyjxf.moddev.runtime.input;

import dev.vfyjxf.moddev.api.model.OperationResult;
import dev.vfyjxf.moddev.api.runtime.ClientScreenProbe;

interface ClientInputBridge extends ClientScreenProbe {

    OperationResult<Void> execute(InputCommand command);
}

