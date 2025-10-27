package com.lowcode.workflow.runner.graph.excutors.entity;

import lombok.Data;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 执行器返回结果
 */
@Data
public class ExecutorResult {
    private Map<String, String> userDefinedData;
    private Map<String, Object> nodeOutputData;


    public ExecutorResult(Map<String, String> userDefinedData) {
        this.userDefinedData = userDefinedData;
        this.nodeOutputData = new HashMap<>();
    }

    public ExecutorResult() {

    }
}
