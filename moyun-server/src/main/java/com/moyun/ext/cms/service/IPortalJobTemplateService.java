package com.moyun.ext.cms.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.moyun.portal.domain.entity.PortalJobTemplate;

import java.util.List;

/**
 * 岗位模板服务接口
 *
 * <p><b>全 portal 岗位配置的唯一来源</b>（v13.37 起）：岗位下拉、JD 回填、难度/题量建议、
 * 简历岗位匹配评分、用户画像必备技能统一读 {@code portal_job_template}。
 * 原 {@code portal_interview_position} 已删除，其 {@code findByName / findByCode} 职责并入本接口。</p>
 *
 * @author moyun
 */
public interface IPortalJobTemplateService extends IService<PortalJobTemplate> {

    /**
     * 获取启用中的岗位模板（portal 前台岗位下拉用；按 category、name 升序）
     */
    List<PortalJobTemplate> listActive();

    /**
     * 按岗位名称反查启用的岗位模板。
     * <p>用于「岗位名称 → 岗位配置」的归一：先精确匹配 {@code name}，未命中再 {@code name LIKE} 模糊兜底
     * （如 "后端" → "Java后端工程师"），以尽量召回 {@code required_skills} 供简历匹配评分与画像抽题。</p>
     *
     * @param name 岗位名称（为空返回 null）
     * @return 岗位模板；不存在时返回 null
     */
    PortalJobTemplate findActiveByName(String name);

    /**
     * 按岗位编码反查启用的岗位模板（如 "java_backend"）。
     *
     * @param code 岗位编码（为空返回 null）
     * @return 岗位模板；不存在时返回 null
     */
    PortalJobTemplate findActiveByCode(String code);

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