package com.lowcode.workflow.runner.graph.excutors.instance;


import com.lowcode.workflow.runner.graph.annotation.NodeExecutorType;
import com.lowcode.workflow.runner.graph.data.struct.instance.FlowInstance;
import com.lowcode.workflow.runner.graph.data.struct.instance.NodeInstance;
import com.lowcode.workflow.runner.graph.excutors.NodeExecutor;
import com.lowcode.workflow.runner.graph.excutors.entity.ExecutorResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@NodeExecutorType("end")
public class EndNodeExecutor implements NodeExecutor {
    @Override
    public ExecutorResult execute(NodeInstance nodeInstance, FlowInstance flowInstance) {
        log.info("EndNodeExecutor execute");
        return null;
    }
}
