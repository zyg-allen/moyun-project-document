package com.moyun.ext.ai.service.impl;

import com.moyun.common.config.MinioConfig;
import com.moyun.common.config.RuoYiConfig;
import com.moyun.ext.ai.config.AiStorageConfig;
import com.moyun.ext.ai.exception.BusinessException;
import com.moyun.ext.ai.exception.ErrorCode;
import com.moyun.ext.ai.service.MinioService;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * MinIO 对象存储服务实现类
 *
 * <p><b>本地自动降级（与 SysFileServiceImpl 同口径）</b>：MinIO 上传/连接异常且
 * {@code minio.auto-fallback=true}（默认）时自动落本地磁盘，返回值带 {@link #LOCAL_MARKER}
 * 前缀；{@link #getFileStream}/{@link #deleteFile} 识别该前缀走本地读写。
 * 降级路径不影响 MinIO 可用时的行为。</p>
 *
 * @author laomao
 */
@Slf4j
@Service
public class MinioServiceImpl implements MinioService {

    /** 本地存储路径标记：filePath 以此开头表示文件落在本地磁盘而非 MinIO */
    private static final String LOCAL_MARKER = "local:";

    @Autowired
    private MinioClient minioClient;

    @Autowired
    private AiStorageConfig aiStorageConfig;

    @Autowired
    private MinioConfig minioConfig;

    /**
     * 初始化存储桶
     */
    @PostConstruct
    public void init() {
        try {
            // 确保知识库文件存储桶存在
            ensureBucketExists(aiStorageConfig.getBucket().getKnowledge());
            // 确保图片存储桶存在
            ensureBucketExists(aiStorageConfig.getBucket().getImages());
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
        String objectName = generateObjectName(fileName != null ? fileName : file.getOriginalFilename());
        try {
            String contentType = file.getContentType();
            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(aiStorageConfig.getBucket().getKnowledge())
                    .object(objectName)
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .contentType(contentType)
                    .build());

            log.info("✅ 文件上传成功: {}/{}", aiStorageConfig.getBucket().getKnowledge(), objectName);
            return objectName;
        } catch (Exception e) {
            if (isAutoFallback()) {
                log.warn("[AI存储] MinIO 上传异常，自动降级到本地存储：{}", e.getMessage());
                try (InputStream in = file.getInputStream()) {
                    return writeLocal(in.readAllBytes(), "knowledge", objectName);
                } catch (Exception localEx) {
                    log.error("❌ 本地降级写入也失败: {}", localEx.getMessage(), localEx);
                }
            }
            log.error("❌ 文件上传失败: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.DOCUMENT_UPLOAD_FAILED, "文件上传失败: " + e.getMessage(), e);
        }
    }

    @Override
    public String uploadKnowledgeFile(InputStream inputStream, String fileName, String contentType) {
        String objectName = generateObjectName(fileName);
        // 先读全量字节（降级时复用同一份，避免流已消费导致降级读到空）
        byte[] bytes;
        try {
            bytes = inputStream.readAllBytes();
        } catch (Exception e) {
            log.error("❌ 读取上传流失败: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.DOCUMENT_UPLOAD_FAILED, "文件上传失败: " + e.getMessage(), e);
        }
        if (contentType == null) {
            contentType = "application/octet-stream";
        }
        try {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(aiStorageConfig.getBucket().getKnowledge())
                    .object(objectName)
                    .stream(new ByteArrayInputStream(bytes), bytes.length, -1)
                    .contentType(contentType)
                    .build());

            log.info("✅ 文件上传成功: {}/{}", aiStorageConfig.getBucket().getKnowledge(), objectName);
            return objectName;
        } catch (Exception e) {
            if (isAutoFallback()) {
                log.warn("[AI存储] MinIO 上传异常，自动降级到本地存储：{}", e.getMessage());
                try {
                    return writeLocal(bytes, "knowledge", objectName);
                } catch (Exception localEx) {
                    log.error("❌ 本地降级写入也失败: {}", localEx.getMessage(), localEx);
                }
            }
            log.error("❌ 文件上传失败: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.DOCUMENT_UPLOAD_FAILED, "文件上传失败: " + e.getMessage(), e);
        }
    }

    @Override
    public String uploadImage(BufferedImage image, Long knowledgeBaseId, int pageNumber, int imageIndex) {
        String objectName = String.format("%d/page_%d_img_%d.jpg", knowledgeBaseId, pageNumber, imageIndex);
        try {
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

            try {
                minioClient.putObject(PutObjectArgs.builder()
                        .bucket(aiStorageConfig.getBucket().getImages())
                        .object(objectName)
                        .stream(new ByteArrayInputStream(imageBytes), imageBytes.length, -1)
                        .contentType(contentType)
                        .build());
                log.info("✅ 图片上传成功: {}/{}", aiStorageConfig.getBucket().getImages(), objectName);
            } catch (Exception minioEx) {
                // MinIO 不可用：降级落本地，图片提取链路继续可用
                if (!isAutoFallback()) {
                    throw minioEx;
                }
                log.warn("[AI存储] MinIO 图片上传异常，自动降级到本地存储：{}", minioEx.getMessage());
                return writeLocal(imageBytes, "images", objectName);
            }

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
            // 返回直接访问路径（使用 objectName 作为兜底）
            return "/" + bucket + "/" + objectName;
        }
    }

    @Override
    public InputStream getFileStream(String objectName, String bucket) {
        // 本地降级文件：直接读磁盘
        if (isLocalPath(objectName)) {
            try {
                return Files.newInputStream(Paths.get(stripLocalMarker(objectName)));
            } catch (Exception e) {
                log.error("❌ 读取本地降级文件失败: {}", e.getMessage(), e);
                throw new BusinessException(ErrorCode.DOCUMENT_NOT_FOUND, "获取文件流失败: " + e.getMessage(), e);
            }
        }
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
        // 本地降级文件：删磁盘文件
        if (isLocalPath(objectName)) {
            try {
                return Files.deleteIfExists(Paths.get(stripLocalMarker(objectName)));
            } catch (Exception e) {
                log.error("❌ 删除本地降级文件失败: {}", e.getMessage(), e);
                return false;
            }
        }
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
                    .bucket(aiStorageConfig.getBucket().getImages())
                    .prefix(prefix)
                    .recursive(true)
                    .build());

            for (Result<Item> result : results) {
                Item item = result.get();
                deleteFile(item.objectName(), aiStorageConfig.getBucket().getImages());
                count++;
            }
            log.info("✅ 删除知识库 {} 的 {} 张图片", knowledgeBaseId, count);
        } catch (Exception e) {
            log.error("❌ 批量删除图片失败: {}", e.getMessage(), e);
        }
        // 本地降级图片目录一并清理（存在才删）
        try {
            Path localDir = Paths.get(RuoYiConfig.getProfile(), "ai", "images", String.valueOf(knowledgeBaseId));
            if (Files.isDirectory(localDir)) {
                try (var stream = Files.walk(localDir)) {
                    count += (int) stream.sorted(java.util.Comparator.reverseOrder())
                            .filter(p -> !p.equals(localDir))
                            .mapToLong(p -> {
                                try {
                                    Files.deleteIfExists(p);
                                    return 1;
                                } catch (Exception ex) {
                                    return 0;
                                }
                            }).sum();
                }
                Files.deleteIfExists(localDir);
            }
        } catch (Exception e) {
            log.warn("[AI存储] 清理本地降级图片目录失败: {}", e.getMessage());
        }
        return count;
    }

    @Override
    public String getKnowledgeBucket() {
        return aiStorageConfig.getBucket().getKnowledge();
    }

    @Override
    public String getImagesBucket() {
        return aiStorageConfig.getBucket().getImages();
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

    /** auto-fallback 开关（yaml minio.auto-fallback，默认 true：MinIO 异常自动降级本地） */
    private boolean isAutoFallback() {
        return minioConfig == null || !Boolean.FALSE.equals(minioConfig.getAutoFallback());
    }

    private boolean isLocalPath(String path) {
        return path != null && path.startsWith(LOCAL_MARKER);
    }

    private String stripLocalMarker(String path) {
        return path.substring(LOCAL_MARKER.length());
    }

    /**
     * 本地降级写入：落 {@code RuoYiConfig.profile/ai/{dir}}，返回 {@code local:绝对路径}。
     * 返回值会存进 knowledge.filePath / DocumentImage 存储字段，读删按前缀识别。
     */
    private String writeLocal(byte[] bytes, String dir, String objectName) throws java.io.IOException {
        Path dirPath = Paths.get(RuoYiConfig.getProfile(), "ai", dir);
        Files.createDirectories(dirPath);
        Path file = dirPath.resolve(objectName);
        Files.write(file, bytes);
        log.info("✅ 已降级写入本地: {}", file);
        return LOCAL_MARKER + file;
    }
}
