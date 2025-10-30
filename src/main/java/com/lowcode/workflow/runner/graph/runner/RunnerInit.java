package com.lowcode.workflow.runner.graph.runner;


import com.lowcode.workflow.runner.graph.data.struct.instance.FlowInstance;
import com.lowcode.workflow.runner.graph.data.struct.template.Flow;
import com.lowcode.workflow.runner.graph.data.struct.template.FlowEdge;
import com.lowcode.workflow.runner.graph.data.struct.template.Node;
import com.lowcode.workflow.runner.graph.pool.FlowThreadPool;
import com.lowcode.workflow.runner.graph.service.FlowInstanceService;
import com.lowcode.workflow.runner.graph.utils.FlowGraphBuilder;
import lombok.extern.slf4j.Slf4j;
import org.jgrapht.Graph;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * 运行前初始化
 * 负责在流程运行前进行必要地初始化操作
 */
@Slf4j
@Component
public class RunnerInit {

    @Autowired
    private FlowInstanceService flowInstanceService;

    @Autowired
    private RunnerDispatcher runnerDispatcher;

    public void start(Flow flow) {
        log.info("等待运行的流程: {}", flow.toString());
        FlowInstance flowInstance = new FlowInstance(flow);
        // 存入数据库
        flowInstanceService.save(flowInstance);
        log.info("——————————————构建一个图数据结构————————————");
        Graph<Node, FlowEdge> graph = FlowGraphBuilder.buildGraph(flowInstance.getNodes(), flowInstance.getEdges());
        runAsync(flowInstance, graph);
    }


    private void runAsync(FlowInstance flowInstance, Graph<Node, FlowEdge> graph) {
        FlowThreadPool pool = getThreadPool();
        Map<String, Integer> inDegreesMap = new ConcurrentHashMap<>();
        graph.vertexSet().forEach(node -> inDegreesMap.put(node.getId(), graph.inDegreeOf(node)));
        Map<String, Node> nodeMap = graph.vertexSet().stream().collect(Collectors.toMap(Node::getId, node -> node));
        Map<String, List<Node>> downStream = new HashMap<>();
        for (Node node : graph.vertexSet()) {
            for (FlowEdge edge : graph.edgeSet()) {
                String source = edge.getEdge().getSource();
                String target = edge.getEdge().getTarget();
                if (source.equals(node.getId()) && target != null && !target.isEmpty()) {
                    if (downStream.containsKey(source)) {
                        downStream.get(source).add(nodeMap.get(target));
                    } else {
                        downStream.put(source, new ArrayList<>());
                        downStream.get(source).add(nodeMap.get(target));
                    }
                }
            }
        }

        // 反向表: downStream 入度表: InDegreesMap 图结构: graph
        // 从这里开启流程, 基于事件来驱动
        // 初始化找到所有入度为0的节点
        List<Node> entryDegreeZeroNodes = getEntryDegreeZero(inDegreesMap, nodeMap);
        log.info("入度为0的节点: {}", entryDegreeZeroNodes);
        log.info("入度为零的节点个数: {}", entryDegreeZeroNodes.size());
        log.info("提交至流程调度器参数: flowInstance: {}\n entryDegreeZeroNodes: {}\n graph: {}\n pool: {}\n inDegreesMap: {}\n downStream: {}\n", flowInstance, entryDegreeZeroNodes, graph, pool, inDegreesMap, downStream);
        CompletableFuture<Void> dispatch = runnerDispatcher.dispatch(flowInstance, entryDegreeZeroNodes, graph, pool, inDegreesMap, downStream);

        // TODO test异步等待
        dispatch.join();
        pool.shutdown();

    }


    private FlowThreadPool getThreadPool() {
        return new FlowThreadPool(4, 8, 60L, 100, "MyTaskPool", new ThreadPoolExecutor.AbortPolicy());
    }

    private List<Node> getEntryDegreeZero(Map<String, Integer> InDegreesMap, Map<String, Node> nodeMap) {
        List<Node> nodes = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : InDegreesMap.entrySet()) {
            if (entry.getValue() == 0) {
                nodes.add(nodeMap.get(entry.getKey()));
            }
        }
        return nodes;
    }


}
