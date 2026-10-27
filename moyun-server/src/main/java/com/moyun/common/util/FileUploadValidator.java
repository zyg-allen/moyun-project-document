package com.moyun.common.util;

import java.util.Set;

import org.springframework.web.multipart.MultipartFile;

/**
 * 门户上传校验：扩展名白名单 + 大小上限 + 空文件。
 *
 * <p>仅做“能不能收”的判断，具体存储交给文件服务；返回 null 表示通过。</p>
 */
public final class FileUploadValidator {

    private static final Set<String> ALLOWED = Set.of(
            "jpg", "jpeg", "png", "webp", "gif", "bmp",
            "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "csv",
            "zip", "rar", "7z",
            "mp3", "m4a", "wav", "amr", "mp4", "mov"
    );

    private FileUploadValidator() {
    }

    public static String validate(MultipartFile file, long maxBytes) {
        if (file == null || file.isEmpty()) {
            return "请选择要上传的文件";
        }
        if (file.getSize() > maxBytes) {
            return "文件过大，最大 " + (maxBytes / 1024 / 1024) + "MB";
        }
        String name = file.getOriginalFilename();
        if (name == null || name.isBlank()) {
            return "文件名不能为空";
        }
        int dot = name.lastIndexOf('.');
        if (dot < 0 || dot == name.length() - 1) {
            return "文件缺少扩展名";
        }
        String ext = name.substring(dot + 1).toLowerCase();
        if (!ALLOWED.contains(ext)) {
            return "不支持的文件类型：" + ext;
        }
        String contentType = file.getContentType();
        if (contentType != null && contentType.toLowerCase().startsWith("application/x-")) {
            return "不支持的文件类型";
        }
        return null;
    }
}
