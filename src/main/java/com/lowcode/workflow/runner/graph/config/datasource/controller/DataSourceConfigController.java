package com.lowcode.workflow.runner.graph.config.datasource.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lowcode.workflow.runner.graph.config.datasource.dto.DataSourceConfigDTO;
import com.lowcode.workflow.runner.graph.config.datasource.service.DataSourceConfigService;
import com.lowcode.workflow.runner.graph.result.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/datasources")
public class DataSourceConfigController {

    @Autowired
    private DataSourceConfigService dataSourceConfigService;

    @GetMapping
    public Result<List<DataSourceConfigDTO>> list() {
        LambdaQueryWrapper<DataSourceConfigDTO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DataSourceConfigDTO::getEnabled, true);
        return Result.success(dataSourceConfigService.list(wrapper));
    }

    @GetMapping("/detail")
    public Result<DataSourceConfigDTO> detail(String id) {
        return Result.success(dataSourceConfigService.getById(id));
    }
}