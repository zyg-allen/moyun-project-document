package com.moyun.ext.cms.service.interview;

import com.moyun.ext.cms.domain.vo.UserProfileSnapshotVO;
import lombok.Data;

/**
 * 智能出题指令（v11.x）
 *
 * <p>由 start() 构建：场景/岗位模板/简历/画像等输入聚合，
 * 权重覆盖（前端传参）优先于配置中心解析链。</p>
 *
 * @author moyun
 */
@Data
public class QuestionPickCommand {

    private Long userId;

    /** 面试岗位 */
    private String position;

    /** 面试场景 */
    private String scene;

    /** 难度 easy/medium/hard */
    private String difficulty;

    /** 简历上下文（resumeId 非空时由调用方构建） */
    private ResumeContext resumeContext;

    /** 用户画像快照（可选） */
    private UserProfileSnapshotVO snapshot;

    /** 是否使用画像出题 */
    private boolean useProfile;

    /** 主问题目总数（默认 5） */
    private int count = 5;

    /** 岗位模板ID（可选，job 题源） */
    private Long jobTemplateId;

    /** 权重覆盖（前端传参，优先于面试配置/岗位模板/默认值） */
    private QuestionWeights weightsOverride;
}