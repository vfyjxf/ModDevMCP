package dev.vfyjxf.moddev.api.operation;

import java.util.Map;

@FunctionalInterface
public interface OperationExecutor {

    Map<String, Object> execute(Map<String, Object> input, String resolvedTargetSide) throws Exception;
}

