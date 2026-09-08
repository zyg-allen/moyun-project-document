package com.moyun.ledger.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.moyun.ledger.domain.entity.LedgerScheduleTask;
import org.apache.ibatis.annotations.Mapper;

/**
 * 定时记账任务 Mapper
 *
 * @author moyun
 */
@Mapper
public interface LedgerScheduleTaskMapper extends BaseMapper<LedgerScheduleTask> {
}