package com.lowcode.workflow.runner.graph.runner;


import com.lowcode.workflow.runner.graph.data.struct.instance.FlowInstance;
import com.lowcode.workflow.runner.graph.data.struct.instance.NodeInstance;
import com.lowcode.workflow.runner.graph.data.struct.template.Node;
import com.lowcode.workflow.runner.graph.exception.custom.CustomException;
import com.lowcode.workflow.runner.graph.excutors.NodeExecutor;
import com.lowcode.workflow.runner.graph.excutors.NodeExecutorRegistry;
import com.lowcode.workflow.runner.graph.excutors.entity.ExecutorResult;
import com.lowcode.workflow.runner.graph.machine.EventDispatcher;
import com.lowcode.workflow.runner.graph.service.NodeInstanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class NodeDispatcher {

    @Autowired
    private NodeInstanceService nodeInstanceService;

    @Autowired
    private NodeExecutorRegistry nodeExecutorRegistry;
    @Autowired
    private EventDispatcher eventDispatcher;


    public ExecutorResult dispatch(Node readyNode, FlowInstance flowInstance) throws CustomException {
        // 实例化
        NodeInstance nodeInstance = this.toNodeInstance(readyNode, flowInstance);
        nodeInstanceService.save(nodeInstance);
        // 执行器
        NodeExecutor nodeExecutor = nodeExecutorRegistry.get(readyNode.getType());
        if (nodeExecutor == null) {
            throw new CustomException(510, "非法的节点类型, 请检查节点类型准确性重试");
        }
        // 执行
        ExecutorResult executorResult = nodeExecutor.execute(nodeInstance, flowInstance);
        if (executorResult == null) {
            executorResult = new ExecutorResult();
        }
        if (executorResult.getExecutorResultType() == ExecutorResult.ExecutorResultType.WAITING) {
            // 构建上下文
            flowInstance.putContext(readyNode.getId(), executorResult);
            // 触发状态变更事件
            eventDispatcher.dispatchEvent(nodeInstance, "waiting", flowInstance);
            return executorResult;
        }
        if (executorResult.getExecutorResultType() == ExecutorResult.ExecutorResultType.FAILED) {
            // 构建上下文
            flowInstance.putContext(readyNode.getId(), executorResult);
            // 触发事件变更
            eventDispatcher.dispatchEvent(nodeInstance, "failed", flowInstance);
            return executorResult;
        }
        // 构建上下文
        flowInstance.putContext(readyNode.getId(), executorResult);
        return executorResult;
    }


    private NodeInstance toNodeInstance(Node currentNode, FlowInstance flowInstance) {
        return new NodeInstance(currentNode, flowInstance);
    }


}
