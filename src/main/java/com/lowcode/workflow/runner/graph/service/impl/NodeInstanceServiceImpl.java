package com.lowcode.workflow.runner.graph.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lowcode.workflow.runner.graph.data.struct.instance.NodeInstance;
import com.lowcode.workflow.runner.graph.mapper.NodeInstanceMapper;
import com.lowcode.workflow.runner.graph.service.NodeInstanceService;
import org.springframework.stereotype.Service;


@Service
public class NodeInstanceServiceImpl extends ServiceImpl<NodeInstanceMapper, NodeInstance> implements NodeInstanceService {
}
