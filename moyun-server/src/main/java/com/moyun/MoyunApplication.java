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
    // 排除 MongoDB 自动配置（项目未使用 MongoDB）
    org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration.class,
    org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration.class
})
@EnableScheduling
public class MoyunApplication {

    public static void main(String[] args) {
        SpringApplication.run(MoyunApplication.class, args);
        System.out.println("========================================");
        System.out.println("    墨韵·智库 启动成功！");
        System.out.println("========================================");
    }

}