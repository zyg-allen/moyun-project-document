package com.moyun.ext.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.moyun.ext.ai.entity.QueryHistory;
import org.apache.ibatis.annotations.Mapper;

/**
 * 查询历史 Mapper
 *
 * @author laomao
 */
@Mapper
public interface QueryHistoryMapper extends BaseMapper<QueryHistory> {
}
