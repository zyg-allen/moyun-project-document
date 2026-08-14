package com.moyun.portal.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 导入模板字段配置 portal_import_template_config
 * <p>
 * 用于运营动态维护各业务的 Excel 导入模板字段（列名、说明、示例、必填、字典等），
 * 无需改代码即可调整模板。后端 {@code importTemplate} 接口优先读此表生成模板，
 * 未命中配置时回退到实体 {@code @Excel} 注解。
 * <p>
 * 一个 businessKey（如 interview_question）对应多条字段配置，按 sort 升序生成列。
 *
 * @author moyun
 */
@Data
@TableName("portal_import_template_config")
public class PortalImportTemplateConfig implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 业务标识（如 interview_question / interview_experience / article / tag）
     * 与各业务 Controller 的导入接口一一对应
     */
    private String businessKey;

    /**
     * 实体字段名（Java 属性名，用于反射取值/赋值，如 title / difficulty）
     */
    private String fieldName;

    /**
     * Excel 列名（表头显示名，如"题目标题"）
     */
    private String columnName;

    /**
     * 字段说明（模板第二行说明文字，如"必填，不超过500字"）
     */
    private String description;

    /**
     * 示例值（模板第三行示例数据，如"请实现一个 LRU 缓存"）
     */
    private String exampleValue;

    /**
     * 是否必填（1=必填 0=可选）
     */
    private Integer required;

    /**
     * 字段类型：string / number / date / dict
     */
    private String fieldType;

    /**
     * 字典 type（fieldType=dict 时生效，如 sys_normal_disable）
     */
    private String dictType;

    /**
     * 下拉可选值（逗号分隔，优先于 dictType，用于枚举型如 easy,medium,hard）
     */
    private String comboValues;

    /**
     * 列宽（Excel 列宽，默认 20）
     */
    private Integer columnWidth;

    /**
     * 排序号（升序生成列，默认按 id 升序）
     */
    private Integer sort;

    /**
     * 状态（0=启用 1=停用，停用的字段不进入模板）
     */
    private String status;

    /**
     * 创建者
     */
    private String createBy;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /**
     * 更新者
     */
    private String updateBy;

    /**
     * 更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    /**
     * 备注（运营维护说明）
     */
    private String remark;
}
