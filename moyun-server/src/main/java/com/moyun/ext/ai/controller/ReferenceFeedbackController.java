package com.moyun.ext.ai.controller;

import com.moyun.core.base.AjaxResult;
import com.moyun.ext.ai.entity.ReferenceFeedback;
import com.moyun.ext.ai.service.ReferenceFeedbackService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Tag(name = "参考来源反馈")
@RestController
@RequestMapping("/cms/ai/reference-feedback")
public class ReferenceFeedbackController {

    @Autowired
    private ReferenceFeedbackService feedbackService;

    @Operation(summary = "提交参考来源反馈")
    @PostMapping("/reference")
    @PreAuthorize("@ss.hasPermi('cms:ai:reference-feedback:add')")
    public AjaxResult submitFeedback(@RequestBody ReferenceFeedback feedback) {
        try {
            feedbackService.saveFeedback(feedback);
            log.info("保存反馈成功 - 反馈ID: {}", feedback.getId());
            return AjaxResult.success("感谢您的反馈！");
        } catch (Exception e) {
            log.error("保存反馈失败", e);
            return AjaxResult.error("提交失败：" + e.getMessage());
        }
    }
}
