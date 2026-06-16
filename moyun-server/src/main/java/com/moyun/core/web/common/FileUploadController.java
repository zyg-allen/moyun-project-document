package com.moyun.core.web.common;

import com.moyun.common.annotation.Anonymous;
import com.moyun.common.config.RuoYiConfig;
import com.moyun.common.exception.system.NonCaptureException;
import com.moyun.core.base.AjaxResult;
import com.moyun.core.config.ServerConfig;
import com.moyun.util.file.FileUploadUtils;
import com.moyun.util.file.FileUtils;
import com.moyun.util.file.MinioUtils;
import com.moyun.util.string.StringUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件上传
 *
 * @author ruoyi
 */
@Anonymous
@Tag(name = "文件上传", description = "通用文件上传接口")
@RestController
@RequestMapping("/file")
@Slf4j
public class FileUploadController {

    @Autowired
    private ServerConfig serverConfig;

    @Autowired
    private MinioUtils minioUtils;

    /**
     * 文件上传
     */
    @Operation(summary = "文件上传", description = "上传文件到服务器或MinIO存储")
    @PostMapping("/upload")
    public AjaxResult uploadFile(
            @Parameter(description = "上传的文件") @RequestParam("file") MultipartFile file) {
        try {
            log.info("文件 {} 上传中...", file.getOriginalFilename());

            String url;
            String fileName;

            // 判断是否启用MinIO
            if (minioUtils.isEnabled()) {
                url = minioUtils.uploadFile(file);
                fileName = url;
            } else {
                // 本地存储
                String filePath = RuoYiConfig.getUploadPath();
                fileName = FileUploadUtils.upload(filePath, file);
                url = serverConfig.getUrl() + fileName;
            }

            AjaxResult ajax = AjaxResult.success();
            ajax.put("url", url);
            ajax.put("fileName", fileName);
            ajax.put("newFileName", FileUtils.getName(fileName));
            ajax.put("originalFilename", file.getOriginalFilename());
            log.info("文件 {} 上传成功！", file.getOriginalFilename());
            return ajax;
        } catch (Exception e) {
            throw new NonCaptureException(StringUtils.format("文件 {} 上传失败！", file.getOriginalFilename()), e);
        }
    }
}
