package com.moyun.ledger.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.moyun.ledger.domain.entity.LedgerStock;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface LedgerStockMapper extends BaseMapper<LedgerStock> {
}