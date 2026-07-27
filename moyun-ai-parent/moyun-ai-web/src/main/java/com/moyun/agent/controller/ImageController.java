package com.moyun.agent.controller;

import com.moyun.agent.service.MinioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.InputStream;

/**
 * 图片访问控制器
 *
 * <p>提供图片资源的访问接口，支持JPEG、PNG、GIF等常见图片格式</p>
 * <p>图片存储在 MinIO 对象存储中</p>
 *
 * @author laomao
 * @time 2025/11/23
 */
@Slf4j
@Tag(name = "图片访问")
@RestController
@RequestMapping("/api/image")
public class ImageController {

    @Autowired
    private MinioService minioService;

    /**
     * 获取图片
     *
     * <p>根据 MinIO 对象名读取并返回图片内容，自动识别图片类型</p>
     *
     * @param path MinIO 图片对象名
     * @return 图片字节数据
     */
    @Operation(summary = "获取图片", description = "根据 MinIO 对象名读取图片文件")
    @GetMapping
    public ResponseEntity<byte[]> getImage(@RequestParam String path) {
        log.info("收到图片请求，MinIO对象名: {}", path);

        try {
            // 从 MinIO 获取图片
            InputStream inputStream = minioService.getFileStream(path, minioService.getImagesBucket());
            
            if (inputStream == null) {
                log.warn("❌ 图片不存在于 MinIO: {}", path);
                return ResponseEntity.notFound().build();
            }

            byte[] imageBytes = inputStream.readAllBytes();
            inputStream.close();
            log.info("✅ 成功从 MinIO 读取图片，大小: {} bytes", imageBytes.length);

            // 根据文件扩展名设置 Content-Type
            String contentType = MediaType.IMAGE_JPEG_VALUE;
            if (path.toLowerCase().endsWith(".png")) {
                contentType = MediaType.IMAGE_PNG_VALUE;
            } else if (path.toLowerCase().endsWith(".gif")) {
                contentType = MediaType.IMAGE_GIF_VALUE;
            }

            log.info("Content-Type: {}", contentType);

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(imageBytes);

        } catch (Exception e) {
            log.error("❌ 读取图片失败: {}", path, e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
