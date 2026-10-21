# Demo04：流式输出 — streamEvents 增量接收

> **导航**：[← 上一篇：Demo03 Call 同步调用](./03-call.md) · [返回首页](./index.md) · [下一篇：Demo05 工具调用 →](./05-tool-calling.md)
>
> **本章源码**：[Demo04StreamController](../src/main/java/com/xb/agentscope/controller/demo04/Demo04StreamController.java)

## 知识点

- `streamEvents()` 实现流式逐块输出
- `TextBlockDeltaEvent` 事件类型
- `Flux<AgentEvent>` 的订阅与收集
- 流式与同步调用的配合方式

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
- `TextBlockDeltaEvent` — 承载增量文本的事件类型
- `tde.getDelta()` — 获取本次增量字符串
- `.doOnNext()` — 在每个事件到达时执行回调
- `.collectList().block()` — 收集所有事件后阻塞等待（阻塞只是为了让 HTTP 接口同步返回，真实前端场景应直接订阅推送）

## 事件流的生命周期

```
订阅 streamEvents()
   │
   ├── AGENT_START          Agent 启动
   ├── MODEL_CALL_START     模型开始生成
   ├── TEXT_BLOCK_DELTA     文本增量 × N  ← Demo04 只关心这个
   ├── MODEL_CALL_END       模型调用结束
   └── AGENT_END            Agent 结束
```

Demo04 的定位是"把文本拼出来给用户看"，因此只处理 `TEXT_BLOCK_DELTA`。全事件类型的订阅与聚合（模型调用次数、工具调用、Token 用量）在 [Demo16：事件驱动可观测](16-event-observability.md) 中展开。

## 流式的价值

| 维度 | 同步 `call()` | 流式 `streamEvents()` |
|------|---------------|------------------------|
| 首字延迟 | 高（等全部生成完） | 低（生成一块推一块） |
| 用户感知 | 转圈等待 | 打字机效果 |
| 实现复杂度 | 低 | 中（需处理事件类型） |
| 适用场景 | 后台任务、短回复 | 聊天界面、长文本 |

## 接口调用

```bash
curl "http://localhost:8080/api/demo04/stream?message=请用三句话介绍 DashScope"
```

返回示例：

```
流式事件条数 = 8
拼接文本 >>>
DashScope 是阿里云推出的大模型服务平台……（逐块拼接后的完整文本）
```

## 小结

- `streamEvents()` 返回事件流，是流式输出的基础
- `TextBlockDeltaEvent.getDelta()` 拿到增量文本，逐块 `append` 即可还原完整回复
- `.collectList().block()` 便于在同步 HTTP 接口中演示，生产前端应直接消费事件流
- 流式与同步调用并存，按交互场景选择
