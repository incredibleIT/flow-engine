package com.lowcode.workflow.runner.graph.config.mysql;


import com.lowcode.workflow.runner.graph.exception.custom.CustomException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;

/**
 * JDBC 封装
 */
@Slf4j
public class JdbcExecutor {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public JdbcExecutor(DataSource dataSource) {
        this.jdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
    }

    /**
     * 查询
     *
     * @param sql    查询 sql
     * @param params 参数
     * @return 查询结果
     */
    public List<Map<String, Object>> query(String sql, Map<String, Object> params) {
        try {
            log.info("mysql query sql: {}", sql);
            return jdbcTemplate.queryForList(sql, params);

        } catch (DataAccessException e) {
            log.error("mysql query error : {}", e.getMessage());
            throw new CustomException("mysql error");
        }
    }

    /**
     * 更新 (insert, update, delete)
     *
     * @param sql    sql
     * @param params 参数
     * @return 更新影响数量
     */
    public int update(String sql, Map<String, Object> params) {
        try {
            return jdbcTemplate.update(sql, params);
        } catch (DataAccessException e) {
            log.error("mysql update error : {}", e.getMessage());
            throw new CustomException("mysql error");
        }
    }

    /**
     * 通用执行
     *
     * @param sql    sql语句
     * @param params 参数
     */
    public void execute(String sql, Map<String, Object> params) {
        try {
            jdbcTemplate.update(sql, params);
        } catch (DataAccessException e) {
            log.error("mysql default error : {}", e.getMessage());
            throw new CustomException("mysql default error");
        }
    }
}
