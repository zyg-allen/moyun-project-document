package com.moyun.community.content.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.moyun.community.content.entity.CmsTag;
import com.moyun.community.content.mapper.CmsTagMapper;
import com.moyun.community.content.service.ICmsTagService;
import org.springframework.stereotype.Service;

@Service
public class CmsTagServiceImpl extends ServiceImpl<CmsTagMapper, CmsTag> implements ICmsTagService {
}