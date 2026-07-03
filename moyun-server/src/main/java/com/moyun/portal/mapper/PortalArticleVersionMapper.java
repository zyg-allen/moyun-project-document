package com.moyun.portal.mapper;

import java.util.List;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.moyun.portal.domain.entity.PortalArticleVersion;

/**
 * 文章版本快照 Mapper
 *
 * @author moyun
 */
@Mapper
public interface PortalArticleVersionMapper extends BaseMapper<PortalArticleVersion> {

    /**
     * 查询指定文章的版本号最大值（用于自增 version_no）
     *
     * @param articleId 文章ID
     * @return 当前最大版本号，无版本时返回 null
     */
    @Select("SELECT MAX(version_no) FROM portal_article_version WHERE article_id = #{articleId}")
    Integer selectMaxVersionNo(@Param("articleId") Long articleId);

    /**
     * 查询指定文章的版本列表（按版本号降序，不含 content / content_markdown 大字段）
     *
     * @param articleId 文章ID
     * @return 版本列表
     */
    @Select("SELECT id, article_id, version_no, title, excerpt, operator_id, created_time " +
            "FROM portal_article_version " +
            "WHERE article_id = #{articleId} " +
            "ORDER BY version_no DESC")
    List<PortalArticleVersion> selectVersionList(@Param("articleId") Long articleId);
}
