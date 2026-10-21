# Demo01：Hello Agent — 第一个 AgentScope 应用

> **导航**：[返回首页](./index.md) · [下一篇：Demo02 DashScope 模型 →](./02-dashscope-model.md)
>
> **本章源码**：[Demo01HelloAgentController](../src/main/java/com/xb/agentscope/controller/demo01/Demo01HelloAgentController.java)

## 知识点

- HarnessAgent 的创建与配置（Builder 模式）
- 全局 Bean 注入与复用（Spring 集成）
- `agent.call()` 发起第一轮对话
- `reply.getTextContent()` 提取回复文本

## HarnessAgent — 核心智能体

AgentScope 中，`HarnessAgent` 是一切交互的入口。它封装了模型、系统提示、工具和记忆等能力：

```java
// 依赖注入全局 Bean（参见 AgentScopeJavaConfig.java）
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

## 接口调用

```bash
curl "http://localhost:8080/api/demo01/hello?message=你好"
```

## 为什么从全局 Bean 开始

Demo01 的重点是"跑通第一个 Agent"，因此把模型、工具、记忆全部放进 `AgentScopeJavaConfig` 统一装配：

- 全局只创建一份模型和 Agent，避免每次请求重复初始化；
- 后续 Demo03/04 直接注入同一个 `assistantAgent` 复用能力；
- 需要理解底层装配时，再看 [Demo02：DashScope 模型](02-dashscope-model.md) 的手动构建方式。

## 小结

- `HarnessAgent` 是核心入口，通过 Builder 模式配置
- 全局 Bean 注入优于手动创建，利于资源复用
- `agent.call()` 是最基础的同步调用方式
- `reply.getTextContent()` 取回纯文本答复
