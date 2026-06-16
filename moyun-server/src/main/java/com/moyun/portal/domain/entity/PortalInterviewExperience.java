package com.moyun.portal.domain.entity;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.io.Serial;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import com.moyun.core.base.BaseEntity;

/**
 * 面经
 *
 * @author moyun
 */
@Data
@TableName("portal_interview_experience")
public class PortalInterviewExperience extends BaseEntity
{
    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 面经标题 */
    @NotBlank(message = "面经标题不能为空")
    @Size(min = 0, max = 500, message = "面经标题长度不能超过500个字符")
    private String title;

    /** 公司 */
    @NotBlank(message = "公司不能为空")
    @Size(min = 0, max = 200, message = "公司名称长度不能超过200个字符")
    private String company;

    /** 岗位 */
    private String position;

    /** 年份 */
    private Integer year;

    /** 月份 */
    private Integer month;

    /** 内容摘要 */
    private String summary;

    /** 正文内容 */
    @NotBlank(message = "面经内容不能为空")
    private String content;

    /** 封面图 */
    private String coverImage;

    /** 标签 */
    private String tags;

    /** 是否置顶 */
    private Boolean isTop;

    /** 浏览数 */
    private Long viewCount;

    /** 点赞数 */
    private Long likeCount;

    /** 评论数 */
    private Long commentCount;

    /** 状态：draft/pending/published/rejected/archived */
    @Size(min = 0, max = 20, message = "状态长度不能超过20个字符")
    private String status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    /** 作者昵称（非持久字段，关联填充） */
    @TableField(exist = false)
    private String userNickname;

    /** 作者头像（非持久字段，关联填充） */
    @TableField(exist = false)
    private String userAvatar;

    public PortalInterviewExperience()
    {
    }

    public PortalInterviewExperience(Long id)
    {
        this.id = id;
    }
}
