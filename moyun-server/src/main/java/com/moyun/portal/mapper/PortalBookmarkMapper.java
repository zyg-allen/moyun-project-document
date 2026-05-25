package com.moyun.portal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.moyun.portal.domain.entity.PortalBookmark;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 门户收藏表 数据层
 *
 * @author moyun
 */
@Mapper
public interface PortalBookmarkMapper extends BaseMapper<PortalBookmark> {

    /**
     * 根据条件分页查询收藏列表
     *
     * @param portalBookmark 收藏信息
     * @return 收藏信息集合信息
     */
    public List<PortalBookmark> selectPortalBookmarkList(PortalBookmark portalBookmark);

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
