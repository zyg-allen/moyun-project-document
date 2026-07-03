package com.moyun.portal.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.moyun.core.base.BaseEntity;
import lombok.Data;

/**
 * 积分商城商品
 *
 * @author moyun
 */
@Data
@TableName("portal_shop_item")
public class PortalShopItem extends BaseEntity {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 商品名称 */
    private String name;

    /** 商品描述 */
    private String description;

    /** 商品封面URL */
    private String cover;

    /** 商品类型 virtual/physical */
    private String type;

    /** 兑换所需积分 */
    private Integer pointsCost;

    /** 库存（-1表示不限） */
    private Integer stock;

    /** 状态 active/inactive */
    private String status;

    public PortalShopItem() {
    }

    public PortalShopItem(Long id) {
        this.id = id;
    }
}
