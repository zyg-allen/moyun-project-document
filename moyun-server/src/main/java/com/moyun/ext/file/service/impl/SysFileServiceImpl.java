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
            
            if (isPortalUser) {
                sysFile.setUploadUserId(PortalSecurityUtils.getUserId());
                sysFile.setUploadUserName(PortalSecurityUtils.getUsername());
            } else {
                sysFile.setUploadUserId(SecurityUtils.getUserId());
                sysFile.setUploadUserName(SecurityUtils.getUsername());
            }

            String fileUrl;
            if (minioUtils.isEnabled()) {
                fileUrl = minioUtils.uploadFile(file);
                sysFile.setStorageType("minio");
                sysFile.setBucketName(minioConfig.getBucketName());
                sysFile.setObjectName(extractObjectNameFromUrl(fileUrl));
                sysFile.setFilePath(fileUrl);
            } else {
                fileUrl = uploadToLocal(file);
                sysFile.setStorageType("local");
                sysFile.setFilePath(fileUrl);
            }
            sysFile.setFileUrl(fileUrl);

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
            sysFile.setUploadUserId(SecurityUtils.getUserId());
            sysFile.setUploadUserName(SecurityUtils.getUsername());

            String fileUrl;
            if (minioUtils.isEnabled()) {
                fileUrl = minioUtils.uploadBytes(bytes, contentType, fileExt);
                sysFile.setStorageType("minio");
                sysFile.setBucketName(minioConfig.getBucketName());
                sysFile.setObjectName(extractObjectNameFromUrl(fileUrl));
                sysFile.setFilePath(fileUrl);
            } else {
                fileUrl = uploadBytesToLocal(bytes, fileName);
                sysFile.setStorageType("local");
                sysFile.setFilePath(fileUrl);
            }
            sysFile.setFileUrl(fileUrl);

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
                if (file.getFilePath() != null) {
                    String localPath = file.getFilePath();
                    if (localPath.startsWith(serverConfig.getUrl())) {
                        localPath = localPath.substring(serverConfig.getUrl().length());
                    }
                    if (localPath.startsWith("/profile")) {
                        localPath = RuoYiConfig.getProfile() + localPath.substring("/profile".length());
                    }
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
        String suffix = originalFileName.substring(originalFileName.lastIndexOf("."));
        String datePath = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        return datePath + "/" + UUID.randomUUID().toString().replace("-", "") + suffix;
    }

    private String uploadToLocal(MultipartFile file) throws IOException {
        String fileName = generateLocalFileName(file.getOriginalFilename());
        String filePath = RuoYiConfig.getProfile() + File.separator + fileName;
        File destFile = new File(filePath);
        if (!destFile.getParentFile().exists()) {
            destFile.getParentFile().mkdirs();
        }
        file.transferTo(destFile);
        return serverConfig.getUrl() + "/profile/" + fileName;
    }

    private String uploadBytesToLocal(byte[] bytes, String originalFileName) throws IOException {
        String fileName = generateLocalFileName(originalFileName);
        String filePath = RuoYiConfig.getProfile() + File.separator + fileName;
        File destFile = new File(filePath);
        if (!destFile.getParentFile().exists()) {
            destFile.getParentFile().mkdirs();
        }
        java.nio.file.Files.write(destFile.toPath(), bytes);
        return serverConfig.getUrl() + "/profile/" + fileName;
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
