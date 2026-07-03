package com.moyun.portal.mapper;

import java.util.List;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import com.moyun.portal.domain.entity.PortalShopItem;

/**
 * 积分商城商品 数据层
 *
 * @author moyun
 */
@Mapper
public interface PortalShopItemMapper extends BaseMapper<PortalShopItem> {

    /**
     * 分页查询商品（含条件过滤）
     */
    Page<PortalShopItem> selectItemPage(Page<PortalShopItem> page, @Param("query") PortalShopItem query);

    /**
     * 查询上架商品列表（前台公开）
     */
    List<PortalShopItem> selectActiveItems();

    /**
     * 原子扣减库存（stock=-1 表示不限，不扣减）
     *
     * @return 影响行数，0 表示库存不足
     */
    @Update("UPDATE portal_shop_item SET stock = stock - 1 " +
            "WHERE id = #{id} AND status = 'active' AND (stock = -1 OR stock > 0)")
    int deductStock(@Param("id") Long id);
}
