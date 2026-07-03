package com.moyun.portal.service;

/**
 * @提及解析与通知服务
 *
 * 解析评论/文章内容中的 @username 模式，提取被提及用户并发送站内通知。
 * 采用正则匹配（@username），不引入复杂 NLP。
 *
 * @author moyun
 */
public interface IMentionService {

    /**
     * 解析内容中的 @username 并向被提及用户发送通知。
     *
     * <p>规则：
     * <ul>
     *   <li>正则提取 @ 后跟随的用户名（字母/数字/下划线/中文）</li>
     *   <li>查库校验用户存在性，不存在的用户名静默跳过</li>
     *   <li>不通知作者本人</li>
     *   <li>同一条内容中重复 @ 同一用户只通知一次</li>
     * </ul>
     *
     * @param content     原始内容（评论正文 / 文章正文）
     * @param authorId    内容作者ID（不通知作者本人）
     * @param entityType  实体类型（comment / article），用于通知跳转
     * @param entityId     实体ID
     */
    void parseAndNotify(String content, Long authorId, String entityType, Long entityId);
}
