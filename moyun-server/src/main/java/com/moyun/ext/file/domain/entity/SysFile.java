package com.moyun.ext.file.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.moyun.core.base.BaseEntity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_file")
public class SysFile extends BaseEntity {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    @NotBlank(message = "文件名称不能为空")
    @Size(min = 0, max = 500, message = "文件名称长度不能超过500个字符")
    private String fileName;

    @Size(min = 0, max = 100, message = "文件扩展名长度不能超过100个字符")
    private String fileExt;

    @Size(min = 0, max = 100, message = "文件类型长度不能超过100个字符")
    private String fileType;

    private Long fileSize;

    @NotBlank(message = "文件URL不能为空")
    @Size(min = 0, max = 1000, message = "文件URL长度不能超过1000个字符")
    private String fileUrl;

    @Size(min = 0, max = 500, message = "文件路径长度不能超过500个字符")
    private String filePath;

    @Size(min = 0, max = 100, message = "存储引擎长度不能超过100个字符")
    private String storageType;

    @Size(min = 0, max = 100, message = "存储桶名称长度不能超过100个字符")
    private String bucketName;

    @Size(min = 0, max = 100, message = "对象名称长度不能超过100个字符")
    private String objectName;

    /**
     * 是否降级存储：0=正常 1=因 MinIO 不可用降级到本地
     */
    private Integer fallback;

    /**
     * 本地备份绝对路径（MinIO 可用时也记录，便于降级访问或后续同步回 MinIO）
     */
    @Size(min = 0, max = 500, message = "本地备份路径长度不能超过500个字符")
    private String localPath;

    @Size(min = 0, max = 500, message = "文件MD5长度不能超过500个字符")
    private String fileMd5;

    private Long uploadUserId;

    @Size(min = 0, max = 100, message = "上传用户名长度不能超过100个字符")
    private String uploadUserName;

    @Size(min = 0, max = 20, message = "状态长度不能超过20个字符")
    private String status;

    @Size(min = 0, max = 500, message = "业务类型长度不能超过500个字符")
    private String businessType;

    @Size(min = 0, max = 100, message = "业务ID长度不能超过100个字符")
    private String businessId;
}
