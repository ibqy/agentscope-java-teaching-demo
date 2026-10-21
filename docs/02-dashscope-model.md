# Demo02：DashScope 模型 — 手动构建 ChatModel

> **导航**：[← 上一篇：Demo01 Hello Agent](./01-agent-hello-world.md) · [返回首页](./index.md) · [下一篇：Demo03 Call 同步调用 →](./03-call.md)
>
> **本章源码**：[Demo02DashScopeModelController](../src/main/java/com/xb/agentscope/controller/demo02/Demo02DashScopeModelController.java)

## 知识点

- `DashScopeChatModel.builder()` 显式构建模型实例
- `apiKey` / `modelName` / `stream` / `formatter` 四个关键参数
- 模型实例直接装配进 `HarnessAgent`
- 手动构建与全局 Bean 方式的取舍

## 为什么要手动构建模型

Demo01 通过全局 Bean 获得了一个"开箱即用"的 Agent，但模型是在配置类里装配的。当出现下面这些需求时，你需要自己构建模型：

- 同一个应用里接入**多个模型**（qwen-plus / qwen-max 切换对比）；
- 不同租户使用**不同的 API Key**；
- 需要实验不同的**流式开关、格式化器**等参数。

## 显式构建 ChatModel

Demo02 在 Controller 中完整展示了模型的手动构建过程：

```java
@GetMapping("/chat")
public String chat(@RequestParam(defaultValue = "介绍一下你自己") String message) {
    // 显式构建模型：需要手动指定 apiKey、模型名、格式化器
    DashScopeChatModel dashScopeModel = DashScopeChatModel.builder()
            .apiKey(apiKey).modelName("qwen-plus").stream(true)
            .formatter(new DashScopeChatFormatter()).build();

    // 用构建好的模型实例创建 Agent
    HarnessAgent agent = HarnessAgent.builder()
            .name("dashscope-explicit-agent")
            .sysPrompt("你是一个由显式 DashScopeChatModel 驱动的智能体。")
            .model(dashScopeModel)
            .workspace(Paths.get(".agentscope/workspace-demo02"))
            .build();

    Msg reply = agent.call(List.<Msg>of(new UserMessage(message)), RuntimeContext.empty()).block();
    return reply != null ? reply.getTextContent() : "（无回复）";
}
```

## 参数说明

| 参数 | 作用 | 示例值 |
|------|------|--------|
| `apiKey` | DashScope 鉴权密钥，来自配置/环境变量 | `sk-xxx` |
| `modelName` | 模型名称 | `qwen-plus` |
| `stream` | 是否启用流式返回 | `true` |
| `formatter` | 消息格式化器，负责 Java 消息 ↔ DashScope 报文互转 | `DashScopeChatFormatter` |

> `formatter` 很容易被忽略：不同模型厂商的消息结构不同，格式化器负责翻译，选错会导致请求报错或内容丢失。

## 手动构建 vs 全局 Bean

| 维度 | 全局 Bean（Demo01） | 手动构建（Demo02） |
|------|---------------------|--------------------|
| 创建时机 | 应用启动时一次创建 | 每次请求/按需创建 |
| 复用性 | 高（多 Controller 共享） | 低（局部使用） |
| 灵活性 | 参数固定 | 可按请求动态指定模型/Key |
| 资源开销 | 低 | 反复创建有开销 |
| 适用场景 | 单一模型的主流程 | 多模型实验、多租户、参数化 |

> 实际项目中推荐 Demo01 的全局 Bean 方式作为默认，只有确实需要动态配置时才手动构建。

## 接口调用

```bash
curl "http://localhost:8080/api/demo02/chat?message=你是谁"
```

## 小结

- `DashScopeChatModel.builder()` 可以完全掌控模型参数
- `apiKey`、`modelName`、`stream`、`formatter` 是四个必知参数
- 构建好的模型实例可直接通过 `.model(...)` 装配进 `HarnessAgent`
- 全局 Bean 适合常规场景，手动构建适合多模型/多租户等动态场景
