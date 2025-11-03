package com.lowcode.workflow.runner.graph.excutors.instance;


import com.lowcode.workflow.runner.graph.annotation.NodeExecutorType;
import com.lowcode.workflow.runner.graph.data.struct.instance.FlowInstance;
import com.lowcode.workflow.runner.graph.data.struct.instance.NodeInstance;
import com.lowcode.workflow.runner.graph.excutors.NodeExecutor;
import com.lowcode.workflow.runner.graph.excutors.entity.ExecutorResult;
import com.lowcode.workflow.runner.graph.machine.EventDispatcher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@NodeExecutorType("approval")
public class ApprovalNodeExecutor implements NodeExecutor {

    @Autowired
    private EventDispatcher eventDispatcher;

    @Override
    public ExecutorResult execute(NodeInstance nodeInstance, FlowInstance flowInstance) {
        // 节点直接挂起，等待审批
        ExecutorResult executorResult = new ExecutorResult();
        executorResult.wait("等待审批中....");
        eventDispatcher.dispatchEvent(nodeInstance, "waiting", flowInstance);
        return executorResult;
    }

    @Override
    public boolean supportResume() {
        return true;
    }
}
