package com.moyun.ledger.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.moyun.ledger.domain.entity.LedgerProductCategory;
import org.apache.ibatis.annotations.Mapper;

/**
 * 商品类目 Mapper。
 *
 * @author moyun
 */
@Mapper
public interface LedgerProductCategoryMapper extends BaseMapper<LedgerProductCategory> {
}
