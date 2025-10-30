package com.lowcode.workflow.runner.graph.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lowcode.workflow.runner.graph.data.struct.template.Edge;
import com.lowcode.workflow.runner.graph.service.EdgeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class GraphService {

    @Autowired
    private EdgeService edgeService;


    /**
     * 获取节点的所有前驱节点id
     * @param nodeId 节点id
     * @return 所有前驱节点id列表
     */
    public List<String> getThePresOfNode(String nodeId) {
        LambdaQueryWrapper<Edge> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Edge::getTarget, nodeId);
        List<Edge> edges = edgeService.list(wrapper);
        return edges.stream().map(Edge::getSource).toList();
    }













}
