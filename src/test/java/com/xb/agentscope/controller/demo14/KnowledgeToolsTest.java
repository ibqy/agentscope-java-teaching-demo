package com.xb.agentscope.controller.demo14;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 知识库检索工具单元测试
 *
 * 工具输出会直接进入模型上下文，格式必须稳定：
 * 来源标题 + 相关度 + 正文缺一不可，无命中时要给出明确提示。
 */
class KnowledgeToolsTest {

    private KnowledgeTools tools;

    @BeforeEach
    void setUp() {
        tools = new KnowledgeTools(new KnowledgeBase());
    }

    @Test
    void testRetrieveKnowledgeFormatsHits() {
        String result = tools.retrieveKnowledge("记忆压缩怎么配置", null);

        assertTrue(result.contains("检索到"));
        assertTrue(result.contains("来源：记忆与上下文压缩"));
        assertTrue(result.contains("相关度："));
    }

    @Test
    void testRetrieveKnowledgeNoMatch() {
        String result = tools.retrieveKnowledge("zzzz", 3);

        assertTrue(result.contains("未找到"));
        assertFalse(result.contains("检索到"));
    }

    @Test
    void testRetrieveKnowledgeRespectsTopK() {
        String result = tools.retrieveKnowledge("agent", 1);

        assertTrue(result.contains("片段 1"));
        assertFalse(result.contains("片段 2"));
    }
}
