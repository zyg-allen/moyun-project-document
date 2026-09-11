package com.moyun.ext.cms.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moyun.portal.domain.entity.PortalWritingPrompt;

import java.time.LocalDate;

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

    /**
     * AI 为指定日期生成写作提示（已存在则跳过，返回已有记录）。
     * 结合当日节日/节气/星期上下文；AI 不可用或失败时回退内置主题池（按日期轮换）。
     *
     * @param date 目标日期
     * @return 生成或已存在的提示
     */
    PortalWritingPrompt aiGenerateForDate(LocalDate date);

    /**
     * AI 批量补生成：从起始日开始，为连续 N 天内缺失提示的日期生成（已存在的跳过）。
     *
     * @param startDate 起始日期（含）
     * @param days      天数（1-30）
     * @return 实际新生成的条数
     */
    int aiGenerateRange(LocalDate startDate, int days);

    /**
     * AI 重新生成指定记录（覆盖标题/描述/分类/节日名，保留日期与ID）。
     *
     * @param id 记录ID
     * @return 更新后的记录
     */
    PortalWritingPrompt aiRegenerate(Long id);
}
