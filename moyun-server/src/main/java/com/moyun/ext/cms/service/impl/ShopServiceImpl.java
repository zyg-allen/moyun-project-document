package com.moyun.ext.cms.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moyun.common.exception.system.ServiceException;
import com.moyun.core.base.page.PageDomain;
import com.moyun.ext.cms.service.IShopService;
import com.moyun.portal.domain.entity.PortalShopExchange;
import com.moyun.portal.domain.entity.PortalShopItem;
import com.moyun.portal.mapper.PortalShopExchangeMapper;
import com.moyun.portal.mapper.PortalShopItemMapper;
import com.moyun.portal.mapper.PortalUserGrowthMapper;
import com.moyun.util.bean.PageUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 积分商城 Service 实现（阶段四 4.4）
 *
 * 兑换流程（事务内）：
 * 1. 校验商品状态与库存
 * 2. 原子扣减积分（余额不足返回 0）
 * 3. 原子扣减库存（不足返回 0，回滚积分）
 * 4. 写入兑换记录
 *
 * @author moyun
 */
@Slf4j
@Service
public class ShopServiceImpl implements IShopService {

    @Autowired private PortalShopItemMapper itemMapper;
    @Autowired private PortalShopExchangeMapper exchangeMapper;
    @Autowired private PortalUserGrowthMapper growthMapper;

    @Override
    public List<PortalShopItem> listItems() {
        return itemMapper.selectActiveItems();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long exchange(Long userId, Long itemId, String address) {
        if (userId == null) {
            throw new ServiceException("请先登录");
        }
        PortalShopItem item = itemMapper.selectById(itemId);
        if (item == null || !"active".equals(item.getStatus())) {
            throw new ServiceException("商品不存在或已下架");
        }
        int cost = item.getPointsCost() != null ? item.getPointsCost() : 0;
        if (cost <= 0) {
            throw new ServiceException("商品不可兑换");
        }
        // 实物商品需填写地址
        if ("physical".equals(item.getType()) && (address == null || address.trim().isEmpty())) {
            throw new ServiceException("实物商品请填写收货地址");
        }
        growthMapper.insertIfNotExists(userId);
        // 1. 原子扣减积分
        int deductPts = growthMapper.deductPoints(userId, cost);
        if (deductPts == 0) {
            throw new ServiceException("积分不足");
        }
        // 2. 原子扣减库存
        int deductStock = itemMapper.deductStock(itemId);
        if (deductStock == 0) {
            // 库存不足，回滚积分
            growthMapper.addPoints(userId, cost);
            throw new ServiceException("商品库存不足");
        }
        // 3. 写入兑换记录
        PortalShopExchange record = new PortalShopExchange();
        record.setUserId(userId);
        record.setItemId(itemId);
        record.setPointsCost(cost);
        record.setStatus("pending");
        record.setAddress(address);
        record.setExchangeTime(LocalDateTime.now());
        exchangeMapper.insert(record);
        return record.getId();
    }

    @Override
    public Page<Map<String, Object>> myExchanges(Long userId, PageDomain query) {
        if (userId == null) {
            throw new ServiceException("请先登录");
        }
        Page<Map<String, Object>> page = PageUtils.buildPage(query);
        return exchangeMapper.selectUserExchanges(page, userId);
    }

    @Override
    public Long myPoints(Long userId) {
        if (userId == null) {
            throw new ServiceException("请先登录");
        }
        growthMapper.insertIfNotExists(userId);
        Long points = growthMapper.selectPoints(userId);
        return points != null ? points : 0L;
    }
}
