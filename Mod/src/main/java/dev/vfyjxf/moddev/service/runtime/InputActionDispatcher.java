package dev.vfyjxf.moddev.service.runtime;

import dev.vfyjxf.moddev.api.model.OperationResult;
import dev.vfyjxf.moddev.runtime.RuntimeRegistries;

import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;

final class InputActionDispatcher {

    private final RuntimeRegistries registries;

    InputActionDispatcher(RuntimeRegistries registries) {
        this.registries = Objects.requireNonNull(registries, "registries");
    }

    DispatchResult dispatch(String action, Map<String, Object> arguments) {
        var rejectionReasons = new ArrayList<String>();
        var sawUnsupportedIntent = false;
        for (var controller : registries.inputControllers()) {
            OperationResult<Void> result;
            try {
                result = controller.perform(action, arguments);
            } catch (IllegalArgumentException exception) {
                return DispatchResult.rejected("invalid_input: " + exception.getMessage());
            }
            if (result.accepted()) {
                return DispatchResult.accepted(controller.getClass().getName(), result.performed());
            }
            if ("unsupported_intent".equals(result.reason())) {
                sawUnsupportedIntent = true;
                continue;
            }
            if (result.reason() != null && !result.reason().isBlank()) {
                rejectionReasons.add(controller.getClass().getName() + ": " + result.reason());
            }
        }
        if (sawUnsupportedIntent && rejectionReasons.isEmpty()) {
            return DispatchResult.rejected("unsupported_intent");
        }
        if (rejectionReasons.isEmpty()) {
            return DispatchResult.rejected("unsupported: no input controller accepted action " + action);
        }
        return DispatchResult.rejected("unsupported: no input controller accepted action " + action + " (" + String.join("; ", rejectionReasons) + ")");
    }

    record DispatchResult(boolean success, String error, String controller, boolean performed) {

        private static DispatchResult accepted(String controller, boolean performed) {
            return new DispatchResult(true, null, controller, performed);
        }

        private static DispatchResult rejected(String error) {
            return new DispatchResult(false, error, null, false);
        }

        Map<String, Object> payload(String action) {
            return Map.of(
                    "action", action,
                    "performed", performed,
                    "controller", controller
            );
        }
    }
}
