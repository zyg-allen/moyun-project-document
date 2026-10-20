package com.moyun.ledger.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.moyun.ledger.domain.entity.LedgerSourceChannel;
import org.apache.ibatis.annotations.Mapper;

/** 进货渠道。 */
@Mapper
public interface LedgerSourceChannelMapper extends BaseMapper<LedgerSourceChannel> {
}
