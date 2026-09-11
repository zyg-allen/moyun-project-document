package com.moyun.ledger.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 记账-分类（系统预设 user_id=0 + is_system=1；用户自定义 user_id>0）
 *
 * @author moyun
 */
@Data
@TableName("ledger_category")
public class LedgerCategory {

    /** 类型：收入 */
    public static final String TYPE_INCOME = "income";
    /** 类型：支出 */
    public static final String TYPE_EXPENSE = "expense";

    /** 状态：启用 */
    public static final int STATUS_ENABLED = 1;
    /** 状态：停用 */
    public static final int STATUS_DISABLED = 0;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 0=系统预设，>0=用户自定义（portal_user.id） */
    private Long userId;

    /** 分类名称 */
    private String name;

    /** 类型：income/expense/transfer/repayment/borrow/adjust */
    private String type;

    /** 语义分组（前端展示分组用，如"生活刚需""负债还款"） */
    private String groupName;

    /** 父分类ID（支持二级分类） */
    private Long parentId;

    /** 图标 */
    private String icon;

    /** 颜色 */
    private String color;

    /** 排序 */
    private Integer sortOrder;

    /** 系统预设：1=是（仅后台可维护） 0=自定义 */
    private Integer isSystem;

    /** 状态：1=启用 0=停用 */
    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    /** 当前用户使用该分类的流水笔数（非表字段，记一笔页常用排序用） */
    @TableField(exist = false)
    private Long usedCount;
}
