package com.moyun.portal.service;

import com.moyun.core.base.AjaxResult;

/**
 * 门户邮件服务
 * <p>
 * 提供：
 * <ul>
 *     <li>发送邮箱验证码（注册 / 找回密码两种场景）</li>
 *     <li>校验邮箱验证码（注册时由 controller 调用、找回密码时由 controller 调用）</li>
 *     <li>重置密码（找回密码最终一步：邮箱+验证码+新密码）</li>
 * </ul>
 * 验证码存 Redis，5 分钟过期，同邮箱 60 秒内禁止重复发送（防刷）。
 * <p>
 * 安全设计：验证码一次性使用，校验后立即删除（无论成功失败），避免被暴力枚举。
 *
 * @author moyun
 */
public interface PortalEmailService {

    /**
     * 发送邮箱验证码
     *
     * @param email 目标邮箱
     * @param type  场景类型：register / reset_password
     * @return 发送结果
     */
    AjaxResult sendCode(String email, String type);

    /**
     * 校验邮箱验证码（不删除验证码，由具体业务在通过后自行决定是否调用 consumeCode）
     * <p>
     * 注册流程：注册接口校验通过后，由 PortalLoginController 调用 consumeCode 删除。
     * 找回密码流程：resetPassword 内部调用 consumeCode 一次性消费。
     *
     * @param email 目标邮箱
     * @param code 用户输入的验证码
     * @param type 场景类型：register / reset_password
     * @return true=校验通过；false=验证码错误或已过期
     */
    boolean verifyCode(String email, String code, String type);

    /**
     * 消费（删除）验证码，使其失效。用于验证通过后的一次性消费。
     *
     * @param email 目标邮箱
     * @param type  场景类型
     */
    void consumeCode(String email, String type);

    /**
     * 找回密码：校验验证码 + 重置密码
     *
     * @param email       注册邮箱
     * @param code        邮箱验证码
     * @param newPassword 新密码（明文，方法内做 BCrypt 加密）
     * @return 重置结果
     */
    AjaxResult resetPassword(String email, String code, String newPassword);
}
