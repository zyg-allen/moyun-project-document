package com.moyun.util.file;

import com.moyun.common.config.MinioConfig;
import io.minio.*;
import io.minio.http.Method;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * MinIO 工具类
 *
 * @author moyun
 */
@Slf4j
@Component
public class MinioUtils {

    @Autowired
    private MinioClient minioClient;

    @Autowired
    private MinioConfig minioConfig;

    /**
     * 检查存储桶是否存在，不存在则创建
     */
    private void checkBucket(String bucketName) {
        try {
            boolean found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            if (!found) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
                log.info("创建存储桶: {}", bucketName);
            }
        } catch (Exception e) {
            log.error("检查存储桶失败", e);
            throw new RuntimeException("检查存储桶失败", e);
        }
    }

    /**
     * 上传文件
     *
     * @param file 文件
     * @return 文件访问URL
     */
    public String uploadFile(MultipartFile file) {
        return uploadFile(minioConfig.getBucketName(), file);
    }

    /**
     * 上传文件到指定存储桶
     *
     * @param bucketName 存储桶名称
     * @param file       文件
     * @return 文件访问URL
     */
    public String uploadFile(String bucketName, MultipartFile file) {
        checkBucket(bucketName);
        String fileName = generateFileName(file.getOriginalFilename());
        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(fileName)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );
            return getFileUrl(bucketName, fileName);
        } catch (Exception e) {
            log.error("上传文件失败", e);
            throw new RuntimeException("上传文件失败", e);
        }
    }

    /**
     * 上传字节数组
     *
     * @param bytes       字节数组
     * @param contentType 内容类型
     * @param suffix      后缀名
     * @return 文件访问URL
     */
    public String uploadBytes(byte[] bytes, String contentType, String suffix) {
        return uploadBytes(minioConfig.getBucketName(), bytes, contentType, suffix);
    }

    /**
     * 上传字节数组到指定存储桶
     *
     * @param bucketName  存储桶名称
     * @param bytes       字节数组
     * @param contentType 内容类型
     * @param suffix      后缀名
     * @return 文件访问URL
     */
    public String uploadBytes(String bucketName, byte[] bytes, String contentType, String suffix) {
        checkBucket(bucketName);
        String fileName = generateFileName("." + suffix);
        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(fileName)
                            .stream(new ByteArrayInputStream(bytes), bytes.length, -1)
                            .contentType(contentType)
                            .build()
            );
            return getFileUrl(bucketName, fileName);
        } catch (Exception e) {
            log.error("上传文件失败", e);
            throw new RuntimeException("上传文件失败", e);
        }
    }

    /**
     * 获取文件访问URL（永久，需要配置存储桶策略为public）
     * <p>v1.1.2 修复：用 accessUrl（对外访问地址，如 CDN）替代 endpoint（内网地址）。
     * 生产环境 MinIO 通常部署在内网，前端无法访问 endpoint；accessUrl 应配置为外网/CDN 域名。
     * 若 accessUrl 未配置，自动回退到 endpoint（兼容开发环境）。</p>
     * <p>v1.1.2 二次修复：兼容 accessUrl 已包含 bucket 名的旧配置。
     * 若 accessUrl 以 "/bucketName" 结尾，不再重复拼接 bucketName，
     * 避免 URL 出现两个 bucket 名（如 http://host/moyun/moyun/...）。
     * 新配置建议 accessUrl 不带 bucket 名（如 http://host:9001）。</p>
     */
    public String getFileUrl(String bucketName, String fileName) {
        String base = minioConfig.getAccessUrlOrEndpoint();
        if (base == null || base.isEmpty()) {
            return "/" + bucketName + "/" + fileName;
        }
        // 去掉末尾斜杠统一处理
        while (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        // 防御：accessUrl 已含 bucket 名时不再重复拼接
        if (base.endsWith("/" + bucketName)) {
            return base + "/" + fileName;
        }
        return base + "/" + bucketName + "/" + fileName;
    }

    /**
     * 获取临时访问URL（默认7天）
     */
    public String getPresignedUrl(String fileName) {
        return getPresignedUrl(minioConfig.getBucketName(), fileName, 7, TimeUnit.DAYS);
    }

    /**
     * 获取临时访问URL
     */
    public String getPresignedUrl(String bucketName, String fileName, int expiry, TimeUnit timeUnit) {
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .bucket(bucketName)
                            .object(fileName)
                            .expiry(expiry, timeUnit)
                            .method(Method.GET)
                            .build()
            );
        } catch (Exception e) {
            log.error("获取临时URL失败", e);
            throw new RuntimeException("获取临时URL失败", e);
        }
    }

    /**
     * 删除文件
     */
    public boolean removeFile(String fileName) {
        return removeFile(minioConfig.getBucketName(), fileName);
    }

    /**
     * 删除指定存储桶的文件
     */
    public boolean removeFile(String bucketName, String fileName) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(fileName)
                            .build()
            );
            return true;
        } catch (Exception e) {
            log.error("删除文件失败", e);
            return false;
        }
    }

    /**
     * 生成文件名
     */
    private String generateFileName(String originalFileName) {
        String suffix = "";
        if (originalFileName != null && originalFileName.contains(".")) {
            suffix = originalFileName.substring(originalFileName.lastIndexOf("."));
        }
        String datePath = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        return datePath + "/" + UUID.randomUUID().toString().replace("-", "") + suffix;
    }

    /**
     * 判断是否启用 MinIO。
     * <p>
     * 综合两个条件：配置 enabled=true 且未手动强制降级（fallbackToLocal=false）。
     * 注意：本方法只看配置，不探测 MinIO 服务是否真的可达，上传时仍需配合 {@link #isAvailable()} 做运行期降级。
     */
    public boolean isEnabled() {
        return Boolean.TRUE.equals(minioConfig.getEnabled())
                && !Boolean.TRUE.equals(minioConfig.getFallbackToLocal());
    }

    /**
     * 探测 MinIO 服务是否真实可达（bucket 是否存在）。
     * <p>
     * 用于上传前的运行期降级判断：配置上启用了 MinIO，但服务实际不可达时自动切到本地。
     * 调用轻量（仅一次 bucketExists 请求），失败立即返回 false，不阻塞。
     */
    public boolean isAvailable() {
        if (!isEnabled()) {
            return false;
        }
        try {
            String bucketName = minioConfig.getBucketName();
            minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            return true;
        } catch (Exception e) {
            log.warn("[MinIO] 服务不可达，将降级到本地存储：{}", e.getMessage());
            return false;
        }
    }
}
