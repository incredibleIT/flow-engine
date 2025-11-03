package com.lowcode.workflow.runner.graph.runner;


import com.lowcode.workflow.runner.graph.data.struct.instance.FlowInstance;
import com.lowcode.workflow.runner.graph.data.struct.template.FlowEdge;
import com.lowcode.workflow.runner.graph.data.struct.template.Node;
import com.lowcode.workflow.runner.graph.exception.custom.CustomException;
import com.lowcode.workflow.runner.graph.excutors.entity.ExecutorResult;
import com.lowcode.workflow.runner.graph.pool.FlowThreadPool;
import lombok.extern.slf4j.Slf4j;
import org.jgrapht.Graph;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 重写异步执行器
 */
@Slf4j
@Component
public class RunnerDispatcherAsync {

    @Autowired
    private NodeDispatcher nodeDispatcher;

    // 主调度
    public void dispatch(FlowInstance flowInstance,
                         Graph<Node, FlowEdge> graph,
                         FlowThreadPool flowThreadPool) {
        ThreadPoolExecutor executor = flowThreadPool.getThreadPoolExecutor();

        // 遍历当前图的所有节点, 注册节点执行逻辑
        graph.vertexSet().forEach(node -> {
            this.registerNodeExecutorAsync(executor, node, flowInstance, graph);
        });
    }



    /**
     * 注册节点异步执行器
     */
    private void registerNodeExecutorAsync(ThreadPoolExecutor executor,
                                           Node node,
                                           FlowInstance flowInstance,
                                           Graph<Node, FlowEdge> graph) {
        // 构建一个触发条件
        CompletableFuture<Void> trigger;
        // 获取节点id
        String nodeId = node.getId();
        // 获取当前节点的所有前序节点id
        List<String> predecessorNodeIds = getPredecessorNodeIds(node, graph);
        // 获取当前节点Future
        CompletableFuture<ExecutorResult> nodeFuture = getNodeFuture(nodeId, flowInstance);

        // 如果当前节点没有前序节点, 那么触发条件就是null, 即立即触发
        // 否则触发条件就是所有的前序节点执行完成
        if (predecessorNodeIds.isEmpty()) {
            trigger = CompletableFuture.completedFuture(null);
        } else {
            CompletableFuture<?>[] predecessorFutures = predecessorNodeIds.stream()
                    .map(id -> getNodeFuture(id, flowInstance))
                    .toArray(CompletableFuture[]::new);
            trigger = CompletableFuture.allOf(predecessorFutures);
        }

        // trigger完成后执行当前节点
        trigger.thenRunAsync(() -> {
            try {
                ExecutorResult result = nodeDispatcher.dispatch(node, flowInstance);

                switch (result.getExecutorResultType()) {
                    case SUCCESS:
                        log.info("节点{}执行成功, 输出数据: {}", nodeId, result);
                        nodeFuture.complete(result);
                        break;
                    case FAILED:
                        log.error("节点{}执行失败, 错误信息: {}", nodeId, result.getErrorMessage());
                        nodeFuture.completeExceptionally(new CustomException(500, result.getErrorMessage()));
                        break;
                    case WAITING:
                        log.info("节点{}等待原因: {}", nodeId, result.getWaitingReason());
                        // 不complete, 等待后续触发
                        break;
                }
            } catch (Exception e) {
                log.error("节点{}运行失败: {}", nodeId, e.getMessage());
                nodeFuture.completeExceptionally(e);
            }
        }, executor).exceptionally(ex -> {
           nodeFuture.completeExceptionally(ex);
           return null;
        });
    }


    /**
     * 获取当前节点的所有前序节点Id
     * @param node 当前节点
     * @param graph 流程图
     * @return 所有前序节点Id
     */
    private List<String> getPredecessorNodeIds(Node node, Graph<Node, FlowEdge> graph) {
        return graph.incomingEdgesOf(node).stream().map(graph::getEdgeSource).map(Node::getId).toList();
    }

    /**
     * 获取当前节点的Future
     * @param nodeId 当前节点Id
     * @param flowInstance 流程实例
     * @return 当前节点的Future
     */
    private CompletableFuture<ExecutorResult> getNodeFuture(String nodeId, FlowInstance flowInstance) {
        return flowInstance.getNodeFutureMap().computeIfAbsent(nodeId, k -> new CompletableFuture<ExecutorResult>());
    }



}
