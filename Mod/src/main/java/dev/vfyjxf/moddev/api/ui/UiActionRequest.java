package dev.vfyjxf.moddev.api.ui;

import java.util.Map;

public record UiActionRequest(
        TargetSelector target,
        String action,
        Map<String, Object> arguments
) {
}

