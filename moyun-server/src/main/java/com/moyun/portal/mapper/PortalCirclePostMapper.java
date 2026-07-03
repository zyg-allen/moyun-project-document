package com.moyun.portal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moyun.ext.cms.domain.vo.CirclePostVO;
import com.moyun.portal.domain.entity.PortalCirclePost;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 圈子帖子 Mapper
 *
 * @author moyun
 */
@Mapper
public interface PortalCirclePostMapper extends BaseMapper<PortalCirclePost> {

    /**
     * 圈子帖子分页（公开，仅 active，含作者信息）
     */
    Page<CirclePostVO> selectPostsByCircle(Page<CirclePostVO> page, @Param("circleId") Long circleId);

    /**
     * 帖子详情（含作者信息）
     */
    CirclePostVO selectPostDetail(@Param("id") Long id);

    /**
     * 后台帖子分页查询（含所有状态）
     */
    Page<CirclePostVO> selectCmsPostsPage(Page<CirclePostVO> page, @Param("circleId") Long circleId,
                                          @Param("keyword") String keyword, @Param("status") String status);

    /**
     * 原子更新浏览数
     */
    @Update("UPDATE portal_circle_post SET view_count = view_count + #{delta} WHERE id = #{id}")
    int updateViewCount(@Param("id") Long id, @Param("delta") int delta);

    /**
     * 原子更新点赞数
     */
    @Update("UPDATE portal_circle_post SET like_count = GREATEST(like_count + #{delta}, 0) WHERE id = #{id}")
    int updateLikeCount(@Param("id") Long id, @Param("delta") int delta);
}
