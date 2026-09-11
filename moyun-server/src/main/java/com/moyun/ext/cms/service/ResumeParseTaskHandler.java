package com.moyun.ext.cms.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.moyun.common.exception.system.ServiceException;
import com.moyun.ext.cms.domain.vo.ResumeParseVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 简历附件解析任务 Handler（v10.23，taskType=resume_parse）
 *
 * <p>异步执行 {@link ResumeParseService#executeParse}：读取已保存的附件源文件、
 * 抽取文本并 LLM 结构化解析，回填附件简历记录。上传接口同步路径只做
 * 保存文件 + 创建记录（{@code prepareAttachmentResume}），本任务承接耗时的 LLM 部分。</p>
 *
 * <p>bizRef 参数：{resumeId: 附件简历记录ID, fileUrl: 源文件URL, fileName: 原始文件名}</p>
 *
 * @author moyun
 */
@Service
public class ResumeParseTaskHandler implements AiTaskHandler {

    private static final Logger log = LoggerFactory.getLogger(ResumeParseTaskHandler.class);

    /** 任务类型标识 */
    public static final String TASK_TYPE = "resume_parse";

    @Autowired
    private ResumeParseService resumeParseService;

    @Override
    public String taskType() {
        return TASK_TYPE;
    }

    @Override
    public Object execute(Long userId, JsonNode bizRef) {
        Long resumeId = bizRef.path("resumeId").asLong(0L);
        String fileUrl = bizRef.path("fileUrl").asText(null);
        String fileName = bizRef.path("fileName").asText(null);
        if (resumeId == null || resumeId <= 0 || fileUrl == null || fileUrl.isBlank()) {
            throw new ServiceException("任务参数缺失：resumeId/fileUrl 必填");
        }
        ResumeParseVO vo = resumeParseService.executeParse(userId, resumeId, fileUrl, fileName);
        log.info("[ResumeParseTask] 解析完成 resumeId={} aiPowered={} textLength={}",
                resumeId, vo.getAiPowered(), vo.getTextLength());
        return vo;
    }
}
