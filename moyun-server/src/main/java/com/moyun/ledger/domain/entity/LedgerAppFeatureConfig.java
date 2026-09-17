package com.moyun.ledger.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 记账小程序功能入口配置 ledger_app_feature_config
 *
 * <p>"我的"页功能宫格可视化运营：开发中功能默认 visible=0 不展示，
 * 由后台按上线节奏开启；排序/名称/图标/角标均可配置。
 *
 * @author moyun
 */
@Data
@TableName("ledger_app_feature_config")
public class LedgerAppFeatureConfig implements Serializable {

    @java.io.Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 功能标识（前端路由映射键） */
    private String featureKey;

    /** 功能名称 */
    private String featureName;

    /** 图标（emoji/符号） */
    private String icon;

    /** 图标颜色 */
    private String iconColor;

    /** 分组：main-主功能宫格 / recommend-推荐小功能 */
    private String groupType;

    /** 组内排序（升序） */
    private Integer sortNum;

    /** 是否展示：1-展示 0-隐藏 */
    private Integer visible;

    /** 状态：done-已上线 / dev-开发中 */
    private String status;

    /** 角标文案（NEW 等） */
    private String badge;

    private String createBy;

    private LocalDateTime createTime;

    private String updateBy;

    private LocalDateTime updateTime;

    private String remark;
}
