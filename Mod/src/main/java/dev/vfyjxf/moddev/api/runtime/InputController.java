package dev.vfyjxf.moddev.api.runtime;

import dev.vfyjxf.moddev.api.model.OperationResult;

import java.util.Map;

/**
 * Abstraction used to dispatch low-level input actions into the active client runtime.
 */
public interface InputController {

    /**
     * Executes the supplied input action.
     */
    OperationResult<Void> perform(String action, Map<String, Object> arguments);
}

