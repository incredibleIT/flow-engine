package com.lowcode.workflow.runner.graph.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lowcode.workflow.runner.graph.data.struct.template.Edge;
import com.lowcode.workflow.runner.graph.mapper.EdgeMapper;
import com.lowcode.workflow.runner.graph.service.EdgeService;
import org.springframework.stereotype.Service;

@Service
public class EdgeServiceImpl extends ServiceImpl<EdgeMapper, Edge> implements EdgeService {
}
