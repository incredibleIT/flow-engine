package com.lowcode.workflow.runner.graph.config.mysql;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JdbcSqlUtils {

    /**
     * 获取 SQL 参数名称
     *
     * @param sql sql语句
     * @return 参数名称
     */
    public static Set<String> getNameParams(String sql) {
        Set<String> names = new HashSet<>();

        Pattern pattern = Pattern.compile(":([a-zA-Z_][a-zA-Z0-9_]*)");
        Matcher matcher = pattern.matcher(sql);

        while (matcher.find()) {
            names.add(matcher.group(1));
        }

        return names;
    }
}
