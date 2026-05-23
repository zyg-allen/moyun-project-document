package com.moyun.community.content.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.moyun.community.content.entity.CmsCategory;
import com.moyun.community.content.mapper.CmsCategoryMapper;
import com.moyun.community.content.service.ICmsCategoryService;
import org.springframework.stereotype.Service;

@Service
public class CmsCategoryServiceImpl extends ServiceImpl<CmsCategoryMapper, CmsCategory> implements ICmsCategoryService {
}