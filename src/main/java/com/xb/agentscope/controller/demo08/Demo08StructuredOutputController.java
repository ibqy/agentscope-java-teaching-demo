package com.xb.agentscope.controller.demo08;

import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.UserMessage;
import io.agentscope.harness.agent.HarnessAgent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.nio.file.Paths;
import java.util.List;

@RestController
@RequestMapping("/api/demo08")
public class Demo08StructuredOutputController {

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
                Movie.class, RuntimeContext.empty()).block();
        Movie movie = reply != null ? reply.getStructuredData(Movie.class) : null;
        return movie != null ? movie.toString() : "（无法解析结构化输出）";
    }
}