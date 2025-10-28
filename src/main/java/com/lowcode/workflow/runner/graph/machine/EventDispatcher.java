package com.lowcode.workflow.runner.graph.machine;


import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class EventDispatcher {
    private final Map<Class<?>, StateMachine<?>> stateMachineMap = new ConcurrentHashMap<>();

    @Autowired
    private ApplicationContext applicationContext;

    @PostConstruct
    public void init() {
        log.info("——————————————正在加载状态机————————————");
        // 注册状态机
        applicationContext.getBeansWithAnnotation(com.lowcode.workflow.runner.graph.annotation.StateMachine.class).values().forEach(stateMachine -> {
            com.lowcode.workflow.runner.graph.annotation.StateMachine annotation = stateMachine.getClass().getAnnotation(com.lowcode.workflow.runner.graph.annotation.StateMachine.class);
            Class<?> type = annotation.type();
            stateMachineMap.put(type, (StateMachine<?>) stateMachine);
        });
        log.info("已加载状态机：{}", stateMachineMap.entrySet().toString());
        log.info("——————————————状态机加载完成————————————");

    }


    // 分发事件
    public <T> void dispatchEvent(T entity, String event) {
        StateMachine<T> stateMachine = (StateMachine<T>) stateMachineMap.get(entity.getClass());
        if (stateMachine == null) {
            log.error("未找到对应状态机: {}", entity.getClass().getName());
            return;
        }

        stateMachine.transition(entity, event);
    }
}
