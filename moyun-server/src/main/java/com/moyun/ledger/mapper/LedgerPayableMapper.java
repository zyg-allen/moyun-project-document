package com.moyun.ledger.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.moyun.ledger.domain.entity.LedgerPayable;
import org.apache.ibatis.annotations.Mapper;

/**
 * LedgerPayable 数据访问（纯 BaseMapper，SQL 由 MyBatis-Plus 依实体自动生成）。
 *
 * @author moyun
 */
@Mapper
public interface LedgerPayableMapper extends BaseMapper<LedgerPayable> {
}