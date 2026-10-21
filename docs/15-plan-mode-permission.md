# Demo15：Plan Mode 与权限系统 — 从"能用"到"可生产部署"

> **导航**：[← 上一篇：Demo14 RAG 知识库检索](./14-rag-knowledge-base.md) · [返回首页](./index.md) · [下一篇：Demo16 事件驱动可观测 →](./16-event-observability.md)
>
> **本章源码**：[Demo15Controller](../src/main/java/com/xb/agentscope/controller/demo15/Demo15Controller.java)

## 知识点

- Plan Mode：Agent 先规划再执行，适合复杂多步骤任务
- Permission System：细粒度控制工具执行权限（ALLOW / DENY / ASK）
- `RuntimeContext`：多用户会话隔离（sessionId + userId）
- `AgentState` + `AgentStateStore`：状态持久化与恢复
- `HarnessAgent` 作为工程化入口的核心配置项

## 为什么需要 Plan Mode

普通 Agent 收到任务后直接执行工具，但复杂任务往往需要"先想清楚再动手"。Plan Mode 让 Agent 分两个阶段工作：

1. **规划阶段**：Agent 分析任务，输出执行计划（不执行任何工具）
2. **执行阶段**：确认计划后，Agent 按计划逐步调用工具

这就像人类面对复杂任务时会先列 TODO 清单一样。

### 启用 Plan Mode

```java
HarnessAgent planner = HarnessAgent.builder()
        .name("planner")
        .enablePlanMode()                    // 开启 Plan Mode
        .planFileDirectory(".agentscope/plans")  // 计划文件存储目录
        .workspace(Paths.get(".agentscope/workspace"))
        .build();
```

### 运行时控制

```java
RuntimeContext ctx = RuntimeContext.builder()
        .sessionId("session-001")
        .userId("user-001")
        .build();

planner.enterPlanMode(ctx);   // 进入规划模式
Msg plan = planner.call("帮我完成XX任务", ctx).block();  // Agent 输出计划

planner.exitPlanMode(ctx);    // 退出规划，进入执行模式
Msg result = planner.call("请按计划执行", ctx).block();  // Agent 执行工具
```

### 接口调用

```
GET /api/demo15/plan?task=帮我查询北京今天的天气，然后计算100乘以25
```

## Permission System：工具执行权限控制

生产环境中，你不能让 Agent 随意执行任何工具。Permission System 提供三层控制：

| 层级 | 机制 | 说明 |
|------|------|------|
| 工具级 | `PermissionRule` | 针对单个工具的 ALLOW / DENY / ASK 规则 |
| 模式级 | `PermissionMode` | 全局行为模式（DEFAULT / BYPASS / ACCEPT_EDITS / DONT_ASK） |
| 上下文级 | `PermissionContextState` | 绑定到会话的权限状态快照 |

### PermissionBehavior 三种行为

| 行为 | 说明 | 适用场景 |
|------|------|---------|
| `ALLOW` | 自动允许执行 | 安全的只读工具（如天气查询） |
| `DENY` | 自动拒绝执行 | 危险操作（如删除文件） |
| `ASK` | 每次执行前询问用户 | 需要人工确认的操作（如发送邮件） |

### 配置权限规则

```java
PermissionRule allowWeather = new PermissionRule(
        "get_weather",           // 工具名称
        "允许天气查询",           // 规则描述
        PermissionBehavior.ALLOW, // 行为
        "my-app"                 // 来源标识
);

PermissionContextState permCtx = PermissionContextState.builder()
        .mode(PermissionMode.DEFAULT)
        .addAllowRule("get_weather", allowWeather)
        .build();

HarnessAgent agent = HarnessAgent.builder()
        .name("guarded-agent")
        .permissionContext(permCtx)
        .build();
```

### 接口调用

```
GET /api/demo15/permission?toolName=get_weather&behavior=ALLOW&message=查询北京天气
```

## RuntimeContext：多用户会话隔离

同一个 Agent 实例可以同时服务多个用户，`RuntimeContext` 确保会话完全隔离：

```java
// 用户 A
RuntimeContext ctxA = RuntimeContext.builder()
        .sessionId("session-A")
        .userId("user-A")
        .build();

// 用户 B
RuntimeContext ctxB = RuntimeContext.builder()
        .sessionId("session-B")
        .userId("user-B")
        .build();

// 同一个 Agent，不同会话
agent.call("你好，我是张三", ctxA).block();
agent.call("你好，我是李四", ctxB).block();
// Agent 不会混淆两个用户的身份和对话历史
```

### 接口调用

```
GET /api/demo15/multi-user?userA=我叫张三&userB=我叫李四
```

## AgentState：状态持久化

`AgentState` 封装了 Agent 的完整运行状态，`AgentStateStore` 负责持久化：

```java
// 创建 JSON 文件存储
JsonFileAgentStateStore store = new JsonFileAgentStateStore(
        Paths.get(".agentscope/state-store")
);

// 构建带状态存储的 Agent
HarnessAgent agent = HarnessAgent.builder()
        .name("stateful-agent")
        .stateStore(store)
        .build();

// 调用后状态自动保存
agent.call("你好", ctx).block();

// 检查状态是否存在
boolean exists = store.exists(sessionId, userId);
```

### AgentState 包含的状态

| 字段 | 说明 |
|------|------|
| `sessionId` | 会话 ID |
| `userId` | 用户 ID |
| `summary` | 对话摘要 |
| `context` | 消息历史列表 |
| `curIter` | 当前迭代次数 |
| `planModeContext` | Plan Mode 状态快照 |
| `permissionContext` | 权限状态快照 |

### 接口调用

```
GET /api/demo15/state?userId=user-demo&message=你好
```

## HarnessAgent vs ReActAgent

| 维度 | HarnessAgent | ReActAgent |
|------|-------------|------------|
| 定位 | 工程化入口（推荐） | 推理核心 |
| Plan Mode | 支持（`enablePlanMode()`） | 不支持 |
| Permission | 支持（`permissionContext()`） | 不支持 |
| 状态持久化 | 支持（`stateStore()`） | 不支持 |
| 中间件 | 支持（`middleware()`） | 不支持 |
| 适用场景 | 生产部署 | 纯推理实验 |

简单来说：`ReActAgent` 是"引擎"，`HarnessAgent` 是"整车"。

## 小结

- Plan Mode 让 Agent 先规划再执行，适合复杂多步骤任务
- Permission System 提供工具级 / 模式级 / 上下文级三层权限控制
- `RuntimeContext` 通过 sessionId + userId 实现多用户会话隔离
- `AgentState` + `AgentStateStore` 实现状态持久化与恢复
- `HarnessAgent` 是生产部署的推荐入口，封装了 Plan Mode、Permission、State 等工程化能力
