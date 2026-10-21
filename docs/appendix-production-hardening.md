# 附录：从 Demo 到生产 — 落地改造清单

> **导航**：[← 上一篇：Demo16 事件驱动可观测](./16-event-observability.md) · [返回首页](./index.md)

本附录不新增 Demo，而是把 16 个示例里已经出现的能力串成一份**上线前的改造清单**。每一项都标注「当前实现（对应 Demo）→ 生产差距 → 改造方向」，帮助你从“能跑通”走到“敢上线”。

## 现状与差距

| 能力 | 当前实现 | 生产差距 | 改造方向 |
|------|---------|---------|---------|
| 同步调用 | `call().block()`（Demo01/03） | 阻塞线程、无超时 | 异步编排 + 超时 + 降级 |
| 流式输出 | `collectList().block()` 拼接后返回（Demo04/11/16） | 不是真正的流式响应 | SSE / WebSocket 逐事件推送 |
| 工具调用 | 模拟天气、内存任务（Demo05/10/11） | 无真实依赖、无工具级超时 | 真实 API + 超时 + 幂等 |
| 记忆 | `MemoryConfig` + 压缩（Demo07/11） | 单机、无清理策略 | 会话 TTL + 持久化 |
| 结构化输出 | 类型反序列化（Demo08） | 无 schema 版本管理 | 校验 + 兼容策略 |
| 多智能体 | 子 Agent 同模型（Demo06） | 成本与角色未分化 | 按角色选模型、限制轮次 |
| MCP / A2A | 直连外部服务（Demo12/13） | 无连接治理 | 连接池 + 认证 + 重试 |
| RAG | TF-IDF 内存知识库（Demo14） | 召回质量与规模受限 | Embedding + 向量库 |
| 权限 / 状态 | 文件状态存储（Demo15） | 多实例不共享 | 数据库 / Redis |
| 可观测 | 事件时间线（Demo16） | 无指标与告警 | Micrometer + 日志 + 追踪 |

## 1. 流式输出：从“收集后返回”到 SSE

Demo04/11 为了便于 curl 观察，把事件流 `collectList().block()` 后一次性返回。生产环境应直接把增量推给前端：

```java
@GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public SseEmitter stream(@RequestParam String message) {
    SseEmitter emitter = new SseEmitter(0L);   // 0 = 不超时，由业务自行控制
    assistantAgent.streamEvents(new UserMessage(message), RuntimeContext.empty())
            .subscribe(
                    event -> {
                        if (event instanceof TextBlockDeltaEvent tde) {
                            try {
                                emitter.send(tde.getDelta());
                            } catch (IOException e) {
                                emitter.completeWithError(e);
                            }
                        }
                    },
                    emitter::completeWithError,
                    emitter::complete);
    return emitter;
}
```

要点：

- **完成与异常回调**必须绑定 `complete` / `completeWithError`，否则连接泄漏
- 长连接需要网关层的读超时、心跳与断线重连策略
- 需要背压时改用 WebFlux 的 `Flux<ServerSentEvent<String>>`
- 首 token 延迟比总耗时更能反映用户体验，可从第一个 `TextBlockDeltaEvent` 的时间戳统计（思路同 Demo16）

## 2. 弹性：超时、重试、熔断、限流

Agent 调用链包含模型、工具、外部 MCP/A2A 服务，任何一段都可能变慢或失败：

```java
agent.call(messages, RuntimeContext.empty())
     .timeout(Duration.ofSeconds(60))              // 整条调用链超时
     .onErrorResume(ex -> fallbackReply(ex));      // 降级：缓存答案 / 静态话术
```

| 关注点 | 建议做法 |
|--------|---------|
| 超时 | 模型调用 30–60s、工具调用 3–10s，分别设置 |
| 重试 | 仅对幂等操作重试；写文件、下单等工具禁止盲目重试 |
| 熔断 | 外部 MCP/A2A 持续失败时快速失败，避免拖垮线程池 |
| 限流 | 按用户 / 会话 / 全局三级限流，防止单用户打满配额 |
| 降级 | 返回缓存答案或提示“稍后再试”，不要直接抛 500 |

> 熔断与限流可选用 Resilience4j、Bucket4j 或网关能力，本项目未引入这些依赖，属于生产改造项。

## 3. 安全与多租户

| 风险 | 现有基础 | 生产要求 |
|------|---------|---------|
| API Key 泄漏 | `application.yml` 使用 `${DASHSCOPE_API_KEY}` 环境变量 | 密钥托管（KMS / 配置中心），禁止入库 |
| 权限越界 | Demo15 的 `ALLOW / DENY / ASK` 工具规则 | 按角色下发权限，敏感工具强制 ASK |
| 用户串话 | Demo15 `RuntimeContext(sessionId, userId)` | 身份从鉴权上下文获取，禁止客户端任意传入 |
| 远程调用认证 | Demo13 `authHeaders` | Token 轮换、短有效期、最小权限 |
| 输入攻击 | Demo11 `@Valid` + 全局异常 | 长度限制、敏感词、Prompt 注入防护 |
| 数据外泄 | 工具返回值未脱敏 | 输出过滤、审计日志、数据分级 |

多租户的关键是**三层隔离**：会话（`sessionId`）、用户（`userId`）、工作区（`workspace` 路径）。三者缺一，都可能出现 A 用户看到 B 用户上下文的问题。

## 4. 成本与性能

- **Token 计量**：Demo16 从 `ModelCallEndEvent` 读取真实用量，生产可按 `userId/sessionId` 聚合，做预算与告警
- **上下文压缩**：Demo07/11 的 `CompactionConfig` 控制窗口；生产还要设置会话 TTL 与历史归档
- **模型分级**：简单意图用轻量模型，复杂规划用强模型；子 Agent 不必与主 Agent 同模型
- **缓存**：相同问题 / 相同工具结果可缓存，注意知识时效性
- **并发**：限制单会话并发轮次，避免重复计费与上下文竞争

```text
成本优化优先级：减少无效轮次 > 压缩上下文 > 模型分级 > 结果缓存
```

## 5. 状态与持久化

Demo15 用 `JsonFileAgentStateStore` 演示状态保存，适合单机教学：

| 问题 | 文件存储 | 生产方案 |
|------|---------|---------|
| 多实例共享 | 不支持 | Redis / 数据库 |
| 并发写入 | 有覆盖风险 | 分布式锁 / 乐观锁 |
| 容量与清理 | 无限增长 | TTL、归档、定期清理 |
| 灾备 | 依赖磁盘 | 备份与恢复演练 |

工作区（workspace）同样需要配额与清理策略，避免上传文件与中间产物占满磁盘。

## 6. 可观测性：从时间线到指标

Demo16 已把事件聚合成执行时间线，生产可在此基础上补齐三层：

| 层级 | 内容 | 关键指标 |
|------|------|---------|
| 指标 | Micrometer / Prometheus（需引入 Actuator，本项目未引入） | QPS、错误率、P95 耗时、首 token 延迟、Token/请求 |
| 日志 | 结构化 JSON + `requestId`（`GlobalExceptionHandler` 已生成） | 每次调用的模型、轮次、工具、耗时 |
| 追踪 | OpenTelemetry（可选） | 模型调用、工具调用、MCP/A2A 远程调用跨度 |

建议至少监控：**工具调用成功率**、**平均迭代轮次**、**单会话 Token 消耗**、**超时与降级次数**。这些指标比单纯的 QPS 更能反映 Agent 的健康度。

## 7. 测试策略

项目当前的 47 个测试全部聚焦**与框架解耦的纯逻辑**（工具、TF-IDF 检索、事件聚合、异常处理），这是最划算的一层：

| 层级 | 测什么 | 现状 |
|------|--------|------|
| 单元测试 | 工具函数、检索打分、事件聚合、异常映射 | ✅ 47 个 |
| 组件测试 | Controller + Mock Agent / 假模型 | 可补充 |
| 契约测试 | MCP 工具 schema、A2A AgentCard | 可补充 |
| 回归评测 | 固定问题集 + 期望要点，Prompt 变更后跑一遍 | 建议建立 |

Agent 的输出具有不确定性，回归评测不要做字符串全等，而应断言**关键实体、工具调用序列、是否命中知识片段**。

## 8. 部署清单

多阶段构建示例：

```dockerfile
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q -DskipTests dependency:go-offline
COPY src ./src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
ENV JAVA_OPTS="-Xms256m -Xmx512m"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
```

上线前确认：

- 配置外置：`DASHSCOPE_API_KEY`、端口、workspace 路径均通过环境变量注入
- 健康检查：引入 `spring-boot-starter-actuator` 后暴露 `/actuator/health`
- 优雅停机：`server.shutdown=graceful`，等待进行中的 Agent 调用结束
- 资源限制：容器内存与 JVM 堆匹配，避免 OOMKilled
- 水平扩展：状态存到共享存储，否则会话会漂移

## 9. 上线检查清单

- [ ] 所有模型/工具调用都有超时
- [ ] 失败路径有降级话术，不向前端抛裸异常
- [ ] 密钥来自环境变量或密钥管理，未写入代码与镜像
- [ ] `sessionId` / `userId` 来自可信身份，而非客户端任意传参
- [ ] 敏感工具配置 `DENY` 或 `ASK` 权限规则
- [ ] 会话状态与工作区有 TTL / 清理策略
- [ ] 记录 Token 用量、首 token 延迟、工具成功率
- [ ] 日志包含 `requestId`，可串联一次完整调用
- [ ] 关键工具与检索逻辑有单元测试
- [ ] Prompt 变更后有回归用例集
- [ ] 容器健康检查、优雅停机、资源限制已配置
- [ ] 多实例部署时会话状态已共享

## 相关章节

| 主题 | 对应章节 |
|------|---------|
| 流式事件 | [Demo04 流式输出](./04-streaming.md)、[Demo16 事件驱动可观测](./16-event-observability.md) |
| 记忆与压缩 | [Demo07 记忆与工作区](./07-memory-workspace.md)、[Demo11 综合实战](./11-comprehensive-demo.md) |
| 工具与外部协议 | [Demo05 工具调用](./05-tool-calling.md)、[Demo12 MCP](./12-mcp-integration.md)、[Demo13 A2A](./13-a2a-communication.md) |
| 权限、隔离与状态 | [Demo15 Plan Mode 与权限系统](./15-plan-mode-permission.md) |
| 检索质量 | [Demo14 RAG 知识库检索](./14-rag-knowledge-base.md) |
| 统一响应与异常 | [Demo11 综合实战 · 工程化配套](./11-comprehensive-demo.md#工程化配套统一响应与异常处理) |

> 说明：本附录中的 SSE、熔断、Actuator、向量库等均为**生产改造建议**，本项目为保证教学可运行性未引入对应依赖。改造时请先补依赖，再按清单逐项验证。
