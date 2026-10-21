# Demo14：RAG 知识库检索 — 让 Agent 基于私有知识回答

> **导航**：[← 上一篇：Demo13 A2A 跨智能体通信](./13-a2a-communication.md) · [返回首页](./index.md) · [下一篇：Demo15 Plan Mode 与权限系统 →](./15-plan-mode-permission.md)
>
> **本章源码**：[Demo14Controller](../src/main/java/com/xb/agentscope/controller/demo14/Demo14Controller.java) · [KnowledgeBase](../src/main/java/com/xb/agentscope/controller/demo14/KnowledgeBase.java) · [KnowledgeTools](../src/main/java/com/xb/agentscope/controller/demo14/KnowledgeTools.java)

## 知识点

- RAG（Retrieval-Augmented Generation）的核心思想与价值
- AgentScope Java 2.0 对 RAG 的定位：内置 rag 包已过时，检索放到应用层
- 应用层 RAG 完整链路：文档入库 → 分块 → 分词建索引 → TF-IDF 打分召回
- 两种接入模式：Generic（代码固定检索 + 提示词注入）与 Agentic（检索工具化）

## 为什么需要 RAG

大模型的知识有两个硬伤：

1. **训练数据有时效性**——问它"我们公司最新的报销制度"，它不可能知道；
2. **会一本正经地编**——不知道也会硬答（幻觉）。

RAG 的解法很直接：**先查资料，再回答**。把私有文档放进知识库，提问时先检索出相关片段，连同问题一起交给模型，让它"看着资料答题"。

```
用户提问 → 检索知识库 → 命中的片段 + 问题 → 大模型 → 有依据的回答
```

## AgentScope Java 2.0 的现状

AgentScope Java 曾内置 `io.agentscope.core.rag` 包（`Knowledge`、`RAGMode`、`KnowledgeRetrievalTools`），但在 2.0.4 中该包已标记为：

```java
@Deprecated(forRemoval = true, since = "2.0.0")
```

官方注释写得很明确：**"The rag package is removed; integrate retrieval at the application layer."**

这其实是好事——检索方案千差万别（Elasticsearch、pgvector、Milvus、云厂商知识库……），框架如果把检索绑死在核心包里反而限制选择。因此 Demo14 演示的是**应用层 RAG**：自己实现知识库与检索，再把检索结果以工具或提示词的方式接入 Agent。

## Demo14 实现链路

### 1. 文档入库与分块

[KnowledgeBase](../src/main/java/com/xb/agentscope/controller/demo14/KnowledgeBase.java) 把文档按**空行分段落**切成多个"分块（chunk）"：

- 分块是检索的最小单位，块太大噪声多，块太小上下文断裂；
- 每个分块独立建索引，命中后只把这一小段交给模型，节省 token。

### 2. 分词与打分

教学版用 **TF-IDF 词法检索**，不依赖 Embedding 服务即可跑通：

| 步骤 | 做法 |
|------|------|
| 分词 | 英文/数字按单词切分，中文按二元组（bigram）切分，过滤疑问词停用词 |
| 标题加权 | 标题词按双倍权重计入索引（标题是强相关信号） |
| IDF | 词越稀有权重越高：`ln(1 + 总块数 / 含该词的块数)` |
| 词频 | `1 + ln(tf)`，对数饱和避免长文刷分 |
| 归一化 | 按查询词总权重归一到 `(0, 1]`，得分可直接作为相关度展示 |

### 3. 两种接入模式

**Generic 模式**（`/ask-direct`）：应用代码固定先检索，把片段拼进提示词：

```java
List<ScoredChunk> hits = knowledgeBase.retrieve(question, 3);
String prompt = "请仅依据以下参考资料回答...\n" + context + "\n问题：" + question;
Msg reply = directAgent.call(List.of(new UserMessage(prompt)), RuntimeContext.empty()).block();
```

**Agentic 模式**（`/ask`）：把检索封装成 `@Tool`，由 Agent 自主决定何时检索：

```java
public class KnowledgeTools {
    @Tool(name = "retrieve_knowledge", description = "检索本地知识库...")
    public String retrieveKnowledge(
            @ToolParam(name = "query", required = true) String query,
            @ToolParam(name = "top_k") Integer topK) {
        // 返回带来源与相关度的片段文本
    }
}

Toolkit toolkit = new Toolkit();
toolkit.registerTool(new KnowledgeTools(knowledgeBase));
```

| 维度 | Generic（`/ask-direct`） | Agentic（`/ask`） |
|------|--------------------------|-------------------|
| 检索时机 | 每次请求必检索，代码控制 | Agent 按需决定 |
| 可控性 | 强（流程固定、易测试） | 弱（依赖模型决策） |
| 灵活性 | 低（闲聊也会触发检索） | 高（可多轮、可改写查询词） |
| 额外成本 | 每次多一次检索 | 多一轮工具调用（token/延迟） |
| 适用场景 | 客服问答、固定知识域 | 通用助手、混合任务 |

## 接口调用

```bash
# 1. 纯检索（不调用大模型）：观察分块、得分与排序
curl "http://localhost:8080/api/demo14/search?query=如何注册工具&topK=3"

# 2. 查看知识库文档
curl "http://localhost:8080/api/demo14/documents"

# 3. 新增文档（空行分隔的段落会切成多个分块）
curl -X POST http://localhost:8080/api/demo14/documents \
  -H "Content-Type: application/json" \
  -d '{"title":"报销制度","content":"差旅报销需在出差结束后 7 个工作日内提交。\n\n单笔超过 5000 元需部门负责人审批。"}'

# 4. Agentic RAG：Agent 自主调用 retrieve_knowledge
curl -X POST http://localhost:8080/api/demo14/ask \
  -H "Content-Type: application/json" \
  -d '{"question":"工具怎么注册？"}'

# 5. Generic RAG：返回答案 + 本次引用的片段
curl -X POST http://localhost:8080/api/demo14/ask-direct \
  -H "Content-Type: application/json" \
  -d '{"question":"记忆压缩怎么配置？"}'

# 6. 重置为教学种子语料
curl -X POST http://localhost:8080/api/demo14/reset
```

## 生产化演进路径

教学版为了"零外部依赖可跑通"选择了 TF-IDF，生产环境建议逐步升级：

| 环节 | 教学版 | 生产版 |
|------|--------|--------|
| 检索算法 | TF-IDF 词法匹配 | Embedding 向量检索 + 关键词混合检索 |
| 向量化 | 无 | DashScope text-embedding 等模型 |
| 存储 | 内存 List | pgvector / Milvus / Elasticsearch |
| 分块 | 按空行段落 | 按语义/滑窗切分 + 重叠 |
| 重排序 | 无 | Cross-Encoder Rerank |
| 引用 | 来源标题 | 文档 ID + 页码 + 原文定位 |

## 小结

- RAG = 先检索、再生成，让模型基于私有资料回答，降低幻觉
- AgentScope Java 2.0 已移除内置 rag 包，官方推荐**在应用层实现检索**
- 检索质量是 RAG 效果的上限：分块策略、分词、打分方式直接决定召回
- Generic 模式可控、Agentic 模式灵活，按业务场景选择或组合使用
- 本 Demo 的 `KnowledgeBase` 接口与存储实现解耦，后续替换为向量检索时 Agent 代码无需改动
