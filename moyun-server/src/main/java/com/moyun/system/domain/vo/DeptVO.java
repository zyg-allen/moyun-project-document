package com.moyun.system.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 部门视图对象
 *
 * @author ruoyi
 */
@Data
@Schema(description = "部门VO")
public class DeptVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 部门ID */
    @Schema(description = "部门ID")
    private Long deptId;

    /** 父部门ID */
    @Schema(description = "父部门ID")
    private Long parentId;

    /** 祖级列表 */
    @Schema(description = "祖级列表")
    private String ancestors;

    /** 部门名称 */
    @Schema(description = "部门名称")
    private String deptName;

    /** 显示顺序 */
    @Schema(description = "显示顺序")
    private Integer orderNum;

    /** 负责人 */
    @Schema(description = "负责人")
    private String leader;

    /** 联系电话 */
    @Schema(description = "联系电话")
    private String phone;

    /** 邮箱 */
    @Schema(description = "邮箱")
    private String email;

    /** 部门状态（0正常 1停用） */
    @Schema(description = "部门状态")
    private String status;

    /** 父部门名称 */
    @Schema(description = "父部门名称")
    private String parentName;

    /** 子部门 */
    @Schema(description = "子部门")
    private List<DeptVO> children;

    /** 创建时间 */
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    /** 更新时间 */
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
