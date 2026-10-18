package com.moyun.ledger.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 商品类目（两级树，仿记账分类）。
 *
 * <p>{@code userId = 0} 表示系统预设（所有用户可见、只读）；
 * 用户自建类目的 {@code userId} 为本人 id。
 * {@code parentId = 0} 表示一级类目（用 0 而非 NULL，避免唯一索引对 NULL 失效）。</p>
 *
 * @author moyun
 */
@Data
@TableName("ledger_product_category")
public class LedgerProductCategory {

    /** 系统预设类目的归属用户 id */
    public static final long SYSTEM_USER_ID = 0L;
    public static final int STATUS_ENABLED = 1;
    public static final int STATUS_DISABLED = 0;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 归属用户，0=系统预设 */
    private Long userId;

    private String name;

    /** 父类目 id，0=一级 */
    private Long parentId;

    /** 图标：emoji key 或图片 URL */
    private String icon;

    private Integer sortOrder;

    /** 1=系统预设 0=用户自建 */
    private Integer isSystem;

    /** 1=启用 0=停用 */
    private Integer status;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    // ---------------- 非表字段 ----------------

    /** 二级类目（树形返回时填充） */
    @TableField(exist = false)
    private List<LedgerProductCategory> children;

    /** 该类目下商品数（列表展示用） */
    @TableField(exist = false)
    private Integer usedCount;

    /** 是否系统预设（前端直接可用，避免数字/字符串比较踩坑） */
    @TableField(exist = false)
    private Boolean system;
}
