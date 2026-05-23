package com.moyun.framework.manager.factory;

import com.moyun.common.core.domain.entity.SysUser;
import com.moyun.common.utils.ServletUtils;
import com.moyun.common.utils.ip.AddressUtils;
import com.moyun.common.utils.ip.IpUtils;
import com.moyun.system.domain.SysLogininfor;
import com.moyun.system.domain.SysOperLog;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.TimerTask;

/**
 * 异步工厂（产生任务用）
 *
 * @author ruoyi
 */
public class AsyncFactory {

    private static final Logger sys_user_logger = LoggerFactory.getLogger("sys-user");

    /**
     * 记录登录信息
     *
     * @param username 用户名
     * @param status   状态
     * @param message  消息
     * @param args     列表
     * @return 任务task
     */
    public static TimerTask recordLogininfor(final String username, final String status, final String message, final Object... args) {
        // 在主线程中获取请求信息，避免在异步线程中访问 Servlet 上下文
        final HttpServletRequest request = ServletUtils.getRequest();
        final String ip = IpUtils.getIpAddr(request);
        final String userAgent = request.getHeader("User-Agent");
        return new TimerTask() {
            @Override
            public void run() {
                String address = AddressUtils.getRealAddressByIP(ip);
                // 使用预先保存的 userAgent，不在异步线程中调用 ServletUtils.getRequest()
                String os = userAgent != null ? userAgent : "unknown";
                // 打印信息到日志
                sys_user_logger.info("[{}]{}-{}-{}-{}", username, ip, address, message, os);
                // 封装对象
                SysLogininfor logininfor = new SysLogininfor();
                logininfor.setUserName(username);
                logininfor.setIpaddr(ip);
                logininfor.setLoginLocation(address);
                logininfor.setMsg(message);
                logininfor.setLoginTime(LocalDateTime.now());
                logininfor.setStatus(status);
                // 插入数据
                // SpringUtils.getBean(ISysLogininforService.class).insertLogininfor(logininfor);
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
                // SpringUtils.getBean(ISysOperLogService.class).insertOperlog(operLog);
            }
        };
    }
}