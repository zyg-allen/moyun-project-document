package com.moyun.ext.ai2.model.data;

import lombok.Data;

import java.util.List;

/**
 * 知识问答场景数据（scene = knowledge_qa，v11.65）
 *
 * <p>统一网关的知识库检索问答输出契约：AI 回答 + 引用来源列表（溯源）。</p>
 *
 * @author laomao
 * @since 2026-09-11
 */
@Data
public class KnowledgeQaSceneData {

    /** AI 回答（基于检索内容生成；检索为空时为标准范围外话术） */
    private String answer;

    /** 引用来源列表（与回答依据的检索片段一一对应，按相关性排序） */
    private List<Reference> references;

    /** 检索命中片段数（重排合并后） */
    private Integer retrievalCount;

    /** 检索片段中的图片数 */
    private Integer imageCount;

    /**
     * 引用来源（溯源信息，字段与检索片段 metadata 对齐）
     */
    @Data
    public static class Reference {

        /** 来源文件名 */
        private String fileName;

        /** 文件类型（pdf/docx/md 等） */
        private String fileType;

        /** 页码（图片/PDF 类文档） */
        private String pageNumber;

        /** 分片索引 */
        private String segmentIndex;

        /** 知识库文档记录 ID（knowledge 表主键） */
        private String knowledgeBaseId;

        /** 图片路径（图片片段专有） */
        private String imagePath;

        /** 重排相似度分数（0-1，未启用重排时为空） */
        private Double rerankScore;

        /** 片段摘录（截断，供前端预览/弹窗） */
        private String excerpt;
    }
}
