package com.moyun.ledger.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.moyun.ledger.domain.entity.LedgerProduct;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface LedgerProductMapper extends BaseMapper<LedgerProduct> {
}