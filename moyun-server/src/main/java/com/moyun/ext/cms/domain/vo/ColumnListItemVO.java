package com.moyun.ext.cms.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 专栏列表简要信息
 *
 * @author moyun
 */
@Data
public class ColumnListItemVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;

    private Long userId;

    private String title;

    private String subtitle;

    private String cover;

    private Long categoryId;

    private String status;

    private Integer articleCount;

    private Integer subscribeCount;

    private Integer viewCount;

    private Integer isFinished;

    private BigDecimal price;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdTime;

    /** 作者昵称 */
    private String authorName;

    /** 作者头像 */
    private String authorAvatar;
}
