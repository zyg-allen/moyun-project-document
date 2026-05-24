package com.moyun.community.content.model.vo;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 前台收藏 VO
 *
 * @author moyun
 */
@Data
public class PortalCollectVo implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long userId;
    private Long articleId;
    private String articleTitle;
    private String articleCover;
    private String articleSummary;
    private String createdAt;
}
