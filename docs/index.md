---
layout: home

hero:
  name: AgentScope Java 教学
  text: 从 Hello World 到生产级智能体
  tagline: 16 个渐进式 Demo · 16 篇配套文档 · 47 个单元测试。覆盖模型接入、工具调用、多智能体、MCP、A2A、RAG、Plan Mode、权限与可观测，每个知识点都能定位到可运行代码。
  actions:
    - theme: brand
      text: 开始学习 →
      link: /01-agent-hello-world
    - theme: alt
      text: 直达综合实战
      link: /11-comprehensive-demo
    - theme: alt
      text: GitHub 源码
      link: https://github.com/ibqy/agentscope-java-teaching-demo

features:
  - icon: 🧱
    title: 基础四课（01-04）
    details: HarnessAgent 创建、DashScopeChatModel 手动构建、call 同步调用、streamEvents 流式输出，先跑通再深入
  - icon: 🛠️
    title: 核心能力（05-10）
    details: "@Tool 工具调用、Subagent 多智能体、记忆与压缩、结构化输出、Prompt 角色工程、多工具聚合"
  - icon: 🚀
    title: 综合实战（11）
    details: 任务管理 + 天气 + 计算 + 长期记忆 + 会话隔离 + 流式输出，组合成一个可用的任务型工作助手
  - icon: 🔌
    title: 生态集成（12-14）
    details: MCP 标准协议接入外部工具、A2A 远程 Agent 发现与认证调用、应用层 RAG 知识库检索
  - icon: 🛡️
    title: 生产化能力（15-16）
    details: Plan Mode 先规划后执行、工具级 ALLOW/DENY/ASK 权限、多用户隔离与状态持久化、全事件可观测
  - icon: ✅
    title: 47 个单元测试
    details: 工具逻辑、TF-IDF 检索、事件聚合、异常处理均有测试覆盖，代码可放心修改
  - icon: 📖
    title: 文档即教材
    details: 每篇文档包含知识点、完整代码、curl 验证、源码链接与上下章导航，与 Demo 编号一一对应
  - icon: 🧭
    title: 学习路径清晰
    details: 四阶段递进——基础入门、核心能力、综合实战、生态与生产化，每章都能独立运行
---

## 四阶段学习路径 {#learning-path}

| 阶段 | 章节 | 目标 | 学完你能做到 |
|------|------|------|-------------|
| ① 基础入门 | [Demo01](./01-agent-hello-world.md) → [Demo04](./04-streaming.md) | 跑通第一个 Agent，理解模型、调用与事件流 | 独立创建一个可对话的 Agent |
| ② 核心能力 | [Demo05](./05-tool-calling.md) → [Demo10](./10-assistant.md) | 工具、多智能体、记忆、结构化输出、Prompt 工程 | 让 Agent 完成有边界的实际任务 |
| ③ 综合实战 | [Demo11](./11-comprehensive-demo.md) | 组合工具 + 记忆 + 会话隔离 + 流式输出 | 构建一个任务型工作助手 |
| ④ 生态与生产化 | [Demo12](./12-mcp-integration.md) → [Demo16](./16-event-observability.md) | MCP、A2A、RAG、Plan Mode、权限、状态与可观测 | 接入外部生态并具备上线意识 |

> 每篇文档顶部都有「上一篇 / 下一篇 / 返回首页」与本章源码链接，底部由文档站自动生成上下章导航。

## Demo 一览 {#demos}

| 编号 | 主题 | 核心 API / 概念 | 文档 |
|------|------|----------------|------|
| Demo01 | Hello Agent | `HarnessAgent`、全局 Bean、`call()` | [阅读](./01-agent-hello-world.md) |
| Demo02 | DashScope 模型 | `DashScopeChatModel.builder()`、模型参数 | [阅读](./02-dashscope-model.md) |
| Demo03 | Call 同步调用 | 消息组装、`RuntimeContext`、`block()` | [阅读](./03-call.md) |
| Demo04 | 流式输出 | `streamEvents()`、`TextBlockDeltaEvent` | [阅读](./04-streaming.md) |
| Demo05 | 工具调用 | `@Tool`、`@ToolParam`、`Toolkit` | [阅读](./05-tool-calling.md) |
| Demo06 | 多智能体协作 | `SubagentDeclaration`、Director 编排 | [阅读](./06-multi-agent.md) |
| Demo07 | 记忆与工作区 | `MemoryConfig`、`CompactionConfig` | [阅读](./07-memory-workspace.md) |
| Demo08 | 结构化输出 | `call(Class)`、`getStructuredData()` | [阅读](./08-structured-output.md) |
| Demo09 | Prompt 模板 | 动态 System Prompt、角色切换 | [阅读](./09-prompt-template.md) |
| Demo10 | 全能助理 | 多工具聚合、`maxIters` | [阅读](./10-assistant.md) |
| Demo11 | 综合实战 | 工具 + 记忆 + 会话隔离 + 流式 | [阅读](./11-comprehensive-demo.md) |
| Demo12 | MCP 工具集成 | `McpClientBuilder`、StdIO / SSE | [阅读](./12-mcp-integration.md) |
| Demo13 | A2A 跨智能体通信 | `A2aAgent`、`WellKnownAgentCardResolver` | [阅读](./13-a2a-communication.md) |
| Demo14 | RAG 知识库检索 | 分段分块 + TF-IDF、Generic / Agentic 双模式 | [阅读](./14-rag-knowledge-base.md) |
| Demo15 | Plan Mode 与权限系统 | 先规划后执行、`PermissionRule`、`AgentState` | [阅读](./15-plan-mode-permission.md) |
| Demo16 | 事件驱动可观测 | 全事件订阅、时间线、Token 计量 | [阅读](./16-event-observability.md) |

## 高阶实战 {#advanced}

后五个 Demo 面向“能上线”的能力，而不是简单的 API 调用：

| 高阶主题 | 你会学到 | 入口 |
|---------|---------|------|
| 协议集成 | MCP 工具自动发现与注册、A2A AgentCard 发现与鉴权调用 | [Demo12](./12-mcp-integration.md) · [Demo13](./13-a2a-communication.md) |
| 私有知识 | 应用层 RAG 完整链路、检索质量调优、Generic 与 Agentic 取舍 | [Demo14](./14-rag-knowledge-base.md) |
| 执行控制 | Plan Mode、工具级权限规则、多用户隔离、状态持久化 | [Demo15](./15-plan-mode-permission.md) |
| 可观测性 | 全事件时间线、工具与模型调用统计、真实 Token 用量 | [Demo16](./16-event-observability.md) |
| 生产化改造 | 超时/重试/降级、SSE、成本控制、多租户、部署与上线清单 | [附录](./appendix-production-hardening.md) |

## 快速开始 {#quick-start}

**前置条件**：JDK 21+、Maven 3.8+、[DashScope API Key](https://bailian.console.aliyun.com/)

```bash
# 1. 配置 API Key（Windows PowerShell）
$env:DASHSCOPE_API_KEY="your-api-key-here"

# Linux / macOS
export DASHSCOPE_API_KEY="your-api-key-here"

# 2. 启动服务
mvn spring-boot:run

# 3. 验证第一个 Agent
curl "http://localhost:8080/api/demo01/hello?message=你好"
# PowerShell 用户使用：curl.exe "http://localhost:8080/api/demo01/hello?message=你好"

# 4. 运行测试
mvn test
```

启动后按 [Demo01](./01-agent-hello-world.md) → [Demo16](./16-event-observability.md) 的顺序学习；每篇文档都提供可复制的 curl 验证命令。

## 工程化与质量 {#quality}

- **统一响应与异常**：`ApiResponse` + `@RestControllerAdvice` 分类处理参数缺失、校验失败、业务异常与未知异常
- **参数校验**：Jakarta Validation（`@NotBlank`）+ `@Valid`，JSON 请求体使用 record 接收
- **测试覆盖**：47 个 JUnit 5 测试，覆盖工具逻辑、TF-IDF 检索、事件聚合与异常处理
- **文档一致性**：16 篇文档与 Demo 编号一一对应，每篇都链接到具体源码文件
- **教学边界透明**：天气为模拟数据、任务存内存、RAG 用词法检索、无前端 UI，详见 [README](https://github.com/ibqy/agentscope-java-teaching-demo#实现边界)

## 相关资源 {#resources}

- [AgentScope for Java 仓库](https://github.com/agentscope-ai/agentscope-java) — 框架源码与官方示例
- [阿里云百炼](https://bailian.console.aliyun.com/) — DashScope API Key 申请
- [项目 README](https://github.com/ibqy/agentscope-java-teaching-demo) — 技术栈、测试矩阵与项目结构
- [早期图文版（Demo01-11）](./agentscope-java-teaching-guide.html) — SDK 2.0.1 时期的静态旧版，仅作对照，最新内容以本站文档为准
