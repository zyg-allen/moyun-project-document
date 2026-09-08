package com.moyun.ext.ai2.model.data;

import lombok.Data;

import java.util.List;

/**
 * 场景3：自动出题数据（scene = question_generate）
 *
 * @author laomao
 * @since 2026-09-09
 */
@Data
public class QuestionSceneData {

    /** 生成的题目列表 */
    private List<GeneratedQuestion> questions;

    /** 总数 */
    private Integer totalCount;

    /** 难度 */
    private String difficulty;

    /**
     * 单道题目
     */
    @Data
    public static class GeneratedQuestion {
        /** 题目内容 */
        private String question;
        /** 题型：八股/算法/场景/项目 */
        private String type;
        /** 难度：easy/medium/hard */
        private String difficulty;
        /** 参考答案 */
        private String answer;
        /** 考察点 */
        private List<String> knowledgePoints;
    }
}
