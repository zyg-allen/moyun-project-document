package com.moyun.portal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.moyun.portal.domain.PortalTag;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 门户标签表 数据层
 *
 * @author moyun
 */
@Mapper
public interface PortalTagMapper extends BaseMapper<PortalTag> {

    /**
     * 根据条件分页查询标签列表
     *
     * @param portalTag 标签信息
     * @return 标签信息集合信息
     */
    public List<PortalTag> selectPortalTagList(PortalTag portalTag);

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
