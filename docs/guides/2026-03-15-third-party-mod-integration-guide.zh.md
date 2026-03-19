# 2026-03-15 第三方 Mod 集成指南

Date: 2026-03-15 17:10 CST
Updated: 2026-03-16 00:55 CST

## 目的

- 说明其他 NeoForge mod 应该如何扩展 ModDevMCP
- 区分稳定扩展点和当前仍偏内部的 runtime API
- 讲清楚 side 边界，避免 client-only 代码泄漏到 dedicated server 路径

## 适用对象

如果你维护的是另一个 mod，并且希望 ModDevMCP 更好地理解这个 mod，就看这篇文档。

典型场景：

- 暴露新的 runtime tools
- 把 tool 调用路由到你自己 mod 的 client 或 server 逻辑
- 为自定义 screen / UI 框架补充更丰富的 UI inspect 或 capture 支持

## 先选扩展路径

先按这个规则判断：

- 如果你是要暴露新的 runtime tool，用 tool registrar
- 如果你是要让 ModDevMCP 现有的 UI、input、capture 工具直接理解你的 runtime 对象，用 runtime adapter

当前推荐：

- 大多数下游 mod，先从 tool registrar 开始
- 只有在你确实要接入 `ui_snapshot`、`ui_query`、`ui_capture`、`ui_action` 等现有 UI 工具时，再去做 runtime adapter

## 消费方工程布局

如果你的 mod 是在一个 Gradle 多工程里消费 ModDevMCP：

- `projectRoot` 继续指向真正的 mod 子工程目录
- `gradleRoot` 指向真正持有 `gradlew(.bat)` 和 `settings.gradle` 的目录
- `classOutputDir` 继续指向 mod 子工程自己的输出目录，通常是 `build/classes/java/main`
- `compileTask` 继续使用子工程任务路径，例如 `:my-mod:compileJava`

原因是 hotswap 编译必须从真正的 Gradle root 发起，但重载的 `.class` 文件仍然来自 mod 子工程。

## Side 模型

ModDevMCP 现在把 tool 注册拆成三类 side：

- `common`：两侧都安全
- `client`：只包含 client runtime 逻辑、screen、input、render、capture
- `server`：只包含 dedicated 或 integrated server runtime 逻辑

选 side 时尽量收窄：

- 只要类里碰到 `Minecraft`、screen 或 rendering API，就放 `client`
- 只要类里碰到 `MinecraftServer`、命令派发器或 server world 状态，就放 `server`
- 只有真正 side-neutral 的 provider 才放 `common`

## 首选路径：基于注解扫描的 Tool Registrar

当前稳定的一等扩展点是 registrar 模型。

可用注解：

- `@CommonMcpRegistrar`
- `@ClientMcpRegistrar`
- `@ServerMcpRegistrar`

对应接口：

- `CommonOperationRegistrar`
- `ClientOperationRegistrar`
- `ServerOperationRegistrar`

对应注册事件：

- `RegisterCommonOperationsEvent`
- `RegisterClientOperationsEvent`
- `RegisterServerOperationsEvent`

每个 registrar：

- 必须是具体类，并且有 public 无参构造
- 应该实现与注解匹配的 registrar 接口
- 只能引用该 side 上合法的类

现在每个 side event 还会直接暴露：

- `register(...)`、`registerToolProvider(...)` 这类 tool 注册 helper
- `api()`，这样 registrar 内不需要直接拿 `ModDevMCP` 实例也能访问 `ModMcpApi`
- `eventPublisher()` 和 `publishEvent(...)`

其中 client event 还会再暴露这些 runtime adapter helper：

- `registerUiDriver(...)`
- `registerInputController(...)`
- `registerUiInteractionStateResolver(...)`
- `registerUiOffscreenCaptureProvider(...)`
- `registerUiFramebufferCaptureProvider(...)`

### 最小 Common Tool 示例

```java
package com.example.examplemod.moddev;

import dev.vfyjxf.moddev.api.event.RegisterCommonOperationsEvent;
import dev.vfyjxf.moddev.api.registrar.CommonMcpRegistrar;
import dev.vfyjxf.moddev.api.registrar.CommonOperationRegistrar;
import dev.vfyjxf.moddev.server.api.McpToolDefinition;
import dev.vfyjxf.moddev.server.api.McpToolProvider;
import dev.vfyjxf.moddev.server.api.ToolResult;
import dev.vfyjxf.moddev.server.runtime.McpToolRegistry;

import java.util.List;
import java.util.Map;

@CommonMcpRegistrar
public final class ExampleCommonRegistrar implements CommonOperationRegistrar {

    @Override
    public void register(RegisterCommonOperationsEvent event) {
        event.registerToolProvider(new ExampleToolProvider());
        event.publishEvent(new EventEnvelope("examplemod", "common-registered", System.currentTimeMillis(), Map.of()));
    }

    private static final class ExampleToolProvider implements McpToolProvider {
        @Override
        public void register(McpToolRegistry registry) {
            registry.registerTool(
                    new McpToolDefinition(
                            "examplemod.ping",
                            "Example Ping",
                            "Returns a simple payload from Example Mod.",
                            Map.of("type", "object"),
                            Map.of("type", "object"),
                            List.of("example"),
                            "common",
                            false,
                            false,
                            "public",
                            "public"
                    ),
                    (context, arguments) -> ToolResult.success(Map.of(
                            "ok", true,
                            "mod", "examplemod",
                            "runtimeSide", context.side()
                    ))
            );
        }
    }
}
```

### 最小 Client Tool 示例

```java
package com.example.examplemod.moddev;

import dev.vfyjxf.moddev.api.event.RegisterClientOperationsEvent;
import dev.vfyjxf.moddev.api.registrar.ClientMcpRegistrar;
import dev.vfyjxf.moddev.api.registrar.ClientOperationRegistrar;

@ClientMcpRegistrar
public final class ExampleClientRegistrar implements ClientOperationRegistrar {

    @Override
    public void register(RegisterClientOperationsEvent event) {
        event.registerToolProvider(new ExampleClientToolProvider());
        event.registerUiDriver(new ExampleScreenUiDriver());
    }
}
```

`server` 侧也是同样模式，只是换成 `@ServerMcpRegistrar`。

## 命名和契约建议

给你的 mod 新增 tool 时：

- 用你自己的命名空间，例如 `examplemod.some_tool`
- schema 尽量窄，尽量 agent 友好
- 返回结构化 payload，不要直接丢日志
- 如果一个 tool 可能同时作用于 client 和 server runtime，side 路由要写清楚

如果这本质上是你 mod 的专属操作，优先加新 tool，而不是去挤现有的通用 ModDevMCP runtime tool。

## Runtime Adapter API

ModDevMCP 还通过 `ModMcpApi` 暴露了一组 runtime adapter 注册入口。

现在在 registrar 回调里也可以直接从 event 拿到这层 API：

```java
event.api().registerToolProvider(new ExampleToolProvider());
event.registerUiDriver(new ExampleScreenUiDriver());
event.publishEvent(new EventEnvelope("examplemod", "registered", System.currentTimeMillis(), Map.of()));
```

当前公开方法包括：

- `registerUiDriver(UiDriver driver)`
- `registerInputController(InputController controller)`
- `registerToolProvider(McpToolProvider provider)`
- `registerUiInteractionStateResolver(UiInteractionStateResolver resolver)`
- `registerUiOffscreenCaptureProvider(UiOffscreenCaptureProvider provider)`
- `registerUiFramebufferCaptureProvider(UiFramebufferCaptureProvider provider)`

当你希望直接接入 ModDevMCP 内置的 UI / capture 工具，而不是另写一套独立 tool 时，这层 API 才真正有价值。

如果同一个 live screen 上可能同时激活多个 UI driver，内置只读 UI 工具现在可以用这些参数收窄结果：

- `driverId`
- `includeDrivers`
- `excludeDrivers`

下游 adapter 也应该预期 `status.live_screen (via POST /api/v1/requests)` 会返回 `drivers[]`，而 `driverId` 现在表示默认或推荐 driver，不再意味着它是唯一匹配。

### 最小 `UiDriver` 形状

```java
public final class ExampleScreenUiDriver implements UiDriver {

    @Override
    public DriverDescriptor descriptor() {
        return new DriverDescriptor("examplemod:screen", "examplemod", 200, Set.of("snapshot", "query"));
    }

    @Override
    public boolean matches(UiContext context) {
        return context.screen() instanceof ExampleScreen;
    }

    @Override
    public UiSnapshot snapshot(UiContext context, SnapshotOptions options) {
        // 在这里为你的 screen 构建稳定 target tree。
        throw new UnsupportedOperationException("example");
    }

    @Override
    public List<UiTarget> query(UiContext context, TargetSelector selector) {
        return snapshot(context, SnapshotOptions.DEFAULT).targets();
    }
}
```

### 最小 Capture Provider 形状

```java
public final class ExampleOffscreenCaptureProvider implements UiOffscreenCaptureProvider {

    @Override
    public String providerId() {
        return "examplemod:offscreen";
    }

    @Override
    public int priority() {
        return 200;
    }

    @Override
    public boolean matches(UiContext context, UiSnapshot snapshot) {
        return context.screen() instanceof ExampleScreen;
    }

    @Override
    public UiCaptureImage capture(
            UiContext context,
            UiSnapshot snapshot,
            CaptureRequest request,
            List<UiTarget> capturedTargets,
            List<UiTarget> excludedTargets
    ) {
        throw new UnsupportedOperationException("example");
    }
}
```

## 当前的重要限制

今天真正一等的第三方扩展路径还是 tool registrar。

虽然 registrar event 现在已经能直接暴露 runtime adapter API，但这并不代表所有 runtime 集成点都已经变成完全 side-agnostic，或者已经脱离 registrar 生命周期自动发现。当前仍然建议：

- 以 side-specific registrar 作为主入口
- mod 专属行为优先做成独立 tool
- 只有当内置 UI、input、capture 流程确实需要直接理解你的 mod 时，再补 runtime adapter

如果你的 mod 是完全外部的，而且你只是想提供给 agent 一些能力，优先走 registrar + 自定义 tool。

## 推荐集成顺序

建议按这个顺序做：

1. 先加 registrar 和一个聚焦的 tool provider，先把 mod 专属能力暴露出来
2. 在真实 服务会话里验证 tool contract
3. 只有在通用 UI 工具必须直接理解你的 screen 框架时，再补 `UiDriver` 或 capture provider

这样集成更小，也能避免过早把你的 mod 绑死在 ModDevMCP 内部细节上。

## Side 安全检查表

- 不要在 `common` 或 `server` registrar 里引用 client-only 类
- 不要把 dedicated-server 逻辑放到 `client` registrar 里
- 如果 tool 运行在 client 但会操作 server 状态，记得考虑 integrated server
- 被扫描的 registrar 类必须是 public 无参构造

## 验证流程

推荐下游验证顺序：

1. 引入发布好的 `dev.vfyjxf:moddevmcp` 依赖和 `dev.vfyjxf.moddevmcp` 插件
2. 用 `service discovery probes` 生成 agent client 配置
3. 启动 `runClient`
4. 连接 agent client，先调用 `GET /api/v1/status`
5. 调用你新增的 tool
6. 如果你还加了 UI adapter，再验证 `status.live_screen (via POST /api/v1/requests)`、`moddev.ui_snapshot`、`moddev.ui_capture`
7. 如果多个 driver 可以共存，再验证 `driverId`、`includeDrivers`、`excludeDrivers`
8. 如果你需要的是原始键盘或鼠标事件注入，改用 `moddev.input_action`，不要混进 UI 语义工具

如果 Gradle 解析失败，要把仓库、TLS、网络问题和代码问题分开处理。
