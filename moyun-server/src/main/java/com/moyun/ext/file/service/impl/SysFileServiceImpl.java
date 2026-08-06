package com.moyun.ext.file.service.impl;


import cn.hutool.crypto.digest.DigestUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moyun.common.exception.system.ServiceException;
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
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
public class SysFileServiceImpl implements ISysFileService {

    /**
     * 允许上传的文件扩展名白名单（小写）
     * <p>
     * 安全策略：uploadFile 在写入存储前先校验扩展名是否在此集合内，
     * 不在白名单的扩展名（如 .exe / .sh / .jsp / .php 等）直接拒绝，规避可执行文件上传与 webshell 风险。
     * </p>
     */
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            // 图片
            "jpg", "jpeg", "png", "gif", "webp", "bmp",
            // 文档
            "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt",
            // 音视频
            "mp4", "mp3", "wav",
            // 压缩
            "zip", "rar"
    );

    @Autowired
    private SysFileMapper sysFileMapper;

    @Autowired
    private MinioUtils minioUtils;

    @Autowired
    private MinioConfig minioConfig;

    @Autowired
    private ServerConfig serverConfig;

    /**
     * v1.1.2 新增：系统配置 Service（用于读取 sys_config 中的存储模式配置）
     * sys_config 优先级 > yaml：若 sys_config 中存在 file.storage.mode 则优先采用，
     * 否则回退到 yaml 的 minio.enabled / fallbackToLocal。
     */
    @Autowired
    private com.moyun.system.service.ISysConfigService sysConfigService;

    /**
     * sys_config 键名常量
     */
    private static final String CONFIG_KEY_STORAGE_MODE = "file.storage.mode";
    private static final String CONFIG_KEY_LOCAL_PATH = "file.storage.local.path";
    private static final String CONFIG_KEY_ACCESS_URL = "file.storage.access.url";

    /**
     * 读取 sys_config 中配置的存储模式（minio / local / auto）。
     * 留空或不存在视为 auto：按 MinIO 可达性自动决定（兼容旧行为）。
     */
    private String resolveStorageMode() {
        try {
            String mode = sysConfigService.selectConfigByKey(CONFIG_KEY_STORAGE_MODE);
            if (mode == null || mode.trim().isEmpty()) {
                return "auto";
            }
            return mode.trim().toLowerCase();
        } catch (Exception e) {
            // 读取 sys_config 失败（如未初始化）时回退到 auto，避免阻塞上传
            log.warn("[文件存储] 读取 sys_config[{}] 失败，回退到 auto 模式：{}", CONFIG_KEY_STORAGE_MODE, e.getMessage());
            return "auto";
        }
    }

    /**
     * 读取本地存储根路径。优先 sys_config[local.path]，留空回退 RuoYiConfig.getProfile()。
     */
    private String resolveLocalRootPath() {
        try {
            String path = sysConfigService.selectConfigByKey(CONFIG_KEY_LOCAL_PATH);
            if (path != null && !path.trim().isEmpty()) {
                return path.trim();
            }
        } catch (Exception e) {
            log.warn("[文件存储] 读取 sys_config[{}] 失败，回退到 RuoYiConfig.getProfile()：{}",
                    CONFIG_KEY_LOCAL_PATH, e.getMessage());
        }
        return RuoYiConfig.getProfile();
    }

    /**
     * 读取本地文件对外访问 URL 前缀。优先 sys_config[access.url]，留空回退 serverConfig.getUrl()。
     */
    private String resolveAccessUrlPrefix() {
        try {
            String url = sysConfigService.selectConfigByKey(CONFIG_KEY_ACCESS_URL);
            if (url != null && !url.trim().isEmpty()) {
                return url.trim();
            }
        } catch (Exception e) {
            log.warn("[文件存储] 读取 sys_config[{}] 失败，回退到 serverConfig.getUrl()：{}",
                    CONFIG_KEY_ACCESS_URL, e.getMessage());
        }
        return serverConfig.getUrl();
    }

    /**
     * 综合判断是否走 MinIO：
     *   - sys_config[file.storage.mode] = "local" → 强制本地（不探测 MinIO 可达性）
     *   - sys_config[file.storage.mode] = "minio" → 强制 MinIO（除非 yaml 未启用 minio.enabled）
     *   - sys_config[file.storage.mode] = "auto" 或不存在 → 探测可达性，不可达走本地
     * 同时尊重 yaml 的 minio.fallbackToLocal=true 手动降级（与 sys_config=local 同义）。
     */
    private boolean resolveUseMinio() {
        // yaml 未启用 MinIO：直接走本地
        if (!Boolean.TRUE.equals(minioConfig.getEnabled())) {
            return false;
        }
        // yaml 手动强制降级：直接走本地
        if (Boolean.TRUE.equals(minioConfig.getFallbackToLocal())) {
            return false;
        }
        String mode = resolveStorageMode();
        switch (mode) {
            case "local":
                return false;
            case "minio":
                // 强制走 MinIO，但若 autoFallback=true 则上传失败仍可降级
                return true;
            case "auto":
            default:
                // 自动模式：探测可达性，不可达走本地
                return minioUtils.isAvailable();
        }
    }

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
    @Transactional(rollbackFor = Exception.class)
    public SysFile uploadFile(MultipartFile file, String businessType, String businessId) {
        return uploadFile(file, businessType, businessId, false);
    }

    /**
     * 上传文件（支持前台用户）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysFile uploadFileForPortal(MultipartFile file, String businessType, String businessId) {
        return uploadFile(file, businessType, businessId, true);
    }

    private SysFile uploadFile(MultipartFile file, String businessType, String businessId, boolean isPortalUser) {
        SysFile sysFile = new SysFile();
        try {
            String fileName = file.getOriginalFilename();
            String fileExt = getFileExt(fileName);

            // 文件类型白名单校验：不在白名单内的扩展名直接拒绝，防止上传可执行文件 / webshell
            if (fileExt == null || fileExt.isEmpty() || !ALLOWED_EXTENSIONS.contains(fileExt)) {
                throw new ServiceException("不支持的文件类型");
            }

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

            // MinIO 可用性判断（v1.1.2 重构）：
            //   - sys_config[file.storage.mode] 优先（local/minio/auto），可在后台界面切换无需重启
            //   - yaml 的 minio.enabled / minio.fallbackToLocal 作为兜底
            //   - mode=auto 时探测 isAvailable()，避免上传阶段才发现 MinIO 不可用
            boolean useMinio = resolveUseMinio();

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
    @Transactional(rollbackFor = Exception.class)
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

            boolean useMinio = resolveUseMinio();

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
    @Transactional(rollbackFor = Exception.class)
    public int deleteFileById(Long id) {
        SysFile file = selectFileById(id);
        if (file != null) {
            deleteFileFromStorage(file);
            return sysFileMapper.deleteById(id);
        }
        return 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
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
                    // v1.1.2：URL 前缀可能来自 sys_config[access.url]，兼容历史 serverConfig.getUrl() 写入的记录
                    String accessUrlPrefix = resolveAccessUrlPrefix();
                    if (localPath.startsWith(accessUrlPrefix)) {
                        localPath = localPath.substring(accessUrlPrefix.length());
                    } else if (localPath.startsWith(serverConfig.getUrl())) {
                        localPath = localPath.substring(serverConfig.getUrl().length());
                    }
                    if (localPath.startsWith("/profile")) {
                        localPath = resolveLocalRootPath() + localPath.substring("/profile".length());
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

    /**
     * 按 fileUrl 删除文件（存储 + DB 记录）。
     * 兼容前端组件只持有访问 URL 的场景：上传后组件存的是 url，删除时只有 url 可用。
     * 校验逻辑：expectUploadUserId 非空时，必须与记录 uploadUserId 一致，防止越权删他人文件。
     * 未找到记录返回 false（不抛异常），便于前端幂等调用（重复删除静默成功）。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteFileByUrl(String fileUrl, Long expectUploadUserId) {
        if (fileUrl == null || fileUrl.trim().isEmpty()) {
            return false;
        }
        SysFile file = sysFileMapper.selectOne(new LambdaQueryWrapper<SysFile>()
                .eq(SysFile::getFileUrl, fileUrl)
                .last("LIMIT 1"));
        if (file == null) {
            // 兼容：前端传相对路径时尝试 strip 域名再查
            String stripped = fileUrl;
            int idx = stripped.indexOf("://");
            if (idx > 0) {
                int slash = stripped.indexOf("/", idx + 3);
                if (slash > 0) {
                    stripped = stripped.substring(slash);
                }
            }
            if (!stripped.equals(fileUrl)) {
                file = sysFileMapper.selectOne(new LambdaQueryWrapper<SysFile>()
                        .eq(SysFile::getFileUrl, stripped)
                        .last("LIMIT 1"));
            }
        }
        if (file == null) {
            return false;
        }
        if (expectUploadUserId != null && file.getUploadUserId() != null
                && !expectUploadUserId.equals(file.getUploadUserId())) {
            throw new RuntimeException("无权删除：文件不属于当前用户");
        }
        deleteFileFromStorage(file);
        return sysFileMapper.deleteById(file.getId()) > 0;
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
        // v1.1.2：本地存储根路径与访问 URL 前缀均改为后台 sys_config 可配
        String localRoot = resolveLocalRootPath();
        String accessUrlPrefix = resolveAccessUrlPrefix();
        String absolutePath = localRoot + File.separator + fileName;
        File destFile = new File(absolutePath);
        if (!destFile.getParentFile().exists()) {
            destFile.getParentFile().mkdirs();
        }
        file.transferTo(destFile);
        // 兼容：accessUrlPrefix 为后端服务地址时走 /profile/** 静态映射（ResourcesConfig 注册）；
        //       配置为 CDN/独立文件服务器时，需保证该前缀能直接访问到本地文件
        String url = accessUrlPrefix + "/profile/" + fileName;
        return new LocalUploadResult(url, absolutePath);
    }

    private LocalUploadResult uploadBytesToLocal(byte[] bytes, String originalFileName) throws IOException {
        String fileName = generateLocalFileName(originalFileName);
        String localRoot = resolveLocalRootPath();
        String accessUrlPrefix = resolveAccessUrlPrefix();
        String absolutePath = localRoot + File.separator + fileName;
        File destFile = new File(absolutePath);
        if (!destFile.getParentFile().exists()) {
            destFile.getParentFile().mkdirs();
        }
        java.nio.file.Files.write(destFile.toPath(), bytes);
        String url = accessUrlPrefix + "/profile/" + fileName;
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
        // v1.1.2 修复：用 lastIndexOf 兼容 accessUrl 已含 bucket 名的旧配置
        // 当 accessUrl 配置成 http://host/moyun（已含 bucket），URL 会是 http://host/moyun/moyun/...
        // 用 indexOf 会切到第一个 moyun/，得到错误 objectName=moyun/2026/...
        // 用 lastIndexOf 能定位最后一个 moyun/，正确提取 fileName=2026/...
        // 安全性：fileName 是 yyyy/MM/dd/UUID.png 格式，不会含 bucketName
        int index = url.lastIndexOf(bucketName + "/");
        if (index != -1) {
            return url.substring(index + bucketName.length() + 1);
        }
        return null;
    }
}
