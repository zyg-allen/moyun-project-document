package com.moyun.portal.mapper;

import java.util.List;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.moyun.portal.domain.entity.PortalTag;
import com.moyun.portal.domain.query.TagQuery;

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

    /**
     * 根据名称精确查询标签（可选 module）
     *
     * @param name   标签名称
     * @param module 所属模块（可为 null）
     * @return 标签对象
     */
    PortalTag selectByNameAndModule(@Param("name") String name, @Param("module") String module);

    /**
     * 根据名称精确查询标签（仅按 name，与 uk_name 唯一约束一致）
     *
     * @param name 标签名称
     * @return 标签对象
     */
    PortalTag selectByName(@Param("name") String name);

    /**
     * 根据实体（entityType + entityId）查询已关联的标签
     *
     * @param entityType 实体类型
     * @param entityId   实体ID
     * @return 标签列表
     */
    List<PortalTag> selectTagsByEntity(@Param("entityType") String entityType, @Param("entityId") Long entityId);

    /**
     * 根据模块（可选）按引用次数降序获取热门标签
     *
     * @param module 所属模块（可为 null 表示全部）
     * @param limit  限制数量
     * @return 标签列表
     */
    List<PortalTag> selectHotTags(@Param("module") String module, @Param("limit") Integer limit);

    /**
     * 批量更新 reference_count（原子增减，delta 为 1 或 -1）
     */
    int updateReferenceCountBatch(@Param("ids") List<Long> ids, @Param("delta") Long delta);

    /**
     * 单条更新 reference_count（原子增减）
     */
    int updateReferenceCount(@Param("id") Long id, @Param("delta") Long delta);
}
