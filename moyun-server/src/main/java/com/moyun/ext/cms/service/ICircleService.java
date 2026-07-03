package com.moyun.ext.cms.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moyun.core.base.page.PageDomain;
import com.moyun.ext.cms.domain.query.CircleQuery;
import com.moyun.ext.cms.domain.vo.CircleListItemVO;
import com.moyun.ext.cms.domain.vo.CirclePostVO;
import com.moyun.ext.cms.domain.vo.CircleVO;

/**
 * 圈子/兴趣小组 Service 接口（社交深化与商业化 4.1）
 *
 * @author moyun
 */
public interface ICircleService {

    /**
     * 圈子公开列表（分页，仅 active）
     */
    Page<CircleListItemVO> listCircles(CircleQuery query);

    /**
     * 圈子详情（公开，含圈主信息、成员前 N、当前用户视角）
     *
     * @param id            圈子ID
     * @param currentUserId 当前登录用户ID（未登录传 null）
     */
    CircleVO getCircleDetail(Long id, Long currentUserId);

    /**
     * 创建圈子（创建者自动成为 owner 成员）
     *
     * @return 圈子ID
     */
    Long createCircle(CircleVO vo, Long userId);

    /**
     * 修改圈子（仅圈主本人）
     *
     * @return 影响行数
     */
    int updateCircle(CircleVO vo, Long userId);

    /**
     * 删除圈子（仅圈主本人，级联删除成员与帖子）
     *
     * @return 影响行数
     */
    int deleteCircle(Long id, Long userId);

    /**
     * 加入圈子（幂等，原子更新成员数）
     *
     * @return true=已加入
     */
    boolean joinCircle(Long circleId, Long userId);

    /**
     * 退出圈子（圈主不可退出，需先转让；原子更新成员数）
     *
     * @return true=已退出
     */
    boolean leaveCircle(Long circleId, Long userId);

    /**
     * 圈子帖子分页（公开，仅 active）
     */
    Page<CirclePostVO> listCirclePosts(Long circleId, PageDomain query);

    /**
     * 发帖（需为圈子成员）
     *
     * @return 帖子ID
     */
    Long createPost(Long circleId, CirclePostVO post, Long userId);

    // ==================== 后台管理 ====================

    /**
     * 后台圈子分页（含所有状态）
     */
    Page<CircleListItemVO> cmsListCircles(CircleQuery query);

    /**
     * 后台审核圈子（启用/禁用）
     *
     * @return 影响行数
     */
    int cmsAuditCircle(Long id, String status);

    /**
     * 后台删除圈子（级联）
     *
     * @return 影响行数
     */
    int cmsDeleteCircle(Long id);

    /**
     * 后台帖子分页（含所有状态）
     */
    Page<CirclePostVO> cmsListPosts(Long circleId, String keyword, String status, PageDomain query);

    /**
     * 后台删除帖子
     *
     * @return 影响行数
     */
    int cmsDeletePost(Long id);
}
