package com.moyun.ext.file.service.impl;


import cn.hutool.crypto.digest.DigestUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moyun.common.config.MinioConfig;
import com.moyun.common.config.RuoYiConfig;
import com.moyun.core.config.ServerConfig;
import com.moyun.ext.file.domain.entity.SysFile;
import com.moyun.ext.file.mapper.SysFileMapper;
import com.moyun.ext.file.service.ISysFileService;
import com.moyun.util.file.MinioUtils;
import com.moyun.util.security.SecurityUtils;
import com.moyun.portal.util.PortalSecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class SysFileServiceImpl implements ISysFileService {

    @Autowired
    private SysFileMapper sysFileMapper;

    @Autowired
    private MinioUtils minioUtils;

    @Autowired
    private MinioConfig minioConfig;

    @Autowired
    private ServerConfig serverConfig;

    @Override
    public Page<SysFile> selectFilePage(Page<SysFile> page, SysFile query) {
        return sysFileMapper.selectPage(page, buildQueryWrapper(query));
    }

    @Override
    public List<SysFile> selectFileList(SysFile query) {
        return sysFileMapper.selectList(buildQueryWrapper(query));
    }

    @Override
    public SysFile selectFileById(Long id) {
        return sysFileMapper.selectById(id);
    }

    @Override
    @Transactional
    public SysFile uploadFile(MultipartFile file, String businessType, String businessId) {
        return uploadFile(file, businessType, businessId, false);
    }

    /**
     * 上传文件（支持前台用户）
     */
    @Override
    @Transactional
    public SysFile uploadFileForPortal(MultipartFile file, String businessType, String businessId) {
        return uploadFile(file, businessType, businessId, true);
    }

    private SysFile uploadFile(MultipartFile file, String businessType, String businessId, boolean isPortalUser) {
        SysFile sysFile = new SysFile();
        try {
            String fileName = file.getOriginalFilename();
            String fileExt = getFileExt(fileName);
            String fileType = getFileType(fileExt);
            byte[] fileBytes = file.getBytes();
            String fileMd5 = DigestUtil.md5Hex(fileBytes);

            sysFile.setFileName(fileName);
            sysFile.setFileExt(fileExt);
            sysFile.setFileType(fileType);
            sysFile.setFileSize(file.getSize());
            sysFile.setFileMd5(fileMd5);
            sysFile.setBusinessType(businessType);
            sysFile.setBusinessId(businessId);
            sysFile.setStatus("0");
            sysFile.setFallback(0);

            if (isPortalUser) {
                sysFile.setUploadUserId(PortalSecurityUtils.getUserId());
                sysFile.setUploadUserName(PortalSecurityUtils.getUsername());
            } else {
                sysFile.setUploadUserId(SecurityUtils.getUserId());
                sysFile.setUploadUserName(SecurityUtils.getUsername());
            }

            // MinIO 可用性判断：配置启用 + 服务真实可达 + 未手动强制降级
            boolean useMinio = minioUtils.isEnabled()
                    && (!Boolean.TRUE.equals(minioConfig.getAutoFallback()) || minioUtils.isAvailable());

            if (useMinio) {
                try {
                    String fileUrl = minioUtils.uploadFile(file);
                    sysFile.setFileUrl(fileUrl);
                    sysFile.setStorageType("minio");
                    sysFile.setBucketName(minioConfig.getBucketName());
                    sysFile.setObjectName(extractObjectNameFromUrl(fileUrl));
                    sysFile.setFilePath(fileUrl);
                    sysFile.setFallback(0);
                } catch (Exception minioEx) {
                    // 上传过程中 MinIO 异常，按 autoFallback 决定是否降级
                    if (Boolean.TRUE.equals(minioConfig.getAutoFallback())) {
                        log.warn("[文件存储] MinIO 上传异常，自动降级到本地存储：{}", minioEx.getMessage());
                        LocalUploadResult local = uploadToLocal(file);
                        sysFile.setFileUrl(local.url);
                        sysFile.setStorageType("local");
                        sysFile.setFilePath(local.url);
                        sysFile.setLocalPath(local.absolutePath);
                        sysFile.setFallback(1);
                    } else {
                        throw minioEx;
                    }
                }
            } else {
                // 配置未启用 MinIO 或手动强制降级到本地
                LocalUploadResult local = uploadToLocal(file);
                sysFile.setFileUrl(local.url);
                sysFile.setStorageType("local");
                sysFile.setFilePath(local.url);
                sysFile.setLocalPath(local.absolutePath);
                // 手动强制降级时也标记 fallback=1
                if (Boolean.TRUE.equals(minioConfig.getEnabled())
                        && Boolean.TRUE.equals(minioConfig.getFallbackToLocal())) {
                    sysFile.setFallback(1);
                }
            }

            sysFileMapper.insert(sysFile);
        } catch (Exception e) {
            throw new RuntimeException("文件上传失败", e);
        }
        return sysFile;
    }

    @Override
    @Transactional
    public SysFile uploadBytes(byte[] bytes, String fileName, String contentType, String businessType, String businessId) {
        SysFile sysFile = new SysFile();
        try {
            String fileExt = getFileExt(fileName);
            String fileType = getFileType(fileExt);
            String fileMd5 = DigestUtil.md5Hex(bytes);

            sysFile.setFileName(fileName);
            sysFile.setFileExt(fileExt);
            sysFile.setFileType(fileType);
            sysFile.setFileSize((long) bytes.length);
            sysFile.setFileMd5(fileMd5);
            sysFile.setBusinessType(businessType);
            sysFile.setBusinessId(businessId);
            sysFile.setStatus("0");
            sysFile.setFallback(0);
            sysFile.setUploadUserId(SecurityUtils.getUserId());
            sysFile.setUploadUserName(SecurityUtils.getUsername());

            boolean useMinio = minioUtils.isEnabled()
                    && (!Boolean.TRUE.equals(minioConfig.getAutoFallback()) || minioUtils.isAvailable());

            if (useMinio) {
                try {
                    String fileUrl = minioUtils.uploadBytes(bytes, contentType, fileExt);
                    sysFile.setStorageType("minio");
                    sysFile.setBucketName(minioConfig.getBucketName());
                    sysFile.setObjectName(extractObjectNameFromUrl(fileUrl));
                    sysFile.setFilePath(fileUrl);
                    sysFile.setFileUrl(fileUrl);
                } catch (Exception minioEx) {
                    if (Boolean.TRUE.equals(minioConfig.getAutoFallback())) {
                        log.warn("[文件存储] MinIO 上传字节异常，自动降级到本地存储：{}", minioEx.getMessage());
                        LocalUploadResult local = uploadBytesToLocal(bytes, fileName);
                        sysFile.setStorageType("local");
                        sysFile.setFileUrl(local.url);
                        sysFile.setFilePath(local.url);
                        sysFile.setLocalPath(local.absolutePath);
                        sysFile.setFallback(1);
                    } else {
                        throw minioEx;
                    }
                }
            } else {
                LocalUploadResult local = uploadBytesToLocal(bytes, fileName);
                sysFile.setStorageType("local");
                sysFile.setFileUrl(local.url);
                sysFile.setFilePath(local.url);
                sysFile.setLocalPath(local.absolutePath);
                if (Boolean.TRUE.equals(minioConfig.getEnabled())
                        && Boolean.TRUE.equals(minioConfig.getFallbackToLocal())) {
                    sysFile.setFallback(1);
                }
            }

            sysFileMapper.insert(sysFile);
        } catch (Exception e) {
            throw new RuntimeException("文件上传失败", e);
        }
        return sysFile;
    }

    @Override
    @Transactional
    public int deleteFileById(Long id) {
        SysFile file = selectFileById(id);
        if (file != null) {
            deleteFileFromStorage(file);
            return sysFileMapper.deleteById(id);
        }
        return 0;
    }

    @Override
    @Transactional
    public int deleteFileByIds(Long[] ids) {
        for (Long id : ids) {
            deleteFileById(id);
        }
        return ids.length;
    }

    @Override
    public boolean deleteFileFromStorage(SysFile file) {
        try {
            if ("minio".equals(file.getStorageType())) {
                if (file.getObjectName() != null) {
                    minioUtils.removeFile(file.getObjectName());
                }
            } else if ("local".equals(file.getStorageType())) {
                // 优先使用 localPath（绝对路径，最可靠）；缺失时回退到 filePath 解析
                String localPath = file.getLocalPath();
                if (localPath == null && file.getFilePath() != null) {
                    localPath = file.getFilePath();
                    if (localPath.startsWith(serverConfig.getUrl())) {
                        localPath = localPath.substring(serverConfig.getUrl().length());
                    }
                    if (localPath.startsWith("/profile")) {
                        localPath = RuoYiConfig.getProfile() + localPath.substring("/profile".length());
                    }
                }
                if (localPath != null) {
                    File localFile = new File(localPath);
                    if (localFile.exists()) {
                        localFile.delete();
                    }
                }
            }
            return true;
        } catch (Exception e) {
            throw new RuntimeException("删除文件失败", e);
        }
    }

    private LambdaQueryWrapper<SysFile> buildQueryWrapper(SysFile query) {
        LambdaQueryWrapper<SysFile> wrapper = new LambdaQueryWrapper<>();
        if (query != null) {
            if (query.getFileName() != null) {
                wrapper.like(SysFile::getFileName, query.getFileName());
            }
            if (query.getFileType() != null) {
                wrapper.eq(SysFile::getFileType, query.getFileType());
            }
            if (query.getStorageType() != null) {
                wrapper.eq(SysFile::getStorageType, query.getStorageType());
            }
            if (query.getBusinessType() != null) {
                wrapper.eq(SysFile::getBusinessType, query.getBusinessType());
            }
            if (query.getBusinessId() != null) {
                wrapper.eq(SysFile::getBusinessId, query.getBusinessId());
            }
            if (query.getStatus() != null) {
                wrapper.eq(SysFile::getStatus, query.getStatus());
            }
        }
        wrapper.orderByDesc(SysFile::getCreateTime);
        return wrapper;
    }

    private String getFileExt(String fileName) {
        if (fileName != null && fileName.contains(".")) {
            return fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
        }
        return "";
    }

    private String getFileType(String ext) {
        String[] imageExts = {"jpg", "jpeg", "png", "gif", "bmp", "webp", "svg"};
        String[] documentExts = {"doc", "docx", "xls", "xlsx", "ppt", "pptx", "pdf", "txt"};
        String[] videoExts = {"mp4", "avi", "mov", "wmv", "flv", "mkv"};
        String[] audioExts = {"mp3", "wav", "wma", "ogg", "aac"};

        if (Arrays.asList(imageExts).contains(ext)) {
            return "image";
        } else if (Arrays.asList(documentExts).contains(ext)) {
            return "document";
        } else if (Arrays.asList(videoExts).contains(ext)) {
            return "video";
        } else if (Arrays.asList(audioExts).contains(ext)) {
            return "audio";
        }
        return "other";
    }

    private String generateLocalFileName(String originalFileName) {
        String suffix = "";
        if (originalFileName != null && originalFileName.contains(".")) {
            suffix = originalFileName.substring(originalFileName.lastIndexOf("."));
        }
        String datePath = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        return datePath + "/" + UUID.randomUUID().toString().replace("-", "") + suffix;
    }

    private LocalUploadResult uploadToLocal(MultipartFile file) throws IOException {
        String fileName = generateLocalFileName(file.getOriginalFilename());
        String absolutePath = RuoYiConfig.getProfile() + File.separator + fileName;
        File destFile = new File(absolutePath);
        if (!destFile.getParentFile().exists()) {
            destFile.getParentFile().mkdirs();
        }
        file.transferTo(destFile);
        String url = serverConfig.getUrl() + "/profile/" + fileName;
        return new LocalUploadResult(url, absolutePath);
    }

    private LocalUploadResult uploadBytesToLocal(byte[] bytes, String originalFileName) throws IOException {
        String fileName = generateLocalFileName(originalFileName);
        String absolutePath = RuoYiConfig.getProfile() + File.separator + fileName;
        File destFile = new File(absolutePath);
        if (!destFile.getParentFile().exists()) {
            destFile.getParentFile().mkdirs();
        }
        java.nio.file.Files.write(destFile.toPath(), bytes);
        String url = serverConfig.getUrl() + "/profile/" + fileName;
        return new LocalUploadResult(url, absolutePath);
    }

    /** 本地上传结果：访问 URL + 绝对路径 */
    private static class LocalUploadResult {
        final String url;
        final String absolutePath;
        LocalUploadResult(String url, String absolutePath) {
            this.url = url;
            this.absolutePath = absolutePath;
        }
    }

    private String extractObjectNameFromUrl(String url) {
        String bucketName = minioConfig.getBucketName();
        int index = url.indexOf(bucketName + "/");
        if (index != -1) {
            return url.substring(index + bucketName.length() + 1);
        }
        return null;
    }
}
