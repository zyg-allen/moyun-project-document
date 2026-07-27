package com.moyun.agent.service.impl;

import com.moyun.agent.config.MinioConfig;
import com.moyun.agent.exception.BusinessException;
import com.moyun.agent.exception.ErrorCode;
import com.moyun.agent.service.MinioService;
import io.minio.*;
import io.minio.messages.Item;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.UUID;

/**
 * MinIO 对象存储服务实现类
 *
 * @author laomao
 */
@Slf4j
@Service
public class MinioServiceImpl implements MinioService {

    @Autowired
    private MinioClient minioClient;

    @Autowired
    private MinioConfig minioConfig;

    /**
     * 初始化存储桶
     */
    @PostConstruct
    public void init() {
        try {
            // 确保知识库文件存储桶存在
            ensureBucketExists(minioConfig.getBucket().getKnowledge());
            // 确保图片存储桶存在
            ensureBucketExists(minioConfig.getBucket().getImages());
            log.info("✅ MinIO 存储桶初始化完成");
        } catch (Exception e) {
            log.error("❌ MinIO 存储桶初始化失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 确保存储桶存在
     */
    private void ensureBucketExists(String bucketName) throws Exception {
        boolean exists = minioClient.bucketExists(BucketExistsArgs.builder()
                .bucket(bucketName)
                .build());
        if (!exists) {
            minioClient.makeBucket(MakeBucketArgs.builder()
                    .bucket(bucketName)
                    .build());
            log.info("✅ 创建存储桶: {}", bucketName);
        }
    }

    @Override
    public String uploadKnowledgeFile(MultipartFile file, String fileName) {
        try {
            String objectName = generateObjectName(fileName != null ? fileName : file.getOriginalFilename());
            String contentType = file.getContentType();
            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(minioConfig.getBucket().getKnowledge())
                    .object(objectName)
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .contentType(contentType)
                    .build());

            log.info("✅ 文件上传成功: {}/{}", minioConfig.getBucket().getKnowledge(), objectName);
            return objectName;
        } catch (Exception e) {
            log.error("❌ 文件上传失败: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.DOCUMENT_UPLOAD_FAILED, "文件上传失败: " + e.getMessage(), e);
        }
    }

    @Override
    public String uploadKnowledgeFile(InputStream inputStream, String fileName, String contentType) {
        try {
            String objectName = generateObjectName(fileName);
            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            // 读取输入流到字节数组以获取大小
            byte[] bytes = inputStream.readAllBytes();

            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(minioConfig.getBucket().getKnowledge())
                    .object(objectName)
                    .stream(new ByteArrayInputStream(bytes), bytes.length, -1)
                    .contentType(contentType)
                    .build());

            log.info("✅ 文件上传成功: {}/{}", minioConfig.getBucket().getKnowledge(), objectName);
            return objectName;
        } catch (Exception e) {
            log.error("❌ 文件上传失败: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.DOCUMENT_UPLOAD_FAILED, "文件上传失败: " + e.getMessage(), e);
        }
    }

    @Override
    public String uploadImage(BufferedImage image, Long knowledgeBaseId, int pageNumber, int imageIndex) {
        try {
            // 生成图片对象名称
            String objectName = String.format("%d/page_%d_img_%d.jpg", knowledgeBaseId, pageNumber, imageIndex);

            // 将 BufferedImage 转换为字节数组
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            boolean success = ImageIO.write(image, "jpg", baos);
            
            if (!success) {
                // 尝试 PNG 格式
                baos.reset();
                ImageIO.write(image, "png", baos);
                objectName = objectName.replace(".jpg", ".png");
            }

            byte[] imageBytes = baos.toByteArray();
            if (imageBytes.length == 0) {
                log.warn("图片转换失败，字节数组为空");
                return null;
            }

            String contentType = objectName.endsWith(".png") ? "image/png" : "image/jpeg";

            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(minioConfig.getBucket().getImages())
                    .object(objectName)
                    .stream(new ByteArrayInputStream(imageBytes), imageBytes.length, -1)
                    .contentType(contentType)
                    .build());

            log.info("✅ 图片上传成功: {}/{}", minioConfig.getBucket().getImages(), objectName);
            return objectName;
        } catch (Exception e) {
            log.error("❌ 图片上传失败: {}", e.getMessage(), e);
            return null;
        }
    }

    @Override
    public String getFileUrl(String objectName, String bucket) {
        try {
            return minioClient.getPresignedObjectUrl(
                    io.minio.GetPresignedObjectUrlArgs.builder()
                            .method(io.minio.http.Method.GET)
                            .bucket(bucket)
                            .object(objectName)
                            .expiry(7 * 24 * 60 * 60) // 7天有效期
                            .build());
        } catch (Exception e) {
            log.error("❌ 获取文件URL失败: {}", e.getMessage(), e);
            // 返回直接访问路径
            return minioConfig.getEndpoint() + "/" + bucket + "/" + objectName;
        }
    }

    @Override
    public InputStream getFileStream(String objectName, String bucket) {
        try {
            return minioClient.getObject(GetObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectName)
                    .build());
        } catch (Exception e) {
            log.error("❌ 获取文件流失败: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.DOCUMENT_NOT_FOUND, "获取文件流失败: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean deleteFile(String objectName, String bucket) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectName)
                    .build());
            log.info("✅ 文件删除成功: {}/{}", bucket, objectName);
            return true;
        } catch (Exception e) {
            log.error("❌ 文件删除失败: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public int deleteImagesByKnowledgeBaseId(Long knowledgeBaseId) {
        int count = 0;
        try {
            String prefix = knowledgeBaseId + "/";
            Iterable<Result<Item>> results = minioClient.listObjects(ListObjectsArgs.builder()
                    .bucket(minioConfig.getBucket().getImages())
                    .prefix(prefix)
                    .recursive(true)
                    .build());

            for (Result<Item> result : results) {
                Item item = result.get();
                deleteFile(item.objectName(), minioConfig.getBucket().getImages());
                count++;
            }
            log.info("✅ 删除知识库 {} 的 {} 张图片", knowledgeBaseId, count);
        } catch (Exception e) {
            log.error("❌ 批量删除图片失败: {}", e.getMessage(), e);
        }
        return count;
    }

    @Override
    public String getKnowledgeBucket() {
        return minioConfig.getBucket().getKnowledge();
    }

    @Override
    public String getImagesBucket() {
        return minioConfig.getBucket().getImages();
    }

    /**
     * 生成唯一的对象名称
     */
    private String generateObjectName(String originalFilename) {
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        return System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8) + extension;
    }
}
