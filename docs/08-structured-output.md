# Demo08：结构化输出 — 类型安全的 JSON 回复

> **导航**：[← 上一篇：Demo07 记忆与工作区](./07-memory-workspace.md) · [返回首页](./index.md) · [下一篇：Demo09 Prompt 模板 →](./09-prompt-template.md)
>
> **本章源码**：[Demo08StructuredOutputController](../src/main/java/com/xb/agentscope/controller/demo08/Demo08StructuredOutputController.java) · [Movie](../src/main/java/com/xb/agentscope/controller/demo08/Movie.java)

## 知识点

- `call()` 传入目标类型实现结构化输出
- `reply.getStructuredData()` 类型安全反序列化
- 系统提示对输出格式的约束

## 结构化输出

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

## 三步实现结构化输出

1. **定义目标类型** — 字段名与 JSON key 对应
2. **系统提示约束** — 明确要求只输出 JSON，不要额外文字
3. **`call()` 传入类型** — AgentScope 自动解析并填充对象

## 为什么需要结构化输出

| 维度 | 纯文本回复 | 结构化输出 |
|------|-----------|-----------|
| 使用方式 | 人工阅读 / 正则提取 | 直接映射为对象，进业务逻辑 |
| 稳定性 | 格式随意，解析易碎 | 类型约束，字段缺失可感知 |
| 典型场景 | 聊天、摘要 | 工单抽取、表单填充、数据入库 |

> 结构化输出并不改变模型的"自由发挥"本性：系统提示里的字段约束依然关键，POJO 字段名必须与 JSON key 一致。

## 接口调用

```bash
curl "http://localhost:8080/api/demo08/movie?title=星际穿越"
```

返回示例：

```
Movie{title='星际穿越', director='克里斯托弗·诺兰', year=2014, rating=9.4}
```

## 小结

- 结构化输出：`call()` 传入目标 `.class` → `getStructuredData()` 取值
- 目标类字段名需与模型输出 JSON 的 key 一致
- 系统提示要明确限定 JSON 字段，避免模型附加解释文字
- 结构化结果可直接进入业务代码，减少正则解析的脆弱性
