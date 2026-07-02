package com.moyun.ext.cms.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户简历 VO
 *
 * @author moyun
 */
@Data
public class UserResumeVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long userId;
    private String title;
    private Long parentId;
    private Integer versionNo;

    // 基本信息
    private String name;
    private String gender;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate birthDate;

    private String phone;
    private String email;
    private String avatar;

    // 结构化内容（后端解析后的强类型）
    private JobIntention jobIntention;
    private List<EducationItem> educations;
    private List<WorkItem> works;
    private List<ProjectItem> projects;
    private List<SkillItem> skills;
    private String selfIntro;

    // 评分
    private Integer score;
    private List<ScoreItem> scoreDetail;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime scoredTime;

    // 导出
    private String fileUrl;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime exportTime;

    /** 状态：draft/published/archived */
    private String status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    /** 是否为当前用户的简历（权限校验用） */
    private Boolean mine;

    // ==================== 嵌套结构类型 ====================

    /** 求职意向 */
    @Data
    public static class JobIntention implements Serializable {
        private static final long serialVersionUID = 1L;
        /** 期望职位 */
        private String position;
        /** 期望城市 */
        private String city;
        /** 最低薪资（万元/月） */
        private Integer salaryMin;
        /** 最高薪资（万元/月） */
        private Integer salaryMax;
        /** 工作性质：全职/兼职/实习 */
        private String jobType;
        /** 到岗时间 */
        private String availableTime;
    }

    /** 教育经历项 */
    @Data
    public static class EducationItem implements Serializable {
        private static final long serialVersionUID = 1L;
        private String school;
        private String major;
        /** 学历：大专/本科/硕士/博士 */
        private String degree;
        private String startDate;
        private String endDate;
        private String description;
    }

    /** 工作经历项 */
    @Data
    public static class WorkItem implements Serializable {
        private static final long serialVersionUID = 1L;
        private String company;
        private String position;
        private String startDate;
        private String endDate;
        private String description;
    }

    /** 项目经历项 */
    @Data
    public static class ProjectItem implements Serializable {
        private static final long serialVersionUID = 1L;
        private String name;
        private String role;
        private String startDate;
        private String endDate;
        private String description;
        private String url;
    }

    /** 技能项 */
    @Data
    public static class SkillItem implements Serializable {
        private static final long serialVersionUID = 1L;
        private String name;
        /** 等级：了解/一般/熟练/精通 */
        private String level;
        /** 分类 */
        private String category;
    }

    /** 评分项 */
    @Data
    public static class ScoreItem implements Serializable {
        private static final long serialVersionUID = 1L;
        /** 评分项名称 */
        private String item;
        /** 满分 */
        private Integer maxScore;
        /** 实际得分 */
        private Integer score;
        /** 评分说明 */
        private String message;
    }
}
