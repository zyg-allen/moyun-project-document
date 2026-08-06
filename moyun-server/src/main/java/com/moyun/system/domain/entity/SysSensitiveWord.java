package com.moyun.system.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.moyun.core.base.BaseEntity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 敏感词库 sys_sensitive_word
 * <p>管理员维护，运行时由 SensitiveWordFilter 加载到内存（DFA 词树）。
 * 词库变更后调用 reload 触发刷新。</p>
 *
 * @author moyun
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_sensitive_word")
public class SysSensitiveWord extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 敏感词 */
    @NotBlank(message = "敏感词不能为空")
    @Size(max = 128, message = "敏感词长度不能超过128")
    private String word;

    /**
     * 分类：politics=政治 / porn=色情 / ad=广告 / insult=辱骂 / other=其他
     */
    @Size(max = 32, message = "分类长度不能超过32")
    private String category;

    /**
     * 状态：0=启用 1=禁用
     * （沿用 RuoYi sys_notice_status 字典语义，0 正常 / 1 停用）
     */
    private String status;
}
