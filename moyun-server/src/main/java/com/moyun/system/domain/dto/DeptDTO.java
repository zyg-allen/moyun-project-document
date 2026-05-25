package com.moyun.system.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 部门数据传输对象
 *
 * @author ruoyi
 */
@Data
@Schema(description = "部门DTO")
public class DeptDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 部门ID */
    @Schema(description = "部门ID")
    private Long deptId;

    /** 父部门ID */
    @Schema(description = "父部门ID")
    private Long parentId;

    /** 部门名称 */
    @NotBlank(message = "部门名称不能为空")
    @Size(min = 0, max = 30, message = "部门名称长度不能超过30个字符")
    @Schema(description = "部门名称")
    private String deptName;

    /** 显示顺序 */
    @NotNull(message = "显示顺序不能为空")
    @Schema(description = "显示顺序")
    private Integer orderNum;

    /** 负责人 */
    @Schema(description = "负责人")
    private String leader;

    /** 联系电话 */
    @Size(min = 0, max = 11, message = "联系电话长度不能超过11个字符")
    @Schema(description = "联系电话")
    private String phone;

    /** 邮箱 */
    @Email(message = "邮箱格式不正确")
    @Size(min = 0, max = 50, message = "邮箱长度不能超过50个字符")
    @Schema(description = "邮箱")
    private String email;

    /** 部门状态（0正常 1停用） */
    @Schema(description = "部门状态")
    private String status;

    /** 备注 */
    @Schema(description = "备注")
    private String remark;

    /** 子部门 */
    @Schema(description = "子部门")
    private List<DeptDTO> children;
}
