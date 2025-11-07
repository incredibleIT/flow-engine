package com.lowcode.workflow.runner.graph.runner;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lowcode.workflow.runner.graph.data.struct.instance.FlowInstance;
import com.lowcode.workflow.runner.graph.data.struct.instance.NodeInstance;
import com.lowcode.workflow.runner.graph.data.struct.template.FlowEdge;
import com.lowcode.workflow.runner.graph.data.struct.template.Node;
import com.lowcode.workflow.runner.graph.exception.custom.CustomException;
import com.lowcode.workflow.runner.graph.excutors.NodeExecutorRegistry;
import com.lowcode.workflow.runner.graph.excutors.entity.ExecutorResult;
import com.lowcode.workflow.runner.graph.pool.FlowThreadPool;
import com.lowcode.workflow.runner.graph.service.FlowInstanceService;
import com.lowcode.workflow.runner.graph.service.NodeInstanceService;
import lombok.extern.slf4j.Slf4j;
import org.jgrapht.Graph;
import org.jgrapht.Graphs;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
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
    @Autowired
    private FlowInstanceService flowInstanceService;
    @Autowired
    private NodeInstanceService nodeInstanceService;

    @Autowired
    private NodeExecutorRegistry nodeExecutorRegistry;

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
                log.info("节点{}开始执行", nodeId);
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

    private void registerNodeExecutorResumeAsync(FlowThreadPool executor, Node node, FlowInstance flowInstance, Graph<Node, FlowEdge> graph, String resumeNodeId) {

        // 首先定义触发条件
        CompletableFuture<Void> trigger;

        // 获取nodeId
        String nodeId = node.getId();
        // 获取当前节点的前序节点
        List<String> predecessorNodeIds = getPredecessorNodeIds(node, graph);
        CompletableFuture<ExecutorResult> nodeFuture = getNodeFuture(nodeId, flowInstance);
        if (predecessorNodeIds.contains(resumeNodeId)) {
            trigger = CompletableFuture.completedFuture(null);
        } else {
            CompletableFuture<?>[] predecessorFutures = predecessorNodeIds.stream().
                    map(id -> getNodeFuture(id, flowInstance))
                    .toArray(CompletableFuture[]::new);
            trigger = CompletableFuture.allOf(predecessorFutures);
        }

        // 触发后执行当前节点的恢复逻辑
        trigger.thenRunAsync(() -> {
            try {
                log.info("节点恢复流程: 节点{}", node);
                ExecutorResult executorResult = nodeDispatcher.dispatch(node, flowInstance);
                if (executorResult.getExecutorResultType() == null) {
                    log.info("executorResultType is null");
                    nodeFuture.complete(null);
                }
                switch (executorResult.getExecutorResultType()) {
                    case SUCCESS:
                        log.info("节点恢复流程: 节点{}执行成功, 输出数据: {}", nodeId, executorResult);
                        nodeFuture.complete(executorResult);
                        break;
                    case FAILED:
                        log.error("节点恢复流程: 节点{}执行失败, 错误信息: {}", nodeId, executorResult.getErrorMessage());
                        nodeFuture.completeExceptionally(new CustomException(500, executorResult.getErrorMessage()));
                        break;
                    case WAITING:
                        log.info("节点恢复流程: 节点{}等待原因: {}", nodeId, executorResult.getWaitingReason());
                        // 不complete, 等待后续触发
                        break;
                    default:
                        nodeFuture.complete(executorResult);
                }
            } catch (Exception e) {
                nodeFuture.completeExceptionally(e);
            }
        });


    }




    public void resume(FlowInstance flowInstance, Node node, Graph<Node, FlowEdge> graph, FlowThreadPool executor) {
        LambdaQueryWrapper<NodeInstance> wrapper = new LambdaQueryWrapper<NodeInstance>();
        wrapper.eq(NodeInstance::getNodeId, node.getId());
        NodeInstance nodeInstance = nodeInstanceService.getOne(wrapper);
        if (nodeInstance == null) {
            throw new CustomException(500, "要恢复的节点实例不存在");
        }
        if (nodeInstance.getStatus() != NodeInstance.NodeInstanceStatus.waiting) {
            throw new CustomException(500, "要恢复的节点实例状态不是等待中");
        }
        if (!nodeExecutorRegistry.get(nodeInstance.getNodeType()).supportResume()) {
            throw new CustomException(500, "要恢复的节点实例不支持恢复");
        }
        String resumeNodeId = node.getId();

        // 可以恢复的节点
        // nodeDispatcher.resume(nodeInstance, flowInstance);
        // 注册需要恢复节点的后续所有节点, 不包含当前节点
        this.getDownstreamVertices(graph, node).forEach(
                down -> registerNodeExecutorResumeAsync(executor, down, flowInstance, graph, resumeNodeId)
        );
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
        CompletableFuture<ExecutorResult> executorResultCompletableFuture = flowInstance.getNodeFutureMap().computeIfAbsent(nodeId, k -> new CompletableFuture<ExecutorResult>());
        flowInstanceService.updateById(flowInstance);
        return executorResultCompletableFuture;
    }


    /**
     * 获取当前节点的所有后续节点
     * @param graph 流程图
     * @param startVertex 当前节点
     * @return 所有后续节点
     * @param <V> 节点类型
     * @param <E> 边类型
     */
    private <V, E> Set<V> getDownstreamVertices(Graph<V, E> graph, V startVertex) {
//        if (graph.containsVertex(startVertex)) {
//            return Collections.emptySet();
//        }

        Set<V> visited = new HashSet<>();
        Queue<V> queue = new LinkedList<>();

        // 从直接后继节点开始
        for (V vertex : Graphs.successorListOf(graph, startVertex)) {
            if (visited.add(vertex)) {
                queue.offer(vertex);
            }
        }

        while (!queue.isEmpty()) {
            V vertex = queue.poll();
            for (V successor : Graphs.successorListOf(graph, vertex)) {
                if (visited.add(successor)) {
                    queue.offer(successor);
                }
            }
        }

        log.info("节点 {} 所有的后继节点有: {}", startVertex, visited);

        return visited;
    }



}
