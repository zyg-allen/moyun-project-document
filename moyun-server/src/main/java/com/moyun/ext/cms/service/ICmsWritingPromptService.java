package com.moyun.ext.cms.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moyun.portal.domain.entity.PortalWritingPrompt;

/**
 * CMS 每日写作 prompt 管理 Service 接口
 *
 * @author moyun
 */
public interface ICmsWritingPromptService {

    Page<PortalWritingPrompt> selectPromptPage(Page<PortalWritingPrompt> page, PortalWritingPrompt prompt);

    PortalWritingPrompt selectPromptById(Long id);

    int insertPrompt(PortalWritingPrompt prompt);

    int updatePrompt(PortalWritingPrompt prompt);

    int deletePromptByIds(Long[] ids);
}
