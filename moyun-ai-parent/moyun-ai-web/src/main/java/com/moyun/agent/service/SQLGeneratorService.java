package com.moyun.agent.service;

import com.moyun.agent.vo.TableSchemaVO;

import java.util.List;

/**
 * SQL生成服务接口
 *
 * @author laomao
 */
public interface SQLGeneratorService {

    /**
     * 根据自然语言生成SQL
     *
     * @param naturalQuery 自然语言查询
     * @param schemas 相关表结构
     * @param sessionId 会话ID(用于上下文)
     * @return 生成的SQL语句
     */
    String generateSQL(String naturalQuery, List<TableSchemaVO> schemas, String sessionId);

    /**
     * 验证SQL安全性
     *
     * @param sql SQL语句
     * @return 是否安全
     */
    boolean validateSQL(String sql);

    /**
     * 优化SQL语句
     *
     * @param sql 原始SQL
     * @return 优化后的SQL
     */
    String optimizeSQL(String sql);

    /**
     * 解析SQL类型
     *
     * @param sql SQL语句
     * @return SQL类型: select, aggregate, join等
     */
    String parseQueryType(String sql);
}
