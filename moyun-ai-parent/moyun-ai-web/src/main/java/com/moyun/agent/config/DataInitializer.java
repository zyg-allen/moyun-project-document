package com.moyun.agent.config;

import com.moyun.agent.entity.SysUser;
import com.moyun.agent.service.SysUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * 数据初始化器
 *
 * <p>应用启动时检查并初始化必要的数据</p>
 *
 * @author laomao
 * @time 2025/11/25
 */
@Slf4j
@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private SysUserService userService;

    /**
     * 应用启动后执行
     *
     * @param args 命令行参数
     */
    @Override
    public void run(String... args) {
        try {
            initAdminUser();
        } catch (Exception e) {
            log.error("初始化管理员账号失败，请确保sys_user表已创建", e);
        }
    }

    /**
     * 初始化管理员账号
     *
     * <p>如果管理员账号不存在，则创建默认管理员账号：
     * <ul>
     *   <li>用户名：laomao</li>
     *   <li>密码：laomao123456</li>
     * </ul>
     * </p>
     */
    private void initAdminUser() {
        String adminUsername = "laomao";

        try {
            // 检查管理员账号是否已存在
            SysUser existingUser = userService.getByUsername(adminUsername);
            if (existingUser != null) {
                log.info("管理员账号已存在: {}", adminUsername);
                return;
            }

            // 创建管理员账号
            SysUser admin = new SysUser();
            admin.setUsername(adminUsername);
            admin.setPassword(userService.encodePassword("laomao123456"));
            admin.setNickname("老猫");
            admin.setStatus(1);

            userService.save(admin);
            log.info("========================================");
            log.info("初始化管理员账号成功!");
            log.info("用户名: {}", adminUsername);
            log.info("密码: laomao123456");
            log.info("========================================");
        } catch (Exception e) {
            log.warn("初始化管理员账号失败: {}，可能表不存在，请先执行SQL创建表", e.getMessage());
        }
    }
}
