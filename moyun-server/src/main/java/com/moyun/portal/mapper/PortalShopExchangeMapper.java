package com.moyun.portal.mapper;

import java.util.List;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.moyun.portal.domain.entity.PortalShopExchange;

/**
 * 积分兑换记录 数据层
 *
 * @author moyun
 */
@Mapper
public interface PortalShopExchangeMapper extends BaseMapper<PortalShopExchange> {

    /**
     * 分页查询兑换记录（后台，含商品名称）
     */
    Page<java.util.Map<String, Object>> selectExchangePage(Page<java.util.Map<String, Object>> page,
                                                           @Param("query") PortalShopExchange query);

    /**
     * 查询用户兑换记录（前台，含商品信息）
     */
    Page<java.util.Map<String, Object>> selectUserExchanges(Page<java.util.Map<String, Object>> page,
                                                             @Param("userId") Long userId);
}
