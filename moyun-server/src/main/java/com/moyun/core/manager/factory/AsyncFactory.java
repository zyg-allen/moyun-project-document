package com.moyun.core.manager.factory;

import com.moyun.system.domain.entity.SysLogininfor;
import com.moyun.system.domain.entity.SysOperLog;
import com.moyun.system.service.ISysLogininforService;
import com.moyun.system.service.ISysOperLogService;
import com.moyun.util.http.ServletUtils;
import com.moyun.util.ip.AddressUtils;
import com.moyun.util.ip.IpUtils;
import com.moyun.util.spring.SpringUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.TimerTask;

/**
 * 异步工厂（产生任务用）
 *
 * @author ruoyi
 */
@Slf4j
public class AsyncFactory {

    /**
     * 记录登录信息（默认后台 sys 用户类型）
     *
     * @param username 用户名
     * @param status   状态
     * @param message  消息
     * @param args     列表
     * @return 任务task
     */
    public static TimerTask recordLogininfor(final String username, final String status, final String message, final Object... args) {
        return recordLogininfor(username, status, message, "sys");
    }

    /**
     * 记录登录信息（支持指定用户类型，用于前后台登录链路区分）
     *
     * @param username 用户名
     * @param status   状态（0成功 1失败）
     * @param message  消息
     * @param userType 登录来源类型（sys=后台用户 portal=门户用户）
     * @return 任务task
     */
    public static TimerTask recordLogininfor(final String username, final String status, final String message, final String userType) {
        // 在主线程中获取请求信息，避免在异步线程中访问 Servlet 上下文
        final HttpServletRequest request = ServletUtils.getRequest();
        final String ip = IpUtils.getIpAddr(request);
        final String userAgentStr = request.getHeader("User-Agent");
        // 主线程解析 UserAgent（eu.bitwalker.UserAgentUtils），避免异步线程重复解析
        String osName = "unknown";
        String browserName = "unknown";
        if (userAgentStr != null && !userAgentStr.isEmpty()) {
            try {
                eu.bitwalker.useragentutils.UserAgent userAgent = eu.bitwalker.useragentutils.UserAgent.parseUserAgentString(userAgentStr);
                if (userAgent != null) {
                    if (userAgent.getOperatingSystem() != null) {
                        osName = userAgent.getOperatingSystem().getName();
                    }
                    if (userAgent.getBrowser() != null && userAgent.getBrowser().getName() != null) {
                        browserName = userAgent.getBrowser().getName();
                    }
                }
            } catch (Exception e) {
                // 解析失败保持 unknown，不影响主流程
            }
        }
        final String os = osName;
        final String browser = browserName;
        return new TimerTask() {
            @Override
            public void run() {
                String address = AddressUtils.getRealAddressByIP(ip);
                // 打印信息到日志
                log.info("[{}][{}]{}-{}-{}-{}", userType, username, ip, address, message, os);
                // 封装对象
                SysLogininfor logininfor = new SysLogininfor();
                logininfor.setUserName(username);
                logininfor.setIpaddr(ip);
                logininfor.setLoginLocation(address);
                logininfor.setBrowser(browser);
                logininfor.setOs(os);
                logininfor.setMsg(message);
                logininfor.setLoginTime(LocalDateTime.now());
                logininfor.setStatus(status);
                logininfor.setUserType(userType);
                // 插入数据
                SpringUtils.getBean(ISysLogininforService.class).insertLogininfor(logininfor);
            }
        };
    }

    /**
     * 操作日志记录
     *
     * @param operLog 操作日志信息
     * @return 任务task
     */
    public static TimerTask recordOper(final SysOperLog operLog) {
        return new TimerTask() {
            @Override
            public void run() {
                // 远程查询操作地点
                operLog.setOperLocation(AddressUtils.getRealAddressByIP(operLog.getOperIp()));
                SpringUtils.getBean(ISysOperLogService.class).insertOperlog(operLog);
            }
        };
    }
}