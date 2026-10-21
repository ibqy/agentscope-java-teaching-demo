package com.xb.agentscope.controller.demo14;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * KnowledgeBase - 应用层 RAG 的轻量内存知识库
 *
 * AgentScope Java 2.0 已移除内置 rag 包（@Deprecated forRemoval），
 * 官方建议在应用层实现检索。本类即演示"应用层 RAG"的最小实现：
 * 文档入库 → 按段落分块 → 分词建索引 → TF-IDF 打分召回。
 *
 * 教学简化：
 * - 用 TF-IDF 词法检索代替向量检索，无需 Embedding 服务即可跑通
 * - 中文按二元组（bigram）切分，英文按单词切分
 * - 数据存于内存，重启丢失
 *
 * @author ibqy
 */
public class KnowledgeBase {

    /**
     * 检索命中的分块（含相关度得分）
     *
     * @param documentId 所属文档 ID
     * @param title      文档标题
     * @param content    分块正文
     * @param score      相关度得分，范围 (0, 1]，越大越相关
     */
    public record ScoredChunk(String documentId, String title, String content, double score) {
    }

    /**
     * 入库后的文档信息
     *
     * @param id         文档 ID
     * @param title      文档标题
     * @param content    文档全文
     * @param chunkCount 切分后的分块数量
     */
    public record Document(String id, String title, String content, int chunkCount) {
    }

    /**
     * 内部分块：正文 + 词频表
     */
    private record Chunk(String documentId, String title, String content, Map<String, Integer> termFreq) {
    }

    /** 分词正则：连续英文/数字视为一个词，连续中文按 bigram 切分 */
    private static final Pattern TOKEN_PATTERN = Pattern.compile("[a-z0-9_]+|[\\u4e00-\\u9fa5]+");

    /** 英文停用词：避免 "the/what/how" 之类噪声词干扰打分 */
    private static final Set<String> STOP_WORDS = Set.of(
            "the", "a", "an", "is", "are", "was", "were", "be", "of", "to", "in", "on",
            "for", "and", "or", "what", "how", "why", "do", "does", "please");

    /** 中文疑问/连接类 bigram 停用词：
     * 这类词几乎出现在任何问句里，保留会误伤排序（如“如何”把无关段落顶上来） */
    private static final Set<String> STOP_BIGRAMS = Set.of(
            "如何", "怎么", "什么", "为何", "是否", "可以", "哪些", "请问", "一下",
            "这个", "那个", "以及", "并且", "还是", "就是", "因为", "所以", "需要");

    private final Map<String, Document> documents = new LinkedHashMap<>();
    private final List<Chunk> chunks = new ArrayList<>();
    private final AtomicLong docCounter = new AtomicLong(1);

    /**
     * 构造时装载教学种子语料，保证 Demo 开箱即用
     */
    public KnowledgeBase() {
        seed();
    }

    /**
     * 新增文档：按空行切分为多个分块后建立词频索引
     *
     * @param title   文档标题
     * @param content 文档正文，支持多段落（空行分隔）
     * @return 入库后的文档信息
     * @throws IllegalArgumentException 标题或正文为空时抛出
     */
    public synchronized Document addDocument(String title, String content) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("title 不能为空");
        }
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("content 不能为空");
        }
        String id = "doc-" + docCounter.getAndIncrement();
        String[] paragraphs = content.strip().split("\\R\\s*\\R");
        int chunkCount = 0;
        for (String paragraph : paragraphs) {
            if (paragraph.isBlank()) {
                continue;
            }
            Map<String, Integer> termFreq = new HashMap<>();
            for (String token : tokenize(paragraph)) {
                termFreq.merge(token, 1, Integer::sum);
            }
            // 标题是强相关信号：标题词按双倍权重计入索引
            for (String token : tokenize(title)) {
                termFreq.merge(token, 2, Integer::sum);
            }
            chunks.add(new Chunk(id, title.strip(), paragraph.strip(), termFreq));
            chunkCount++;
        }
        Document document = new Document(id, title.strip(), content.strip(), chunkCount);
        documents.put(id, document);
        return document;
    }

    /**
     * 列出当前知识库中的全部文档
     *
     * @return 按入库顺序排列的文档列表
     */
    public synchronized List<Document> listDocuments() {
        return List.copyOf(documents.values());
    }

    /**
     * 检索与查询最相关的分块
     *
     * 打分方式：对每个查询词计算 IDF 权重，再乘以分块内词频（对数饱和），
     * 最后按查询词总权重归一化到 (0, 1]。
     *
     * @param query 查询语句
     * @param topK  返回的最大条数，自动限制在 1~20
     * @return 按相关度降序排列的分块列表；无命中时返回空列表
     */
    public synchronized List<ScoredChunk> retrieve(String query, int topK) {
        int limit = Math.max(1, Math.min(topK, 20));
        Set<String> queryTokens = new LinkedHashSet<>(tokenize(query));
        if (queryTokens.isEmpty() || chunks.isEmpty()) {
            return List.of();
        }

        // 计算查询词的 IDF 权重：包含该词的分块越少，词越"稀有"，权重越高
        Map<String, Double> idfCache = new HashMap<>();
        double totalWeight = 0;
        for (String token : queryTokens) {
            long docFreq = chunks.stream().filter(c -> c.termFreq().containsKey(token)).count();
            double idf = Math.log(1 + (double) chunks.size() / Math.max(docFreq, 1));
            idfCache.put(token, idf);
            totalWeight += idf;
        }

        List<ScoredChunk> results = new ArrayList<>();
        for (Chunk chunk : chunks) {
            double matchedWeight = 0;
            for (String token : queryTokens) {
                Integer freq = chunk.termFreq().get(token);
                if (freq != null) {
                    // 1 + ln(tf)：词频越高贡献越大，但对数饱和避免长文刷分
                    matchedWeight += idfCache.get(token) * (1 + Math.log(freq));
                }
            }
            if (matchedWeight <= 0) {
                continue;
            }
            double score = Math.min(1.0, matchedWeight / totalWeight);
            results.add(new ScoredChunk(chunk.documentId(), chunk.title(), chunk.content(), score));
        }
        results.sort(Comparator.comparingDouble(ScoredChunk::score).reversed());
        return results.size() > limit ? new ArrayList<>(results.subList(0, limit)) : results;
    }

    /**
     * 重置知识库：清空所有文档并重新装载种子语料
     *
     * @return 重置后的文档数量
     */
    public synchronized int reset() {
        documents.clear();
        chunks.clear();
        docCounter.set(1);
        seed();
        return documents.size();
    }

    /**
     * 分词：英文/数字按单词切分，中文按二元组（bigram）切分
     *
     * @param text 原始文本
     * @return 词元列表
     */
    static List<String> tokenize(String text) {
        List<String> tokens = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return tokens;
        }
        Matcher matcher = TOKEN_PATTERN.matcher(text.toLowerCase(Locale.ROOT));
        while (matcher.find()) {
            String token = matcher.group();
            if (token.charAt(0) < 128) {
                if (!STOP_WORDS.contains(token)) {
                    tokens.add(token);
                }
            } else if (token.length() == 1) {
                tokens.add(token);
            } else {
                for (int i = 0; i < token.length() - 1; i++) {
                    String bigram = token.substring(i, i + 2);
                    if (!STOP_BIGRAMS.contains(bigram)) {
                        tokens.add(bigram);
                    }
                }
            }
        }
        return tokens;
    }

    /**
     * 装载教学种子语料，覆盖本项目各 Demo 的核心概念
     */
    private void seed() {
        addDocument("AgentScope Java 简介",
                "AgentScope for Java 是面向 Java 生态的智能体开发框架，提供 ReActAgent、HarnessAgent 等开箱即用的智能体实现，默认接入 DashScope（通义千问）等大模型。\n\n"
                        + "HarnessAgent 是框架提供的装配式智能体，通过 Builder 组合模型、系统提示词、工具包、记忆与工作空间，适合快速构建生产级 Agent。");
        addDocument("工具调用 Toolkit",
                "在方法上添加 @Tool 注解、参数上添加 @ToolParam 注解，再用 Toolkit.registerTool(new XxxTools()) 注册，普通 Java 方法即可变成模型可调用的工具。\n\n"
                        + "Agent 会依据工具描述自主决定是否调用以及如何填写参数；maxIters 控制工具调用循环的最大迭代次数，防止死循环。");
        addDocument("记忆与上下文压缩",
                "MemoryConfig 开启长期记忆，让 Agent 跨轮次记住对话内容；CompactionConfig 在消息数或 token 数超过阈值时自动压缩历史，节省上下文窗口。\n\n"
                        + "CompactionConfig 常用参数：triggerMessages 为触发压缩的消息数，triggerTokens 为触发的 token 数，keepMessages 为压缩后保留的最近消息数。");
        addDocument("MCP 与 A2A",
                "MCP（Model Context Protocol）是工具接入标准，通过 McpClientBuilder 支持 StdIO 和 SSE 两种传输，自动发现并注册外部工具。\n\n"
                        + "A2A（Agent-to-Agent）是跨智能体通信标准，通过 WellKnownAgentCardResolver 访问 /.well-known/agent-card.json 自动发现远程 Agent，再用 A2aAgent 调用。");
        addDocument("Plan Mode 与权限系统",
                "Plan Mode 让 Agent 先输出执行计划再逐步执行，适合复杂多步骤任务。\n\n"
                        + "权限系统为工具配置 ALLOW、DENY、ASK 三类规则，结合 RuntimeContext 实现多用户隔离与 AgentState 持久化。");
        addDocument("RAG 检索增强生成",
                "RAG 通过先检索知识库、再把命中的资料注入提示词或作为工具结果，让模型基于私有知识回答，降低幻觉。\n\n"
                        + "常见两种模式：Generic 模式在每轮推理前自动检索并注入上下文；Agentic 模式把检索封装成工具，由 Agent 自主决定何时检索。\n\n"
                        + "AgentScope Java 2.0 已移除内置 rag 包并标记为过时，推荐在应用层实现知识库与检索，再通过工具或提示词注入的方式接入 Agent。");
    }
}
