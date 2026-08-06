package com.moyun.ext.ai.service;

import org.springframework.web.multipart.MultipartFile;

import java.awt.image.BufferedImage;
import java.io.InputStream;

/**
 * MinIO 对象存储服务接口
 *
 * @author laomao
 */
public interface MinioService {

    /**
     * 上传知识库文件
     *
     * @param file     文件
     * @param fileName 文件名（可选，为空则使用原始文件名）
     * @return 文件访问路径
     */
    String uploadKnowledgeFile(MultipartFile file, String fileName);

    /**
     * 上传知识库文件（从输入流）
     *
     * @param inputStream 输入流
     * @param fileName    文件名
     * @param contentType 内容类型
     * @return 文件访问路径
     */
    String uploadKnowledgeFile(InputStream inputStream, String fileName, String contentType);

    /**
     * 上传图片
     *
     * @param image           图片
     * @param knowledgeBaseId 知识库ID
     * @param pageNumber      页码
     * @param imageIndex      图片索引
     * @return 图片访问路径
     */
    String uploadImage(BufferedImage image, Long knowledgeBaseId, int pageNumber, int imageIndex);

    /**
     * 获取文件访问URL
     *
     * @param objectName 对象名称
     * @param bucket     存储桶名称
     * @return 访问URL
     */
    String getFileUrl(String objectName, String bucket);

    /**
     * 获取文件输入流
     *
     * @param objectName 对象名称
     * @param bucket     存储桶名称
     * @return 输入流
     */
    InputStream getFileStream(String objectName, String bucket);

    /**
     * 删除文件
     *
     * @param objectName 对象名称
     * @param bucket     存储桶名称
     * @return 是否成功
     */
    boolean deleteFile(String objectName, String bucket);

    /**
     * 删除知识库相关的所有图片
     *
     * @param knowledgeBaseId 知识库ID
     * @return 删除的文件数量
     */
    int deleteImagesByKnowledgeBaseId(Long knowledgeBaseId);

    /**
     * 获取知识库文件存储桶名称
     */
    String getKnowledgeBucket();

    /**
     * 获取图片存储桶名称
     */
    String getImagesBucket();
}
