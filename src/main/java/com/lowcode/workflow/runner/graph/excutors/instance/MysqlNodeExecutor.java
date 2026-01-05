package com.lowcode.workflow.runner.graph.excutors.instance;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lowcode.workflow.runner.graph.annotation.NodeExecutorType;
import com.lowcode.workflow.runner.graph.config.datasource.runtime.DataSourceManager;
import com.lowcode.workflow.runner.graph.config.mysql.JdbcExecutor;
import com.lowcode.workflow.runner.graph.config.mysql.JdbcSqlUtils;
import com.lowcode.workflow.runner.graph.data.struct.instance.FlowInstance;
import com.lowcode.workflow.runner.graph.data.struct.instance.NodeInstance;
import com.lowcode.workflow.runner.graph.data.struct.template.Edge;
import com.lowcode.workflow.runner.graph.data.struct.template.NodeType;
import com.lowcode.workflow.runner.graph.excutors.NodeExecutor;
import com.lowcode.workflow.runner.graph.excutors.entity.ExecutorResult;
import com.lowcode.workflow.runner.graph.service.EdgeService;
import com.lowcode.workflow.runner.graph.service.NodeTypeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Component
@NodeExecutorType("mysql")
public class MysqlNodeExecutor implements NodeExecutor {

    @Autowired
    private NodeTypeService nodeTypeService;

    @Autowired
    private EdgeService edgeService;

    @Autowired
    private DataSourceManager dataSourceManager;

    @Override
    public ExecutorResult execute(NodeInstance nodeInstance, FlowInstance flowInstance) {

        // 获取节点需要的配置项
        String typeKey = nodeInstance.getNodeType();
        NodeType nodeType = nodeTypeService.getById(typeKey);
        Map<String, Object> config = nodeType.getConfigSchema();
        Map<String, Object> properties = (Map<String, Object>) config.get("properties");
        List<String> configKeys = properties.keySet().stream().toList();
        configKeys.forEach(configKey -> log.info("mysql node config key {}", configKey));
        //TODO 获取用户自定义数据

        // 通过配置项找配置数据
        Map<String, Object> inputData = nodeInstance.getInputData();
        Map<String, Object> configMap = new HashMap<>();
        for (String k : configKeys) {
            try {
                configMap.put(k, inputData.get(k));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        // 根据配置参数执行逻辑
        Map<String, List<Map<String, Object>>> nodeOutput = new HashMap<>();
        String dsId = (String) configMap.get("datasourceId");
        DataSource ds = dataSourceManager.get(dsId);
        JdbcExecutor jdbcExecutor = new JdbcExecutor(ds);
        // 获取映射参数
        Map<String, String> params = (Map<String, String>) configMap.get("params");
        // 加载上一个节点结果
        ExecutorResult lastNodeResult = null;
        LambdaQueryWrapper<Edge> edgeWrapper = new LambdaQueryWrapper<>();
        edgeWrapper.eq(Edge::getTarget, nodeInstance.getNodeId());
        Edge edge = edgeService.getOne(edgeWrapper);
        if (edge != null && flowInstance.getContext().get(edge.getSource()) != null) {
            lastNodeResult = flowInstance.getContext().get(edge.getSource());
            log.info("获取上一个节点结果: {}", lastNodeResult);
        }
        // 根据sql中的name找到key对应value, 未找到应该去上一个节点结果中找
        Set<String> names = JdbcSqlUtils.getNameParams((String) configMap.get("sql"));
        Map<String, Object> finalMap = new HashMap<>();
        for (String name : names) {
            String value = params.get(name);
            if (value == null && lastNodeResult != null && lastNodeResult.getNodeOutputData().get(name) != null) {
                finalMap.put(name, lastNodeResult.getNodeOutputData().get(name));
            } else if (value != null) {
                finalMap.put(name, value);
            } else {
                throw new RuntimeException("参数 " + name + " 未找到");
            }

        }

        log.info("sql name map: {}", finalMap);

        String sqlType = (String) configMap.get("operation");
        List<Map<String, Object>> sqlResult = null;
        if ("query".equals(sqlType)) {
            sqlResult = jdbcExecutor.query((String) configMap.get("sql"), finalMap);
        } else if ("update".equals(sqlType) || "insert".equals(sqlType) || "delete".equals(sqlType)) {
            Map<String, Object> r = new HashMap<>();
            r.put("count", jdbcExecutor.update((String) configMap.get("sql"), finalMap));
            sqlResult = List.of(r);
        } else {
            throw new RuntimeException("sqlType " + sqlType + " 不支持");
        }
        sqlResult.forEach(result -> {
            log.info("sql result: {}", result);
        });

        Map<String, Object> outputData = new HashMap<>();

        outputData.put("result", sqlResult);
        ExecutorResult executorResult = new ExecutorResult();
        executorResult.setNodeOutputData(outputData);
        executorResult.setExecutorResultType(ExecutorResult.ExecutorResultType.SUCCESS);
        return executorResult;
    }
}
