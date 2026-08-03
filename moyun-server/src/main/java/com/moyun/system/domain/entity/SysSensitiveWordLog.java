package com.moyun.system.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 敏感词命中记录 sys_sensitive_word_log
 * <p>用于审计与误判复核。每次拦截 / 转待审核 / 标记时写入一条记录。</p>
 *
 * @author moyun
 */
@Data
@TableName("sys_sensitive_word_log")
public class SysSensitiveWordLog implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 业务类型：article / column / topic / topic_post / topic_comment / report
     */
    @Size(max = 32, message = "业务类型长度不能超过32")
    private String bizType;

    /** 业务主键ID */
    private Long bizId;

    /** 提交人ID（portal_user.id） */
    private Long userId;

    /** 被检测的原始内容片段（截断 1000 字以内） */
    private String content;

    /** 命中的敏感词列表（逗号分隔） */
    @Size(max = 500, message = "命中敏感词列表长度不能超过500")
    private String hitWords;

    /** 命中数量 */
    private Integer hitCount;

    /**
     * 处理动作：block=拦截 / pending=转待审核 / flag=仅标记
     */
    @Size(max = 16, message = "处理动作长度不能超过16")
    private String action;

    /** 检测时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
