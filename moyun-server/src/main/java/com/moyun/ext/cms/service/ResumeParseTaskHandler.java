package com.moyun.ext.cms.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.moyun.common.exception.system.ServiceException;
import com.moyun.ext.cms.domain.vo.ResumeParseVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 简历附件解析任务 Handler（taskType=resume_parse）
 *
 * <p>异步执行 {@link ResumeParseService#executeParse}：对<b>上传阶段已就地抽取的文本</b>
 * 做 LLM 结构化解析，并在<b>解析成功后</b>创建简历记录。</p>
 *
 * <p><b>不读取任何文件</b>（v13.38 架构修正）：附件是客户端一次性输入，只取其内容，
 * 不落盘、不进对象存储；抽取文本经 {@code portal_ai_task.payload} 传入本任务。
 * 解析失败不产生任何记录，杜绝空简历脏数据。</p>
 *
 * <p>bizRef 参数：{fileName: 原始文件名（仅展示）}；payload：抽取的简历文本</p>
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
    public Object execute(Long userId, JsonNode bizRef, String payload) {
        String fileName = bizRef == null ? null : bizRef.path("fileName").asText(null);
        if (payload == null || payload.isBlank()) {
            throw new ServiceException("任务参数缺失：简历文本为空（上传阶段未成功抽取）");
        }
        ResumeParseVO vo = resumeParseService.executeParse(userId, payload, fileName);
        log.info("[ResumeParseTask] 解析完成 resumeId={} aiPowered={} textLength={}",
                vo.getAttachmentResumeId(), vo.getAiPowered(), vo.getTextLength());
        return vo;
    }
}
