package com.moyun.community.content.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.moyun.community.content.entity.CmsSearchHistory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CmsSearchHistoryMapper extends BaseMapper<CmsSearchHistory> {

    List<CmsSearchHistory> selectSearchHistoryList(@Param("userId") Long userId);

    int deleteUserSearchHistory(@Param("userId") Long userId);
}