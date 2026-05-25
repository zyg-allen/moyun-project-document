package com.moyun.system.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 用户与岗位关联表 sys_user_post
 *
 * @author ruoyi
 */
@Data
@TableName("sys_user_post")
public class SysUserPost implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 用户ID */
    private Long userId;

    /** 岗位ID */
    private Long postId;
}
