package com.moyun.ledger.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.moyun.ledger.domain.entity.PortalDataAuth;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户数据授权 Mapper
 *
 * @author moyun
 */
@Mapper
public interface PortalDataAuthMapper extends BaseMapper<PortalDataAuth> {
}
