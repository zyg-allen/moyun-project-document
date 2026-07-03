package com.moyun.ext.cms.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 圈子列表简要信息
 *
 * @author moyun
 */
@Data
public class CircleListItemVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;

    private String name;

    private String description;

    private String cover;

    private Long ownerId;

    private Integer memberCount;

    private Integer postCount;

    private String category;

    private String status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdTime;

    /** 圈主昵称 */
    private String ownerName;

    /** 圈主头像 */
    private String ownerAvatar;
}
