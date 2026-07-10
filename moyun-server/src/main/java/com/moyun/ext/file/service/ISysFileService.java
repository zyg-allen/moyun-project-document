package com.moyun.ext.file.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moyun.ext.file.domain.entity.SysFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ISysFileService {

    Page<SysFile> selectFilePage(Page<SysFile> page, SysFile query);

    List<SysFile> selectFileList(SysFile query);

    SysFile selectFileById(Long id);

    SysFile uploadFile(MultipartFile file, String businessType, String businessId);

    SysFile uploadFileForPortal(MultipartFile file, String businessType, String businessId);

    SysFile uploadBytes(byte[] bytes, String fileName, String contentType, String businessType, String businessId);

    int deleteFileById(Long id);

    int deleteFileByIds(Long[] ids);

    boolean deleteFileFromStorage(SysFile file);

    /**
     * 按 fileUrl 删除文件（存储 + DB 记录）。
     * 用于前端组件删除/替换附件时清理：组件只持有访问 URL，无 fileId。
     * 可选校验上传者本人，防止越权删除他人文件。
     *
     * @param fileUrl         文件访问 URL
     * @param expectUploadUserId 期望的上传者ID（非空时校验，为空则不校验）
     * @return 是否删除成功（记录不存在视为未删除返回 false）
     */
    boolean deleteFileByUrl(String fileUrl, Long expectUploadUserId);
}
