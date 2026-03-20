# Third-Party Operation Integration Guide

This guide describes how another mod can extend ModDevMCP with operation registrars and runtime adapters for the HTTP + skill architecture.

## 1. Extension Points

Use registrar interfaces (not tool providers):

- `CommonOperationRegistrar`
- `ClientOperationRegistrar`
- `ServerOperationRegistrar`

Use registrar annotations for discovery:

- `@CommonRegistrar`
- `@ClientRegistrar`
- `@ServerRegistrar`

## 2. Register Operations

```java
package example.moddev;

import dev.vfyjxf.moddev.api.event.RegisterCommonOperationsEvent;
import dev.vfyjxf.moddev.api.operation.OperationExecutor;
import dev.vfyjxf.moddev.api.registrar.CommonOperationRegistrar;
import dev.vfyjxf.moddev.api.registrar.CommonRegistrar;
import dev.vfyjxf.moddev.service.operation.OperationDefinition;

import java.util.Map;
import java.util.Set;

@CommonRegistrar
public final class ExampleCommonRegistrar implements CommonOperationRegistrar {
    @Override
    public void register(RegisterCommonOperationsEvent event) {
        event.registerOperation(
                new OperationDefinition(
                        "example.ping",
                        "example",
                        "Ping",
                        "Returns a simple heartbeat payload.",
                        false,
                        Set.of(),
                        Map.of("type", "object"),
                        Map.of("operationId", "example.ping", "input", Map.of())
                ),
                executor("ok")
        );
    }

    private static OperationExecutor executor(String value) {
        return (input, targetSide) -> Map.of("status", value);
    }
}
```

## 3. Register Runtime Adapters (Client Example)

```java
package example.moddev;

import dev.vfyjxf.moddev.api.event.RegisterClientOperationsEvent;
import dev.vfyjxf.moddev.api.registrar.ClientOperationRegistrar;
import dev.vfyjxf.moddev.api.registrar.ClientRegistrar;

@ClientRegistrar
public final class ExampleClientRegistrar implements ClientOperationRegistrar {
    @Override
    public void register(RegisterClientOperationsEvent event) {
        event.registerUiDriver(new ExampleUiDriver());
        event.registerInputController(new ExampleInputController());
    }
}
```

## 4. UI Filtering Contract

When your driver participates in UI flows, keep request filtering compatible with shared fields:

- `includeDrivers`
- `excludeDrivers`

Both fields are consumed by operation-side UI queries and actions, so your driver should expose stable `driverId` values.

## 5. Runtime Request Flow

Active execution path:

1. HTTP client calls `POST /api/v1/requests`
2. service resolves `targetSide`
3. operation executor runs in runtime service registry
4. response is returned as operation output/error

No legacy stdio tool flow is used.
