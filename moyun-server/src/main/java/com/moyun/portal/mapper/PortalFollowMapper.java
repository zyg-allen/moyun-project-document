package com.moyun.portal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moyun.portal.domain.entity.PortalFollow;
import com.moyun.portal.domain.query.FollowQuery;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 门户关注表 数据层
 *
 * @author moyun
 */
@Mapper
public interface PortalFollowMapper extends BaseMapper<PortalFollow> {

    /**
     * 根据条件分页查询关注列表
     *
     * @param page 分页参数
     * @param query 查询条件
     * @return 关注信息集合信息
     */
    Page<PortalFollow> selectPortalFollowPage(Page<PortalFollow> page, @Param("params") FollowQuery query);

    /**
     * 根据条件查询关注列表（不分页，用于导出等场景）
     *
     * @param query 查询条件
     * @return 关注信息集合信息
     */
    List<PortalFollow> selectPortalFollowList(@Param("params") FollowQuery query);

    /**
     * 通过关注ID查询关注
     *
     * @param id 关注ID
     * @return 关注对象信息
     */
    public PortalFollow selectPortalFollowById(Long id);

    /**
     * 新增关注信息
     *
     * @param portalFollow 关注信息
     * @return 结果
     */
    public int insertPortalFollow(PortalFollow portalFollow);

    /**
     * 修改关注信息
     *
     * @param portalFollow 关注信息
     * @return 结果
     */
    public int updatePortalFollow(PortalFollow portalFollow);

    /**
     * 通过关注ID删除关注
     *
     * @param id 关注ID
     * @return 结果
     */
    public int deletePortalFollowById(Long id);

    /**
     * 批量删除关注信息
     *
     * @param ids 需要删除的关注ID
     * @return 结果
     */
    public int deletePortalFollowByIds(Long[] ids);
}
