# Demo04 / Demo07：流式输出与长期记忆

## 知识点

- `streamEvents()` 实现流式逐块输出
- `TextBlockDeltaEvent` 事件类型
- `MemoryConfig` 长期记忆配置
- `CompactionConfig` 上下文压缩策略

## 流式输出 — streamEvents()

Demo04 演示了如何实时接收模型生成的增量文本。相比于 `call()` 的一次性返回，`streamEvents()` 通过事件流逐块推送：

```java
@GetMapping("/stream")
public String stream(@RequestParam(defaultValue = "请用三句话介绍 DashScope") String message) {
    StringBuilder collected = new StringBuilder();
    List<AgentEvent> events = assistantAgent.streamEvents(
                    new UserMessage(message), RuntimeContext.empty())
            .doOnNext(event -> {
                if (event instanceof TextBlockDeltaEvent tde) {
                    collected.append(tde.getDelta());
                }
            })
            .collectList().block();
    int eventCount = events == null ? 0 : events.size();
    return "流式事件条数 = " + eventCount + "\n拼接文本 >>>\n" + collected;
}
```

关键点：
- `streamEvents()` 返回 `Flux<AgentEvent>` 事件流
- `TextBlockDeltaEvent` — 承载增量文本的**唯一事件类型**
- `tde.getDelta()` — 获取本次增量字符串
- `.doOnNext()` — 在每个事件到达时执行回调
- `.collectList().block()` — 收集所有事件后阻塞等待

## 长期记忆 — MemoryConfig

Demo07 引入了对话记忆能力。同一个用户（按 name 区分）在多次请求中，Agent 都能记住之前说过的内容：

```java
private HarnessAgent buildMemoryAgent(String name) {
    MemoryConfig memoryConfig = MemoryConfig.builder()
        .model("dashscope:qwen-plus").build();

    CompactionConfig compactionConfig = CompactionConfig.builder()
        .triggerMessages(6)    // 消息数达到 6 条时触发压缩
        .triggerTokens(1200)   // token 数达到 1200 时触发压缩
        .keepMessages(3)       // 压缩后保留最近 3 条消息
        .build();

    return HarnessAgent.builder()
        .name("memory-agent")
        .sysPrompt("你是一个有长期记忆的助手。")
        .model("dashscope:qwen-plus")
        .workspace(Paths.get(".agentscope/workspace-" + name))
        .memory(memoryConfig)
        .compaction(compactionConfig)
        .build();
}
```

### MemoryConfig

配置记忆模型，AgentScope 会自动将对话历史持久化到 workspace 目录中。

### CompactionConfig — 压缩策略

当对话历史过长时，自动触发压缩以控制上下文长度：

| 参数 | 含义 | 示例值 |
|------|------|--------|
| `triggerMessages` | 消息条数阈值 | 6 |
| `triggerTokens` | Token 数阈值 | 1200 |
| `keepMessages` | 压缩后保留最近 N 条 | 3 |

两个触发条件满足任意一个即执行压缩。

### 会话缓存

Demo07 使用 `ConcurrentHashMap` 按用户名称缓存 Agent 实例，确保同一用户共享记忆：

```java
private final Map<String, HarnessAgent> agentCache = new ConcurrentHashMap<>();

HarnessAgent agent = agentCache.computeIfAbsent(name, k -> buildMemoryAgent(k));
```

## 小结

- `streamEvents()` + `TextBlockDeltaEvent` 实现实时流式输出
- `MemoryConfig` 持久化对话历史到 workspace
- `CompactionConfig` 在消息/Token 超阈值时自动压缩
- 用 Map 缓存 Agent 实例实现会话隔离
