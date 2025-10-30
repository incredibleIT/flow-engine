package com.lowcode.workflow.runner.graph.excutors;

import com.lowcode.workflow.runner.graph.data.struct.instance.FlowInstance;
import com.lowcode.workflow.runner.graph.data.struct.instance.NodeInstance;
import com.lowcode.workflow.runner.graph.excutors.entity.ExecutorResult;

public interface NodeExecutor {

    ExecutorResult execute(NodeInstance nodeInstance, FlowInstance flowInstance);

    // 是否支持恢复
    default boolean supportResume() {return false;}
}
