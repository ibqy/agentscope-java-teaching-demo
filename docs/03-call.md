# Demo03：Call 同步调用 — 消息组装与 RuntimeContext

> **导航**：[← 上一篇：Demo02 DashScope 模型](./02-dashscope-model.md) · [返回首页](./index.md) · [下一篇：Demo04 流式输出 →](./04-streaming.md)
>
> **本章源码**：[Demo03CallController](../src/main/java/com/xb/agentscope/controller/demo03/Demo03CallController.java)

## 知识点

- `agent.call(List<Msg>, RuntimeContext)` 标准调用签名
- `UserMessage` / `SystemMessage` / `Msg` 的消息类型
- `block()` 阻塞等待与空值处理
- `RuntimeContext` 在调用链中的作用

## call() — 最基础的调用方式

AgentScope 的同步调用接口只做三件事：**接收消息列表 → 执行推理 → 返回一条 `Msg`**：

```java
@GetMapping("/call")
public String call(@RequestParam(defaultValue = "用一句话介绍 AgentScope") String message) {
    List<Msg> messages = List.of(new UserMessage(message));
    Msg reply = assistantAgent.call(messages, RuntimeContext.empty()).block();
    return reply != null ? reply.getTextContent() : "（无回复）";
}
```

## 消息组装

`call()` 的第一个参数是 `List<Msg>`，这意味着你可以一次传入多条消息构建上下文：

| 消息类型 | 作用 | 典型场景 |
|----------|------|----------|
| `UserMessage` | 用户输入 | 提问、下指令 |
| `SystemMessage` | 系统级约束 | 追加角色设定、输出格式要求 |
| `Msg` | 消息通用接口 | 方法签名中的统一类型 |

```java
// 单条用户消息（Demo03）
List<Msg> messages = List.of(new UserMessage("用一句话介绍 AgentScope"));

// 追加系统约束（Demo07/09 会用到）
List<Msg> messages = List.of(
        new SystemMessage("请严格保持当前角色身份。"),
        new UserMessage("解释什么是大模型"));
```

## RuntimeContext

第二个参数 `RuntimeContext` 描述"这次调用属于谁、在哪个会话里执行"：

```java
agent.call(messages, RuntimeContext.empty()).block();
```

- Demo03 只做单轮对话，用 `RuntimeContext.empty()` 即可；
- 多用户隔离、会话状态、权限上下文等能力都挂在这里，Demo15 会详细展开。

## 阻塞式返回

`call()` 返回 Reactor 的 `Mono<Msg>`，`.block()` 将其转换为同步结果：

- **优点**：代码直观，适合 Spring MVC 的同步 Controller；
- **注意**：
  - `block()` 会占用当前线程，不要在事件循环线程中调用；
  - 返回值为 `null` 表示没有拿到回复，必须做空值兜底；
  - `reply.getTextContent()` 只取文本，思考过程、工具调用记录不在其中。

## 同步 vs 流式

| 维度 | `call()`（Demo03） | `streamEvents()`（Demo04） |
|------|--------------------|----------------------------|
| 返回形式 | 一次性 `Msg` | 事件流 `Flux<AgentEvent>` |
| 用户感知 | 等待完整结果 | 逐字输出（打字机效果） |
| 适用场景 | 后台任务、批处理 | 聊天界面、长文本生成 |

需要流式体验时，转到 [Demo04：流式输出](04-streaming.md)。

## 接口调用

```bash
curl "http://localhost:8080/api/demo03/call?message=用一句话介绍 AgentScope"
```

## 小结

- `call(List<Msg>, RuntimeContext)` 是 AgentScope 的标准同步调用签名
- 消息列表可以承载多轮上下文与系统约束
- `RuntimeContext` 是会话隔离与状态管理的入口，入门阶段用 `empty()`
- `.block()` 阻塞等待结果，返回 `Mono<Msg>` 需做空值处理
