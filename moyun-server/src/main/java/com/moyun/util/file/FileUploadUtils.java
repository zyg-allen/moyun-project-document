package com.moyun.util.file;

import com.moyun.common.config.RuoYiConfig;
import com.moyun.common.constant.Constants;
import com.moyun.common.exception.system.file.FileNameLengthLimitExceededException;
import com.moyun.common.exception.system.file.FileSizeLimitExceededException;
import com.moyun.common.exception.system.file.InvalidExtensionException;
import com.moyun.util.date.DateUtils;
import com.moyun.util.string.StringUtils;
import com.moyun.util.uuid.Seq;
import org.apache.commons.io.FilenameUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Set;

public class FileUploadUtils {
    public static final long DEFAULT_MAX_SIZE = 50 * 1024 * 1024L;

    public static final int DEFAULT_FILE_NAME_LENGTH = 100;

    /**
     * 需要执行文件头魔数校验的图片扩展名（小写）
     */
    public static final Set<String> IMAGE_MAGIC_EXTENSION = Set.of("jpg", "jpeg", "png", "gif", "webp", "bmp");

    private static String defaultBaseDir = RuoYiConfig.getProfile();

    public static void setDefaultBaseDir(String defaultBaseDir) {
        FileUploadUtils.defaultBaseDir = defaultBaseDir;
    }

    public static String getDefaultBaseDir() {
        return defaultBaseDir;
    }

    public static final String upload(MultipartFile file) throws IOException {
        try {
            return upload(getDefaultBaseDir(), file, MimeTypeUtils.DEFAULT_ALLOWED_EXTENSION);
        } catch (Exception e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    public static final String upload(String baseDir, MultipartFile file) throws IOException {
        try {
            return upload(baseDir, file, MimeTypeUtils.DEFAULT_ALLOWED_EXTENSION);
        } catch (Exception e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    public static final String upload(String baseDir, MultipartFile file, String[] allowedExtension)
            throws FileSizeLimitExceededException, IOException, FileNameLengthLimitExceededException,
            InvalidExtensionException {
        int fileNamelength = Objects.requireNonNull(file.getOriginalFilename()).length();
        if (fileNamelength > FileUploadUtils.DEFAULT_FILE_NAME_LENGTH) {
            throw new FileNameLengthLimitExceededException(FileUploadUtils.DEFAULT_FILE_NAME_LENGTH);
        }

        assertAllowed(file, allowedExtension);

        String fileName = extractFilename(file);

        String absPath = getAbsoluteFile(baseDir, fileName).getAbsolutePath();
        file.transferTo(Paths.get(absPath));
        return getPathFileName(baseDir, fileName);
    }

    public static final String extractFilename(MultipartFile file) {
        return StringUtils.format("{}/{}_{}.{}", DateUtils.datePath(),
                FilenameUtils.getBaseName(file.getOriginalFilename()), Seq.getId(Seq.uploadSeqType), getExtension(file));
    }

    public static final File getAbsoluteFile(String uploadDir, String fileName) throws IOException {
        File desc = new File(uploadDir + File.separator + fileName);

        if (!desc.exists()) {
            if (!desc.getParentFile().exists()) {
                desc.getParentFile().mkdirs();
            }
        }
        return desc;
    }

    public static final String getPathFileName(String uploadDir, String fileName) throws IOException {
        int dirLastIndex = RuoYiConfig.getProfile().length() + 1;
        String currentDir = StringUtils.substring(uploadDir, dirLastIndex);
        return Constants.RESOURCE_PREFIX + "/" + currentDir + "/" + fileName;
    }

    public static final void assertAllowed(MultipartFile file, String[] allowedExtension)
            throws FileSizeLimitExceededException, InvalidExtensionException {
        long size = file.getSize();
        if (size > DEFAULT_MAX_SIZE) {
            throw new FileSizeLimitExceededException(DEFAULT_MAX_SIZE / 1024 / 1024);
        }

        String fileName = file.getOriginalFilename();
        String extension = getExtension(file);
        if (allowedExtension != null && !isAllowedExtension(extension, allowedExtension)) {
            if (allowedExtension == MimeTypeUtils.IMAGE_EXTENSION) {
                throw new InvalidExtensionException.InvalidImageExtensionException(allowedExtension, extension,
                        fileName);
            } else if (allowedExtension == MimeTypeUtils.FLASH_EXTENSION) {
                throw new InvalidExtensionException.InvalidFlashExtensionException(allowedExtension, extension,
                        fileName);
            } else if (allowedExtension == MimeTypeUtils.MEDIA_EXTENSION) {
                throw new InvalidExtensionException.InvalidMediaExtensionException(allowedExtension, extension,
                        fileName);
            } else if (allowedExtension == MimeTypeUtils.VIDEO_EXTENSION) {
                throw new InvalidExtensionException.InvalidVideoExtensionException(allowedExtension, extension,
                        fileName);
            } else {
                throw new InvalidExtensionException(allowedExtension, extension, fileName);
            }
        }
    }

    public static final boolean isAllowedExtension(String extension, String[] allowedExtension) {
        for (String str : allowedExtension) {
            if (str.equalsIgnoreCase(extension)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 图片文件头魔数校验：防止改后缀伪装成图片上传脚本/可执行文件
     * <p>
     * JPEG: FF D8 FF；PNG: 89 50 4E 47；GIF: 47 49 46 38（GIF8）；
     * WebP: "RIFF" 前缀 + 第 8~11 字节 "WEBP"；BMP: 42 4D（BM）
     * </p>
     *
     * @param bytes     文件内容字节
     * @param extension 文件扩展名（小写）
     * @return 魔数与扩展名匹配返回 true；字节过短、扩展名非法或魔数不匹配返回 false
     */
    public static final boolean hasValidImageMagicNumber(byte[] bytes, String extension) {
        if (bytes == null || bytes.length < 12 || StringUtils.isEmpty(extension)) {
            return false;
        }
        String ext = extension.toLowerCase();
        switch (ext) {
            case "jpg", "jpeg" -> {
                return (bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8 && (bytes[2] & 0xFF) == 0xFF;
            }
            case "png" -> {
                return (bytes[0] & 0xFF) == 0x89 && (bytes[1] & 0xFF) == 0x50
                        && (bytes[2] & 0xFF) == 0x4E && (bytes[3] & 0xFF) == 0x47;
            }
            case "gif" -> {
                return (bytes[0] & 0xFF) == 0x47 && (bytes[1] & 0xFF) == 0x49
                        && (bytes[2] & 0xFF) == 0x46 && (bytes[3] & 0xFF) == 0x38;
            }
            case "webp" -> {
                return (bytes[0] & 0xFF) == 0x52 && (bytes[1] & 0xFF) == 0x49
                        && (bytes[2] & 0xFF) == 0x46 && (bytes[3] & 0xFF) == 0x46
                        && (bytes[8] & 0xFF) == 0x57 && (bytes[9] & 0xFF) == 0x45
                        && (bytes[10] & 0xFF) == 0x42 && (bytes[11] & 0xFF) == 0x50;
            }
            case "bmp" -> {
                return (bytes[0] & 0xFF) == 0x42 && (bytes[1] & 0xFF) == 0x4D;
            }
            default -> {
                return false;
            }
        }
    }

    public static final String getExtension(MultipartFile file) {
        String extension = FilenameUtils.getExtension(file.getOriginalFilename());
        if (StringUtils.isEmpty(extension)) {
            extension = MimeTypeUtils.getExtension(Objects.requireNonNull(file.getContentType()));
        }
        return extension;
    }
}
