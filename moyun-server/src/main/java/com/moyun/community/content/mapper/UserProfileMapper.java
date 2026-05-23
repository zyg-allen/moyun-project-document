package com.moyun.community.content.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.moyun.community.content.entity.UserProfile;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserProfileMapper extends BaseMapper<UserProfile> {

    UserProfile selectByUserId(@Param("userId") Long userId);

    int incrementArticleCount(@Param("userId") Long userId);

    int decrementArticleCount(@Param("userId") Long userId);

    int incrementPoints(@Param("userId") Long userId, @Param("points") Integer points);

    int decrementPoints(@Param("userId") Long userId, @Param("points") Integer points);
}
