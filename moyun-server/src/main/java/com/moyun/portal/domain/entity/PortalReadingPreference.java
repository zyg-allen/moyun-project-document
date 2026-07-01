package com.moyun.portal.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import com.moyun.core.base.BaseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户阅读偏好实体 portal_reading_preference
 * 每用户单条，通过 UK(user_id) 约束，upsert 更新
 *
 * @author moyun
 */
@Data
@TableName("portal_reading_preference")
public class PortalReadingPreference extends BaseEntity {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 正文字号（px，12-32） */
    private Integer fontSize;

    /** 行距（倍，1.2-3.0） */
    private BigDecimal lineHeight;

    /** 阅读主题：default=跟随 / light=亮色 / dark=暗色 / sepia=护眼黄 */
    private String theme;

    /** 字体：system=系统默认 / serif=衬线 / song=宋体 / hei=黑体 */
    private String fontFamily;

    /** 字间距（px，-1.0-5.0） */
    private BigDecimal letterSpacing;

    /** 段间距（em，0.5-5.0） */
    private BigDecimal paragraphSpacing;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    public PortalReadingPreference() {
    }

    public PortalReadingPreference(Long id) {
        this.id = id;
    }
}
