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
}
