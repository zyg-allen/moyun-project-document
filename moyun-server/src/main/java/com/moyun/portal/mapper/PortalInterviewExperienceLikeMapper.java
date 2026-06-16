package com.moyun.portal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.moyun.portal.domain.entity.PortalInterviewExperienceLike;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 面经点赞 Mapper
 *
 * @author moyun
 */
@Mapper
public interface PortalInterviewExperienceLikeMapper extends BaseMapper<PortalInterviewExperienceLike>
{
    /**
     * 查询用户是否已点赞某面经
     */
    public PortalInterviewExperienceLike selectLike(@Param("experienceId") Long experienceId, @Param("userId") Long userId);
}
