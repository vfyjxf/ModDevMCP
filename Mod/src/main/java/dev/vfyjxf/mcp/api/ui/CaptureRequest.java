package dev.vfyjxf.mcp.api.ui;

import java.util.List;

public record CaptureRequest(
        String mode,
        List<TargetSelector> target,
        List<TargetSelector> exclude,
        boolean withOverlays
) {
    public CaptureRequest {
        target = target == null ? List.of() : List.copyOf(target);
        exclude = exclude == null ? List.of() : List.copyOf(exclude);
    }
}
