package com.moyun.community.content.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.moyun.community.content.entity.Banner;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface BannerMapper extends BaseMapper<Banner> {

    @Select("SELECT * FROM cms_banner WHERE status = '0' AND position = #{position} " +
            "AND (start_time IS NULL OR start_time <= #{now}) " +
            "AND (end_time IS NULL OR end_time >= #{now}) " +
            "ORDER BY sort ASC")
    List<Banner> selectActiveBanners(@org.apache.ibatis.annotations.Param("position") String position, @org.apache.ibatis.annotations.Param("now") LocalDateTime now);

    @Select("SELECT * FROM cms_banner WHERE position = #{position} ORDER BY sort ASC")
    List<Banner> selectByPosition(String position);
}
