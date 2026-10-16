package com.moyun.ledger.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.moyun.ledger.domain.entity.LedgerPurchaseOrderItem;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface LedgerPurchaseOrderItemMapper extends BaseMapper<LedgerPurchaseOrderItem> {
}