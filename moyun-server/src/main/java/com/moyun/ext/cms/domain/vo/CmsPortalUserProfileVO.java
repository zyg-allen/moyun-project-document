package com.moyun.ext.cms.domain.vo;

import java.util.List;

import lombok.Data;

/**
 * 后台门户用户画像 VO
 *
 * <p>用于后台"用户画像"抽屉展示，聚合三类信息：
 * <ul>
 *   <li>{@link #user}：完整用户画像（CmsPortalUserVO，含学校/公司/地点/认证/VIP等）</li>
 *   <li>{@link #stats}：用户业务统计（文章/评论/收藏/书架/反馈/举报/粉丝/关注等）</li>
 *   <li>{@link #links}：快速跳转入口（文章管理、评论管理、反馈管理、举报管理，带用户筛选参数）</li>
 * </ul>
 *
 * <p>统计数据来源：
 * <ul>
 *   <li>文章/读书/面试/粉丝/关注/签到：复用 {@link com.moyun.portal.service.IPortalGrowthService#getUserStats(Long)}</li>
 *   <li>反馈/举报/话题帖/简历/收藏：本服务补充聚合</li>
 * </ul>
 *
 * @author moyun
 */
@Data
public class CmsPortalUserProfileVO {

    /** 用户完整画像 */
    private CmsPortalUserVO user;

    /** 用户业务统计 */
    private PortalUserBusinessStatsVO stats;

    /** 快速跳转入口列表（带用户筛选参数） */
    private List<ProfileQuickLink> links;

    /**
     * 快速跳转入口
     */
    @Data
    public static class ProfileQuickLink {
        /** 跳转菜单 key（前端 router 路径，如 /cms/article） */
        private String menuPath;
        /** 显示名称（如 "查看文章"） */
        private String label;
        /** 数量（用于在按钮上显示角标，0 时前端可灰显） */
        private Integer count;
        /** 跳转时携带的 query 参数名（如 authorId / userId） */
        private String queryKey;
        /** 跳转时携带的 query 参数值（用户 id） */
        private Long queryValue;
        /** 图标（Element Plus 图标名，如 Document） */
        private String icon;

        public ProfileQuickLink(String menuPath, String label, Integer count, String queryKey, Long queryValue, String icon) {
            this.menuPath = menuPath;
            this.label = label;
            this.count = count;
            this.queryKey = queryKey;
            this.queryValue = queryValue;
            this.icon = icon;
        }
    }
}
