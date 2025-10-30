package com.lowcode.workflow.runner.graph.machine;


import com.lowcode.workflow.runner.graph.data.struct.instance.FlowInstance;
import com.lowcode.workflow.runner.graph.data.struct.instance.NodeInstance;
import com.lowcode.workflow.runner.graph.exception.custom.CustomException;
import com.lowcode.workflow.runner.graph.excutors.entity.ExecutorResult;
import com.lowcode.workflow.runner.graph.service.impl.GraphService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class EventDispatcher {
    private final Map<Class<?>, StateMachine<?>> stateMachineMap = new ConcurrentHashMap<>();

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private GraphService graphService;

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
    public <T> void dispatchEvent(T entity, String event, FlowInstance flowInstance) {
        @SuppressWarnings("unchecked")
        StateMachine<T> stateMachine = (StateMachine<T>) stateMachineMap.get(entity.getClass());
        if (stateMachine == null) {
            log.error("未找到对应状态机: {}", entity.getClass().getName());
            return;
        }
        NodeInstance nodeInstance = (NodeInstance) entity;

        List<String> pres = graphService.getThePresOfNode(nodeInstance.getNodeId());
        Map<String, ExecutorResult> context = flowInstance.getContext();
        for (String pre : pres) {
            if (!context.containsKey(pre)) {
                throw new CustomException(500, "前序节点" + pre + "的执行结果不存在");
            }
        }
        stateMachine.transition(entity, event);
    }
}
