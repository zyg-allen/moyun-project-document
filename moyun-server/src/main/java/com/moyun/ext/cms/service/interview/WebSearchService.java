package com.moyun.ext.cms.service.interview;

/**
 * 面试联网核查服务（Phase 4 预留接口）
 *
 * <p>用途：对候选人声称的事实性内容（技术版本、性能数据、社区最佳实践等）进行联网核查，
 * 核查结果注入 {@code InterviewPromptAssembler.TaskContext.webSearchContext}，
 * 供面试官追问与报告阶段引用。</p>
 *
 * <p>开关：sys_config {@code voice.interview.webSearchEnabled}（默认关闭）。
 * 当前提供 {@code NoopWebSearchServiceImpl} 空实现，后续可选实现：</p>
 * <ul>
 *   <li>dashscope-sdk-java：OpenAiChatModel#builder().enableSearch(true)（百炼内置联网搜索）</li>
 *   <li>Bocha AI Web Search API（博查，国内可用）</li>
 *   <li>Tavily Search API（海外）</li>
 * </ul>
 *
 * @author moyun
 */
public interface WebSearchService {

    /**
     * 联网搜索
     *
     * @param query 检索关键词（候选人声称的事实）
     * @return 检索结果摘要文本（可直接注入提示词）；不可用/失败返回 null（调用方跳过注入）
     */
    String search(String query);
}
