package dev.vfyjxf.mcp.service.request;

import java.util.LinkedHashMap;
import java.util.Map;

public record OperationResponse(
        String requestId,
        String operationId,
        String targetSide,
        String status,
        Map<String, Object> output,
        String errorCode,
        String errorMessage
) {

    public static OperationResponse success(
            String requestId,
            String operationId,
            String targetSide,
            Map<String, Object> output
    ) {
        return new OperationResponse(
                requestId,
                operationId,
                targetSide,
                "ok",
                output == null ? Map.of() : JsonValueNormalizer.freezeObject(output, "output"),
                null,
                null
        );
    }

    public static OperationResponse error(
            String requestId,
            String operationId,
            String targetSide,
            OperationError error
    ) {
        return new OperationResponse(
                requestId,
                operationId,
                targetSide,
                "error",
                Map.of(),
                error.errorCode(),
                error.errorMessage()
        );
    }

    public Map<String, Object> toPayload() {
        var payload = new LinkedHashMap<String, Object>();
        payload.put("requestId", requestId);
        payload.put("operationId", operationId);
        payload.put("targetSide", targetSide);
        payload.put("status", status);
        payload.put("output", output);
        payload.put("errorCode", errorCode);
        payload.put("errorMessage", errorMessage);
        return payload;
    }
}
