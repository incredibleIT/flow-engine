package com.lowcode.workflow.runner.graph.utils;

import com.lowcode.workflow.runner.graph.data.struct.template.Edge;
import com.lowcode.workflow.runner.graph.data.struct.template.FlowEdge;
import com.lowcode.workflow.runner.graph.data.struct.template.Node;
import com.lowcode.workflow.runner.graph.exception.custom.NodeIsNotExist;
import lombok.extern.slf4j.Slf4j;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultDirectedGraph;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
public class FlowGraphBuilder {


    /**
     * 构建图结构
     * @param nodes 节点列表
     * @param edges 边列表
     * @return 图结构
     */
    public static Graph<Node, FlowEdge> buildGraph(List<Node> nodes, List<Edge> edges) {

        // 创建有向图
        DefaultDirectedGraph<Node, FlowEdge> graph = new DefaultDirectedGraph<>(FlowEdge.class);
        // 定义节点
        nodes.forEach(graph::addVertex);
        // 定义id -> 节点 映射
        Map<String, Node> nodeMap = nodes.stream().collect(Collectors.toMap(Node::getId, Function.identity()));
        // 定义边
        edges.forEach(edge -> {
            Node sourceNode = nodeMap.get(edge.getSource());
            if (sourceNode == null) {
                log.error("边{}, 所要相连的源节点{}不存在", edge.getId(), edge.getSource());
                throw new NodeIsNotExist(500, String.format("边[%s], 所要相连的源节点[%s]不存在", edge.getId(), edge.getSource()));
            }
            Node targetNode = nodeMap.get(edge.getTarget());
            if (targetNode == null) {
                log.error("边{}, 所要相连的目标节点{}不存在", edge.getId(), edge.getTarget());
                throw new NodeIsNotExist(500, String.format("边[%s], 所要相连的目标节点[%s]不存在", edge.getId(), edge.getTarget()));
            }
            graph.addEdge(sourceNode, targetNode, new FlowEdge(edge));
        });
        return graph;
    }
}
