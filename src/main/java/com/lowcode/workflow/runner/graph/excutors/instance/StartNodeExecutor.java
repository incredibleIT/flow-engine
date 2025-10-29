package com.lowcode.workflow.runner.graph.excutors.instance;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lowcode.workflow.runner.graph.annotation.NodeExecutorType;
import com.lowcode.workflow.runner.graph.data.struct.instance.FlowInstance;
import com.lowcode.workflow.runner.graph.data.struct.instance.NodeInstance;
import com.lowcode.workflow.runner.graph.data.struct.template.Edge;
import com.lowcode.workflow.runner.graph.excutors.NodeExecutor;
import com.lowcode.workflow.runner.graph.excutors.entity.ExecutorResult;
import com.lowcode.workflow.runner.graph.machine.EventDispatcher;
import com.lowcode.workflow.runner.graph.service.EdgeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@NodeExecutorType(value = "start")
public class StartNodeExecutor implements NodeExecutor {
    @Autowired
    private EdgeService edgeService;

    @Autowired
    private EventDispatcher eventDispatcher;

    @Override
    public ExecutorResult execute(NodeInstance nodeInstance, FlowInstance flowInstance) {
        eventDispatcher.dispatchEvent(nodeInstance, "running");
        LambdaQueryWrapper<Edge> edgeLambdaQueryWrapper = new LambdaQueryWrapper<>();
        edgeLambdaQueryWrapper.eq(Edge::getTarget, nodeInstance.getNodeId());
        ExecutorResult lastNodeResult = null;
        Edge edge = edgeService.getOne(edgeLambdaQueryWrapper);
        if (edge != null) {
            ExecutorResult context = flowInstance.getContext(edge.getSource());
            if (context != null) {
                lastNodeResult = context;
            }
            log.info("加载上一个节点的结果: {}", lastNodeResult);
        }

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
        eventDispatcher.dispatchEvent(nodeInstance, "completed");
        log.info("向量机执行完成: startNodeState: {}", nodeInstance.getStatus());
        return executorResult;
    }
}
