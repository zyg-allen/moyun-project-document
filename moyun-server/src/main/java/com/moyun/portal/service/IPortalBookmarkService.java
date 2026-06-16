package com.moyun.portal.service;

import java.util.List;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.moyun.portal.domain.entity.PortalBookmark;
import com.moyun.portal.domain.query.BookmarkQuery;

/**
 * 门户收藏 业务层
 *
 * @author moyun
 */
public interface IPortalBookmarkService {

    /**
     * 根据条件分页查询收藏列表
     *
     * @param page 分页参数
     * @param query 查询条件
     * @return 分页结果
     */
    Page<PortalBookmark> selectPortalBookmarkPage(Page<PortalBookmark> page, BookmarkQuery query);

    /**
     * 根据条件查询收藏列表（不分页，用于导出等场景）
     *
     * @param query 查询条件
     * @return 收藏信息集合
     */
    List<PortalBookmark> selectPortalBookmarkList(BookmarkQuery query);

    /**
     * 通过收藏ID查询收藏
     *
     * @param id 收藏ID
     * @return 收藏对象信息
     */
    public PortalBookmark selectPortalBookmarkById(Long id);

    /**
     * 新增收藏信息
     *
     * @param portalBookmark 收藏信息
     * @return 结果
     */
    public int insertPortalBookmark(PortalBookmark portalBookmark);

    /**
     * 修改收藏信息
     *
     * @param portalBookmark 收藏信息
     * @return 结果
     */
    public int updatePortalBookmark(PortalBookmark portalBookmark);

    /**
     * 通过收藏ID删除收藏
     *
     * @param id 收藏ID
     * @return 结果
     */
    public int deletePortalBookmarkById(Long id);

    /**
     * 批量删除收藏信息
     *
     * @param ids 需要删除的收藏ID
     * @return 结果
     */
    public int deletePortalBookmarkByIds(Long[] ids);
}
