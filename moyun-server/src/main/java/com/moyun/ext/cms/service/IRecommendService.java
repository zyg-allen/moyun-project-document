package com.moyun.ext.cms.service;

import com.moyun.ext.cms.domain.vo.RecommendItemVO;

import java.util.List;

/**
 * 个性化推荐 Service 接口（任务 4.8）
 *
 * <p>融合两类算法（简化版）：
 * <ul>
 *   <li>算法 A 基于内容：从用户行为（点赞文章 / 收藏书籍 / 刷题标签）提取兴趣标签，按标签召回同标签内容</li>
 *   <li>算法 B 协同过滤简化：找出与当前用户行为相似的用户（共同点赞 / 收藏），推荐他们喜欢但当前用户未看过的内容</li>
 * </ul>
 * 召回顺序：内容 -> 协同 -> 热门兜底；按 id 去重，最终返回 Top 10。
 * userId 为 null 时（未登录）直接返回热门数据。</p>
 *
 * @author moyun
 */
public interface IRecommendService {

    /**
     * 推荐文章（Top 10）
     *
     * @param userId 当前用户ID，可为 null（未登录）
     * @return 推荐列表
     */
    List<RecommendItemVO> recommendArticles(Long userId);

    /**
     * 推荐题目（Top 10）
     *
     * @param userId 当前用户ID，可为 null（未登录）
     * @return 推荐列表
     */
    List<RecommendItemVO> recommendQuestions(Long userId);

    /**
     * 推荐书籍（Top 10）
     *
     * @param userId 当前用户ID，可为 null（未登录）
     * @return 推荐列表
     */
    List<RecommendItemVO> recommendBooks(Long userId);

    /**
     * 推荐创作者（Top 10）
     *
     * @param userId 当前用户ID，可为 null（未登录）
     * @return 推荐列表
     */
    List<RecommendItemVO> recommendCreators(Long userId);
}
