package com.moyun.portal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
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
     * 统计专栏文章数
     */
    @Select("SELECT COUNT(*) FROM portal_column_article WHERE column_id = #{columnId}")
    int countByColumn(@Param("columnId") Long columnId);
}
