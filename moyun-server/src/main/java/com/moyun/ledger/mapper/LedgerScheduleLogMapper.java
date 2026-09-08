package com.moyun.ledger.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.moyun.ledger.domain.entity.LedgerScheduleLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 定时记账执行日志 Mapper
 *
 * @author moyun
 */
@Mapper
public interface LedgerScheduleLogMapper extends BaseMapper<LedgerScheduleLog> {
}