package com.moyun.ledger.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.moyun.ledger.domain.entity.LedgerObligation;
import org.apache.ibatis.annotations.Mapper;

/**
 * 债权债务统一表 Mapper（纯 BaseMapper，无手写 XML —— 避免字段白名单漏列导致的空 SQL）。
 *
 * @author moyun
 */
@Mapper
public interface LedgerObligationMapper extends BaseMapper<LedgerObligation> {
}
