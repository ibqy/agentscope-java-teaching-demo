# Demo13：A2A 跨智能体通信 — 让 Agent 互相调用

> **导航**：[← 上一篇：Demo12 MCP 工具集成](./12-mcp-integration.md) · [返回首页](./index.md) · [下一篇：Demo14 RAG 知识库检索 →](./14-rag-knowledge-base.md)
>
> **本章源码**：[Demo13Controller](../src/main/java/com/xb/agentscope/controller/demo13/Demo13Controller.java)

## 知识点

- A2A（Agent-to-Agent）协议概念与核心价值
- `A2aAgent` 构建远程 Agent 代理
- `WellKnownAgentCardResolver` 自动发现远程 Agent
- AgentCard 机制：Agent 的"名片"
- 认证请求头传递（Bearer Token）

## 什么是 A2A

A2A（Agent-to-Agent）是 Google 提出的跨智能体通信标准协议。它解决的核心问题是：**不同框架、不同语言构建的 Agent 如何互相发现、互相调用**。

如果说 MCP 让 Agent 能调用"工具"，那 A2A 让 Agent 能调用"另一个 Agent"——而且这个 Agent 可以是 Python 的 LangChain 构建的、也可以是 Go 的 CrewAI 构建的，只要它们都支持 A2A 协议。

### AgentCard：Agent 的"名片"

每个 A2A Agent 都会发布一张 **AgentCard**（通常位于 `/.well-known/agent-card.json`），包含：

| 字段 | 说明 |
|------|------|
| `name` | Agent 名称 |
| `description` | 能力描述 |
| `skills` | 技能列表 |
| `url` | 通信端点 |
| `version` | 协议版本 |

客户端通过 AgentCard 了解远程 Agent 能做什么，然后构建 A2A 请求发送任务。

## 依赖配置

A2A 客户端需要额外引入依赖（MCP 不需要）：

```xml
<dependency>
    <groupId>io.agentscope</groupId>
    <artifactId>agentscope-extensions-a2a-client</artifactId>
    <version>2.0.4</version>
</dependency>
```

> **注意**：不要引入 `agentscope-a2a-spring-boot-starter`，它会拉取 Spring Boot 4.x，与项目的 Spring Boot 3.5.x 冲突。

## WellKnown 路径发现

最常用的方式是让 `WellKnownAgentCardResolver` 自动访问目标服务器的 `/.well-known/agent-card.json`，获取 AgentCard 后构建 A2A 请求：

```java
// Step 1: 创建 AgentCard 解析器
WellKnownAgentCardResolver resolver = WellKnownAgentCardResolver.builder()
        .baseUrl("http://localhost:9090")
        .relativeCardPath("/.well-known/agent-card.json")
        .build();

// Step 2: 构建 A2A Agent
A2aAgent agent = A2aAgent.builder()
        .name("remote-caller")
        .agentCardResolver(resolver)
        .build();

// Step 3: 像本地 Agent 一样调用
Msg response = agent.call(List.of(new UserMessage("你好，请做个自我介绍"))).block();
```

### 调用流程

```
客户端                              远程 A2A 服务
  |                                      |
  |--- GET /.well-known/agent-card.json ->|
  |<----------- AgentCard JSON -----------|
  |                                      |
  |--- A2A Protocol Request (JSON-RPC) ->|
  |<----------- A2A Response -------------|
```

### 接口调用

```
GET /api/demo13/call?remoteUrl=http://localhost:9090&message=你好
```

## 携带认证信息

生产环境中 A2A 服务通常需要认证。通过 `authHeaders` 传递自定义请求头：

```java
WellKnownAgentCardResolver resolver = WellKnownAgentCardResolver.builder()
        .baseUrl(remoteUrl)
        .relativeCardPath("/.well-known/agent-card.json")
        .authHeaders(Map.of("Authorization", "Bearer " + token))
        .build();
```

### 接口调用

```
GET /api/demo13/auth?remoteUrl=http://localhost:9090&token=your-token&message=你好
```

## A2A 与 Subagent 的区别

| 维度 | Subagent（Demo06） | A2A（Demo13） |
|------|-------------------|--------------|
| 通信范围 | 同一进程内的 Agent | 跨进程/跨网络的 Agent |
| 协议 | AgentScope 内部 API | A2A 标准协议（JSON-RPC） |
| 发现方式 | 代码中直接声明 | AgentCard 自动发现 |
| 跨框架 | 不支持（必须同框架） | 支持（Python/Go/Java 互通） |
| 适用场景 | 编排内部 Agent 团队 | 对接外部 Agent 服务 |

简单来说：Subagent 是"公司内部协作"，A2A 是"跨公司合作"。

## 小结

- A2A 是跨智能体通信的**开放标准**，让不同框架的 Agent 互相发现和调用
- `WellKnownAgentCardResolver` 通过 `/.well-known/agent-card.json` 自动发现远程 Agent
- `A2aAgent` 封装了 A2A 协议细节，调用远程 Agent 与调用本地 Agent 体验一致
- 支持 `authHeaders` 传递认证信息，适配生产环境
- 需要引入 `agentscope-extensions-a2a-client` 依赖，注意避免 Spring Boot 版本冲突
