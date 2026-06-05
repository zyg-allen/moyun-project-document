package com.moyun.portal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moyun.portal.domain.entity.PortalTag;
import com.moyun.portal.domain.query.TagQuery;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

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
     * @param page 分页参数
     * @param query 查询条件
     * @return 标签信息集合信息
     */
    Page<PortalTag> selectPortalTagPage(Page<PortalTag> page, @Param("params") TagQuery query);

    /**
     * 根据条件查询标签列表（不分页，用于导出等场景）
     *
     * @param query 查询条件
     * @return 标签信息集合信息
     */
    List<PortalTag> selectPortalTagList(@Param("params") TagQuery query);

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
