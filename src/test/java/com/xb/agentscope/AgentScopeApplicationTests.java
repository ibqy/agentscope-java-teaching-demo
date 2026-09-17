package com.xb.agentscope;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 基础启动类测试
 *
 * 心得：不加载完整 Spring 上下文（需要外部 API Key），
 * 仅验证启动类存在且可实例化，作为最基本的冒烟测试
 */
class AgentScopeApplicationTests {

    @Test
    void mainClassExists() {
        AgentScopeJavaTeachingDemoApplication app = new AgentScopeJavaTeachingDemoApplication();
        assertNotNull(app);
    }
}
