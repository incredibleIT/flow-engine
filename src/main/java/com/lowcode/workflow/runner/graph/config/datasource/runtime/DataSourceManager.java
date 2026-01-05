package com.lowcode.workflow.runner.graph.config.datasource.runtime;

import com.lowcode.workflow.runner.graph.config.datasource.dto.DataSourceConfigDTO;
import com.lowcode.workflow.runner.graph.config.datasource.service.DataSourceConfigService;
import com.lowcode.workflow.runner.graph.exception.custom.CustomException;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 管理数据源
 */
@Component
public class DataSourceManager {

    @Autowired
    private DataSourceConfigService dataSourceConfigService;

    private final Map<String, DataSource> cache = new ConcurrentHashMap<>();

    public DataSource get(String id) {
        return cache.computeIfAbsent(id, this::create);
    }


    /**
     * 创建数据源
     * @param id 数据源 id
     * @return 数据源
     */
    private DataSource create(String id) {

        DataSourceConfigDTO dataSourceConfigDTO = dataSourceConfigService.getById(id);
        if (dataSourceConfigDTO == null || !dataSourceConfigDTO.getEnabled()) {
            throw new CustomException("数据源不存在或未启用");
        }

        return switch (dataSourceConfigDTO.getType()) {
            case "mysql" -> creatMysql(dataSourceConfigDTO);
            case "redis" -> null;
            default -> throw new CustomException("数据源类型错误");
        };
    }

    private DataSource creatMysql(DataSourceConfigDTO dataSourceConfigDTO) {
        HikariDataSource hikariDataSource = new HikariDataSource();
        hikariDataSource.setJdbcUrl(dataSourceConfigDTO.getUrl());
        hikariDataSource.setUsername(dataSourceConfigDTO.getUsername());
        hikariDataSource.setPassword(dataSourceConfigDTO.getPassword());
        hikariDataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
        hikariDataSource.setPoolName("mysql-" + dataSourceConfigDTO.getId());
        return hikariDataSource;
    }







}
