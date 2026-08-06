package com.moyun.ext.ai.service;

/**
 * 文档类型检测器接口
 *
 * <p>根据文档内容和元数据自动识别文档类型，用于选择合适的分片策略</p>
 *
 * @author laomao
 * @since 2025-01-22
 */
public interface DocumentTypeDetector {

    /**
     * 检测文档类型
     *
     * @param fileName 文件名
     * @param content  文档内容（前1000字符）
     * @return 文档类型：general, faq, table, code, technical
     */
    String detectDocumentType(String fileName, String content);

    /**
     * 是否为FAQ文档
     *
     * @param content 文档内容
     * @return true if FAQ
     */
    boolean isFaqDocument(String content);

    /**
     * 是否为表格密集型文档
     *
     * @param content 文档内容
     * @return true if table-heavy
     */
    boolean isTableDocument(String content);

    /**
     * 是否为代码文档
     *
     * @param fileName 文件名
     * @param content  文档内容
     * @return true if code
     */
    boolean isCodeDocument(String fileName, String content);

    /**
     * 是否为技术文档
     *
     * @param content 文档内容
     * @return true if technical
     */
    boolean isTechnicalDocument(String content);
}
