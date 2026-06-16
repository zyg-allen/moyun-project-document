package com.moyun.portal.service.impl;

import java.util.List;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.moyun.portal.domain.entity.PortalBookmark;
import com.moyun.portal.domain.query.BookmarkQuery;
import com.moyun.portal.mapper.PortalBookmarkMapper;
import com.moyun.portal.service.IPortalBookmarkService;
import com.moyun.portal.util.PortalSecurityUtils;

/**
 * 门户收藏 业务层处理
 *
 * @author moyun
 */
@Service
public class PortalBookmarkServiceImpl extends ServiceImpl<PortalBookmarkMapper, PortalBookmark> implements IPortalBookmarkService {

    @Autowired
    private PortalBookmarkMapper portalBookmarkMapper;

    /**
     * 根据条件分页查询收藏列表
     *
     * @param page  分页参数
     * @param query 查询条件
     * @return 分页结果
     */
    @Override
    public Page<PortalBookmark> selectPortalBookmarkPage(Page<PortalBookmark> page, BookmarkQuery query) {
        return baseMapper.selectPortalBookmarkPage(page, query);
    }

    /**
     * 根据条件查询收藏列表（不分页，用于导出等场景）
     *
     * @param query 查询条件
     * @return 收藏信息集合
     */
    @Override
    public List<PortalBookmark> selectPortalBookmarkList(BookmarkQuery query) {
        return baseMapper.selectPortalBookmarkList(query);
    }

    /**
     * 通过收藏ID查询收藏
     *
     * @param id 收藏ID
     * @return 收藏对象信息
     */
    @Override
    public PortalBookmark selectPortalBookmarkById(Long id) {
        return portalBookmarkMapper.selectPortalBookmarkById(id);
    }

    /**
     * 新增收藏信息
     * 自动获取当前登录用户的ID
     *
     * @param portalBookmark 收藏信息
     * @return 结果
     */
    @Override
    public int insertPortalBookmark(PortalBookmark portalBookmark) {
        // 自动填充用户ID
        if (portalBookmark.getUserId() == null) {
            portalBookmark.setUserId(PortalSecurityUtils.getUserId());
        }
        return portalBookmarkMapper.insertPortalBookmark(portalBookmark);
    }

    /**
     * 修改收藏信息
     *
     * @param portalBookmark 收藏信息
     * @return 结果
     */
    @Override
    public int updatePortalBookmark(PortalBookmark portalBookmark) {
        return portalBookmarkMapper.updatePortalBookmark(portalBookmark);
    }

    /**
     * 通过收藏ID删除收藏
     *
     * @param id 收藏ID
     * @return 结果
     */
    @Override
    public int deletePortalBookmarkById(Long id) {
        return portalBookmarkMapper.deletePortalBookmarkById(id);
    }

    /**
     * 批量删除收藏信息
     *
     * @param ids 需要删除的收藏ID
     * @return 结果
     */
    @Override
    public int deletePortalBookmarkByIds(Long[] ids) {
        return portalBookmarkMapper.deletePortalBookmarkByIds(ids);
    }
}
