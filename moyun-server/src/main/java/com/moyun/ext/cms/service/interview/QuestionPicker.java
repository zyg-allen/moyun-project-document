package com.moyun.ext.cms.service.interview;

/**
 * 智能出题器（v11.x）
 *
 * <p>四路题源按权重分配题额（最大余数法），题源不足自动向后续题源流转，
 * 最终随机兜底补满；题库完全为空时抛出原 ServiceException。</p>
 *
 * <p>权重解析链（内部落定）：前端传参(weightsOverride) > 面试配置 > 岗位模板 > 默认 40/30/20/10。</p>
 *
 * @author moyun
 */
public interface QuestionPicker {

    /**
     * 智能出题
     *
     * @param command 出题指令
     * @return 题单 + 锚定题快照 + 题源分布
     */
    QuestionPickResult pick(QuestionPickCommand command);
}