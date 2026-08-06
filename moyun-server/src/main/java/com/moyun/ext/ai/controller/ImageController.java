package com.moyun.ext.ai.controller;

import com.moyun.ext.ai.service.MinioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.InputStream;

@Slf4j
@Tag(name = "图片访问")
@RestController
@RequestMapping("/cms/ai/image")
public class ImageController {

    @Autowired
    private MinioService minioService;

    @Operation(summary = "获取图片", description = "根据 MinIO 对象名读取图片文件")
    @GetMapping
    @PreAuthorize("@ss.hasPermi('cms:ai:image:query')")
    public ResponseEntity<byte[]> getImage(@RequestParam String path) {
        log.info("收到图片请求，MinIO对象名: {}", path);

        try {
            InputStream inputStream = minioService.getFileStream(path, minioService.getImagesBucket());
            
            if (inputStream == null) {
                log.warn("❌ 图片不存在于 MinIO: {}", path);
                return ResponseEntity.notFound().build();
            }

            byte[] imageBytes = inputStream.readAllBytes();
            inputStream.close();
            log.info("✅ 成功从 MinIO 读取图片，大小: {} bytes", imageBytes.length);

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
