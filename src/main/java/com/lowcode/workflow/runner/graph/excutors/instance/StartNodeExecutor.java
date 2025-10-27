package com.lowcode.workflow.runner.graph.excutors.instance;


import com.lowcode.workflow.runner.graph.annotation.NodeExecutorType;
import com.lowcode.workflow.runner.graph.data.struct.instance.FlowInstance;
import com.lowcode.workflow.runner.graph.data.struct.instance.NodeInstance;
import com.lowcode.workflow.runner.graph.excutors.NodeExecutor;
import com.lowcode.workflow.runner.graph.excutors.entity.ExecutorResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@NodeExecutorType(value = "start")
public class StartNodeExecutor implements NodeExecutor {
    @Override
    public ExecutorResult execute(NodeInstance nodeInstance, FlowInstance flowInstance) {
        // 从key中取出用户自定义数据, 构建为ExecutorResult
        Map<String, Object> data = nodeInstance.getInputData();
        Map<String, String> res = new HashMap<>();

        if (nodeInstance.getNodeDataFieldKey() != null && !nodeInstance.getNodeDataFieldKey().isEmpty()) {
            List<String> keys = Arrays.asList(nodeInstance.getNodeDataFieldKey().split(","));
            for (String key : keys) {
                Object d = data.get(key);
                try {
                    String value = d.toString();
                    res.put(key, value);

                } catch (Exception e) {
                    log.error("StartNodeExecutor error, key: {}, value: {}", key, d);
                }
            }
            if (res.isEmpty()) {
                log.info("StartNodeExecutor: res is empty");
            }
        }
        ExecutorResult executorResult = new ExecutorResult(res);
        log.info("StartNodeExecutor: executorResult: {}", executorResult);
        return executorResult;
    }
}
