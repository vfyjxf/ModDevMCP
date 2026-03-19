package dev.vfyjxf.moddev.server.transport;

import java.util.Map;
import java.util.Optional;

public interface JsonRpcRequestHandler {

    Optional<Map<String, Object>> handle(Map<String, Object> request);

    Map<String, Object> initializedNotification();
}

