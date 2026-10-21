# Demo07：记忆与工作区 — 让 Agent 记住你

> **导航**：[← 上一篇：Demo06 多智能体协作](./06-multi-agent.md) · [返回首页](./index.md) · [下一篇：Demo08 结构化输出 →](./08-structured-output.md)
>
> **本章源码**：[Demo07MemoryWorkspaceController](../src/main/java/com/xb/agentscope/controller/demo07/Demo07MemoryWorkspaceController.java)

## 知识点

- `MemoryConfig` 长期记忆配置
- `CompactionConfig` 上下文压缩策略
- `workspace` 工作空间与记忆持久化
- `ConcurrentHashMap` 按会话缓存 Agent 实例

## 长期记忆 — MemoryConfig

Demo07 引入了对话记忆能力。同一个会话名称（`name`）在多次请求中，Agent 都能记住之前说过的内容：

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

配置记忆模型，AgentScope 会自动将对话历史持久化到 workspace 目录中。`model` 指定用于记忆摘要/整理的模型。

### CompactionConfig — 压缩策略

当对话历史过长时，自动触发压缩以控制上下文长度：

| 参数 | 含义 | 示例值 |
|------|------|--------|
| `triggerMessages` | 消息条数阈值 | 6 |
| `triggerTokens` | Token 数阈值 | 1200 |
| `keepMessages` | 压缩后保留最近 N 条 | 3 |

两个触发条件满足任意一个即执行压缩。

### 工作空间隔离

`workspace` 路径包含会话名称（`.agentscope/workspace-<name>`），不同会话的记忆文件互不覆盖：

```
.agentscope/
├── workspace-alice/     ← alice 的对话记忆
└── workspace-bob/       ← bob 的对话记忆
```

## 会话缓存

Demo07 使用 `ConcurrentHashMap` 按用户名称缓存 Agent 实例，确保同一用户共享记忆：

```java
private final Map<String, HarnessAgent> agentCache = new ConcurrentHashMap<>();

HarnessAgent agent = agentCache.computeIfAbsent(name, k -> buildMemoryAgent(k));
```

每次请求额外追加一条 `SystemMessage` 强化记忆指令：

```java
List<Msg> messages = new ArrayList<>();
messages.add(new SystemMessage("你拥有记忆能力，请记住用户在本会话中提到过的关键信息。"));
messages.add(new UserMessage(text));
```

## 验证记忆效果

连续调用两次，第二次询问第一次提过的信息：

```bash
# 第一次：告知姓名
curl "http://localhost:8080/api/demo07/memory?name=alice&text=我叫小明，请记住"

# 第二次：同一 name，询问姓名
curl "http://localhost:8080/api/demo07/memory?name=alice&text=我叫什么名字？"

# 换个 name：应回答不知道
curl "http://localhost:8080/api/demo07/memory?name=bob&text=我叫什么名字？"
```

## 记忆 vs 无记忆

| 维度 | 无记忆（Demo01/03） | 有记忆（Demo07） |
|------|---------------------|------------------|
| 上下文 | 每次请求独立 | 跨请求累积 |
| 存储 | 无 | workspace 目录持久化 |
| 隔离 | 天然隔离 | 按 name/workspace 隔离 |
| 成本 | 低 | 历史消息消耗 token，需要压缩策略 |

## 小结

- `MemoryConfig` 持久化对话历史到 workspace
- `CompactionConfig` 在消息/Token 超阈值时自动压缩
- 用 Map 缓存 Agent 实例实现会话隔离
- workspace 路径按会话命名，保证记忆文件互不覆盖
