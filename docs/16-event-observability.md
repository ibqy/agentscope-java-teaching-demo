# Demo16：事件驱动可观测 — 看懂 Agent 的每一步

> **导航**：[← 上一篇：Demo15 Plan Mode 与权限系统](./15-plan-mode-permission.md) · [返回首页](./index.md) · [下一篇：附录 · 生产化改造清单 →](./appendix-production-hardening.md)
>
> **本章源码**：[Demo16EventObservabilityController](../src/main/java/com/xb/agentscope/controller/demo16/Demo16EventObservabilityController.java) · [AgentEventTimeline](../src/main/java/com/xb/agentscope/controller/demo16/AgentEventTimeline.java)

## 知识点

- `streamEvents()` 的**全事件类型**订阅（不止文本增量）
- 事件流 → **执行时间线**的分类聚合方法
- `ModelCallEndEvent` 自带的 `ChatUsage`：真实 Token 计量（输入/输出/合计/耗时）
- 可观测的核心维度：模型调用次数、工具调用清单、迭代上限熔断、HITL 确认点

## 为什么需要全事件订阅

demo04 教了 `streamEvents()`，但只处理了 `TextBlockDeltaEvent`——那是面向 C 端体验的"打字机效果"。
生产环境你需要回答的是另一组问题：

| 问题 | 对应事件 |
|---|---|
| 这次请求模型往返了几次？ | `MODEL_CALL_START` / `MODEL_CALL_END` |
| Agent 到底调了哪些工具？有没有幻觉调用？ | `TOOL_CALL_START` / `TOOL_CALL_END` |
| Token 花了多少？耗时多少？ | `ModelCallEndEvent.getUsage()` |
| 任务是被正常结束还是迭代熔断？ | `AGENT_END` / `EXCEED_MAX_ITERS` |
| 哪里在等人工审批？ | `REQUIRE_USER_CONFIRM` |

## 核心方法：分类聚合，不是逐条打印

直接 `doOnNext(System.out::println)` 会得到一屏碎片——文本增量是 token 级的，
一次请求几百条。正确做法（见 `AgentEventTimeline`）：

```java
switch (event.getType()) {
    case MODEL_CALL_END -> {
        // 模型结束事件自带真实用量，比按字符估算准得多
        ChatUsage usage = ((ModelCallEndEvent) event).getUsage();
        inputTokens += usage.getInputTokens();
        modelTimeSeconds += usage.getTime();
    }
    case TOOL_CALL_START -> toolCalls++;   // 工具名入表
    case TEXT_BLOCK_DELTA -> textChars += delta.length();  // 只计数，不入表
    default -> /* 未知事件类型兜底，框架升级加事件不至于崩 */;
}
```

三条设计原则：

1. **增量事件只计数**——时间线是给人看的，不是给机器 dump 的
2. **用量以模型返回为准**——`ChatUsage` 是真实计量；按字符除 2 估算是粗方案，中文场景误差能到 50%
3. **聚合逻辑与传输解耦**——`AgentEventTimeline` 不依赖 Spring 也不依赖 LLM，喂事件对象就能跑，所以能离线单测（`AgentEventTimelineTest` 9 个用例全覆盖）

## 接口调用

```bash
# 执行时间线（默认问题会触发天气工具，便于观察工具事件）
curl "http://localhost:8080/api/demo16/timeline?message=北京今天天气怎么样？需要带伞吗？"

# Token 用量统计
curl "http://localhost:8080/api/demo16/usage?message=北京今天天气怎么样？"
```

timeline 输出示例：

```
=== Agent 执行时间线 ===
  1. AGENT_START        Agent 启动: assistant
  2. MODEL_CALL_START   第 1 次模型调用
  3. TOOL_CALL_START    调用工具: get_weather
  4. TOOL_CALL_END      工具返回: get_weather
  5. MODEL_CALL_END     第 1 次调用结束: 输入 120 / 输出 35 / 合计 155 tokens, 耗时 0.80s
---
事件总数: 7（文本增量 15 字符、思考增量 0 字符未逐条列出）
Agent 启动: 1 次 | 模型调用: 1 次 | 工具调用: 1 次
Token: 输入 120 / 输出 35 / 合计 155，模型累计耗时 0.80s
超迭代终止: 否
```

## 从 Demo 到生产

| Demo 做法 | 生产做法 |
|---|---|
| 时间线渲染成文本返回 | 事件进 Kafka/OTLP，前端看板实时渲染 |
| 进程内聚合 | 按 sessionId/taskId 分桶，落 ClickHouse |
| 单次请求视角 | 跨请求聚合：按用户/按天统计 Token 成本 |
| `EXCEED_MAX_ITERS` 只记录 | 触发告警 + 自动熔断该用户的请求 |

## 小结

- `streamEvents()` 是全事件入口：文本增量面向体验，模型/工具/迭代事件面向生产
- 用 `AgentEventTimeline` 分类聚合，避免把 token 级碎片直接打印
- Token 用量以 `ModelCallEndEvent.getUsage()` 为准，是成本核算与预算告警的基线
- 聚合逻辑与框架、Spring 解耦，可离线单测；上线前再接入指标、日志与追踪（见[附录](./appendix-production-hardening.md)）

## 相关

- demo04：流式文本增量（C 端体验向）
- demo15：Plan Mode / 权限 / 状态持久化（生产部署向）
- [AgentScope 事件类型全集](https://github.com/agentscope-ai/agentscope-java)：`AgentEventType` 枚举
