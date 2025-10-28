package com.lowcode.workflow.runner.graph.runner;


import com.lowcode.workflow.runner.graph.data.struct.instance.FlowInstance;
import com.lowcode.workflow.runner.graph.data.struct.instance.NodeInstance;
import com.lowcode.workflow.runner.graph.data.struct.template.Flow;
import com.lowcode.workflow.runner.graph.data.struct.template.FlowEdge;
import com.lowcode.workflow.runner.graph.data.struct.template.Node;
import com.lowcode.workflow.runner.graph.exception.custom.CustomException;
import com.lowcode.workflow.runner.graph.excutors.NodeExecutor;
import com.lowcode.workflow.runner.graph.excutors.NodeExecutorRegistry;
import com.lowcode.workflow.runner.graph.excutors.entity.ExecutorResult;
import com.lowcode.workflow.runner.graph.service.FlowInstanceService;
import com.lowcode.workflow.runner.graph.service.NodeInstanceService;
import com.lowcode.workflow.runner.graph.utils.FlowGraphBuilder;
import lombok.extern.slf4j.Slf4j;
import org.jgrapht.Graph;
import org.jgrapht.traverse.BreadthFirstIterator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 运行前初始化
 * 负责在流程运行前进行必要地初始化操作
 */
@Slf4j
@Component
public class RunnerInit {

    @Autowired
    private NodeExecutorRegistry nodeExecutorRegistry;

    @Autowired
    private FlowInstanceService flowInstanceService;

    @Autowired
    private NodeInstanceService nodeInstanceService;

    public void start(Flow flow) {
        log.info("等待运行的流程: {}", flow.toString());
        FlowInstance flowInstance = new FlowInstance(flow);
        // 存入数据库
        flowInstanceService.save(flowInstance);
        // TODO 构建一个图数据结构
        log.info("——————————————构建一个图数据结构————————————");
        Graph<Node, FlowEdge> graph = FlowGraphBuilder.buildGraph(flowInstance.getNodes(), flowInstance.getEdges());
        run(flowInstance, graph);
    }

    private void run (FlowInstance flowInstance, Graph<Node, FlowEdge> graph) {
        Node entryNode = theEntryDegreeZero(graph);
        BreadthFirstIterator<Node, FlowEdge> breadthFirstIterator = new BreadthFirstIterator<>(graph, entryNode);

        while (breadthFirstIterator.hasNext()) {
            Node currentNode = breadthFirstIterator.next();
            NodeInstance currentNodeInstance = toNodeInstance(currentNode, flowInstance);
            nodeInstanceService.save(currentNodeInstance);

            // 运行
            NodeExecutor nodeExecutor = nodeExecutorRegistry.get(currentNodeInstance.getNodeType());
            if (nodeExecutor == null) {
                throw new CustomException(510, "非法的节点类型, 请检查节点类型准确性重试");
            }
            ExecutorResult executorResult = nodeExecutor.execute(currentNodeInstance, flowInstance);
            if (executorResult == null) {
                executorResult = new ExecutorResult();
            }
            // 添加到context中
            log.info("——————————当前运行节点: {}, 运行结果: {}", currentNodeInstance.getId(), executorResult);
            flowInstance.putContext(currentNodeInstance.getNodeId(), executorResult);
            log.info("——————————当前节点: {}, 运行后的上下文: {}", currentNodeInstance.getId(), flowInstance.getContext());

        }
    }

    // TODO 目前暂时指定一个流程一个起点
    // 后续可以考虑支持多个起点
    private Node theEntryDegreeZero(Graph<Node, FlowEdge> graph) {
        List<Node> entryZeroNodes = graph.vertexSet().stream()
                .filter(node -> graph.inDegreeOf(node) == 0)
                .toList();

        if (entryZeroNodes.isEmpty()) {
            throw new CustomException(510, "流程定义异常, 没有正确的入口");
        }

        if (entryZeroNodes.size() > 1) {
            throw new CustomException(510, "流程定义异常, 只能有一个入口");
        }

        return entryZeroNodes.get(0);
    }


    private NodeInstance toNodeInstance(Node currentNode, FlowInstance flowInstance) {
        return new NodeInstance(currentNode, flowInstance);
    }
}
