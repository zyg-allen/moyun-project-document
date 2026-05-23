package com.moyun.community.content.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.moyun.community.content.entity.CmsBanner;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CmsBannerMapper extends BaseMapper<CmsBanner> {

    List<CmsBanner> selectBannerList();

    int incrementClickCount(Long bannerId);

    int incrementExposureCount(Long bannerId);
}