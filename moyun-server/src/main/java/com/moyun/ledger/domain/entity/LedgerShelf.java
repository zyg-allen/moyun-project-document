package com.moyun.ledger.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 仓库货架（存放位置）。
 *
 * <p>归属：{@code warehouse_id} 决定它属于哪个仓，{@code user_id} 记录归属人。
 * 图片不进本表，统一写 {@code sys_file}
 * （business_type='ledger_shelf'，business_id=货架 id，按 id 升序首张为封面）。</p>
 *
 * @author moyun
 */
@Data
@TableName("ledger_shelf")
public class LedgerShelf {

    /** 货架类型：普通货架 */
    public static final String TYPE_NORMAL = "normal";
    /** 货架类型：冷藏/冷冻 */
    public static final String TYPE_REFRIGERATED = "refrigerated";
    /** 货架类型：保险柜/上锁 */
    public static final String TYPE_LOCKED = "locked";
    /** 货架类型：临时暂存 */
    public static final String TYPE_TEMP = "temp";

    public static final int STATUS_ENABLED = 1;
    public static final int STATUS_DISABLED = 0;

    /** sys_file.business_type：货架图片 */
    public static final String FILE_BIZ_TYPE = "ledger_shelf";
    /** sys_file.business_type：商品图片 */
    public static final String PRODUCT_FILE_BIZ_TYPE = "ledger_product";

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 归属：所属仓库 */
    private Long warehouseId;

    /** 归属：归属人 */
    private Long userId;

    /** 货架编号（仓库内唯一，如 A-01） */
    private String code;

    /** 货架名称 */
    private String name;

    /** 区域/分区（如 A区、冷藏区） */
    private String zone;

    /** 排 */
    private Integer rowNo;

    /** 层 */
    private Integer layerNo;

    /** 位 */
    private Integer positionNo;

    /** 位置描述（后端按 区/排/层/位 自动拼装） */
    private String locationText;

    /** 类型：normal/refrigerated/locked/temp */
    private String shelfType;

    /** 容量（件，超容量仅提示不阻断） */
    private Integer capacity;

    /** 排序 */
    private Integer sortOrder;

    /** 备注 */
    private String remark;

    /** 乐观锁版本号 */
    private Integer version;

    /** 状态：1=启用 0=停用 */
    private Integer status;

    private String createBy;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    // ---------------- 非表字段（列表/详情展示用） ----------------

    /** 该货架上的商品种类数 */
    @TableField(exist = false)
    private Integer productCount;

    /** 该货架上的库存总件数 */
    @TableField(exist = false)
    private Integer stockQuantity;

    /** 该货架上的库存成本价值 */
    @TableField(exist = false)
    private BigDecimal costValue;

    /** 所属仓库名称（列表展示用） */
    @TableField(exist = false)
    private String warehouseName;

    /** 封面图 URL（sys_file 中该货架的第一张，列表网格展示用） */
    @TableField(exist = false)
    private String coverUrl;
}
