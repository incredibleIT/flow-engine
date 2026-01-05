package com.lowcode.workflow.runner.graph.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lowcode.workflow.runner.graph.data.struct.template.Edge;
import com.lowcode.workflow.runner.graph.result.Result;
import com.lowcode.workflow.runner.graph.service.EdgeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotNull;
import java.util.List;

@RestController
@CrossOrigin
@RequestMapping("/api/edge")
public class EdgeController {

    @Autowired
    private EdgeService edgeService;
    /**
     * 根据flowid来查询链接
     */
    @GetMapping("/detail/{flowId}")
    public Result<List<Edge>> detail(@PathVariable("flowId") @NotNull(message = "") String flowId) {
        LambdaQueryWrapper<Edge> edgeLambdaQueryWrapper = new LambdaQueryWrapper<>();
        edgeLambdaQueryWrapper.eq(Edge::getFlowId, flowId);
        List<Edge> edgeList = edgeService.list(edgeLambdaQueryWrapper);

        return Result.success(edgeList);
    }
}
