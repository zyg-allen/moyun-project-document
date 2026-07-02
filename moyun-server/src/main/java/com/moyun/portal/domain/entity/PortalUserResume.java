package com.moyun.portal.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.moyun.core.base.BaseEntity;
import lombok.Data;

import java.io.Serial;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 用户简历
 *
 * @author moyun
 */
@Data
@TableName("portal_user_resume")
public class PortalUserResume extends BaseEntity
{
    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 简历名称 */
    private String title;

    /** 父简历ID（版本历史关联，首次创建为 null） */
    private Long parentId;

    /** 版本号 */
    private Integer versionNo;

    // ==================== 基本信息 ====================

    /** 姓名 */
    private String name;

    /** 性别：男/女 */
    private String gender;

    /** 出生日期 */
    private LocalDate birthDate;

    /** 联系电话 */
    private String phone;

    /** 邮箱 */
    private String email;

    /** 头像URL */
    private String avatar;

    // ==================== 结构化内容（JSON 字符串） ====================

    /** 求职意向（JSON） */
    private String jobIntention;

    /** 教育经历（JSON 数组） */
    private String educations;

    /** 工作经历（JSON 数组） */
    private String works;

    /** 项目经历（JSON 数组） */
    private String projects;

    /** 技能列表（JSON 数组） */
    private String skills;

    /** 自我介绍 */
    private String selfIntro;

    // ==================== 评分 ====================

    /** 评分（0-100） */
    private Integer score;

    /** 评分明细（JSON 数组） */
    private String scoreDetail;

    /** 评分时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime scoredTime;

    // ==================== 导出 ====================

    /** PDF 导出文件URL */
    private String fileUrl;

    /** 最后导出时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime exportTime;

    // ==================== 状态 ====================

    /** 状态：draft/published/archived */
    private String status;

    public PortalUserResume() {}

    public PortalUserResume(Long id) {
        this.id = id;
    }
}
