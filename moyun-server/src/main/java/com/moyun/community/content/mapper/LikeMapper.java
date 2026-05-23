package com.moyun.community.content.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.moyun.community.content.entity.Like;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface LikeMapper extends BaseMapper<Like> {

    @Select("SELECT COUNT(*) > 0 FROM cms_like WHERE user_id = #{userId} AND target_type = #{targetType} AND target_id = #{targetId} AND status = 1")
    boolean existsLike(@Param("userId") Long userId, @Param("targetType") String targetType, @Param("targetId") Long targetId);

    @Select("UPDATE cms_like SET status = 0 WHERE user_id = #{userId} AND target_type = #{targetType} AND target_id = #{targetId}")
    void cancelLike(@Param("userId") Long userId, @Param("targetType") String targetType, @Param("targetId") Long targetId);

    @Select("INSERT INTO cms_like (user_id, target_type, target_id, status) VALUES (#{userId}, #{targetType}, #{targetId}, 1)")
    void addLike(@Param("userId") Long userId, @Param("targetType") String targetType, @Param("targetId") Long targetId);
}
