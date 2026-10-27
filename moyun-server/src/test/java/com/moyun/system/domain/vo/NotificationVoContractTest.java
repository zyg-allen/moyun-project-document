package com.moyun.system.domain.vo;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moyun.system.domain.entity.SysNotification;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 消息中心 VO 契约测试：与 App 消息页字段对齐。
 */
class NotificationVoContractTest {

    @Test
    void notificationJsonKeepsContractFields() throws Exception {
        SysNotification n = new SysNotification();
        n.setId(1L);
        n.setType("system");
        n.setTitle("今天该存「买房」第 1 期");
        n.setContent("本期目标 10.00 元");
        n.setData("{\"bizType\":\"saving\",\"planId\":1,\"recordId\":2}");
        n.setScope("user");
        n.setNoticeType("1");
        n.setStatus("0");
        n.setUserId(9L);
        n.setUserType("portal");

        NotificationVO vo = NotificationVO.from(n);
        String json = new ObjectMapper().writeValueAsString(vo);

        assertTrue(json.contains("\"id\""), "缺少 id");
        assertTrue(json.contains("\"type\""), "缺少 type");
        assertTrue(json.contains("\"title\""), "缺少 title");
        assertTrue(json.contains("\"content\""), "缺少 content");
        assertTrue(json.contains("\"data\""), "缺少 data");
        assertFalse(json.contains("\"userId\""), "不应暴露 userId");
        assertFalse(json.contains("\"userType\""), "不应暴露 userType");
    }
}
