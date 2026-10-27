package com.moyun.portal.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.moyun.core.base.AjaxResult;
import com.moyun.portal.domain.entity.PortalUser;
import com.moyun.portal.mapper.PortalUserMapper;
import com.moyun.portal.service.WechatMiniProgramService;
import com.moyun.portal.util.PortalSecurityUtils;

/**
 * 小程序微信能力：订阅消息配置 + openid 绑定。
 *
 * <p>前端在小程序端保存"开启提醒"的待办时调用：
 * uni.login 拿 code → POST /portal/wechat/bind → 再 requestSubscribeMessage。</p>
 */
@RestController
@RequestMapping("/portal/wechat")
public class PortalWechatController {

    @Autowired
    private WechatMiniProgramService wechatService;

    @Autowired
    private PortalUserMapper portalUserMapper;

    /** 前端用于判断是否展示"微信提醒"能力（未配置时 enabled=false） */
    @GetMapping("/config")
    public AjaxResult config() {
        Map<String, Object> data = new HashMap<>();
        data.put("enabled", wechatService.isReady());
        data.put("memoTemplateId", wechatService.isReady() ? wechatService.getTemplateId() : "");
        return AjaxResult.success(data);
    }

    /** 绑定当前登录用户的微信 openid（幂等：同一 openid 只保留一个用户） */
    @com.moyun.common.annotation.RepeatSubmit(interval = 2000)
    @com.moyun.common.annotation.RateLimiter(key = "portalwechat:", time = 60, count = 120)
    @PostMapping("/bind")
    public AjaxResult bind(@jakarta.validation.Valid @RequestBody com.moyun.portal.domain.dto.WechatBindDTO body) {
        Long userId = PortalSecurityUtils.getUserId();
        if (userId == null) {
            return AjaxResult.error("未登录");
        }
        String code = body == null ? null : body.getCode();
        String openid = wechatService.code2Openid(code);
        if (openid == null) {
            return AjaxResult.error("微信绑定失败，请在小程序内重试");
        }
        // 先解绑同 openid 的其他用户，避免唯一键冲突
        portalUserMapper.update(null, new LambdaUpdateWrapper<PortalUser>()
                .eq(PortalUser::getWxOpenid, openid)
                .ne(PortalUser::getId, userId)
                .set(PortalUser::getWxOpenid, null));
        PortalUser update = new PortalUser();
        update.setId(userId);
        update.setWxOpenid(openid);
        portalUserMapper.updateById(update);
        Map<String, Object> data = new HashMap<>();
        data.put("bound", true);
        return AjaxResult.success(data);
    }
}
