package com.lowcode.workflow.runner.graph.config.datasource.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lowcode.workflow.runner.graph.config.datasource.dto.DataSourceConfigDTO;
import com.lowcode.workflow.runner.graph.config.datasource.mapper.DataSourceConfigMapper;
import org.springframework.stereotype.Service;


@Service
public class DataSourceConfigServiceImpl extends ServiceImpl<DataSourceConfigMapper, DataSourceConfigDTO> implements DataSourceConfigService {
}
