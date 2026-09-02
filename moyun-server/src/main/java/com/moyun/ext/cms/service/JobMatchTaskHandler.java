package com.moyun.ext.cms.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.moyun.common.exception.system.ServiceException;
import com.moyun.ext.cms.domain.vo.UserResumeVO;
import com.moyun.portal.domain.entity.PortalResumeJobMatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 岗位匹配分析任务 Handler（v10.23，taskType=job_match）
 *
 * <p>异步执行 {@link ResumeJobMatchService#analyze}：LLM 四维匹配分析
 * （关键词/经验/技能/结构）+ 规则兜底，结果存档为匹配报告（含 reportId）。</p>
 *
 * <p>bizRef 参数：{resumeId: 简历ID, jobTargetId: 岗位目标ID}</p>
 *
 * @author moyun
 */
@Service
public class JobMatchTaskHandler implements AiTaskHandler {

    /** 任务类型标识 */
    public static final String TASK_TYPE = "job_match";

    @Autowired
    private ResumeJobMatchService resumeJobMatchService;

    @Autowired
    private IUserResumeService userResumeService;

    @Override
    public String taskType() {
        return TASK_TYPE;
    }

    @Override
    public Object execute(Long userId, JsonNode bizRef) {
        Long resumeId = bizRef.path("resumeId").asLong(0L);
        Long jobTargetId = bizRef.path("jobTargetId").asLong(0L);
        if (resumeId == null || resumeId <= 0 || jobTargetId == null || jobTargetId <= 0) {
            throw new ServiceException("任务参数缺失：resumeId/jobTargetId 必填");
        }
        // 简历归属校验
        UserResumeVO resume = userResumeService.selectResumeDetail(resumeId, userId);
        if (resume == null) {
            throw new ServiceException("简历不存在或无权访问");
        }
        // analyze 内部校验岗位目标归属并落库报告
        PortalResumeJobMatch report = resumeJobMatchService.analyze(userId, resume, jobTargetId);
        return report;
    }
}
