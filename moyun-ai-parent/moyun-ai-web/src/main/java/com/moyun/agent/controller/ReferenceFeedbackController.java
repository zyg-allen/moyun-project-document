package com.moyun.agent.controller;

import com.moyun.agent.common.ApiResponse;
import com.moyun.agent.entity.ReferenceFeedback;
import com.moyun.agent.service.ReferenceFeedbackService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 参考来源反馈控制器
 *
 * <p>处理用户对AI生成内容参考来源的反馈</p>
 *
 * @author laomao
 * @time 2025/11/23
 */
@Slf4j
@Tag(name = "参考来源反馈")
@RestController
@RequestMapping("/api/feedback")
public class ReferenceFeedbackController {

    @Autowired
    private ReferenceFeedbackService feedbackService;

    /**
     * 提交参考来源反馈
     *
     * <p>用户可以对AI生成内容的参考来源进行反馈，帮助改进系统</p>
     *
     * @param feedback 反馈信息
     * @return 提交结果
     */
    @Operation(summary = "提交参考来源反馈")
    @PostMapping("/reference")
    public ResponseEntity<ApiResponse<Void>> submitFeedback(@RequestBody ReferenceFeedback feedback) {
        try {
            feedbackService.saveFeedback(feedback);
            log.info("保存反馈成功 - 反馈ID: {}", feedback.getId());
            return ResponseEntity.ok(ApiResponse.success("感谢您的反馈！"));
        } catch (Exception e) {
            log.error("保存反馈失败", e);
            return ResponseEntity.ok(ApiResponse.error("提交失败：" + e.getMessage()));
        }
    }
}
