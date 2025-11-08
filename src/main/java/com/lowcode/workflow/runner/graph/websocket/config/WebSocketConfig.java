package com.lowcode.workflow.runner.graph.websocket.config;

import com.lowcode.workflow.runner.graph.websocket.handler.NodeStatusWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(new NodeStatusWebSocketHandler(), "/node-status").setAllowedOrigins("*");
    }
}
