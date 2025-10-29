package com.lowcode.workflow.runner.graph.runner;


import com.lowcode.workflow.runner.graph.data.struct.instance.FlowInstance;
import com.lowcode.workflow.runner.graph.data.struct.instance.NodeInstance;
import com.lowcode.workflow.runner.graph.data.struct.template.Node;
import com.lowcode.workflow.runner.graph.exception.custom.CustomException;
import com.lowcode.workflow.runner.graph.excutors.NodeExecutor;
import com.lowcode.workflow.runner.graph.excutors.NodeExecutorRegistry;
import com.lowcode.workflow.runner.graph.excutors.entity.ExecutorResult;
import com.lowcode.workflow.runner.graph.service.NodeInstanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class NodeDispatcher {


    @Autowired
    private NodeInstanceService nodeInstanceService;

    @Autowired
    private NodeExecutorRegistry nodeExecutorRegistry;


    public void dispatch(Node readyNode, FlowInstance flowInstance) throws CustomException {
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
        // 构建上下文
        flowInstance.putContext(readyNode.getId(), executorResult);
    }


    private NodeInstance toNodeInstance(Node currentNode, FlowInstance flowInstance) {
        return new NodeInstance(currentNode, flowInstance);
    }


}
