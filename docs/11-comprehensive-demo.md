# Demo11：综合实战 — 任务型工作助手

> **导航**：[← 上一篇：Demo10 全能助理](./10-assistant.md) · [返回首页](./index.md) · [下一篇：Demo12 MCP 工具集成 →](./12-mcp-integration.md)
>
> **本章源码**：[Demo11Controller](../src/main/java/com/xb/agentscope/controller/demo11/Demo11Controller.java) · [TaskTools](../src/main/java/com/xb/agentscope/controller/demo11/TaskTools.java) · [Demo11TaskRequest](../src/main/java/com/xb/agentscope/controller/demo11/Demo11TaskRequest.java)

## 知识点

- 多工具聚合（跨 Demo 复用 `WeatherTools`、`AssistantTools`、新增 `TaskTools`）
- `MemoryConfig` + `CompactionConfig` 实现长期记忆与上下文压缩
- 多会话隔离（`ConcurrentHashMap` 按 sessionId 缓存 Agent）
- `streamEvents` 流式事件收集与增量拼接

## 概览

Demo11 是整个教学项目的压轴综合示例，它将前序 Demo 学到的能力整合为一个**任务型工作助手**：

- 用户可创建、查看、完成结构化任务
- 可查询天气、做四则运算、保存备忘
- 支持多会话隔离，每个会话拥有独立记忆
- 支持流式输出，前端可逐块显示

## 工具集组装

Demo11 从已有 Demo 中复用工具，并新增任务管理工具：

```java
Toolkit toolkit = new Toolkit();
toolkit.registerTool(new TaskTools());       // Demo11 新增：任务管理
toolkit.registerTool(new WeatherTools());    // 复用 Demo05：天气查询
toolkit.registerTool(new AssistantTools());  // 复用 Demo10：计算器 + 备忘
```

`TaskTools` 提供三个工具方法：

| 工具名 | 功能 | 关键参数 |
|--------|------|---------|
| `create_task` | 创建新任务并自动分配 ID | `title`（必填）、`detail`（可选） |
| `list_tasks` | 列出全部任务及状态 | 无 |
| `complete_task` | 将指定任务标记为已完成 | `id`（必填） |

任务使用 `ConcurrentHashMap` 存储，`AtomicLong` 自增 ID，保证线程安全。

## 记忆与压缩配置

Demo11 复用 Demo07 的 `MemoryConfig` 和 `CompactionConfig` 方案，让 Agent 具备长期记忆能力：

```java
MemoryConfig memoryConfig = MemoryConfig.builder()
    .model("dashscope:qwen-plus")    // 用模型辅助记忆摘要
    .build();

CompactionConfig compactionConfig = CompactionConfig.builder()
    .triggerMessages(8)     // 消息数达到 8 条时触发压缩
    .triggerTokens(1600)    // token 数达到 1600 时触发压缩
    .keepMessages(4)        // 压缩后保留最近 4 条消息
    .build();
```

两个配置协同工作：当对话消息数或 token 数达到阈值时，自动对历史消息进行摘要压缩，只保留最近的关键消息。这样既能维持上下文连贯性，又不会超出模型的上下文窗口。

## 构建 Agent

将工具、记忆、会话隔离整合在一起：

```java
private final Map<String, HarnessAgent> agentCache = new ConcurrentHashMap<>();

private HarnessAgent buildAssistant(String sessionId) {
    Toolkit toolkit = new Toolkit();
    toolkit.registerTool(new TaskTools());
    toolkit.registerTool(new WeatherTools());
    toolkit.registerTool(new AssistantTools());

    MemoryConfig memoryConfig = MemoryConfig.builder()
        .model("dashscope:qwen-plus").build();
    CompactionConfig compactionConfig = CompactionConfig.builder()
        .triggerMessages(8).triggerTokens(1600).keepMessages(4).build();

    return HarnessAgent.builder()
        .name("task-assistant")
        .sysPrompt("你是一位'长期记忆的任务式工作助手'。你可以帮助用户："
            + "①创建/查看/完成结构化任务(create_task/list_tasks/complete_task)；"
            + "②查询天气、做四则运算；③记住用户的偏好并在后续对话中回忆。"
            + "请高效使用工具完成用户指令，并以简洁清晰的中文作答。")
        .model("dashscope:qwen-plus")
        .toolkit(toolkit)
        .maxIters(6)
        .workspace(Paths.get(".agentscope/workspace-demo11", sessionId))
        .memory(memoryConfig)
        .compaction(compactionConfig)
        .build();
}
```

要点：

- **会话隔离**：`agentCache.computeIfAbsent(sessionId, ...)` 保证每个 session 有独立 Agent 实例
- **工作空间隔离**：`workspace` 路径包含 sessionId，避免不同会话的文件互相覆盖
- **系统提示明确列出工具名**：引导 Agent 正确调用 `create_task` 等工具

## 接口设计

Demo11 提供三种调用方式：

### 同步调用（表单参数）

```
POST /api/demo11/task?session=demo&instruction=帮我创建一个任务：写周报
```

```java
@PostMapping("/task")
public String task(@RequestParam(defaultValue = "demo") String session,
                   @RequestParam String instruction) {
    HarnessAgent assistant = assistantFor(session);
    Msg reply = assistant.call(
        List.of(new UserMessage(instruction)),
        RuntimeContext.empty()
    ).block();
    return reply != null ? reply.getTextContent() : "（无回复）";
}
```

### 同步调用（JSON 请求体）

```
POST /api/demo11/execute
Content-Type: application/json

{ "session": "user-001", "instruction": "查一下杭州今天的天气" }
```

使用 `Demo11TaskRequest` record 接收 JSON 请求体，`@NotBlank` 校验 instruction 非空。

### 流式调用

```
GET /api/demo11/stream?session=demo&message=请帮我列出当前所有任务
```

```java
@GetMapping("/stream")
public String stream(@RequestParam(defaultValue = "demo") String session,
                     @RequestParam(defaultValue = "请帮我列出当前所有任务") String message) {
    HarnessAgent assistant = assistantFor(session);
    StringBuilder collected = new StringBuilder();
    List<AgentEvent> events = assistant.streamEvents(
            new UserMessage(message), RuntimeContext.empty())
        .doOnNext(event -> {
            if (event instanceof TextBlockDeltaEvent tde)
                collected.append(tde.getDelta());
        })
        .collectList().block();
    int eventCount = events == null ? 0 : events.size();
    return "流式事件条数 = " + eventCount + System.lineSeparator()
        + "逐块增量拼接结果 >>>" + System.lineSeparator() + collected;
}
```

`streamEvents` 返回 `AgentEvent` 流，其中 `TextBlockDeltaEvent` 携带增量文本片段。逐块 `append` 即可还原完整回复——这正是前端打字机效果的底层原理。

## 工程化配套：统一响应与异常处理

前面 10 个 Demo 大多直接返回文本，方便用 curl 观察 Agent 的原始输出。但真实服务需要统一的错误契约，项目在 `common` 包中提供了这套基础设施（由 Spring Boot 自动生效）：

| 组件 | 源码 | 作用 |
|------|------|------|
| `ApiResponse<T>` | [ApiResponse](../src/main/java/com/xb/agentscope/common/ApiResponse.java) | 统一 `{ code, message, data }` 结构，成功时 `code=0` |
| `GlobalExceptionHandler` | [GlobalExceptionHandler](../src/main/java/com/xb/agentscope/common/GlobalExceptionHandler.java) | `@RestControllerAdvice` 分类处理参数缺失、校验失败、业务异常与未知异常 |
| `Demo11TaskRequest` | [Demo11TaskRequest](../src/main/java/com/xb/agentscope/controller/demo11/Demo11TaskRequest.java) | `record` + `@NotBlank`，配合 `@Valid` 校验 JSON 请求体 |

异常分类与 HTTP 状态码：

| 异常场景 | HTTP 状态 | 响应 message |
|---------|----------|-------------|
| 缺少请求参数 | 400 | `缺少必要参数: instruction` |
| `@Valid` 校验失败 | 400 | `instruction: instruction 不能为空` |
| 业务 `IllegalArgumentException` | 400 | 异常消息 |
| 未知异常 | 500 | `服务内部错误，requestId=xxxxxxxx` |

验证方式：

```bash
curl -X POST http://localhost:8080/api/demo11/execute \
  -H "Content-Type: application/json" \
  -d '{"instruction": ""}'
# {"code":400,"message":"instruction: instruction 不能为空","data":null}
```

未知异常会生成 8 位 `requestId` 并写入 error 日志，便于把用户反馈与后端堆栈对应起来。这套“统一响应 + 参数校验 + 兜底异常”的组合，是 Agent 服务对外提供 API 时的最小工程化基线；更完整的生产化改造见[附录](./appendix-production-hardening.md)。

## 能力回顾

Demo11 按需复用了前序 Demo 的能力，而不是机械堆砌：

| 知识领域 | 来源 Demo | Demo11 中的实际体现 |
|---------|-----------|----------------|
| 基础 Agent 构建 | Demo01-03 | HarnessAgent builder 链式构建 |
| 流式输出 | Demo04 | `streamEvents` + `TextBlockDeltaEvent` |
| 工具调用 | Demo05、Demo10 | 复用 WeatherTools、AssistantTools |
| 记忆与压缩 | Demo07 | MemoryConfig + CompactionConfig |
| 提示设计 | Demo09 | 系统提示明确列出工具名与职责边界 |
| 结构化请求体 | Demo08、Demo11 | `record` + `@Valid` 接收 JSON；TaskTools 返回格式化文本 |
| 会话隔离 | Demo15（预习） | 按 sessionId 缓存 Agent 与 workspace，提前用到后续章节的思路 |

## 小结

- Demo11 是一个**功能完整的任务型工作助手**，综合运用了全部前序知识点
- `TaskTools` 新增任务管理能力：创建、查看、完成，线程安全
- `MemoryConfig` + `CompactionConfig` 实现**长期记忆**与**上下文压缩**，避免超出窗口限制
- 按 sessionId 隔离 Agent 实例和工作空间，支持多用户并发
- `streamEvents` 流式接口为前端打字机效果提供底层支持
