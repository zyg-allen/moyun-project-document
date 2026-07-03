package com.moyun.ext.cms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moyun.ext.cms.service.ICmsWritingPromptService;
import com.moyun.portal.domain.entity.PortalWritingPrompt;
import com.moyun.portal.mapper.PortalWritingPromptMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;

/**
 * CMS 每日写作 prompt 管理 Service 实现
 *
 * @author moyun
 */
@Service
public class CmsWritingPromptServiceImpl implements ICmsWritingPromptService {

    @Autowired
    private PortalWritingPromptMapper promptMapper;

    @Override
    public Page<PortalWritingPrompt> selectPromptPage(Page<PortalWritingPrompt> page, PortalWritingPrompt prompt) {
        LambdaQueryWrapper<PortalWritingPrompt> wrapper = new LambdaQueryWrapper<>();
        if (prompt != null) {
            if (prompt.getTitle() != null && !prompt.getTitle().isEmpty()) {
                wrapper.like(PortalWritingPrompt::getTitle, prompt.getTitle());
            }
            if (prompt.getCategory() != null && !prompt.getCategory().isEmpty()) {
                wrapper.eq(PortalWritingPrompt::getCategory, prompt.getCategory());
            }
        }
        wrapper.orderByDesc(PortalWritingPrompt::getPromptDate);
        return promptMapper.selectPage(page, wrapper);
    }

    @Override
    public PortalWritingPrompt selectPromptById(Long id) {
        return promptMapper.selectById(id);
    }

    @Override
    public int insertPrompt(PortalWritingPrompt prompt) {
        return promptMapper.insert(prompt);
    }

    @Override
    public int updatePrompt(PortalWritingPrompt prompt) {
        return promptMapper.updateById(prompt);
    }

    @Override
    public int deletePromptByIds(Long[] ids) {
        return promptMapper.deleteBatchIds(Arrays.asList(ids));
    }
}
