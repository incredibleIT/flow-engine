package com.lowcode.workflow.runner.graph.service;

import com.lowcode.workflow.runner.graph.data.struct.instance.FlowInstance;
import com.lowcode.workflow.runner.graph.data.struct.template.Node;

public interface ResumeService {

    void resume(FlowInstance flowInstance, Node node);
}
