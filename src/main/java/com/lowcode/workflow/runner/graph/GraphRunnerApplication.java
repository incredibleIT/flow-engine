package com.lowcode.workflow.runner.graph;


import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan({"com.lowcode.workflow.runner.graph.mapper", "com.lowcode.workflow.runner.graph.config.datasource.mapper"})
public class GraphRunnerApplication {
    public static void main(String[] args) {
        SpringApplication.run(GraphRunnerApplication.class, args);
    }
}
