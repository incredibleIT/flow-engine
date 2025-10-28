package com.lowcode.workflow.runner.graph.machine;

import com.lowcode.workflow.runner.graph.data.struct.instance.FlowInstance;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Objects;


@Component
@com.lowcode.workflow.runner.graph.annotation.StateMachine(value = "flow", type = FlowInstance.class)
public class FlowInstanceStateMachine implements StateMachine<FlowInstance> {
    @Override
    public void transition(FlowInstance flow, String event) {
        if (Objects.requireNonNull(flow.getStatus()) == FlowInstance.FlowInstanceStatus.running) {
            if ("completed".equals(event)) {
                flow.setStatus(FlowInstance.FlowInstanceStatus.completed);
                flow.setEndedAt(LocalDateTime.now());
            } else if ("failed".equals(event)) {
                flow.setStatus(FlowInstance.FlowInstanceStatus.failed);
                flow.setErrorMessage("流程执行失败");
                flow.setEndedAt(LocalDateTime.now());
            }
        }
    }
}
