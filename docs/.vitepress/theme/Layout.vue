<script setup>
import DefaultTheme from 'vitepress/theme'

const { Layout } = DefaultTheme
</script>

<template>
  <Layout>
    <template #home-features-before>
      <section class="sig">
        <div class="sig-card">
          <span class="sig-label">COLLABORATION · DIRECTOR</span>
          <h3 class="sig-title">Director 编排，Agent 各司其职</h3>
          <div class="as-stage">
            <div class="as-director">Director · 任务编排</div>
            <div class="as-bubbles">
              <div class="as-b as-l">把这个任务拆成两步：一个 Agent 查资料，一个 Agent 写总结。</div>
              <div class="as-b as-r">检索完成，正在流式撰写总结……（会话记忆保持上下文）</div>
            </div>
            <div class="as-agents">
              <span class="as-agent">ReActAgent · 检索</span>
              <span class="as-agent">ReActAgent · 撰写</span>
            </div>
          </div>
          <div class="sg-chips">
            <span class="sg-chip">流式输出</span>
            <span class="sg-chip">会话记忆 · 上下文压缩</span>
            <span class="sg-chip">Toolkit 工具调用</span>
            <span class="sg-chip">结构化输出</span>
          </div>
          <p class="sig-note">从 Hello Agent 到 Director 编排，五个渐进式示例——先让一个 Agent 说上话，再让一群 Agent 分工合作。</p>
        </div>
      </section>
    </template>
    <template #home-features-after>
      <div class="sig-more"><a href="https://ibqy.github.io/" target="_blank" rel="noopener">← ibqy.github.io · 探索全部项目</a></div>
    </template>
  </Layout>
</template>
