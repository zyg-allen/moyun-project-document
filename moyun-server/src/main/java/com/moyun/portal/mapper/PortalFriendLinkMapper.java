package com.moyun.portal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moyun.portal.domain.entity.PortalFriendLink;
import com.moyun.portal.domain.query.FriendLinkQuery;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 门户友情链接表 数据层
 *
 * @author moyun
 */
@Mapper
public interface PortalFriendLinkMapper extends BaseMapper<PortalFriendLink> {

    /**
     * 根据条件分页查询友情链接列表
     *
     * @param page 分页参数
     * @param query 查询条件
     * @return 友情链接信息集合信息
     */
    Page<PortalFriendLink> selectPortalFriendLinkPage(Page<PortalFriendLink> page, @Param("params") FriendLinkQuery query);

    /**
     * 根据条件查询友情链接列表（不分页，用于导出等场景）
     *
     * @param query 查询条件
     * @return 友情链接信息集合信息
     */
    List<PortalFriendLink> selectPortalFriendLinkList(@Param("params") FriendLinkQuery query);

    /**
     * 通过友情链接ID查询友情链接
     *
     * @param id 友情链接ID
     * @return 友情链接对象信息
     */
    public PortalFriendLink selectPortalFriendLinkById(Long id);

    /**
     * 新增友情链接信息
     *
     * @param portalFriendLink 友情链接信息
     * @return 结果
     */
    public int insertPortalFriendLink(PortalFriendLink portalFriendLink);

    /**
     * 修改友情链接信息
     *
     * @param portalFriendLink 友情链接信息
     * @return 结果
     */
    public int updatePortalFriendLink(PortalFriendLink portalFriendLink);

    /**
     * 通过友情链接ID删除友情链接
     *
     * @param id 友情链接ID
     * @return 结果
     */
    public int deletePortalFriendLinkById(Long id);

    /**
     * 批量删除友情链接信息
     *
     * @param ids 需要删除的友情链接ID
     * @return 结果
     */
    public int deletePortalFriendLinkByIds(Long[] ids);
}
