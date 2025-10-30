package com.lowcode.workflow.runner.graph.runner;

import com.lowcode.workflow.runner.graph.data.struct.instance.FlowInstance;
import com.lowcode.workflow.runner.graph.data.struct.template.FlowEdge;
import com.lowcode.workflow.runner.graph.data.struct.template.Node;
import com.lowcode.workflow.runner.graph.excutors.entity.ExecutorResult;
import com.lowcode.workflow.runner.graph.pool.FlowThreadPool;
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



    public CompletableFuture<Void> dispatch(FlowInstance flowInstance, List<Node> readyNodes, Graph<Node, FlowEdge> graph, FlowThreadPool flowThreadPool, Map<String, Integer> inDegreesMap, Map<String, List<Node>> downStream) {
        // 初始状态, 将开始节点加入队列
        BlockingQueue<Node> readyNodesBlocking = new LinkedBlockingQueue<>(readyNodes);

        CountDownLatch latch = new CountDownLatch(graph.vertexSet().size());

        CompletableFuture<Void> future = new CompletableFuture<>();

        CompletableFuture.runAsync(() -> {
            try {
                log.info("latch count: {}", latch.getCount());
                while (latch.getCount() > 0) {
                    Node node = readyNodesBlocking.poll(1, TimeUnit.SECONDS);
                    if (node == null) {
                        continue;
                    }

                    flowThreadPool.execute(() -> {
                        try {
                            ExecutorResult executorResult = nodeDispatcher.dispatch(node, flowInstance);
                            if (executorResult.getExecutorResultType() == ExecutorResult.ExecutorResultType.WAITING) {
                                log.info("节点 {} 等待, 等待原因: {}", node.getId(), executorResult.getWaitingReason());
                                // 再次入队
                                readyNodesBlocking.offer(node);
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
                                        }
                                    });
                                }
                            }

                        } catch (Exception e) {
                            // TODO 将信息抛出去
                            log.error("节点 {} 运行异常", node.getId(), e);
                        } finally {
                            latch.countDown();
                        }
                    });
                }
            } catch (InterruptedException e) {
                future.completeExceptionally(e);
                throw new RuntimeException(e);
            }

            try {
                latch.await(100, TimeUnit.MINUTES);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            future.complete(null);

        }, flowThreadPool.getThreadPoolExecutor());
        return future;
    }
}
