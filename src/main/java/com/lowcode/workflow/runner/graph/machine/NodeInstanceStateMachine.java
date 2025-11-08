package com.lowcode.workflow.runner.graph.machine;

import com.lowcode.workflow.runner.graph.data.struct.instance.NodeInstance;
import com.lowcode.workflow.runner.graph.websocket.handler.NodeStatusWebSocketHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;


/**
 * 节点实例状态机
 */
@Component
@com.lowcode.workflow.runner.graph.annotation.StateMachine(value = "node", type = NodeInstance.class)
public class NodeInstanceStateMachine implements StateMachine<NodeInstance>{

    @Autowired
    private NodeStatusWebSocketHandler webSocketHandler;


    @Override
    public void transition(NodeInstance nodeInstance, String event) {
        switch (nodeInstance.getStatus()) {
            case pending:
                if ("running".equals(event)) {
                    nodeInstance.setStatus(NodeInstance.NodeInstanceStatus.running);
                    webSocketHandler.sendNodeStatusUpdate(nodeInstance.getId(), nodeInstance.getNodeId(), NodeInstance.NodeInstanceStatus.running.toString());
                    nodeInstance.setStartedAt(LocalDateTime.now());
                } else if ("waiting".equals(event)) {
                    nodeInstance.setStatus(NodeInstance.NodeInstanceStatus.waiting);
                }
                break;
            case running:
                if ("completed".equals(event)) {
                    nodeInstance.setStatus(NodeInstance.NodeInstanceStatus.completed);
                    nodeInstance.setEndedAt(LocalDateTime.now());
                } else if ("failed".equals(event)) {
                    nodeInstance.setStatus(NodeInstance.NodeInstanceStatus.failed);
                    nodeInstance.setErrorMessage("节点运行失败");
                } else if ("waiting".equals(event)) {
                    nodeInstance.setStatus(NodeInstance.NodeInstanceStatus.waiting);
                } else if ("retrying".equals(event)) {
                    nodeInstance.setStatus(NodeInstance.NodeInstanceStatus.retrying);
                    nodeInstance.setRetryCount(nodeInstance.getRetryCount() + 1);
                }
                break;
            case retrying:
                if ("completed".equals(event)) {
                    nodeInstance.setStatus(NodeInstance.NodeInstanceStatus.completed);
                    nodeInstance.setEndedAt(LocalDateTime.now());
                }
        }
    }
}
