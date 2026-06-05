package com.moyun.portal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moyun.portal.domain.entity.PortalLike;
import com.moyun.portal.domain.query.LikeQuery;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 门户点赞表 数据层
 *
 * @author moyun
 */
@Mapper
public interface PortalLikeMapper extends BaseMapper<PortalLike> {

    /**
     * 根据条件分页查询点赞列表
     *
     * @param page 分页参数
     * @param query 查询条件
     * @return 点赞信息集合信息
     */
    Page<PortalLike> selectPortalLikePage(Page<PortalLike> page, @Param("params") LikeQuery query);

    /**
     * 根据条件查询点赞列表（不分页，用于导出等场景）
     *
     * @param query 查询条件
     * @return 点赞信息集合信息
     */
    List<PortalLike> selectPortalLikeList(@Param("params") LikeQuery query);

    /**
     * 通过点赞ID查询点赞
     *
     * @param id 点赞ID
     * @return 点赞对象信息
     */
    public PortalLike selectPortalLikeById(Long id);

    /**
     * 新增点赞信息
     *
     * @param portalLike 点赞信息
     * @return 结果
     */
    public int insertPortalLike(PortalLike portalLike);

    /**
     * 修改点赞信息
     *
     * @param portalLike 点赞信息
     * @return 结果
     */
    public int updatePortalLike(PortalLike portalLike);

    /**
     * 通过点赞ID删除点赞
     *
     * @param id 点赞ID
     * @return 结果
     */
    public int deletePortalLikeById(Long id);

    /**
     * 批量删除点赞信息
     *
     * @param ids 需要删除的点赞ID
     * @return 结果
     */
    public int deletePortalLikeByIds(Long[] ids);
}
