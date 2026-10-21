<p align="center">
  <a href="https://ibqy.github.io/agentscope-java-teaching-demo/"><img src="https://img.shields.io/badge/在线课件-2F6FEB?style=flat-square" alt="在线课件"></a>
  <img src="https://img.shields.io/badge/Java-21-2266EE?style=flat-square" alt="Java 21">
  <img src="https://img.shields.io/badge/Spring_Boot-3.5.16-6DB33F?style=flat-square" alt="Spring Boot 3.5.16">
  <img src="https://img.shields.io/badge/AgentScope-2.0.4-7B3FE4?style=flat-square" alt="AgentScope 2.0.4">
  <img src="https://img.shields.io/badge/Demo-16-8957E5?style=flat-square" alt="16 个 Demo">
  <img src="https://img.shields.io/github/license/ibqy/agentscope-java-teaching-demo?style=flat-square" alt="License">
  <img src="https://img.shields.io/github/stars/ibqy/agentscope-java-teaching-demo?style=flat-square" alt="Stars">
</p>

> **课程位置** 阶段 2 · AI 应用工程入门 — 第 4 站
> [上一站 Semantic Kernel](https://ibqy.github.io/semantic-kernel-java-teaching-demo/) → **AgentScope 多智能体** → [下一站 SAA Graph](https://ibqy.github.io/spring-ai-alibaba-graph-demo/) ｜ [完整学习路线](https://ibqy.github.io/curriculum)

---

# AgentScope for Java 教学演示

基于 [AgentScope for Java v2](https://github.com/agentscope-ai/agentscope-java) 构建的教学演示项目，通过 16 个渐进式 Demo 帮助初学者快速掌握 AgentScope 的核心概念与实战技巧。

## 技术栈

| 组件 | 版本 |
|------|------|
| Spring Boot | 3.5.16 |
| AgentScope for Java | 2.0.4 |
| Java | 21 |
| 模型 | DashScope (qwen-plus) |

## Demo 一览

| 编号 | 名称 | 核心知识点 | API 端点 |
|------|------|-----------|----------|
| Demo01 | Hello Agent | HarnessAgent 创建、全局 Bean 注入、基本对话 | `/api/demo01/hello` |
| Demo02 | DashScope 模型 | 显式构建 DashScopeChatModel、Workspace | `/api/demo02/chat` |
| Demo03 | Call 调用 | `agent.call()` 同步调用模式 | `/api/demo03/call` |
| Demo04 | 流式输出 | `streamEvents()` 逐块增量接收、TextBlockDeltaEvent | `/api/demo04/stream` |
| Demo05 | 工具调用 | `@Tool` 注解、Toolkit 注册、自定义工具函数 | `/api/demo05/weather` |
| Demo06 | 多智能体协作 | SubagentDeclaration 声明式子智能体、Director 模式 | `/api/demo06/team` |
| Demo07 | 记忆与工作区 | MemoryConfig 长期记忆、CompactionConfig 压缩策略 | `/api/demo07/memory` |
| Demo08 | 结构化输出 | `call()` 传入目标类型、`reply.getStructuredData()` | `/api/demo08/movie` |
| Demo09 | Prompt 模板 | 动态 System Prompt、角色切换（老师/导游/评审） | `/api/demo09/persona` |
| Demo10 | 全能助理 | 多工具聚合（计算器+备忘）、maxIters 控制 | `/api/demo10/assistant` |
| Demo11 | 综合实战 | 任务管理+天气+计算+记忆+流式输出全组合 | `/api/demo11/task` + `/api/demo11/stream` |
| Demo12 | MCP 工具集成 | McpClientBuilder、StdIO/SSE 传输、MCP 工具自动发现与注册 | `/api/demo12/filesystem` + `/api/demo12/sse` |
| Demo13 | A2A 跨智能体通信 | A2aAgent、WellKnownAgentCardResolver、AgentCard 自动发现 | `/api/demo13/call` + `/api/demo13/auth` |
| Demo14 | RAG 知识库检索 | 应用层知识库、段落分块 + TF-IDF 检索、Generic/Agentic 双模式 | `/api/demo14/search` + `/api/demo14/ask` + `/api/demo14/ask-direct` |
| Demo15 | Plan Mode 与权限系统 | 先规划后执行、工具权限规则（ALLOW/DENY/ASK）、RuntimeContext 多用户隔离、AgentState 持久化 | `/api/demo15/plan` + `/api/demo15/permission` + `/api/demo15/multi-user` + `/api/demo15/state` |
| Demo16 | 事件驱动可观测 | streamEvents 全事件订阅：执行时间线 + 真实 Token 用量统计 | `/api/demo16/timeline` + `/api/demo16/usage` |

## 快速启动

### 前置条件

- JDK 21+
- Maven 3.8+
- DashScope API Key（[阿里云百炼平台](https://bailian.console.aliyun.com/) 申请）

### 环境变量

```bash
# Windows PowerShell
$env:DASHSCOPE_API_KEY="your-api-key-here"

# Linux / macOS
export DASHSCOPE_API_KEY="your-api-key-here"
```

### 运行

```bash
git clone <repo-url>
cd agentscope-java-teaching-demo
mvn spring-boot:run
```

启动后访问 `http://localhost:8080/api/demo01/hello?message=你好` 验证。

## 教学路径

建议按以下顺序学习，每个 Demo 都建立在前一个的基础上：

```
Demo01 Hello Agent     →  理解 HarnessAgent 与全局 Bean
    ↓
Demo02 DashScope 模型   →  手动构建模型、Workspace
    ↓
Demo03 Call 调用       →  同步调用模式 RuntimeContext
    ↓
Demo04 流式输出        →  streamEvents 实时增量事件
    ↓
Demo05 工具调用        →  @Tool + Toolkit 注册
    ↓
Demo06 多智能体协作     →  SubagentDeclaration 子智能体
    ↓
Demo07 记忆与工作区     →  MemoryConfig + CompactionConfig
    ↓
Demo08 结构化输出       →  类型安全的 JSON 反序列化
    ↓
Demo09 Prompt 模板     →  动态 System Prompt 角色工程
    ↓
Demo10 全能助理        →  多工具聚合 + maxIters
    ↓
Demo11 综合实战        →  全能力组合：任务+工具+记忆+流式
    ↓
Demo12 MCP 工具集成     →  标准协议接入外部工具生态
    ↓
Demo13 A2A 跨智能体通信  →  AgentCard 发现 + 远程 Agent 调用
    ↓
Demo14 RAG 知识库检索    →  应用层检索 + Generic/Agentic 双模式
    ↓
Demo15 Plan Mode 与权限  →  先规划后执行 + 工具权限 + 会话隔离 + 状态持久化
    ↓
Demo16 事件驱动可观测    →  全事件订阅：执行时间线 + 真实 Token 计量
    ↓
附录 生产化改造清单      →  超时/重试/SSE/成本/多租户/部署与上线检查
```

> 说明：Demo14 曾因 AgentScope Java 2.0 移除内置 rag 包而暂缓，
> 本次按官方建议在应用层实现检索后补全，编号与 Controller 包名保持一致。

## 实现边界

### 已实现 ✅

- 16 个渐进式 Demo 控制器，覆盖 AgentScope for Java 2.0 核心能力
- HarnessAgent / DashScopeChatModel / SubagentDeclaration 完整构建链
- 工具系统：`@Tool` 注解 + Toolkit 注册（天气、计算器、任务管理）
- MCP 工具集成：McpClientBuilder + StdIO/SSE 传输，接入标准协议工具生态
- A2A 跨智能体通信：A2aAgent + WellKnownAgentCardResolver，远程 Agent 发现与调用
- 应用层 RAG（demo14）：内存知识库 + 段落分块 + TF-IDF 检索，Generic 提示词注入与 Agentic 检索工具双模式
- 流式输出：`streamEvents()` 增量事件处理
- 结构化输出：目标类型反序列化
- 记忆与工作区：MemoryConfig + CompactionConfig
- Plan Mode + 权限系统（demo15）：先规划后执行、工具级 ALLOW/DENY/ASK 规则、RuntimeContext 多用户隔离、AgentState 文件持久化
- 事件驱动可观测（demo16）：全事件类型订阅、执行时间线聚合、ModelCallEndEvent 真实 Token 计量
- 全局异常处理：`@ControllerAdvice` + `ApiResponse` 统一响应包装
- 参数校验：Jakarta Validation（`@NotBlank`）+ `@Valid` + JSON Body 端点
- 47 个单元测试（JUnit 5），覆盖工具类纯逻辑 + 检索打分 + 事件聚合逻辑 + 异常处理

### 教学简化 ⚠️

- **工具实现**：WeatherTools 返回模拟数据，不接入真实天气 API
- **计算器**：使用 `ScriptEngineManager` JavaScript 引擎（JDK 15+ 已移除 Nashorn，仅作演示）
- **任务管理**：TaskTools 使用内存 `ConcurrentHashMap` 存储，重启丢失
- **多智能体**：Demo06 子智能体使用相同模型，未做角色差异化调优
- **RAG 检索**：Demo14 用 TF-IDF 词法检索代替 Embedding 向量检索，知识库存于内存，重启丢失

### 未实现 ❌

- 持久化存储（数据库/文件）
- 用户认证与权限控制
- 生产级错误重试与熔断
- 前端 UI 界面（仅提供 REST API）
- 向量数据库与 Embedding 服务（Demo14 仅演示词法检索）

## 测试

```bash
mvn test    # 47 个单元测试，约 4 秒
```

| 测试类 | 数量 | 覆盖范围 |
|--------|------|---------|
| AgentScopeApplicationTests | 1 | 启动类冒烟测试 |
| WeatherToolsTest | 3 | 天气工具默认/自定义单位、格式 |
| AssistantToolsTest | 6 | 计算器（适配 JDK 21 无 Nashorn）+ 备忘 |
| TaskToolsTest | 9 | 任务 CRUD、ID 自增、状态转换、错误处理 |
| KnowledgeBaseTest | 10 | 文档入库分块、中文分词、TF-IDF 排序、topK/无命中、重置 |
| KnowledgeToolsTest | 3 | 检索工具输出格式、无命中提示、top_k 截断 |
| GlobalExceptionHandlerTest | 6 | ApiResponse 包装、校验异常、缺参异常、未知异常 |
| AgentEventTimelineTest | 9 | 事件分类计数、Token 用量累计、增量只计数不入表、null 容错、超迭代标记 |

## 项目结构

```
agentscope-java-teaching-demo/
├── pom.xml                                      # Maven 配置，AgentScope 2.0.4
├── README.md
├── docs/
│   ├── public/
│   │   └── agentscope-java-teaching-guide.html   # 早期图文版（Demo01-11，SDK 2.0.1）
│   ├── 01-agent-hello-world.md                   # Demo01：Hello Agent
│   ├── 02-dashscope-model.md                     # Demo02：DashScope 模型
│   ├── 03-call.md                                # Demo03：Call 同步调用
│   ├── 04-streaming.md                           # Demo04：流式输出
│   ├── 05-tool-calling.md                        # Demo05：工具调用
│   ├── 06-multi-agent.md                         # Demo06：多智能体协作
│   ├── 07-memory-workspace.md                    # Demo07：记忆与工作区
│   ├── 08-structured-output.md                   # Demo08：结构化输出
│   ├── 09-prompt-template.md                     # Demo09：Prompt 模板
│   ├── 10-assistant.md                           # Demo10：全能助理
│   ├── 11-comprehensive-demo.md                  # Demo11：综合实战
│   ├── 12-mcp-integration.md                     # Demo12：MCP 工具集成
│   ├── 13-a2a-communication.md                   # Demo13：A2A 跨智能体通信
│   ├── 14-rag-knowledge-base.md                  # Demo14：RAG 知识库检索
│   ├── 15-plan-mode-permission.md                # Demo15：Plan Mode 与权限系统
│   ├── 16-event-observability.md                 # Demo16：事件驱动可观测
│   └── appendix-production-hardening.md          # 附录：从 Demo 到生产的改造清单
└── src/main/
    ├── java/com/xb/agentscope/
    │   ├── AgentScopeJavaTeachingDemoApplication.java  # 启动类
    │   ├── config/
    │   │   └── AgentScopeJavaConfig.java               # 全局 Bean：HarnessAgent/Toolkit/DashScopeChatModel
    │   └── controller/
    │       ├── demo01/  Demo01HelloAgentController.java
    │       ├── demo02/  Demo02DashScopeModelController.java
    │       ├── demo03/  Demo03CallController.java
    │       ├── demo04/  Demo04StreamController.java
    │       ├── demo05/  Demo05ToolCallingController.java / WeatherTools.java
    │       ├── demo06/  Demo06SubagentController.java
    │       ├── demo07/  Demo07MemoryWorkspaceController.java
    │       ├── demo08/  Demo08StructuredOutputController.java / Movie.java
    │       ├── demo09/  Demo09PromptController.java
    │       ├── demo10/  Demo10AssistantController.java / AssistantTools.java
    │       ├── demo11/  Demo11Controller.java / TaskTools.java
    │       ├── demo12/  Demo12Controller.java
    │       ├── demo13/  Demo13Controller.java
    │       ├── demo14/  Demo14Controller.java / KnowledgeBase.java / KnowledgeTools.java
    │       ├── demo15/  Demo15Controller.java
    │       └── demo16/  Demo16EventObservabilityController.java / AgentEventTimeline.java
    └── resources/
        └── application.yml                      # 配置：端口/DashScope API Key
```