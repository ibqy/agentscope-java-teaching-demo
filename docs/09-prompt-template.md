# Demo09：Prompt 模板 — 动态角色切换

> **导航**：[← 上一篇：Demo08 结构化输出](./08-structured-output.md) · [返回首页](./index.md) · [下一篇：Demo10 全能助理 →](./10-assistant.md)
>
> **本章源码**：[Demo09PromptController](../src/main/java/com/xb/agentscope/controller/demo09/Demo09PromptController.java)

## 知识点

- 动态 System Prompt 角色切换
- `switch` 表达式组织多套提示词
- `SystemMessage` 与 `sysPrompt` 的双重约束
- Prompt Engineering 基础思路

## 动态 Prompt 模板

Demo09 演示了如何根据参数动态切换 Agent 的角色人格——同一个 Agent 框架，换一段系统提示词就换一种专业身份：

```java
@GetMapping("/persona")
public String persona(
        @RequestParam(defaultValue = "teacher") String persona,
        @RequestParam(defaultValue = "解释什么是大模型") String message) {

    String sysPrompt = switch (persona) {
        case "tour_guide"   -> "你是一位热情的旅游导游，回答要生动有趣，并给出实用的小贴士。";
        case "code_reviewer" -> "你是一位资深代码评审专家，回答严谨，专注于可读性、规范与潜在缺陷。";
        default              -> "你是一位耐心的好老师，回答循序渐进、通俗易懂，并善于举例。";
    };

    HarnessAgent agent = HarnessAgent.builder()
        .name("persona-agent")
        .sysPrompt(sysPrompt)
        .model("dashscope:qwen-plus")
        .workspace(Paths.get(".agentscope/workspace-demo09"))
        .build();

    List<Msg> messages = List.of(
        new SystemMessage("请严格保持当前角色身份，不要偏离设定的语气。"),
        new UserMessage(message)
    );
    Msg reply = agent.call(messages, RuntimeContext.empty()).block();
    return reply != null ? reply.getTextContent() : "（无回复）";
}
```

## 内置角色

| persona 参数 | 角色 | 提示词风格 |
|--------------|------|-----------|
| `teacher`（默认） | 耐心的老师 | 循序渐进、通俗易懂、善举例 |
| `tour_guide` | 热情导游 | 生动有趣、给实用小贴士 |
| `code_reviewer` | 资深代码评审专家 | 严谨、关注可读性与潜在缺陷 |

## 核心技巧

- `sysPrompt` 决定 Agent 的基础人格
- `SystemMessage` 在请求中追加角色约束，双重强化
- `switch` 表达式让角色切换清晰易维护，新增角色只需加一个 case

## Prompt 设计要点

| 要点 | 差的做法 | 好的做法 |
|------|---------|---------|
| 身份 | "你是一个助手" | "你是一位资深代码评审专家" |
| 风格 | 不描述 | 明确"严谨、关注可读性" |
| 边界 | 不提 | "不要偏离设定的语气" |
| 输出 | 不约束 | 指定结构/长度/语气 |

> 提示词不是越长越好：角色 + 风格 + 边界三件事说清楚，比堆砌形容词更有效。

## 接口调用

```bash
# 老师人格
curl "http://localhost:8080/api/demo09/persona?persona=teacher&message=解释什么是大模型"

# 导游人格
curl "http://localhost:8080/api/demo09/persona?persona=tour_guide&message=介绍一下杭州"

# 代码评审人格
curl "http://localhost:8080/api/demo09/persona?persona=code_reviewer&message=评审这段冒泡排序"
```

## 小结

- 动态 Prompt：用参数拼接 system prompt 实现角色切换
- 联合使用 `SystemMessage` + `sysPrompt` 双重锁定角色行为
- `switch` 表达式是组织多套提示词的清晰方式
- Prompt 设计抓三点：身份、风格、边界
