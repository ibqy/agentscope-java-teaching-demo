# AgentScope for Java 教学演示

基于 [AgentScope for Java v2](https://github.com/agentscope-ai/agentscope-java) 构建的教学演示项目，通过 11 个渐进式 Demo 帮助初学者快速掌握 AgentScope 的核心概念与实战技巧。

## 技术栈

| 组件 | 版本 |
|------|------|
| Spring Boot | 3.4.4 |
| AgentScope for Java | 2.0.1 |
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
```

## 项目结构

```
agentscope-java-teaching-demo/
├── pom.xml                                      # Maven 配置，AgentScope 2.0.1
├── README.md
├── docs/
│   ├── agentscope-java-teaching-guide.html       # 交互式教学导航页
│   ├── 01-agent-hello-world.md                   # 入门：HarnessAgent 基本对话
│   ├── 02-streaming-memory.md                    # 流式输出与长期记忆
│   ├── 03-multi-agent.md                         # 多智能体协作
│   ├── 04-tool-calling.md                        # 工具注册与使用
│   └── 05-structured-prompt.md                   # 结构化输出与Prompt模板
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
    │       └── demo11/  Demo11Controller.java / TaskTools.java
    └── resources/
        └── application.yml                      # 配置：端口/DashScope API Key
```