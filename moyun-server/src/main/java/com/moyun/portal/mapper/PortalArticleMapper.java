package com.moyun.portal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.moyun.portal.domain.entity.PortalArticle;
import com.moyun.portal.domain.query.ArticleQuery;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 门户文章表 数据层
 *
 * @author moyun
 */
@Mapper
public interface PortalArticleMapper extends BaseMapper<PortalArticle> {

    /**
     * 根据条件分页查询文章列表
     *
     * @param portalArticle 文章信息
     * @return 文章信息集合信息
     */
    public List<PortalArticle> selectPortalArticleList(ArticleQuery portalArticle);

    /**
     * 通过文章ID查询文章
     *
     * @param id 文章ID
     * @return 文章对象信息
     */
    public PortalArticle selectPortalArticleById(Long id);

    /**
     * 新增文章信息
     *
     * @param portalArticle 文章信息
     * @return 结果
     */
    public int insertPortalArticle(PortalArticle portalArticle);

    /**
     * 修改文章信息
     *
     * @param portalArticle 文章信息
     * @return 结果
     */
    public int updatePortalArticle(PortalArticle portalArticle);

    /**
     * 通过文章ID删除文章
     *
     * @param id 文章ID
     * @return 结果
     */
    public int deletePortalArticleById(Long id);

    /**
     * 批量删除文章信息
     *
     * @param ids 需要删除的文章ID
     * @return 结果
     */
    public int deletePortalArticleByIds(Long[] ids);
}
