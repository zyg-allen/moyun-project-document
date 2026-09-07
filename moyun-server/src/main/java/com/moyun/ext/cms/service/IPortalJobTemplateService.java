package com.moyun.ext.cms.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.moyun.portal.domain.entity.PortalJobTemplate;

import java.util.List;

/**
 * 岗位模板服务接口（v11.x 智能出题）
 *
 * @author moyun
 */
public interface IPortalJobTemplateService extends IService<PortalJobTemplate> {

    /**
     * 获取启用中的岗位模板（portal 前台下拉用）
     */
    List<PortalJobTemplate> listActive();

    /**
     * JD 关键词提取：优先 LLM 结构化提取，失败回退规则分词（停用词过滤 + 技术词表匹配）
     *
     * @param jdText 岗位 JD 原文
     * @return 关键词列表（上限 15）
     */
    List<String> extractKeywords(String jdText);

    /**
     * 关联题目（全量覆盖题目归属）：先清空模板下所有题目的 job_template_id，再绑定指定题目
     *
     * @param templateId  岗位模板ID
     * @param questionIds 题目ID列表（空列表 = 解绑全部）
     * @return 绑定题目数
     */
    int bindQuestions(Long templateId, List<Long> questionIds);
}