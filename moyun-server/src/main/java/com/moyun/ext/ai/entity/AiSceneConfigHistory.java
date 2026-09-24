package com.moyun.ext.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * AI 场景配置版本快照实体类
 *
 * <p>对应数据库表 ai_scene_config_history。管理端保存场景配置时自动快照，
 * 支持一键回滚历史版本（配置版本化为 prompt 迁移期的回滚安全网）。</p>
 *
 * <p><b>不继承 AiBaseEntity 的原因</b>：本表仅有 create_time 列，无 update_time/deleted，
 * 继承会导致 MyBatis-Plus 映射不存在的列。</p>
 *
 * @author moyun
 * @since 2026-09-24
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("ai_scene_config_history")
public class AiSceneConfigHistory {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 源配置主键（逻辑关联 ai_scene_config.id，无物理外键） */
    private Long configId;

    /** 场景代码（冗余快照，支持 scene:task 全码） */
    private String sceneCode;

    /** 快照版本号（快照时源行的 config_version） */
    private Integer configVersion;

    /** 配置行完整快照(JSON) */
    private String snapshot;

    /** 操作人（保存/回滚触发者） */
    private String operator;

    /** 快照时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
