# 第三方 Operation 集成指南

本文说明第三方模组如何在 ModDevMCP 的 HTTP + skill 架构下扩展 operation registrar 与 runtime adapter。

## 1. 扩展入口

使用 registrar 接口（不再使用 tool provider）：

- `CommonOperationRegistrar`
- `ClientOperationRegistrar`
- `ServerOperationRegistrar`

使用注解完成发现：

- `@CommonRegistrar`
- `@ClientRegistrar`
- `@ServerRegistrar`

## 2. 注册 Operation

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
                        "返回简单心跳信息。",
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

## 3. 注册 Runtime Adapter（客户端示例）

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

## 4. UI 过滤约定

当你的 driver 参与 UI 流程时，需要兼容以下过滤字段：

- `includeDrivers`
- `excludeDrivers`

这两个字段会在 operation 侧 UI 查询/动作流程中使用，因此应保证 `driverId` 稳定可识别。

## 5. 请求执行路径

当前有效执行链路：

1. HTTP 客户端调用 `POST /api/v1/requests`
2. 服务解析 `targetSide`
3. 在 runtime service registry 中执行 operation executor
4. 返回 operation 输出或错误

不再走旧的 stdio tool 流程。
