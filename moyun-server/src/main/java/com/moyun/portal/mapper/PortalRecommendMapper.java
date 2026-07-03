package com.moyun.portal.mapper;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 个性化推荐 聚合查询 Mapper（任务 4.8）
 *
 * <p>实现两类算法的 SQL 聚合：
 * <ul>
 *   <li>算法A 基于内容：从用户行为（点赞/收藏/答题）提取兴趣标签，按标签召回同标签内容</li>
 *   <li>算法B 协同过滤简化：找出与当前用户行为相似的用户，推荐他们喜欢但当前用户未看过的内容</li>
 * </ul>
 * 所有查询返回 List&lt;Map&gt;，由 Service 统一转换为 RecommendItemVO。
 * userId 为 null 时仅调用 hot 兜底查询。</p>
 *
 * @author moyun
 */
@Mapper
public interface PortalRecommendMapper {

    // ==================== 文章推荐 ====================

    /** 文章：基于内容（与用户点赞文章共享标签，且用户未看过） */
    List<Map<String, Object>> selectArticleByContent(@Param("userId") Long userId, @Param("limit") int limit);

    /** 文章：协同过滤（点赞过同文章的相似用户喜欢的文章，当前用户未看过） */
    List<Map<String, Object>> selectArticleByCollaborative(@Param("userId") Long userId, @Param("limit") int limit);

    /** 文章：热门兜底（按浏览+点赞加权） */
    List<Map<String, Object>> selectHotArticles(@Param("limit") int limit);

    // ==================== 题目推荐 ====================

    /** 题目：基于内容（与用户答题/点赞/收藏题目共享标签，且用户未做过） */
    List<Map<String, Object>> selectQuestionByContent(@Param("userId") Long userId, @Param("limit") int limit);

    /** 题目：协同过滤（行为相似用户点赞/收藏的题目，当前用户未做过） */
    List<Map<String, Object>> selectQuestionByCollaborative(@Param("userId") Long userId, @Param("limit") int limit);

    /** 题目：热门兜底（按点赞+提交加权） */
    List<Map<String, Object>> selectHotQuestions(@Param("limit") int limit);

    // ==================== 书籍推荐 ====================

    /** 书籍：基于内容（与用户收藏书籍同分类，或同作者） */
    List<Map<String, Object>> selectBookByContent(@Param("userId") Long userId, @Param("limit") int limit);

    /** 书籍：协同过滤（行为相似用户收藏的书籍，当前用户未收藏） */
    List<Map<String, Object>> selectBookByCollaborative(@Param("userId") Long userId, @Param("limit") int limit);

    /** 书籍：热门兜底（按阅读人数+评分加权） */
    List<Map<String, Object>> selectHotBooks(@Param("limit") int limit);

    // ==================== 创作者推荐 ====================

    /** 创作者：协同过滤（行为相似用户关注的创作者，当前用户未关注） */
    List<Map<String, Object>> selectCreatorByCollaborative(@Param("userId") Long userId, @Param("limit") int limit);

    /** 创作者：热门兜底（按文章总浏览量+粉丝数加权） */
    List<Map<String, Object>> selectHotCreators(@Param("limit") int limit);
}
