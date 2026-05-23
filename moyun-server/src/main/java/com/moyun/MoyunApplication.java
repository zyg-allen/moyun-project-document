package com.moyun;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 墨韵·智库 应用启动类
 * 
 * @author moyun
 */
@SpringBootApplication(exclude = {
    // 排除 MongoDB 自动配置（避免与 Flowable 冲突）
    org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration.class,
    org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration.class,
    // 临时排除 Flowable 自动配置（调试用，问题解决后请注释掉）
    // org.flowable.spring.boot.FlowableAutoConfiguration.class  // 已修复，重新启用
})
@EnableScheduling
public class MoyunApplication {

    public static void main(String[] args) {
        SpringApplication.run(MoyunApplication.class, args);
        System.out.println("========================================");
        System.out.println("    墨韵·智库 启动成功！");
        System.out.println("    Flowable 工作流引擎已就绪");
        System.out.println("========================================");
    }

}