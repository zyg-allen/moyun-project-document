package com.moyun.portal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moyun.ext.cms.domain.query.CircleQuery;
import com.moyun.ext.cms.domain.vo.CircleListItemVO;
import com.moyun.ext.cms.domain.vo.CircleMemberVO;
import com.moyun.ext.cms.domain.vo.CircleVO;
import com.moyun.portal.domain.entity.PortalCircle;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 圈子 Mapper
 *
 * @author moyun
 */
@Mapper
public interface PortalCircleMapper extends BaseMapper<PortalCircle> {

    /**
     * 圈子公开列表分页（仅 active，含圈主信息）
     */
    Page<CircleListItemVO> selectListPage(Page<CircleListItemVO> page, @Param("query") CircleQuery query);

    /**
     * 圈子详情（含圈主信息，不含成员列表与当前用户视角）
     */
    CircleVO selectDetailById(@Param("id") Long id);

    /**
     * 我创建的圈子分页
     */
    Page<CircleListItemVO> selectMyCirclesPage(Page<CircleListItemVO> page, @Param("userId") Long userId);

    /**
     * 我加入的圈子分页
     */
    Page<CircleListItemVO> selectJoinedCirclesPage(Page<CircleListItemVO> page, @Param("userId") Long userId);

    /**
     * 圈子成员列表（分页）
     */
    List<CircleMemberVO> selectMembersByCircle(@Param("circleId") Long circleId, @Param("limit") Integer limit);

    /**
     * 原子更新成员数（递减时使用 GREATEST 防止出现负数）
     */
    @Update("UPDATE portal_circle SET member_count = GREATEST(member_count + #{delta}, 0) WHERE id = #{id}")
    int updateMemberCount(@Param("id") Long id, @Param("delta") int delta);

    /**
     * 原子更新帖子数
     */
    @Update("UPDATE portal_circle SET post_count = GREATEST(post_count + #{delta}, 0) WHERE id = #{id}")
    int updatePostCount(@Param("id") Long id, @Param("delta") int delta);

    /**
     * 后台分页查询（含所有状态，支持按状态/分类/关键词筛选）
     */
    Page<CircleListItemVO> selectCmsListPage(Page<CircleListItemVO> page, @Param("query") CircleQuery query);
}
