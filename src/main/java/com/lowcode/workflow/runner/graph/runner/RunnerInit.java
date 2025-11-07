package com.lowcode.workflow.runner.graph.runner;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lowcode.workflow.runner.graph.data.struct.instance.FlowInstance;
import com.lowcode.workflow.runner.graph.data.struct.template.Flow;
import com.lowcode.workflow.runner.graph.data.struct.template.FlowEdge;
import com.lowcode.workflow.runner.graph.data.struct.template.Node;
import com.lowcode.workflow.runner.graph.pool.FlowThreadPool;
import com.lowcode.workflow.runner.graph.service.FlowInstanceService;
import com.lowcode.workflow.runner.graph.service.FlowService;
import com.lowcode.workflow.runner.graph.utils.FlowGraphBuilder;
import lombok.extern.slf4j.Slf4j;
import org.jgrapht.Graph;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.*;

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
    private RunnerDispatcherAsync runnerDispatcherAsync;

    public void start(Flow flow) {
        log.info("等待运行的流程: {}", flow.toString());
        FlowInstance flowInstance = new FlowInstance(flow);
        // 存入数据库
        flowInstanceService.save(flowInstance);
        log.info("——————————————构建一个图数据结构————————————");
        Graph<Node, FlowEdge> graph = FlowGraphBuilder.buildGraph(flowInstance.getNodes(), flowInstance.getEdges());
        log.info("——————————————图数据结构构建完成————————————");
        runAsync(flowInstance, graph);
    }

    public void resumeStart(FlowInstance flowInstance, Node node) {
        LambdaQueryWrapper<Flow> flowLambdaQueryWrapper = new LambdaQueryWrapper<>();
        flowLambdaQueryWrapper.eq(Flow::getId, flowInstance.getFlowId());
        log.info("——————————————构建一个图数据结构————————————");
        Graph<Node, FlowEdge> graph = FlowGraphBuilder.buildGraph(flowInstance.getNodes(), flowInstance.getEdges());
        log.info("——————————————图数据结构构建完成————————————");
        resume(flowInstance, node, graph);
    }


    private void runAsync(FlowInstance flowInstance, Graph<Node, FlowEdge> graph) {
        FlowThreadPool pool = getThreadPool();
        runnerDispatcherAsync.dispatch(flowInstance, graph, pool);
        CompletableFuture.allOf(flowInstance.getNodeFutureMap().values().toArray(new CompletableFuture[0])).thenRun(() -> {
            log.info("所有节点运行完成");
        });
    }

    private void resume(FlowInstance flowInstance, Node node, Graph<Node, FlowEdge> graph) {
        FlowThreadPool executor = getThreadPool();
        runnerDispatcherAsync.resume(flowInstance, node, graph, executor);
        CompletableFuture.allOf(flowInstance.getNodeFutureMap().values().toArray(new CompletableFuture[0])).join();
    }


    private FlowThreadPool getThreadPool() {
        return new FlowThreadPool(4, 8, 60L, 100, "MyTaskPool", new ThreadPoolExecutor.AbortPolicy());
    }


}
