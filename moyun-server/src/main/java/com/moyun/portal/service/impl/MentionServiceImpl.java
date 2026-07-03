package com.moyun.portal.service.impl;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.moyun.portal.domain.entity.PortalUser;
import com.moyun.portal.mapper.PortalUserMapper;
import com.moyun.portal.service.IMentionService;
import com.moyun.system.domain.entity.SysNotification;
import com.moyun.system.service.ISysNotificationService;

/**
 * @提及解析与通知服务实现
 *
 * 正则提取 @username，查库校验后向被提及用户发送站内通知（type=mention）。
 *
 * @author moyun
 */
@Slf4j
@Service
public class MentionServiceImpl implements IMentionService {

    /** 匹配 @username：@ 后跟随字母/数字/下划线/中文，长度 1-50 */
    private static final Pattern MENTION_PATTERN = Pattern.compile("@([A-Za-z0-9_\\u4e00-\\u9fa5]{1,50})");

    /** 通知内容摘要最大长度 */
    private static final int SNIPPET_MAX = 60;

    /** 门户用户类型标识（与 PortalNotificationController 保持一致） */
    private static final String USER_TYPE_PORTAL = "portal";

    @Autowired
    private PortalUserMapper portalUserMapper;

    @Autowired
    private ISysNotificationService sysNotificationService;

    @Override
    public void parseAndNotify(String content, Long authorId, String entityType, Long entityId) {
        if (content == null || content.isEmpty() || authorId == null) {
            return;
        }

        // 1. 正则提取去重用户名（保持顺序）
        Set<String> usernames = new LinkedHashSet<>();
        Matcher matcher = MENTION_PATTERN.matcher(content);
        while (matcher.find()) {
            usernames.add(matcher.group(1));
        }
        if (usernames.isEmpty()) {
            return;
        }

        // 2. 查询作者信息（用于通知展示名）
        PortalUser author = portalUserMapper.selectPortalUserById(authorId);
        String authorName = author != null
                ? (author.getNickname() != null && !author.getNickname().isEmpty() ? author.getNickname() : author.getUsername())
                : "有人";

        // 3. 内容摘要
        String snippet = content.length() > SNIPPET_MAX ? content.substring(0, SNIPPET_MAX) + "..." : content;
        String typeLabel = "article".equals(entityType) ? "文章" : "comment".equals(entityType) ? "评论" : "内容";

        // 4. 逐个查库校验并发通知
        for (String username : usernames) {
            try {
                PortalUser mentioned = portalUserMapper.selectPortalUserByUsername(username);
                // 用户不存在或为作者本人则跳过
                if (mentioned == null || mentioned.getId() == null
                        || mentioned.getId().equals(authorId)) {
                    continue;
                }
                sendMentionNotification(mentioned, authorId, authorName, entityType, entityId, typeLabel, snippet);
            } catch (Exception e) {
                // 单个用户处理失败不影响整体
                log.warn("解析 @提及 失败：username={}, authorId={}, err={}", username, authorId, e.getMessage());
            }
        }
    }

    /**
     * 构造并发送一条提及通知
     */
    private void sendMentionNotification(PortalUser mentioned, Long authorId, String authorName,
                                         String entityType, Long entityId, String typeLabel, String snippet) {
        SysNotification notification = new SysNotification();
        notification.setType("mention");
        notification.setTitle("你被提及了");
        notification.setContent(authorName + " 在" + typeLabel + "中提到了你：" + snippet);
        // data 为简单 JSON，供前端跳转使用
        notification.setData("{\"entityType\":\"" + entityType + "\",\"entityId\":"
                + (entityId == null ? "null" : entityId) + ",\"authorId\":" + authorId + "}");
        notification.setScope("user");
        notification.setUserId(mentioned.getId());
        notification.setUserType(USER_TYPE_PORTAL);
        notification.setStatus("0");
        notification.setCreateTime(LocalDateTime.now());
        sysNotificationService.save(notification);
    }
}
