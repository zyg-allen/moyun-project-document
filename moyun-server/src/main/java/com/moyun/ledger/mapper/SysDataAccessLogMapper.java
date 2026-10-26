package com.moyun.ledger.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.moyun.ledger.domain.entity.SysDataAccessLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 敏感数据访问日志 Mapper（只增不改）
 *
 * @author moyun
 */
@Mapper
public interface SysDataAccessLogMapper extends BaseMapper<SysDataAccessLog> {
}
