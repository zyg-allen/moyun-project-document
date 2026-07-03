package com.moyun.portal.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.moyun.core.base.page.PageDomain;
import com.moyun.portal.domain.entity.PortalWritingPrompt;

/**
 * 每日写作 prompt 业务层
 *
 * @author moyun
 */
public interface IPortalWritingPromptService extends IService<PortalWritingPrompt> {

    /**
     * 今日 prompt（按 prompt_date = CURDATE() 查询，不存在则返回最近一条）
     */
    PortalWritingPrompt getToday();

    /**
     * 历史 prompt 分页（按 prompt_date 降序）
     */
    Page<PortalWritingPrompt> listHistory(PageDomain pageDomain, String category);
}
