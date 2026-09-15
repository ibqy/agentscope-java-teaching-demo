# Demo08 / Demo09：结构化输出与 Prompt 模板

## 知识点

- `call()` 传入目标类型实现结构化输出
- `reply.getStructuredData()` 类型安全反序列化
- 动态 System Prompt 角色切换

## 结构化输出 — Demo08

AgentScope 支持让模型返回强类型对象，而非纯文本。在 `call()` 时传入目标 Class，即可自动将模型输出反序列化：

```java
@GetMapping("/movie")
public String movie(@RequestParam(defaultValue = "星际穿越") String title) {
    HarnessAgent agent = HarnessAgent.builder()
        .name("movie-agent")
        .sysPrompt("你擅长电影信息。回答时务必只给出结构化 JSON 字段：title、director、year、rating。")
        .model("dashscope:qwen-plus")
        .workspace(Paths.get(".agentscope/workspace-demo08"))
        .build();

    Msg reply = agent.call(
        List.<Msg>of(new UserMessage("请返回电影《" + title + "》的标题、导演、年份和评分。")),
        Movie.class,              // ← 关键：指定目标类型
        RuntimeContext.empty()
    ).block();

    Movie movie = reply != null ? reply.getStructuredData(Movie.class) : null;
    return movie != null ? movie.toString() : "（无法解析结构化输出）";
}
```

目标类型 `Movie` 是一个普通 POJO：

```java
public class Movie {
    private String title;
    private String director;
    private int year;
    private double rating;
    // getter / setter / toString
}
```

三步实现结构化输出：
1. **定义目标类型** — 字段名与 JSON key 对应
2. **系统提示约束** — 明确要求只输出 JSON，不要额外文字
3. **`call()` 传入类型** — AgentScope 自动解析并填充对象

## 动态 Prompt 模板 — Demo09

Demo09 演示了如何根据参数动态切换 Agent 的角色人格：

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

核心技巧：
- `sysPrompt` 决定 Agent 的基础人格
- `SystemMessage` 在请求中追加角色约束，双重强化
- `switch` 表达式让角色切换清晰易维护

## 小结

- 结构化输出：`call()` 传入目标 `.class` → `getStructuredData()` 取值
- 目标类字段名需与模型输出 JSON 的 key 一致
- 动态 Prompt：用参数拼接 system prompt 实现角色切换
- 联合使用 `SystemMessage` + `sysPrompt` 双重锁定角色行为
