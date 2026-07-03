package com.moyun.portal.service.impl;

import java.time.LocalDate;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import com.moyun.core.base.page.PageDomain;
import com.moyun.portal.domain.entity.PortalWritingPrompt;
import com.moyun.portal.mapper.PortalWritingPromptMapper;
import com.moyun.portal.service.IPortalWritingPromptService;
import com.moyun.util.bean.PageUtils;

/**
 * 每日写作 prompt 业务层实现
 *
 * @author moyun
 */
@Service
public class PortalWritingPromptServiceImpl extends ServiceImpl<PortalWritingPromptMapper, PortalWritingPrompt> implements IPortalWritingPromptService {

    @Override
    public PortalWritingPrompt getToday() {
        LocalDate today = LocalDate.now();
        LambdaQueryWrapper<PortalWritingPrompt> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PortalWritingPrompt::getPromptDate, today);
        PortalWritingPrompt prompt = baseMapper.selectOne(wrapper);
        if (prompt != null) {
            return prompt;
        }
        // 今日未配置则返回最近一条 prompt（按 prompt_date 降序取第一条）
        LambdaQueryWrapper<PortalWritingPrompt> latestWrapper = new LambdaQueryWrapper<>();
        latestWrapper.orderByDesc(PortalWritingPrompt::getPromptDate).last("LIMIT 1");
        return baseMapper.selectOne(latestWrapper);
    }

    @Override
    public Page<PortalWritingPrompt> listHistory(PageDomain pageDomain, String category) {
        Page<PortalWritingPrompt> page = PageUtils.buildPage(pageDomain);
        LambdaQueryWrapper<PortalWritingPrompt> wrapper = new LambdaQueryWrapper<>();
        if (category != null && !category.isEmpty()) {
            wrapper.eq(PortalWritingPrompt::getCategory, category);
        }
        wrapper.orderByDesc(PortalWritingPrompt::getPromptDate);
        return baseMapper.selectPage(page, wrapper);
    }
}
