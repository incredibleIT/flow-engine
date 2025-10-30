package com.lowcode.workflow.runner.graph;

import com.lowcode.workflow.runner.graph.data.struct.FlowToGraphMapper;
import com.lowcode.workflow.runner.graph.data.struct.instance.FlowInstance;
import com.lowcode.workflow.runner.graph.data.struct.instance.NodeInstance;
import com.lowcode.workflow.runner.graph.machine.EventDispatcher;
import org.jgraph.graph.DefaultEdge;
import org.jgraph.graph.Edge;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;

@SpringBootTest
public class GraphTest {

    @Autowired
    private EventDispatcher eventDispatcher;



//    @Test
//    public void test01() {
//        DefaultDirectedGraph<Node, Edge> nodeEdgeDefaultDirectedGraph = new DefaultDirectedGraph<>(DefaultEdge.class);
//        nodeEdgeDefaultDirectedGraph.addVertex(new Node() {
//            @Override
//            public String getId() {
//                return "nodeId";
//            }
//        });
//
//        FlowToGraphMapper.addGraphByFlowId("flowId", nodeEdgeDefaultDirectedGraph);
//        System.out.println(FlowToGraphMapper.getGraphsByFlowId("flowId"));
//        FlowToGraphMapper.removeGraphNodeByFlowIdAndNodeId("flowId", "nodeId");
//        System.out.println(FlowToGraphMapper.getGraphsByFlowId("flowId"));
//    }

    @Test
    public void test() {
        NodeInstance nodeInstance = new NodeInstance();
        FlowInstance flowInstance = new FlowInstance();
        flowInstance.setStatus(FlowInstance.FlowInstanceStatus.running);

//        eventDispatcher.dispatchEvent(flowInstance, "completed");
        System.out.println(flowInstance);
    }
}