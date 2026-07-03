package com.moyun.ext.cms.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moyun.portal.domain.entity.PortalShopItem;

import java.util.List;
import java.util.Map;

/**
 * 积分商城 Service（阶段四 4.4）
 *
 * @author moyun
 */
public interface IShopService {

    /**
     * 上架商品列表（公开）
     */
    List<PortalShopItem> listItems();

    /**
     * 兑换商品（需登录）
     *
     * @param userId  用户ID
     * @param itemId  商品ID
     * @param address 收货地址（实物商品）
     * @return 兑换记录ID
     */
    Long exchange(Long userId, Long itemId, String address);

    /**
     * 我的兑换记录（需登录，分页）
     */
    Page<Map<String, Object>> myExchanges(Long userId, com.moyun.core.base.page.PageDomain query);

    /**
     * 用户积分余额（需登录）
     */
    Long myPoints(Long userId);
}
