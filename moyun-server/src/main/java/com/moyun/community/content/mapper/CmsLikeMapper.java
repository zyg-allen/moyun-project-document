package com.moyun.community.content.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.moyun.community.content.entity.CmsLike;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface CmsLikeMapper extends BaseMapper<CmsLike> {

    CmsLike selectByUserAndTarget(@Param("userId") Long userId,
                                  @Param("targetType") String targetType,
                                  @Param("targetId") Long targetId);

    int updateStatus(@Param("id") Long id, @Param("status") Integer status);
}