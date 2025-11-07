package com.lowcode.workflow.runner.graph.service.impl;

import com.lowcode.workflow.runner.graph.data.struct.instance.FlowInstance;
import com.lowcode.workflow.runner.graph.data.struct.template.Node;
import com.lowcode.workflow.runner.graph.runner.RunnerInit;
import com.lowcode.workflow.runner.graph.service.ResumeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Service
public class ResumeServiceImpl implements ResumeService {

    @Autowired
    private RunnerInit runnerInit;

    @Override
    public void resume(FlowInstance flowInstance, Node node) {
        runnerInit.resumeStart(flowInstance, node);
    }
}
