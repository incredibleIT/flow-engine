package com.lowcode.workflow.runner.graph.excutors.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 执行器返回结果
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ExecutorResult {
    private Map<String, String> userDefinedData;
    private Map<String, Object> nodeOutputData;
    private String errorMessage;
    private String waitingReason;
    private ExecutorResultType executorResultType;


    public ExecutorResult(Map<String, String> userDefinedData) {
        this.userDefinedData = userDefinedData;
        this.nodeOutputData = new HashMap<>();
    }

    public ExecutorResult(Map<String, String> userDefinedData, Map<String, Object> nodeOutputData) {
        this.userDefinedData = userDefinedData;
        this.nodeOutputData = nodeOutputData;
    }

    public void wait(String waitingReason) {
        this.waitingReason = waitingReason;
        this.executorResultType = ExecutorResultType.WAITING;
    }

    public enum ExecutorResultType {
        SUCCESS,
        FAILED,
        WAITING
    }
}
