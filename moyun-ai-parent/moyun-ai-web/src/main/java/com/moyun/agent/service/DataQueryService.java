package com.moyun.agent.service;

import com.moyun.agent.dto.DataQueryRequest;
import com.moyun.agent.vo.DataQueryResponse;

/**
 * 数据查询服务接口
 *
 * @author laomao
 */
public interface DataQueryService {

    /**
     * 智能数据查询
     * 
     * 流程:
     * 1. 自然语言 → SQL生成
     * 2. SQL安全验证
     * 3. 执行查询
     * 4. 智能分析
     * 5. 图表推荐
     * 6. 生成洞察
     *
     * @param request 查询请求
     * @return 查询响应(包含数据、分析、图表)
     */
    DataQueryResponse intelligentQuery(DataQueryRequest request);

    /**
     * 执行SQL查询(已生成SQL的情况)
     *
     * @param datasourceId 数据源ID
     * @param sql SQL语句
     * @return 查询结果
     */
    DataQueryResponse executeSQL(Long datasourceId, String sql);
}
