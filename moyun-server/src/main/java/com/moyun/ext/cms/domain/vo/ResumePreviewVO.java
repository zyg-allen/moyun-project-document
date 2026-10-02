package com.moyun.ext.cms.domain.vo;

import com.moyun.ext.cms.domain.vo.UserResumeVO.EducationItem;
import com.moyun.ext.cms.domain.vo.UserResumeVO.JobIntention;
import com.moyun.ext.cms.domain.vo.UserResumeVO.ProjectItem;
import com.moyun.ext.cms.domain.vo.UserResumeVO.SkillItem;
import com.moyun.ext.cms.domain.vo.UserResumeVO.WorkItem;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 简历解析「预览」结果 VO
 *
 * <p>与 {@link ResumeParseVO} 的区别：</p>
 * <ul>
 *   <li>{@code ResumeParseVO} 是<b>解析产物</b>（会被落库为简历记录）；</li>
 *   <li>{@code ResumePreviewVO} 是<b>供用户校对的前端视图</b>：
 *       额外携带 {@code previewToken}（确认落库用）与 {@code rawText}（左右对照用）。</li>
 * </ul>
 *
 * <h3>为什么需要预览 + 确认两步</h3>
 * <p>规则解析准确率不是 100%（业界无人追求全自动 100%），真正的保障是
 * <b>「让错误可见」</b>：把原始文本与解析结果<b>左右对照</b>展示，用户扫一眼即可校对。
 * 因此解析阶段<b>不落库</b>（也避免失败留脏数据），确认后才入库。</p>
 *
 * @author moyun
 */
@Data
public class ResumePreviewVO implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 预览令牌：调用确认接口落库时回传（短期有效，绑定 userId） */
    private String previewToken;

    /** 原始文件名（展示用） */
    private String fileName;

    /**
     * 抽取出的原文（左右对照用）。
     * <p>前端按章节高亮命中片段时使用；同时保证「内容永不丢失」可被人工核对。</p>
     */
    private String rawText;

    /** 原文长度（前端提示用） */
    private Integer textLength;

    /** 是否由 LLM 结构化（false = 纯规则解析，字段覆盖度低但确定、离线可用） */
    private Boolean aiPowered;

    // ==================== 解析结果（与在线简历表单同构） ====================

    private String name;
    private String gender;
    /** 出生日期（yyyy-MM-dd，可能仅精确到月） */
    private String birthDate;
    private String phone;
    private String email;

    private JobIntention jobIntention;
    private List<EducationItem> educations;
    private List<WorkItem> works;
    private List<ProjectItem> projects;
    private List<SkillItem> skills;
    private String selfIntro;

    // ==================== 识别质量提示（供前端标注「待确认」） ====================

    /**
     * 识别到的大类数量（用于提示"是否切分成功"）。
     * <p>为 0 表示章节标题未识别到，内容可能整块落在基本信息区 —— 前端应提示用户手动整理。</p>
     */
    private Integer sectionCount;

    /** 未识别到任何章节标题时为 true（前端给出降级提示） */
    private Boolean sectionDetectFailed;

    /** 抽取到的文本是否为疑似扫描件（无文本层） */
    private Boolean scannedLike;
}
