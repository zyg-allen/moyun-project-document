package com.moyun.core.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * MyBatis-Plus 自动填充处理器
 * 用于自动填充创建时间和更新时间
 *
 * @author ruoyi
 */
@Component
public class MyMetaObjectHandler implements MetaObjectHandler {

    /**
     * 插入时自动填充
     * 同时兼容 ruoyi 标准字段（createTime/updateTime）和 AI 模块字段（createdAt/updatedAt）
     */
    @Override
    public void insertFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now();
        // ruoyi 标准字段
        this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, now);
        this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, now);
        // AI 模块字段
        this.strictInsertFill(metaObject, "createdAt", LocalDateTime.class, now);
        this.strictInsertFill(metaObject, "updatedAt", LocalDateTime.class, now);
    }

    /**
     * 更新时自动填充
     * 同时兼容 ruoyi 标准字段（updateTime）和 AI 模块字段（updatedAt）
     */
    @Override
    public void updateFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now();
        // ruoyi 标准字段
        this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, now);
        // AI 模块字段
        this.strictUpdateFill(metaObject, "updatedAt", LocalDateTime.class, now);
    }
}
