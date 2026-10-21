package com.xb.agentscope.controller.demo14;

import com.xb.agentscope.common.ApiResponse;
import com.xb.agentscope.controller.demo14.KnowledgeBase.ScoredChunk;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.UserMessage;
import io.agentscope.core.tool.Toolkit;
import io.agentscope.harness.agent.HarnessAgent;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Demo14Controller - RAG 知识库检索增强示例控制器
 *
 * 教学定位：AgentScope Java 2.0 已把内置 rag 包标记为过时（forRemoval），
 * 官方建议"在应用层集成检索"。本 Demo 演示应用层 RAG 的完整链路：
 * 文档入库 → 分块 + TF-IDF 检索 → 两种接入 Agent 的模式。
 *
 * 两种模式对比：
 * - Generic 模式（/ask-direct）：应用代码固定先检索，再把结果拼进提示词，
 *   检索时机由代码决定，可控性强；
 * - Agentic 模式（/ask）：检索封装成 retrieve_knowledge 工具，
 *   由 Agent 自主决定何时检索、检索什么，更灵活。
 *
 * 接口一览：
 * - GET  /api/demo14/search      纯检索（不调用大模型），观察打分与排序
 * - GET  /api/demo14/documents   查看知识库文档
 * - POST /api/demo14/documents   新增文档（支持多段落）
 * - POST /api/demo14/ask         Agentic RAG 问答
 * - POST /api/demo14/ask-direct  Generic RAG 问答（返回答案 + 引用片段）
 * - POST /api/demo14/reset       重置为种子语料
 *
 * @author ibqy
 */
@RestController
@RequestMapping("/api/demo14")
public class Demo14Controller {

    /** 应用层知识库，所有请求共享同一实例 */
    private final KnowledgeBase knowledgeBase = new KnowledgeBase();

    /** Agentic 模式：带检索工具的 Agent */
    private final HarnessAgent ragAgent;

    /** Generic 模式：不带工具、只按注入资料作答的 Agent */
    private final HarnessAgent directAgent;

    public Demo14Controller() {
        Toolkit toolkit = new Toolkit();
        toolkit.registerTool(new KnowledgeTools(knowledgeBase));
        this.ragAgent = HarnessAgent.builder()
                .name("rag-agent")
                .sysPrompt("你是一名严谨的知识库问答助手。回答任何问题前，必须先调用 retrieve_knowledge "
                        + "工具检索本地知识库；只依据检索到的片段作答，并注明来源标题；"
                        + "如果检索结果为空或与问题无关，明确告知“知识库中没有相关资料”，绝不编造。")
                .model("dashscope:qwen-plus")
                .toolkit(toolkit).maxIters(4)
                .workspace(Paths.get(".agentscope/workspace-demo14"))
                .build();
        this.directAgent = HarnessAgent.builder()
                .name("rag-direct-agent")
                .sysPrompt("你是一名严谨的知识库问答助手。用户消息中会附带【参考资料】，"
                        + "请只依据参考资料回答并注明来源；资料不足时明确说明，不要编造。")
                .model("dashscope:qwen-plus")
                .workspace(Paths.get(".agentscope/workspace-demo14-direct"))
                .build();
    }

    /**
     * 纯检索：只跑知识库打分，不调用大模型
     *
     * 用途：直观看懂"检索"这一步——为什么这条命中、得分多少，
     * 与后面 LLM 的回答质量解耦排查。
     *
     * @param query 查询语句
     * @param topK  返回条数，默认 3
     * @return 按相关度降序排列的分块列表
     */
    @GetMapping("/search")
    public ApiResponse<List<ScoredChunk>> search(
            @RequestParam(defaultValue = "RAG 是什么") String query,
            @RequestParam(defaultValue = "3") int topK) {
        return ApiResponse.ok(knowledgeBase.retrieve(query, topK));
    }

    /**
     * 查看知识库中的全部文档
     *
     * @return 文档列表
     */
    @GetMapping("/documents")
    public ApiResponse<List<KnowledgeBase.Document>> documents() {
        return ApiResponse.ok(knowledgeBase.listDocuments());
    }

    /**
     * 新增文档入库（空行分隔的段落会被切成多个分块）
     *
     * @param request 标题 + 正文
     * @return 入库后的文档信息（含分块数量）
     */
    @PostMapping("/documents")
    public ApiResponse<KnowledgeBase.Document> addDocument(
            @Valid @RequestBody Demo14DocumentRequest request) {
        return ApiResponse.ok(knowledgeBase.addDocument(request.title(), request.content()));
    }

    /**
     * Agentic RAG 问答：由 Agent 自主决定何时调用检索工具
     *
     * @param request 用户问题
     * @return Agent 的文本回答
     */
    @PostMapping("/ask")
    public String ask(@Valid @RequestBody Demo14AskRequest request) {
        Msg reply = ragAgent.call(
                List.of(new UserMessage(request.question())), RuntimeContext.empty()).block();
        return reply != null ? reply.getTextContent() : "（无回复）";
    }

    /**
     * Generic RAG 问答：应用层固定先检索，再把资料注入提示词
     *
     * @param request 用户问题
     * @return 答案 + 本次引用的检索片段，便于对照答案与依据
     */
    @PostMapping("/ask-direct")
    public ApiResponse<DirectAnswer> askDirect(@Valid @RequestBody Demo14AskRequest request) {
        List<ScoredChunk> hits = knowledgeBase.retrieve(request.question(), 3);
        String context = hits.isEmpty()
                ? "（无检索结果）"
                : hits.stream()
                        .map(hit -> "【" + hit.title() + "】" + System.lineSeparator() + hit.content())
                        .collect(Collectors.joining(System.lineSeparator() + System.lineSeparator()));
        String prompt = """
                请仅依据以下参考资料回答问题；资料不足时明确说明“知识库中没有相关信息”，不要编造。
                === 参考资料 ===
                %s
                === 问题 ===
                %s
                """.formatted(context, request.question());
        Msg reply = directAgent.call(
                List.of(new UserMessage(prompt)), RuntimeContext.empty()).block();
        return ApiResponse.ok(new DirectAnswer(
                reply != null ? reply.getTextContent() : "（无回复）", hits));
    }

    /**
     * 重置知识库为教学种子语料
     *
     * @return 重置后的文档数量
     */
    @PostMapping("/reset")
    public ApiResponse<Integer> reset() {
        return ApiResponse.ok(knowledgeBase.reset());
    }

    /**
     * Generic RAG 的问答结果
     *
     * @param answer     模型答案
     * @param references 本次注入提示词的检索片段
     */
    public record DirectAnswer(String answer, List<ScoredChunk> references) {
    }
}
