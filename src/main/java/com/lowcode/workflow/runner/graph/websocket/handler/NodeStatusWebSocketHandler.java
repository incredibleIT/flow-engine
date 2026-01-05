package com.lowcode.workflow.runner.graph.websocket.handler;

import com.lowcode.workflow.runner.graph.exception.custom.CustomException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


@Slf4j
@Component
public class NodeStatusWebSocketHandler extends TextWebSocketHandler {

    private static final List<WebSocketSession> sessions = new ArrayList<>();

    /**
     * 当一个连接建立时, 调用该方法, 加入到会话列表中
     * @param session 新建立的会话
     * @throws Exception 连接建立失败时抛出
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.add(session);
        log.info("a new session: {}, established", session);
    }

    /**
     * 处理接收的文本消息
     * @param session 会话
     * @param message 文本消息
     * @throws Exception 处理消息失败时抛出
     */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        // 处理接收的消息
        String payload = message.getPayload();
        log.info("receive the message: {}", payload);
    }


    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        session.close();
        log.info("session closed, {}", session);
    }

    public void sendNodeStatusUpdate(String nodeInstanceId, String nodeId, String status) {
        WebSocketMessage<String> nodeStatusUpdateMessage = new TextMessage(String.format("{\"nodeInstanceId\":\"%s\",\"nodeId\":\"%s\",\"status\":\"%s\"}", nodeInstanceId, nodeId, status));
        log.info("send the node status update to the node: {}, status: {}, message: {}", nodeInstanceId, status, nodeStatusUpdateMessage);
        sessions.stream().filter(WebSocketSession::isOpen).forEach(session -> {
            try {
                session.sendMessage(nodeStatusUpdateMessage);
            } catch (IOException e) {
                throw new CustomException(500, "发送节点状态更新消息失败");
            }
        });
    }
}
