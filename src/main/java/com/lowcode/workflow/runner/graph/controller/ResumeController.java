package com.lowcode.workflow.runner.graph.controller;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lowcode.workflow.runner.graph.data.struct.instance.FlowInstance;
import com.lowcode.workflow.runner.graph.data.struct.instance.NodeInstance;
import com.lowcode.workflow.runner.graph.data.struct.template.Edge;
import com.lowcode.workflow.runner.graph.data.struct.template.Node;
import com.lowcode.workflow.runner.graph.exception.custom.CustomException;
import com.lowcode.workflow.runner.graph.excutors.NodeExecutor;
import com.lowcode.workflow.runner.graph.excutors.NodeExecutorRegistry;
import com.lowcode.workflow.runner.graph.machine.EventDispatcher;
import com.lowcode.workflow.runner.graph.result.Result;
import com.lowcode.workflow.runner.graph.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.constraints.NotBlank;
import java.util.List;

@RestController
@CrossOrigin
public class ResumeController {

    @Autowired
    private ResumeService resumeService;

    @Autowired
    private EventDispatcher eventDispatcher;

    @Autowired
    private FlowInstanceService flowInstanceService;

    @Autowired
    private NodeInstanceService nodeInstanceService;

    @Autowired
    private NodeService nodeService;

    @Autowired
    private NodeExecutorRegistry nodeExecutorRegistry;
    @Autowired
    private EdgeService edgeService;

    /**
     * 触发恢复等待节点执行
     * @param flowInstanceId 流程实例ID
     * @param nodeInstanceId 节点实例ID
     */
    @PostMapping("/resume/{flowInstanceId}/{nodeInstanceId}")
    public Result<Void> resume(@PathVariable("flowInstanceId") @NotBlank(message = "流程实例ID不能为空") String flowInstanceId,
                               @PathVariable("nodeInstanceId") @NotBlank(message = "节点实例ID不能为空") String nodeInstanceId) {
        FlowInstance flowInstance = flowInstanceService.getById(flowInstanceId);
        if (flowInstance == null) {
            throw new CustomException(500, "流程实例不存在");
        }
        // TODO 构建nodes和edges
        LambdaQueryWrapper<Node> nodeLambadaWrapper = new LambdaQueryWrapper<>();
        LambdaQueryWrapper<Edge> edgeLambdaQueryWrapper = new LambdaQueryWrapper<>();

        nodeLambadaWrapper.eq(Node::getFlowId, flowInstance.getFlowId());
        edgeLambdaQueryWrapper.eq(Edge::getFlowId, flowInstance.getFlowId());

        List<Node>  nodeList = nodeService.list(nodeLambadaWrapper);
        List<Edge> edgeList = edgeService.list(edgeLambdaQueryWrapper);
        NodeInstance nodeInstance = nodeInstanceService.getById(nodeInstanceId);
        flowInstance.setEdges(edgeList);
        flowInstance.setNodes(nodeList);

        if (nodeInstance == null) {
            throw new CustomException(500, "节点实例不存在");
        }
        LambdaQueryWrapper<Node> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Node::getId, nodeInstance.getNodeId());
        Node node = nodeService.getOne(wrapper);

        if (nodeInstance.getStatus() != NodeInstance.NodeInstanceStatus.waiting) {
            throw new CustomException(500, "节点实例状态不是等待中，不能恢复");
        }
        // 获取节点类型的执行器
        NodeExecutor nodeExecutor = nodeExecutorRegistry.get(nodeInstance.getNodeType());
        if (nodeExecutor == null) {
            throw new CustomException(500, "节点类型的执行器不存在");
        }
        if (!nodeExecutor.supportResume()) {
            throw new CustomException(500, "当前节点类型的执行器不支持恢复");
        }
        // 触发节点恢复状态事件
        eventDispatcher.dispatchEvent(nodeInstance, "resumed", flowInstance);
        resumeService.resume(flowInstance, node);

        return Result.success();
    }
}
