# Demo12：MCP 工具集成 — 接入标准协议工具生态

## 知识点

- MCP（Model Context Protocol）协议概念与三种传输方式
- `McpClientBuilder` 构建 MCP 客户端
- `Toolkit.registerMcpClient()` 将 MCP 工具注册到 AgentScope
- StdIO 与 SSE 两种传输方式的实际用法

## 什么是 MCP

MCP（Model Context Protocol）是连接 AI 应用与外部工具/数据源的**开放标准协议**。它定义了一套统一的接口，让 AI Agent 能够以标准化的方式发现、调用外部工具——类似于 USB 接口让各种设备都能接入电脑。

AgentScope Java 原生支持 MCP 协议，核心类位于 `io.agentscope.core.tool.mcp` 包中，**无需额外依赖**。

### 三种传输方式

| 传输方式 | 场景 | 说明 |
|---------|------|------|
| **StdIO** | 本地工具 | 启动子进程，通过标准输入/输出通信 |
| **SSE** | 远程工具服务器 | HTTP Server-Sent Events 流式通信 |
| **HTTP** | 无状态远程工具 | Streamable HTTP 请求/响应 |

## StdIO 传输：本地文件系统工具

StdIO 是最常用的本地工具接入方式。AgentScope 会启动一个 MCP 服务器子进程，通过标准输入/输出与之通信。

```java
// 启动 MCP 文件系统工具服务器
McpClientWrapper mcpClient = McpClientBuilder.create("fs-mcp")
        .stdioTransport("npx", "-y",
                "@modelcontextprotocol/server-filesystem", directory)
        .buildAsync()
        .block();
```

`stdioTransport` 的参数就是启动子进程的命令行：
- `npx -y @modelcontextprotocol/server-filesystem /tmp`
- 需要本地安装 Node.js（>= 18）

### 注册 MCP 工具到 Toolkit

```java
Toolkit toolkit = new Toolkit();
toolkit.registerMcpClient(mcpClient).block();
```

`registerMcpClient` 会自动调用 MCP 的 `tools/list` 方法，发现服务器提供的所有工具（如 `read_file`、`write_file`、`list_directory`），并注册到 Toolkit 中。

### 构建拥有 MCP 工具的 Agent

```java
HarnessAgent agent = HarnessAgent.builder()
        .name("mcp-fs-agent")
        .sysPrompt("你是一位文件管理助手。你可以通过 MCP 工具操作文件系统中的文件，"
                + "包括读取文件内容、写入文件、列出目录等。请根据用户需求合理使用工具。")
        .model("dashscope:qwen-plus")
        .toolkit(toolkit)
        .maxIters(5)
        .workspace(Paths.get(".agentscope/workspace-demo12"))
        .build();
```

Agent 会自主决定调用哪些 MCP 工具来完成用户请求——整个过程对 Agent 来说与调用本地 `@Tool` 方法没有区别。

### 接口调用

```
GET /api/demo12/filesystem?directory=/tmp&message=请列出当前目录下的所有文件
```

## SSE 传输：远程工具服务器

SSE 适合连接部署在远程的 MCP 工具服务器，通过 HTTP Server-Sent Events 流式通信。

```java
McpClientWrapper mcpClient = McpClientBuilder.create("remote-mcp")
        .sseTransport("http://localhost:3001/sse")
        .buildAsync()
        .block();
```

与 StdIO 不同，SSE 不需要启动子进程，而是直接连接远程服务器。适合团队共享的工具服务器场景。

### 接口调用

```
GET /api/demo12/sse?endpoint=http://localhost:3001/sse&message=请列出你可以使用的所有工具
```

## MCP 与本地工具的对比

| 维度 | 本地 `@Tool` | MCP 工具 |
|------|-------------|---------|
| 注册方式 | `toolkit.registerTool(new MyTools())` | `toolkit.registerMcpClient(mcpClient)` |
| 工具来源 | 项目内 Java 方法 | 任何 MCP 兼容服务器 |
| 部署位置 | 与 Agent 同进程 | 独立进程/远程服务器 |
| 工具发现 | 编译时确定 | 运行时动态发现 |
| 生态规模 | 自己编写 | MCP 生态中数千个现成工具 |

两者可以共存于同一个 Toolkit 中——Agent 可以同时使用本地工具和 MCP 工具。

## 小结

- MCP 是连接 AI Agent 与外部工具的**标准协议**，AgentScope Java 原生支持
- `McpClientBuilder` 支持 StdIO、SSE、HTTP 三种传输方式
- `toolkit.registerMcpClient()` 自动发现并注册 MCP 工具，Agent 透明使用
- StdIO 适合本地工具（需 Node.js），SSE 适合远程工具服务器
- MCP 工具与本地 `@Tool` 可以共存，Agent 统一调度
