package com.moyun.portal.mapper;

import java.util.List;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

import com.moyun.portal.domain.entity.PortalArticleTag;

/**
 * 门户文章标签关联表 数据层
 *
 * @author moyun
 */
@Mapper
public interface PortalArticleTagMapper extends BaseMapper<PortalArticleTag> {

    /**
     * 根据条件分页查询文章标签关联列表
     *
     * @param portalArticleTag 文章标签关联信息
     * @return 文章标签关联信息集合信息
     */
    public List<PortalArticleTag> selectPortalArticleTagList(PortalArticleTag portalArticleTag);

    /**
     * 通过文章ID和标签ID查询关联
     *
     * @param articleId 文章ID
     * @param tagId     标签ID
     * @return 文章标签关联对象信息
     */
    public PortalArticleTag selectPortalArticleTagById(Long articleId, Long tagId);

    /**
     * 新增文章标签关联信息
     *
     * @param portalArticleTag 文章标签关联信息
     * @return 结果
     */
    public int insertPortalArticleTag(PortalArticleTag portalArticleTag);

    /**
     * 批量新增文章标签关联
     *
     * @param portalArticleTagList 文章标签关联列表
     * @return 结果
     */
    public int batchInsertPortalArticleTag(List<PortalArticleTag> portalArticleTagList);

    /**
     * 通过文章ID删除文章标签关联
     *
     * @param articleId 文章ID
     * @return 结果
     */
    public int deletePortalArticleTagByArticleId(Long articleId);

    /**
     * 批量删除文章标签关联信息
     *
     * @param articleIds 需要删除的文章ID
     * @return 结果
     */
    public int deletePortalArticleTagByArticleIds(Long[] articleIds);
}
