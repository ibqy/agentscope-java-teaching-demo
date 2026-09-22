package com.xb.agentscope;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * AgentScopeJavaTeachingDemoApplication - Spring Boot 教学演示项目启动入口
 *
 * 本类是整个教学 Demo 的启动引导类，通过 @SpringBootApplication 自动扫描
 * 并加载所有 Controller、Config、Tool 等组件，演示 Spring Boot + AgentScope 的快速集成。
 *
 * @author ibqy
 */
@SpringBootApplication
public class AgentScopeJavaTeachingDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(AgentScopeJavaTeachingDemoApplication.class, args);
    }
}