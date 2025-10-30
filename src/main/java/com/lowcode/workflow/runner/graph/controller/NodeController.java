package com.lowcode.workflow.runner.graph.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lowcode.workflow.runner.graph.data.struct.instance.FlowInstance;
import com.lowcode.workflow.runner.graph.data.struct.instance.NodeInstance;
import com.lowcode.workflow.runner.graph.data.struct.template.Node;
import com.lowcode.workflow.runner.graph.data.struct.template.NodeType;
import com.lowcode.workflow.runner.graph.exception.custom.CustomException;
import com.lowcode.workflow.runner.graph.excutors.NodeExecutor;
import com.lowcode.workflow.runner.graph.excutors.NodeExecutorRegistry;
import com.lowcode.workflow.runner.graph.excutors.entity.ExecutorResult;
import com.lowcode.workflow.runner.graph.machine.EventDispatcher;
import com.lowcode.workflow.runner.graph.pool.FlowThreadPool;
import com.lowcode.workflow.runner.graph.result.Result;
import com.lowcode.workflow.runner.graph.runner.RunnerDispatcher;
import com.lowcode.workflow.runner.graph.runner.RunnerInit;
import com.lowcode.workflow.runner.graph.service.FlowInstanceService;
import com.lowcode.workflow.runner.graph.service.NodeInstanceService;
import com.lowcode.workflow.runner.graph.service.NodeService;
import com.lowcode.workflow.runner.graph.service.NodeTypeService;
import com.lowcode.workflow.runner.graph.validation.CreatGroup;
import com.lowcode.workflow.runner.graph.validation.UpdateGroup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;

@RestController
@RequestMapping("/api/node")
public class NodeController {

    @Autowired
    private NodeService nodeService;

    @Autowired
    private NodeTypeService nodeTypeService;

    @Autowired
    private FlowInstanceService flowInstanceService;

    @Autowired
    private NodeInstanceService nodeInstanceService;

    @Autowired
    private NodeExecutorRegistry nodeExecutorRegistry;
    @Autowired
    private EventDispatcher eventDispatcher;
    @Autowired
    private RunnerDispatcher runnerDispatcher;
    @Autowired
    private RunnerInit runnerInit;

    /**
     * 查找一个流程模版下所有节点
     * @param flowId 流程模版ID
     * @return 节点列表
     */
    @GetMapping("/list/{flowId}")
    public Result<List<Node>> list(@PathVariable("flowId") @NotNull(message = "流程模版ID不能为空") String flowId) {
        LambdaQueryWrapper<Node> flowWrapper = new LambdaQueryWrapper<>();
        flowWrapper.eq(Node::getFlowId, flowId);
        List<Node> nodeList = nodeService.list(flowWrapper);
        for (Node node : nodeList) {
            LambdaQueryWrapper<NodeType> nodeTypeWrapper = new LambdaQueryWrapper<>();
            nodeTypeWrapper.eq(NodeType::getTypeKey, node.getType());
            node.setNodeType(nodeTypeService.getOne(nodeTypeWrapper));
        }
        return Result.success(nodeList);
    }

    /**
     * 查找一个节点的详细信息, 要拼接节点类型
     * @param nodeId 节点ID
     * @return 节点详细信息
     */
    @GetMapping("/detail/{nodeId}")
    public Result<Node> detail(@PathVariable("nodeId") @NotNull(message = "节点ID不能为空") String nodeId) {
        Node node = nodeService.getById(nodeId);
        if (node == null) {
            throw new CustomException(500, "节点不存在");
        }
        LambdaQueryWrapper<NodeType> nodeTypeWrapper = new LambdaQueryWrapper<>();
        nodeTypeWrapper.eq(NodeType::getTypeKey, node.getType());
        node.setNodeType(nodeTypeService.getOne(nodeTypeWrapper));
        return Result.success(node);
    }

    @PostMapping("/create")
    public Result<Void> create(@Validated(CreatGroup.class) @RequestBody Node node) {
        nodeService.save(node);
        return Result.success();
    }

    /**
     * 更新一个节点的信息
     * @param node 节点信息
     * @return 空结果
     */
    @PutMapping("/update")
    public Result<Void> update(@Validated(UpdateGroup.class) @RequestBody Node node) {
        nodeService.updateById(node);
        return Result.success();
    }

    /**
     * 删除一个节点
     * @param nodeId 节点ID
     * @return 空结果
     */
    @DeleteMapping("/delete/{nodeId}")
    public Result<Void> delete(@PathVariable("nodeId") @NotNull(message = "节点ID不能为空") String nodeId) {
        nodeService.removeById(nodeId);
        return Result.success();
    }

    @PostMapping("/resume/{flowInstanceId}/{nodeInstanceId}")
    public Result<Void> resume(@PathVariable("flowInstanceId") @NotBlank(message = "流程实例ID不能为空") String flowInstanceId,
                               @PathVariable("nodeInstanceId") @NotBlank(message = "节点实例ID不能为空") String nodeInstanceId,
                               @RequestBody Map<String, Object> resumeData) {
        FlowInstance flowInstance = flowInstanceService.getById(flowInstanceId);
        NodeInstance nodeInstance = nodeInstanceService.getById(nodeInstanceId);
        if (flowInstance == null) {
            throw new CustomException(500, "流程实例不存在");
        }
        if (nodeInstance == null) {
            throw new CustomException(500, "节点实例不存在");
        }
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

        // TODO 将审批后的数据添加到上下文中
        flowInstance.putContext(nodeInstance.getNodeId(), new ExecutorResult(new HashMap<>(), resumeData));

        // 触发节点恢复状态事件
        eventDispatcher.dispatchEvent(nodeInstance, "resumed", flowInstance);
        runnerDispatcher.resume(flowInstance, nodeInstance, getThreadPool());
        return Result.success();
    }

    private FlowThreadPool getThreadPool() {
        return new FlowThreadPool(4, 8, 60L, 100, "MyTaskPool", new ThreadPoolExecutor.AbortPolicy());
    }


}
