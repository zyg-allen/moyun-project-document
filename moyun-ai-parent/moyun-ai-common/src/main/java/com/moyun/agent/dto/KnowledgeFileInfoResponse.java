package com.moyun.agent.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 知识库文件信息响应DTO
 *
 * @author laomao
 * @time 2025/11/23
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeFileInfoResponse {

    /**
     * 文件名
     */
    private String fileName;

    /**
     * 文件类型
     */
    private String fileType;

    /**
     * 文件路径（原始文件）
     */
    private String filePath;

    /**
     * PDF文件路径（用于预览）
     */
    private String pdfFilePath;

    /**
     * 文件大小
     */
    private Long fileSize;

    /**
     * 是否存在
     */
    private Boolean exists;

    /**
     * 访问URL（如果适用）
     */
    private String accessUrl;
}
