package com.moyun.community.content.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.moyun.community.content.entity.CmsBanner;
import com.moyun.community.content.mapper.CmsBannerMapper;
import com.moyun.community.content.service.ICmsBannerService;
import org.springframework.stereotype.Service;

@Service
public class CmsBannerServiceImpl extends ServiceImpl<CmsBannerMapper, CmsBanner> implements ICmsBannerService {
}