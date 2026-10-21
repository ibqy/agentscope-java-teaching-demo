package com.xb.agentscope.controller.demo14;

import com.xb.agentscope.controller.demo14.KnowledgeBase.ScoredChunk;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 知识库检索单元测试
 *
 * 心得：检索质量是 RAG 效果的上限，必须覆盖：
 * 1. 入库与分段（chunk）逻辑
 * 2. 相关文档排序（检索的意义所在）
 * 3. topK 截断与无命中场景
 * 4. 参数校验与重置
 */
class KnowledgeBaseTest {

    private KnowledgeBase knowledgeBase;

    @BeforeEach
    void setUp() {
        knowledgeBase = new KnowledgeBase();
    }

    @Test
    void testSeedCorpusLoaded() {
        List<KnowledgeBase.Document> documents = knowledgeBase.listDocuments();
        assertFalse(documents.isEmpty());
        assertTrue(documents.stream().anyMatch(d -> d.title().contains("RAG")));
    }

    @Test
    void testAddDocumentSplitsParagraphs() {
        KnowledgeBase.Document doc = knowledgeBase.addDocument(
                "测试文档", "第一段内容。\n\n第二段内容。\n\n第三段内容。");

        assertEquals(3, doc.chunkCount());
        assertTrue(doc.id().startsWith("doc-"));
        assertTrue(knowledgeBase.listDocuments().contains(doc));
    }

    @Test
    void testAddDocumentRejectsBlankArguments() {
        assertThrows(IllegalArgumentException.class,
                () -> knowledgeBase.addDocument("  ", "有正文"));
        assertThrows(IllegalArgumentException.class,
                () -> knowledgeBase.addDocument("有标题", "  "));
    }

    @Test
    void testRetrieveRanksRelevantChunkFirst() {
        List<ScoredChunk> hits = knowledgeBase.retrieve("如何注册工具", 3);

        assertFalse(hits.isEmpty());
        assertEquals("工具调用 Toolkit", hits.get(0).title());
        assertTrue(hits.get(0).content().contains("Toolkit.registerTool"));
    }

    @Test
    void testRetrieveMemoryQuestionHitsMemoryDoc() {
        List<ScoredChunk> hits = knowledgeBase.retrieve("怎么配置记忆压缩", 3);

        assertFalse(hits.isEmpty());
        assertEquals("记忆与上下文压缩", hits.get(0).title());
    }

    @Test
    void testRetrieveRespectsTopK() {
        List<ScoredChunk> hits = knowledgeBase.retrieve("agent", 2);

        assertFalse(hits.isEmpty());
        assertTrue(hits.size() <= 2);
    }

    @Test
    void testRetrieveNoMatchReturnsEmpty() {
        assertTrue(knowledgeBase.retrieve("zzzz", 3).isEmpty());
        assertTrue(knowledgeBase.retrieve("   ", 3).isEmpty());
    }

    @Test
    void testScoresAreNormalizedAndSortedDesc() {
        List<ScoredChunk> hits = knowledgeBase.retrieve("工具 记忆 检索", 10);

        assertFalse(hits.isEmpty());
        for (int i = 0; i < hits.size(); i++) {
            double score = hits.get(i).score();
            assertTrue(score > 0 && score <= 1.0, "得分应落在 (0, 1]");
            if (i > 0) {
                assertTrue(hits.get(i - 1).score() >= score, "结果应按得分降序排列");
            }
        }
    }

    @Test
    void testResetRestoresSeedCorpus() {
        int seedCount = knowledgeBase.listDocuments().size();
        knowledgeBase.addDocument("临时文档", "临时内容。");
        assertEquals(seedCount + 1, knowledgeBase.listDocuments().size());

        int afterReset = knowledgeBase.reset();

        assertEquals(seedCount, afterReset);
        assertTrue(knowledgeBase.listDocuments().stream()
                .noneMatch(d -> d.title().equals("临时文档")));
    }

    @Test
    void testTokenizeSplitsChineseBigramsAndEnglishWords() {
        List<String> tokens = KnowledgeBase.tokenize("HarnessAgent 注册工具");

        assertTrue(tokens.contains("harnessagent"));
        assertTrue(tokens.contains("注册"));
        assertTrue(tokens.contains("工具"));
    }
}
