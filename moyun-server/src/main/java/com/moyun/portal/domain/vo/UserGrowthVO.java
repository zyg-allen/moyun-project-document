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

    /**
     * 距离下一级**还差**多少成长值（增量，非绝对值）。
     *
     * <p>注意语义：这是 {@code 下一级阈值 - 当前成长值}，不是绝对阈值。</p>
     */
    private Integer nextLevelGrowth;

    /**
     * 当前等级的起点成长值（即本级阈值）。
     *
     * <p>等级阈值是**非线性**的（0/100/300/700/1500/3000/6000/10000/20000），
     * 前端无法自行推算"本级从多少开始"——此前两个页面都按"每级 100"硬算，
     * 进度条与"还差多少"因此显示错误（清单 #2/#33）。</p>
     */
    private Integer levelBaseGrowth;

    /**
     * 本级进度百分比（0..100，已到满级为 100）。
     *
     * <p>由服务端按阈值口径统一计算，避免前端各写一份除法（含除零与满级处理）。</p>
     */
    private Integer levelProgress;

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
