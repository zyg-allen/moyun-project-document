package com.moyun.ext.cms.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 简历附件解析结果 VO（v10.12）
 * <p>
 * 由 {@link com.moyun.ext.cms.service.ResumeParseService} 从上传的附件
 * （PDF/Word/TXT）中抽取文本并结构化后返回，字段语义与 {@link UserResumeVO}
 * 一一对应（birthDate 例外：这里用 String 承载，避免不完整日期反序列化失败）。
 * 前端确认后按字段映射覆盖填充到在线简历表单。
 *
 * @author moyun
 */
@Data
public class ResumeParseVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String name;
    private String gender;
    /** 出生日期（yyyy-MM-dd；原文只有年月时补 01，前端 input date 需要） */
    private String birthDate;
    private String phone;
    private String email;
    private String title;

    private UserResumeVO.JobIntention jobIntention;
    private List<UserResumeVO.EducationItem> educations;
    private List<UserResumeVO.WorkItem> works;
    private List<UserResumeVO.ProjectItem> projects;
    private List<UserResumeVO.SkillItem> skills;
    private String selfIntro;

    /** 是否由 LLM 结构化解析（false = 正则规则粗解析，字段覆盖度低） */
    private Boolean aiPowered;
    /** 附件中抽取到的原始文本长度（前端提示用） */
    private Integer textLength;
}
