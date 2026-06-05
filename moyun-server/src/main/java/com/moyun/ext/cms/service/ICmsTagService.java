package com.moyun.ext.cms.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moyun.ext.cms.domain.query.CmsTagQuery;
import com.moyun.ext.cms.domain.vo.CmsTagVO;
import com.moyun.portal.domain.entity.PortalTag;

import java.util.List;

/**
 * CMS标签服务接口
 *
 * @author moyun
 */
public interface ICmsTagService
{
    /**
     * 查询标签分页列表
     *
     * @param page 分页参数
     * @param query 查询条件
     * @return 标签分页列表
     */
    Page<CmsTagVO> selectTagPage(Page<CmsTagVO> page, CmsTagQuery query);

    /**
     * 查询标签列表
     *
     * @param query 查询条件
     * @return 标签列表
     */
    List<PortalTag> selectTagList(CmsTagQuery query);

    /**
     * 查询标签详情
     *
     * @param id 标签ID
     * @return 标签信息
     */
    PortalTag selectTagById(Long id);

    /**
     * 新增标签
     *
     * @param tag 标签信息
     * @return 结果
     */
    int insertTag(PortalTag tag);

    /**
     * 修改标签
     *
     * @param tag 标签信息
     * @return 结果
     */
    int updateTag(PortalTag tag);

    /**
     * 批量删除标签
     *
     * @param ids 需要删除的标签ID
     * @return 结果
     */
    int deleteTagByIds(Long[] ids);
}
