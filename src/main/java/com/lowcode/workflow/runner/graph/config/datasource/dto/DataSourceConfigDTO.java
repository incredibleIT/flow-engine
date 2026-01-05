package com.lowcode.workflow.runner.graph.config.datasource.dto;


import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("datasource_config")
public class DataSourceConfigDTO {

    /**
     * 唯一标识
     */
    private String id;

    /**
     * 类型: mysql / redis / pgsql / es ...
     */
    private String type;

    /**
     * 连接地址
     */
    private String url;

    private String username;

    private String password;

    /**
     * 是否启用
     */
    private Boolean enabled;
}
