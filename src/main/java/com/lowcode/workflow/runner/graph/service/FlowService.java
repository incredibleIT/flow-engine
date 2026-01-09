package com.lowcode.workflow.runner.graph.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.lowcode.workflow.runner.graph.data.struct.instance.FlowInstance;
import com.lowcode.workflow.runner.graph.data.struct.template.Flow;

public interface FlowService extends IService<Flow> {
    FlowInstance start(Flow flow);
}
