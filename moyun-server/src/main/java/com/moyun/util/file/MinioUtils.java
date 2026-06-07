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
     */
    public String getFileUrl(String bucketName, String fileName) {
        return minioConfig.getEndpoint() + "/" + bucketName + "/" + fileName;
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
        String suffix = originalFileName.substring(originalFileName.lastIndexOf("."));
        String datePath = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        return datePath + "/" + UUID.randomUUID().toString().replace("-", "") + suffix;
    }

    /**
     * 判断是否启用MinIO
     */
    public boolean isEnabled() {
        return Boolean.TRUE.equals(minioConfig.getEnabled());
    }
}
