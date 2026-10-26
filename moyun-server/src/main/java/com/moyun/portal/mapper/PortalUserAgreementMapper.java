package com.moyun.portal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

import com.moyun.portal.domain.entity.PortalUserAgreement;

/**
 * 门户用户协议同意记录 数据层
 *
 * @author moyun
 */
@Mapper
public interface PortalUserAgreementMapper extends BaseMapper<PortalUserAgreement> {
}
