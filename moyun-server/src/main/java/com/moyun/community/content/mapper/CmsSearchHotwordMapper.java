package com.moyun.community.content.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.moyun.community.content.entity.CmsSearchHotword;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CmsSearchHotwordMapper extends BaseMapper<CmsSearchHotword> {

    List<CmsSearchHotword> selectHotwordList();

    int incrementSearchCount(@Param("hotwordId") Long hotwordId);
}