package com.lowcode.workflow.runner.graph.context;

import com.lowcode.workflow.runner.graph.data.struct.template.Node;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 快照
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SuspendedNodeContext {

    // 被挂起的节点模板
    private Node node;

    // 挂起时的入度快照（只包含该节点下游相关的部分，或全图）
    private Map<String, Integer> inDegreesSnapshot;

    // 下游依赖关系（可选，如果模板不变可不存）
    private Map<String, List<Node>> downStreamSnapshot;

    // 挂起原因（可选）
    private String waitingReason;

    // 挂起时间
    private long suspendedAt = System.currentTimeMillis();
}
