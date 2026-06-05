package com.moyun.portal.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moyun.portal.domain.entity.PortalTag;
import com.moyun.portal.domain.query.TagQuery;

import java.util.List;

/**
 * 门户标签 业务层
 *
 * @author moyun
 */
public interface IPortalTagService {

    /**
     * 根据条件分页查询标签列表
     *
     * @param page 分页参数
     * @param query 查询条件
     * @return 分页结果
     */
    Page<PortalTag> selectPortalTagPage(Page<PortalTag> page, TagQuery query);

    /**
     * 根据条件查询标签列表（不分页，用于导出等场景）
     *
     * @param query 查询条件
     * @return 标签信息集合
     */
    List<PortalTag> selectPortalTagList(TagQuery query);

    /**
     * 通过标签ID查询标签
     *
     * @param id 标签ID
     * @return 标签对象信息
     */
    public PortalTag selectPortalTagById(Long id);

    /**
     * 新增标签信息
     *
     * @param portalTag 标签信息
     * @return 结果
     */
    public int insertPortalTag(PortalTag portalTag);

    /**
     * 修改标签信息
     *
     * @param portalTag 标签信息
     * @return 结果
     */
    public int updatePortalTag(PortalTag portalTag);

    /**
     * 通过标签ID删除标签
     *
     * @param id 标签ID
     * @return 结果
     */
    public int deletePortalTagById(Long id);

    /**
     * 批量删除标签信息
     *
     * @param ids 需要删除的标签ID
     * @return 结果
     */
    public int deletePortalTagByIds(Long[] ids);
}
