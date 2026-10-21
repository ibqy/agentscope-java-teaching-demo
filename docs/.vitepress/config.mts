import { defineConfig } from 'vitepress'

export default defineConfig({
  lang: 'zh-CN',
  title: 'AgentScope Java 教学',
  description: 'AgentScope for Java v2 渐进式教学：16 个 Demo + 16 篇文档，从 Hello Agent 到 MCP、A2A、RAG 与生产级可观测',
  base: '/agentscope-java-teaching-demo/',
  lastUpdated: true,
  // public 下的静态 HTML 教程不是 VitePress 页面，跳过死链检查
  ignoreDeadLinks: [/agentscope-java-teaching-guide\.html/],
  head: [
    ['link', { rel: 'icon', type: 'image/svg+xml', href: '/agentscope-java-teaching-demo/favicon.svg' }]
  ],
  markdown: {
    config(md) {
      const defaultLink =
        md.renderer.rules.link_open ||
        ((tokens, idx, options, _env, self) => self.renderToken(tokens, idx, options))
      md.renderer.rules.link_open = (tokens, idx, options, env, self) => {
        const href = tokens[idx].attrGet('href')
        if (href && href.startsWith('../')) {
          const rel = href.replace(/^(\.\.\/)+/, '')
          const kind = /\.[A-Za-z]+$/.test(rel) ? 'blob' : 'tree'
          tokens[idx].attrSet('href', `https://github.com/ibqy/agentscope-java-teaching-demo/${kind}/main/${rel}`)
        }
        return defaultLink(tokens, idx, options, env, self)
      }
    }
  },
  themeConfig: {
    nav: [
      { text: '首页', link: '/' },
      { text: '本站路线', link: '/#learning-path' },
      { text: '学习路径', link: 'https://ibqy.github.io/curriculum' },
      { text: 'Demo 一览', link: '/#demos' },
      { text: 'GitHub', link: 'https://github.com/ibqy/agentscope-java-teaching-demo' }
    ],
    sidebar: [
      {
        text: '① 基础入门 · 跑通 Agent',
        items: [
          { text: '01 · Hello Agent', link: '/01-agent-hello-world' },
          { text: '02 · DashScope 模型', link: '/02-dashscope-model' },
          { text: '03 · Call 同步调用', link: '/03-call' },
          { text: '04 · 流式输出', link: '/04-streaming' }
        ]
      },
      {
        text: '② 核心能力 · 构建 Agent',
        items: [
          { text: '05 · 工具调用', link: '/05-tool-calling' },
          { text: '06 · 多智能体协作', link: '/06-multi-agent' },
          { text: '07 · 记忆与工作区', link: '/07-memory-workspace' },
          { text: '08 · 结构化输出', link: '/08-structured-output' },
          { text: '09 · Prompt 模板', link: '/09-prompt-template' },
          { text: '10 · 全能助理', link: '/10-assistant' }
        ]
      },
      {
        text: '③ 实战与生产化 · 从综合到上线',
        items: [
          { text: '11 · 综合实战', link: '/11-comprehensive-demo' },
          { text: '12 · MCP 工具集成', link: '/12-mcp-integration' },
          { text: '13 · A2A 跨智能体通信', link: '/13-a2a-communication' },
          { text: '14 · RAG 知识库检索', link: '/14-rag-knowledge-base' },
          { text: '15 · Plan Mode 与权限系统', link: '/15-plan-mode-permission' },
          { text: '16 · 事件驱动可观测', link: '/16-event-observability' },
          { text: '附录 · 生产化改造清单', link: '/appendix-production-hardening' }
        ]
      }
    ],
    socialLinks: [
      { icon: 'github', link: 'https://github.com/ibqy/agentscope-java-teaching-demo' }
    ],
    search: { provider: 'local' },
    outline: { level: [2, 3], label: '本页目录' },
    docFooter: { prev: '上一篇', next: '下一篇' },
    lastUpdated: { text: '最后更新于' },
    darkModeSwitchLabel: '外观',
    lightModeSwitchTitle: '切换到浅色模式',
    darkModeSwitchTitle: '切换到深色模式',
    sidebarMenuLabel: '文档',
    returnToTopLabel: '回到顶部',
    footer: {
      message: '个人教学项目 · 代码可跑 · 注释记录设计取舍',
      copyright: 'Copyright © 2026 ibqy'
    }
  }
})
