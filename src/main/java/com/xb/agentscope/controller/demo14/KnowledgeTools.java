package com.xb.agentscope.controller.demo14;

import com.xb.agentscope.controller.demo14.KnowledgeBase.ScoredChunk;
import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;

import java.util.List;

/**
 * KnowledgeTools - 知识库检索工具（Agentic RAG 模式的入口）
 *
 * 把应用层的 KnowledgeBase 包装成 @Tool，Agent 会在对话中自主决定
 * 何时调用、用什么查询词检索。这正是 AgentScope 官方移除内置 rag 包后
 * 推荐的做法："在应用层集成检索"，再以工具形式接入 Agent。
 *
 * @author ibqy
 */
public class KnowledgeTools {

    private final KnowledgeBase knowledgeBase;

    public KnowledgeTools(KnowledgeBase knowledgeBase) {
        this.knowledgeBase = knowledgeBase;
    }

    /**
     * 从知识库检索与问题相关的片段
     *
     * @param query 检索词，应包含问题中的关键实体与概念
     * @param topK  返回的最大片段数（可选），为 null 时默认 3
     * @return 格式化后的检索结果文本，供模型阅读引用
     */
    @Tool(name = "retrieve_knowledge",
            description = "检索本地知识库。回答知识库相关问题前必须先调用本工具，"
                    + "根据返回的片段作答并注明来源；没有命中时要如实说明知识库中没有资料。")
    public String retrieveKnowledge(
            @ToolParam(name = "query", description = "检索关键词或问题描述", required = true) String query,
            @ToolParam(name = "top_k", description = "返回的最大片段数，默认 3") Integer topK) {
        int limit = topK == null ? 3 : topK;
        List<ScoredChunk> hits = knowledgeBase.retrieve(query, limit);
        if (hits.isEmpty()) {
            return "知识库中未找到与「" + query + "」相关的内容。";
        }
        StringBuilder sb = new StringBuilder("检索到 " + hits.size() + " 个相关片段：");
        for (int i = 0; i < hits.size(); i++) {
            ScoredChunk hit = hits.get(i);
            sb.append(System.lineSeparator())
                    .append("【片段 ").append(i + 1)
                    .append(" | 来源：").append(hit.title())
                    .append(" | 相关度：").append(String.format("%.3f", hit.score()))
                    .append("】").append(System.lineSeparator())
                    .append(hit.content());
        }
        return sb.toString();
    }
}
