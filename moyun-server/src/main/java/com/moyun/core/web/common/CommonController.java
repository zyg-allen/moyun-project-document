package com.moyun.core.web.common;

import com.moyun.common.config.RuoYiConfig;
import com.moyun.common.constant.Constants;
import com.moyun.core.base.AjaxResult;
import com.moyun.ext.file.domain.entity.SysFile;
import com.moyun.ext.file.service.ISysFileService;
import com.moyun.util.file.FileUtils;
import com.moyun.util.string.StringUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

/**
 * 通用请求处理
 * <p>
 * 文件上传统一走 {@link ISysFileService}（兼容 MinIO / 本地存储），下载仍读取本地磁盘。
 *
 * @author ruoyi
 */
@Tag(name = "通用请求处理", description = "通用文件下载上传接口")
@RestController
@RequestMapping("/common")
@Slf4j
public class CommonController {

    @Autowired
    private ISysFileService sysFileService;

    private static final String FILE_DELIMETER = ",";

    /**
     * 通用下载请求
     */
    @Operation(summary = "文件下载", description = "下载指定文件")
    @GetMapping("/download")
    public void fileDownload(
            @Parameter(description = "文件名") String fileName,
            @Parameter(description = "是否删除") Boolean delete,
            HttpServletResponse response) {
        try {
            if (!FileUtils.checkAllowDownload(fileName)) {
                throw new Exception(StringUtils.format("文件名称({})非法，不允许下载。 ", fileName));
            }
            String realFileName = System.currentTimeMillis() + fileName.substring(fileName.indexOf("_") + 1);
            String filePath = RuoYiConfig.getDownloadPath() + fileName;

            response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
            FileUtils.setAttachmentResponseHeader(response, realFileName);
            FileUtils.writeBytes(filePath, response.getOutputStream());
            if (delete) {
                FileUtils.deleteFile(filePath);
            }
        } catch (Exception e) {
            log.error("下载文件失败", e);
        }
    }

    /**
     * 通用上传请求（单个）
     * <p>
     * 走 {@link ISysFileService#uploadFile}，兼容 MinIO / 本地存储，元数据落 sys_file 表。
     * 返回字段兼容前端 ImageUpload 组件（fileName 存访问 URL，组件内部拼接 baseUrl）。
     */
    @Operation(summary = "单文件上传", description = "上传单个文件")
    @PostMapping("/upload")
    public AjaxResult uploadFile(
            @Parameter(description = "上传的文件") MultipartFile file) {
        try {
            SysFile sysFile = sysFileService.uploadFile(file, "common", null);
            String url = sysFile.getFileUrl();
            AjaxResult ajax = AjaxResult.success();
            ajax.put("url", url);
            ajax.put("fileName", url);
            ajax.put("newFileName", FileUtils.getName(url));
            ajax.put("originalFilename", file.getOriginalFilename());
            ajax.put("fileId", sysFile.getId());
            return ajax;
        } catch (Exception e) {
            return AjaxResult.error(e.getMessage());
        }
    }

    /**
     * 通用上传请求（多个）
     * <p>
     * 循环调用 {@link ISysFileService#uploadFile}，兼容 MinIO / 本地存储。
     */
    @Operation(summary = "多文件上传", description = "上传多个文件")
    @PostMapping("/uploads")
    public AjaxResult uploadFiles(
            @Parameter(description = "上传的文件列表") List<MultipartFile> files) {
        try {
            List<String> urls = new ArrayList<>();
            List<String> fileNames = new ArrayList<>();
            List<String> newFileNames = new ArrayList<>();
            List<String> originalFilenames = new ArrayList<>();
            List<Long> fileIds = new ArrayList<>();
            for (MultipartFile file : files) {
                SysFile sysFile = sysFileService.uploadFile(file, "common", null);
                String url = sysFile.getFileUrl();
                urls.add(url);
                fileNames.add(url);
                newFileNames.add(FileUtils.getName(url));
                originalFilenames.add(file.getOriginalFilename());
                fileIds.add(sysFile.getId());
            }
            AjaxResult ajax = AjaxResult.success();
            ajax.put("urls", StringUtils.join(urls, FILE_DELIMETER));
            ajax.put("fileNames", StringUtils.join(fileNames, FILE_DELIMETER));
            ajax.put("newFileNames", StringUtils.join(newFileNames, FILE_DELIMETER));
            ajax.put("originalFilenames", StringUtils.join(originalFilenames, FILE_DELIMETER));
            ajax.put("fileIds", StringUtils.join(fileIds, FILE_DELIMETER));
            return ajax;
        } catch (Exception e) {
            return AjaxResult.error(e.getMessage());
        }
    }

    /**
     * 本地资源通用下载
     */
    @Operation(summary = "本地资源下载", description = "下载本地资源文件")
    @GetMapping("/download/resource")
    public void resourceDownload(
            @Parameter(description = "资源路径") String resource,
            HttpServletResponse response) {
        try {
            if (!FileUtils.checkAllowDownload(resource)) {
                throw new Exception(StringUtils.format("资源文件({})非法，不允许下载。 ", resource));
            }
            // 本地资源路径
            String localPath = RuoYiConfig.getProfile();
            // 数据库资源地址
            String downloadPath = localPath + StringUtils.substringAfter(resource, Constants.RESOURCE_PREFIX);
            // 下载名称
            String downloadName = StringUtils.substringAfterLast(downloadPath, "/");
            response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
            FileUtils.setAttachmentResponseHeader(response, downloadName);
            FileUtils.writeBytes(downloadPath, response.getOutputStream());
        } catch (Exception e) {
            log.error("下载文件失败", e);
        }
    }
}
