package com.moyun.ext.aigateway.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.moyun.ext.aigateway.entity.AiExecuteLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * AI统一接入层-执行日志 Mapper
 *
 * @author laomao
 * @since 2026-09-09
 */
@Mapper
public interface AiExecuteLogMapper extends BaseMapper<AiExecuteLog> {
}
