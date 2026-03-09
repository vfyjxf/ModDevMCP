package dev.vfyjxf.mcp.api.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OperationResultTest {

    @Test
    void operationResultKeepsAcceptedPerformedReasonAndValue() {
        var result = OperationResult.success("snapshot-1");

        assertTrue(result.accepted());
        assertTrue(result.performed());
        assertEquals("snapshot-1", result.value());
    }
}
