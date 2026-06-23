package com.moyun.portal.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户成长信息 VO
 *
 * @author moyun
 */
@Data
public class UserGrowthVO {

    /** 用户ID */
    private Long userId;

    /** 成长值（累计） */
    private Integer growthValue;

    /** 当前等级 */
    private Integer level;

    /** 当前头衔 */
    private String title;

    /** 本季成长值 */
    private Integer seasonValue;

    /** 距离下一级所需成长值 */
    private Integer nextLevelGrowth;

    /** 下一级头衔 */
    private String nextLevelTitle;

    /** 本季排名（可选） */
    private Integer seasonRank;

    /** 昵称（排行榜场景使用） */
    private String nickname;

    /** 头像（排行榜场景使用） */
    private String avatar;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
