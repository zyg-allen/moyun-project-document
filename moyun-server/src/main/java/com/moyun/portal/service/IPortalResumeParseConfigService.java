package com.moyun.portal.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.moyun.portal.domain.entity.PortalResumeParseConfig;

import java.util.List;
import java.util.Map;

/**
 * 简历解析配置服务
 *
 * <p>为规则解析引擎提供四类词表：章节标题词典（section）/ 技能词域（skill）/
 * 学历词（degree）/ 岗位词（position）。</p>
 *
 * <p><b>降级策略</b>：任何一类词表在库中为空或查询失败时，返回<b>空集合</b>，
 * 由规则引擎使用内置默认词典兜底 —— 配置问题绝不导致解析失败。</p>
 *
 * @author moyun
 */
public interface IPortalResumeParseConfigService extends IService<PortalResumeParseConfig> {

    /** 配置类型：章节标题词典 */
    String TYPE_SECTION = "section";
    /** 配置类型：技能词域 */
    String TYPE_SKILL = "skill";
    /** 配置类型：学历词 */
    String TYPE_DEGREE = "degree";
    /** 配置类型：岗位词 */
    String TYPE_POSITION = "position";

    /**
     * 按类型取启用配置（按 sort 升序）
     *
     * @param configType 配置类型（见本接口常量）
     * @return 启用中的配置列表；查询失败返回空列表（不抛异常）
     */
    List<PortalResumeParseConfig> listActiveByType(String configType);

    /**
     * 取「章节标题 → 目标大类」映射
     *
     * @return key=章节标题关键词，value=目标大类（edu/work/project/...）；空表示使用内置默认
     */
    Map<String, String> loadSectionKeywordMap();

    /**
     * 取启用中的词条集合（skill / degree / position 用）
     *
     * @param configType 配置类型
     * @return 关键词集合（已按逗号拆分、去空白）；空表示使用内置默认
     */
    List<String> loadKeywords(String configType);
}
