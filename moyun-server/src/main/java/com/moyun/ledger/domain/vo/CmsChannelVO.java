package com.moyun.ledger.domain.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 渠道统一视图（售卖渠道 / 进货渠道）
 *
 * <p>两张表结构一致，后台用一个页面 + 一个接口按 kind 区分，
 * 但读写仍各走各自的实体与表，不做跨表合并存储。</p>
 *
 * @author moyun
 */
@Data
public class CmsChannelVO implements Serializable {

    @java.io.Serial
    private static final long serialVersionUID = 1L;

    /** 渠道类型：sale=售卖 / source=进货 */
    public static final String KIND_SALE = "sale";
    public static final String KIND_SOURCE = "source";

    private Long id;

    /** 请求参数：渠道类型（仅新增时由前端传入，非表字段） */
    private String kind;

    /** 归属用户：0=系统预设 */
    private Long userId;

    private String name;

    private String icon;

    private Integer sortOrder;

    private Integer isSystem;

    private Integer status;

    /** 被引用笔数（售卖=出库流水数 / 进货=商品数） */
    private Integer usedCount;
}
