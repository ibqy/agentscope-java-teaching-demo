<script setup>
import DefaultTheme from 'vitepress/theme'

const { Layout } = DefaultTheme
</script>

<template>
  <Layout>
    <template #home-features-before>
      <section class="sig">
        <div class="sig-card">
          <span class="sig-label">16 DEMOS · AGENT SCOPE JAVA</span>
          <h3 class="sig-title">从单 Agent 到生产级智能体</h3>
          <div class="as-stage">
            <div class="as-director">Director · 任务编排（Demo06）</div>
            <div class="as-bubbles">
              <div class="as-b as-l">把这个任务拆成两步：一个子 Agent 查资料，一个子 Agent 翻译成英文。</div>
              <div class="as-b as-r">检索完成，正在输出……（会话记忆与上下文压缩保持连贯）</div>
            </div>
            <div class="as-agents">
              <span class="as-agent">researcher · 检索</span>
              <span class="as-agent">translator · 翻译</span>
            </div>
          </div>
          <div class="sg-chips">
            <span class="sg-chip">流式输出</span>
            <span class="sg-chip">工具调用</span>
            <span class="sg-chip">会话记忆 · 上下文压缩</span>
            <span class="sg-chip">MCP 工具生态</span>
            <span class="sg-chip">A2A 跨智能体通信</span>
            <span class="sg-chip">RAG 知识库</span>
            <span class="sg-chip">Plan Mode · 权限</span>
            <span class="sg-chip">事件可观测</span>
          </div>
          <p class="sig-note">从 Hello Agent 到 MCP / A2A / RAG，十六个渐进式示例——先让一个 Agent 说上话，再让一群 Agent 分工合作，最后接入标准协议并走向生产。</p>
        </div>
      </section>
    </template>
    <template #home-features-after>
      <div class="sig-more"><a href="https://ibqy.github.io/" target="_blank" rel="noopener">← ibqy.github.io · 探索全部项目</a></div>
    </template>
  </Layout>
</template>
