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

    @Autowired
    private NodeStatusWebSocketHandler nodeStatusWebSocketHandler;


    @Override
    public void transition(NodeInstance nodeInstance, String event) {
        switch (nodeInstance.getStatus()) {
            case pending:
                if ("running".equals(event)) {
                    nodeInstance.setStatus(NodeInstance.NodeInstanceStatus.running);
                    webSocketHandler.sendNodeStatusUpdate(nodeInstance.getId(), nodeInstance.getNodeId(), NodeInstance.NodeInstanceStatus.running.toString());
                    nodeInstance.setStartedAt(LocalDateTime.now());
                    nodeStatusWebSocketHandler.sendNodeStatusUpdate(nodeInstance.getId(), nodeInstance.getNodeId(), NodeInstance.NodeInstanceStatus.running.getValue());
                } else if ("waiting".equals(event)) {
                    nodeInstance.setStatus(NodeInstance.NodeInstanceStatus.waiting);
                    nodeStatusWebSocketHandler.sendNodeStatusUpdate(nodeInstance.getId(), nodeInstance.getNodeId(), NodeInstance.NodeInstanceStatus.waiting.getValue());
                }
                break;
            case running:
                if ("completed".equals(event)) {
                    nodeInstance.setStatus(NodeInstance.NodeInstanceStatus.completed);
                    nodeInstance.setEndedAt(LocalDateTime.now());
                    nodeStatusWebSocketHandler.sendNodeStatusUpdate(nodeInstance.getId(), nodeInstance.getNodeId(), NodeInstance.NodeInstanceStatus.completed.getValue());
                } else if ("failed".equals(event)) {
                    nodeInstance.setStatus(NodeInstance.NodeInstanceStatus.failed);
                    nodeInstance.setErrorMessage("节点运行失败");
                    nodeStatusWebSocketHandler.sendNodeStatusUpdate(nodeInstance.getId(), nodeInstance.getNodeId(), NodeInstance.NodeInstanceStatus.failed.getValue());
                } else if ("waiting".equals(event)) {
                    nodeInstance.setStatus(NodeInstance.NodeInstanceStatus.waiting);
                    nodeStatusWebSocketHandler.sendNodeStatusUpdate(nodeInstance.getId(), nodeInstance.getNodeId(), NodeInstance.NodeInstanceStatus.waiting.getValue());
                } else if ("retrying".equals(event)) {
                    nodeInstance.setStatus(NodeInstance.NodeInstanceStatus.retrying);
                    nodeInstance.setRetryCount(nodeInstance.getRetryCount() + 1);
                    nodeStatusWebSocketHandler.sendNodeStatusUpdate(nodeInstance.getId(), nodeInstance.getNodeId(), NodeInstance.NodeInstanceStatus.retrying.getValue());
                }
                break;
            case retrying:
                if ("completed".equals(event)) {
                    nodeInstance.setStatus(NodeInstance.NodeInstanceStatus.completed);
                    nodeInstance.setEndedAt(LocalDateTime.now());
                    nodeStatusWebSocketHandler.sendNodeStatusUpdate(nodeInstance.getId(), nodeInstance.getNodeId(), NodeInstance.NodeInstanceStatus.completed.getValue());
                }
        }
    }
}
