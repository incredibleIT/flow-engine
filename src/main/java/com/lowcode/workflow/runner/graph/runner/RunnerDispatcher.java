package com.lowcode.workflow.runner.graph.runner;

import com.lowcode.workflow.runner.graph.context.SuspendedNodeContext;
import com.lowcode.workflow.runner.graph.data.struct.instance.FlowInstance;
import com.lowcode.workflow.runner.graph.data.struct.instance.NodeInstance;
import com.lowcode.workflow.runner.graph.data.struct.template.FlowEdge;
import com.lowcode.workflow.runner.graph.data.struct.template.Node;
import com.lowcode.workflow.runner.graph.exception.custom.CustomException;
import com.lowcode.workflow.runner.graph.excutors.entity.ExecutorResult;
import com.lowcode.workflow.runner.graph.machine.EventDispatcher;
import com.lowcode.workflow.runner.graph.pool.FlowThreadPool;
import com.lowcode.workflow.runner.graph.service.FlowInstanceService;
import lombok.extern.slf4j.Slf4j;
import org.jgrapht.Graph;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

@Slf4j
@Component
public class RunnerDispatcher {

    @Autowired
    private NodeDispatcher nodeDispatcher;
    @Autowired
    private FlowInstanceService flowInstanceService;
    @Autowired
    private EventDispatcher eventDispatcher;


    public CompletableFuture<Void> dispatch(FlowInstance flowInstance, List<Node> readyNodes, Graph<Node, FlowEdge> graph, FlowThreadPool flowThreadPool, Map<String, Integer> inDegreesMap, Map<String, List<Node>> downStream) {
        // 初始状态, 将开始节点加入队列
        BlockingQueue<Node> readyNodesBlocking = new LinkedBlockingQueue<>(readyNodes);

//        CountDownLatch latch = new CountDownLatch(graph.vertexSet().size());
        Phaser phaser = new Phaser(1);
        for (Node node : readyNodes) {
            phaser.register();
        }
        CompletableFuture<Void> future = new CompletableFuture<>();

        CompletableFuture.runAsync(() -> {
            try {
                log.info("phaser count: {}", phaser.getRegisteredParties());
                while (true) {
                    Node node = readyNodesBlocking.poll(1, TimeUnit.SECONDS);
                    if (node == null) {
                        if (phaser.getRegisteredParties() == 1) {
                            break;
                        }
                        continue;
                    }

                    flowThreadPool.execute(() -> {
                        try {
                            ExecutorResult executorResult = nodeDispatcher.dispatch(node, flowInstance);
                            if (executorResult.getExecutorResultType() == ExecutorResult.ExecutorResultType.WAITING) {
                                log.info("节点 {} 等待, 等待原因: {}", node.getId(), executorResult.getWaitingReason());
                                // 创建挂起上下文
                                SuspendedNodeContext context = new SuspendedNodeContext(node, inDegreesMap, downStream, executorResult.getWaitingReason(), System.currentTimeMillis());
                                flowInstance.putSuspendedNodeContext(node.getId(), context);
                                // 更新流程实例
                                flowInstanceService.updateById(flowInstance);
                                future.complete(null);
                            } else if (executorResult.getExecutorResultType() == ExecutorResult.ExecutorResultType.FAILED) {
                                log.info("节点 {} 执行失败, 失败原因: {}", node.getId(), executorResult.getErrorMessage());
                                // TODO可添加逻辑 这里可以检查节点的重试机制, 如果具有重试剩余次数, 则减少重试次数, 并重新入队

                            } else {
                                // 减少所有下游节点的入度, 如果入度为0, 则加入队列
                                if (downStream.containsKey(node.getId())) {
                                    downStream.get(node.getId()).forEach(n -> {
                                        log.info("节点 {} 入度减少前为 {}", n.getId(), inDegreesMap.get(n.getId()));
                                        Integer i = inDegreesMap.computeIfPresent(n.getId(), (key, oldValue) -> oldValue - 1);
                                        log.info("节点 {} 入度减少为 {}", n.getId(), i);
                                        if (i != null && i == 0) {
                                            readyNodesBlocking.offer(n);
                                            phaser.register();
                                        }
                                    });
                                }
                            }

                        } catch (Exception e) {
                            // TODO 将信息抛出去
                            log.error("节点 {} 运行异常", node.getId(), e);
                        } finally {
                            phaser.arrive();
                        }
                    });
                }
            } catch (InterruptedException e) {
                future.completeExceptionally(e);
                throw new RuntimeException(e);
            }

            phaser.arriveAndAwaitAdvance();
            future.complete(null);

        }, flowThreadPool.getThreadPoolExecutor());
        return future;
    }


    public void resume(FlowInstance flowInstance, NodeInstance nodeInstance, FlowThreadPool flowThreadPool) {
        // 从上下文获取恢复数据
        SuspendedNodeContext suspendedNodeContext = flowInstance.getSuspendedNodeContext().get(nodeInstance.getNodeId());
        if (suspendedNodeContext == null) {
            throw new CustomException(500, "重大错误, 节点 " + nodeInstance.getNodeId() + " 不存在挂起上下文");
        }
        Map<String, Integer> inDegreesSnapshot = suspendedNodeContext.getInDegreesSnapshot();
        Map<String, List<Node>> downStreamSnapshot = suspendedNodeContext.getDownStreamSnapshot();

        LinkedBlockingQueue<Node> resumeNodes = new LinkedBlockingQueue<>();
        // 触发挂起节点的状态变更
        eventDispatcher.dispatchEvent(nodeInstance, "completed", flowInstance);
        Phaser phaser = new Phaser(1);
        // 初始化先将下游节点入度减少
        downStreamSnapshot.get(nodeInstance.getNodeId()).forEach(n -> {
            log.info("节点 {} 入度减少前为 {}", n.getId(), inDegreesSnapshot.get(n.getId()));
            Integer i = inDegreesSnapshot.computeIfPresent(n.getId(), (key, oldValue) -> oldValue - 1);
            log.info("节点 {} 入度减少为 {}", n.getId(), i);
            if (i != null && i == 0) {
                resumeNodes.offer(n);
            }
        });

        CompletableFuture.runAsync(() -> {
            try {
                while (true) {
                    Node node = resumeNodes.poll(1, TimeUnit.SECONDS);
                    if (node == null) {
                        if (phaser.getRegisteredParties() == 1) {
                            break ;
                        }
                        continue;
                    }

                    flowThreadPool.execute(() -> {
                        try {
                            ExecutorResult executorResult = nodeDispatcher.dispatch(node, flowInstance);
                            if (executorResult.getExecutorResultType() == ExecutorResult.ExecutorResultType.WAITING) {
                                log.info("节点 {} 等待, 等待原因: {}", node.getId(), executorResult.getWaitingReason());
                                // 创建快照
                                SuspendedNodeContext context = new SuspendedNodeContext(node, inDegreesSnapshot, downStreamSnapshot, executorResult.getWaitingReason(), System.currentTimeMillis());
                                flowInstance.putSuspendedNodeContext(node.getId(), context);
                                // 更新流程实例
                                flowInstanceService.updateById(flowInstance);
                            } else if (executorResult.getExecutorResultType() == ExecutorResult.ExecutorResultType.FAILED) {
                                log.info("节点 {} 执行失败, 失败原因: {}", node.getId(), executorResult.getErrorMessage());
                                // TODO可添加逻辑 这里可以检查节点的重试机制, 如果具有重试剩余次数, 则减少重试次数, 并重新入队

                            } else {
                                // 减少所有下游节点的入度, 如果入度为0, 则加入队列
                                if (inDegreesSnapshot.containsKey(node.getId())) {
                                    downStreamSnapshot.get(node.getId()).forEach(n -> {
                                        log.info("节点 {} 入度减少前为 {}", n.getId(), inDegreesSnapshot.get(n.getId()));
                                        Integer i = inDegreesSnapshot.computeIfPresent(n.getId(), (key, oldValue) -> oldValue - 1);
                                        log.info("节点 {} 入度减少为 {}", n.getId(), i);
                                        if (i != null && i == 0) {
                                            resumeNodes.offer(n);
                                            phaser.register();
                                        }
                                    });
                                }
                            }

                        } catch (Exception e) {
                            // TODO 将信息抛出去
                            log.error("节点 {} 运行异常", node.getId(), e);
                        } finally {
                            phaser.arrive();
                        }
                    });
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            phaser.arriveAndAwaitAdvance();

        }, flowThreadPool.getThreadPoolExecutor());
    }
}