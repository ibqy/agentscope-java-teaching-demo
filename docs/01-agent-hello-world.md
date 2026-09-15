# Demo01-03：AgentScope 入门

## 知识点

- HarnessAgent 的创建与配置
- 全局 Bean 注入与复用
- `agent.call()` 同步调用模式
- DashScopeChatModel 的手动构建

## HarnessAgent — 核心智能体

AgentScope 中，`HarnessAgent` 是一切交互的入口。它封装了模型、系统提示、工具和记忆等能力：

```java
// 方式一：依赖注入全局 Bean（参见 AgentScopeJavaConfig.java）
private final HarnessAgent assistantAgent;

public Demo01HelloAgentController(HarnessAgent assistantAgent) {
    this.assistantAgent = assistantAgent;
}
```

全局配置在 `AgentScopeJavaConfig` 中完成：

```java
@Bean
public HarnessAgent assistantAgent(Toolkit toolkit) {
    return HarnessAgent.builder()
        .name("assistant")
        .sysPrompt("你是一位耐心、专业、乐于助人的中文助手。")
        .model(model)           // dashscope:qwen-plus
        .toolkit(toolkit)
        .workspace(Paths.get(".agentscope/workspace"))
        .memory(memoryConfig)
        .compaction(compactionConfig)
        .build();
}
```

## 基本对话 — agent.call()

调用方式非常简洁：构造 `UserMessage`，传入 `RuntimeContext.empty()`，`block()` 等待结果：

```java
@GetMapping("/hello")
public String hello(@RequestParam(defaultValue = "你好，请做个自我介绍") String message) {
    UserMessage userMessage = new UserMessage(message);
    Msg reply = assistantAgent.call(List.<Msg>of(userMessage), RuntimeContext.empty()).block();
    return reply != null ? reply.getTextContent() : "（无回复）";
}
```

关键步骤：
1. `UserMessage` — 封装用户输入
2. `List.<Msg>of(...)` — 构建消息列表（可包含多条历史消息）
3. `RuntimeContext.empty()` — 空的运行时上下文
4. `.block()` — 阻塞等待结果返回
5. `reply.getTextContent()` — 提取文本内容

## 手动构建模型 — DashScopeChatModel

Demo02 展示了不依赖全局 Bean，直接手动创建模型实例：

```java
DashScopeChatModel dashScopeModel = DashScopeChatModel.builder()
    .apiKey(apiKey)
    .modelName("qwen-plus")
    .stream(true)
    .formatter(new DashScopeChatFormatter())
    .build();

HarnessAgent agent = HarnessAgent.builder()
    .name("dashscope-explicit-agent")
    .sysPrompt("你是一个由显式 DashScopeChatModel 驱动的智能体。")
    .model(dashScopeModel)
    .workspace(Paths.get(".agentscope/workspace-demo02"))
    .build();
```

> **提示**：实际项目中推荐使用 Demo01 的全局 Bean 注入方式，避免重复创建模型实例。

## 小结

- `HarnessAgent` 是核心入口，通过 Builder 模式配置
- `agent.call()` 是最基础的同步调用方式
- 全局 Bean 注入优于手动创建，利于资源复用
- `RuntimeContext` 用于场景扩展，入门阶段用 `empty()` 即可
