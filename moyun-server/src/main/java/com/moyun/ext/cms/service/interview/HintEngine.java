package com.moyun.ext.cms.service.interview;

import com.moyun.ext.cms.domain.vo.HintVO;
import com.moyun.portal.domain.entity.PortalInterviewQuestion;

import java.util.List;

/**
 * 面试提示引擎（规则版 1.0）
 *
 * <p>语音面试官与提词器共享的提示生成引擎。V10.0 为纯规则版，不依赖 LLM：
 * <ul>
 *   <li>{@link #generateKeywords} 从题目的 tags + solution 提取关键词（复用 MockInterview 逻辑）</li>
 *   <li>{@link #generateHint} 按级别生成分层提示</li>
 * </ul>
 *
 * <p>后续 V10.2 可升级为 LLM 增强版，接口不变。
 *
 * @author moyun
 */
public interface HintEngine {

    /**
     * 从题目提取关键词（去重、过滤停用词）
     *
     * @param question 面试题目
     * @return 关键词列表（最多 12 个）
     */
    List<String> generateKeywords(PortalInterviewQuestion question);

    /**
     * 生成分级提示
     *
     * @param question 面试题目
     * @param level    提示级别 1~3
     * @return 提示对象
     */
    HintVO generateHint(PortalInterviewQuestion question, int level);
}
