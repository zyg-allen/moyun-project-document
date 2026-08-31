package com.moyun.portal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moyun.ext.cms.domain.vo.ArticleSimpleVO;
import com.moyun.portal.domain.entity.PortalColumnArticle;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 专栏-文章关联 Mapper
 *
 * @author moyun
 */
@Mapper
public interface PortalColumnArticleMapper extends BaseMapper<PortalColumnArticle> {

    /**
     * 查询专栏文章目录（关联 portal_article 取标题、封面、摘要、viewCount、likeCount、createdTime）
     *
     * @param columnId 专栏ID
     * @return 文章目录（按 sort_order 升序）
     */
    List<ArticleSimpleVO> selectArticlesByColumn(@Param("columnId") Long columnId);

    /**
     * CMS后台：分页查询专栏已绑定的文章列表（关联 portal_article + portal_user，含作者昵称/用户名）
     *
     * @param page     分页参数
     * @param columnId 专栏ID
     * @param keyword  文章标题关键词（可空）
     * @return 分页结果
     */
    Page<ArticleSimpleVO> selectColumnArticlesPage(
            Page<ArticleSimpleVO> page,
            @Param("columnId") Long columnId,
            @Param("keyword") String keyword);

    /**
     * 统计专栏文章数
     */
    @Select("SELECT COUNT(*) FROM portal_column_article WHERE column_id = #{columnId}")
    int countByColumn(@Param("columnId") Long columnId);
}
