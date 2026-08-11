package com.moyun.core.base;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Entity基类
 *
 * @author ruoyi
 */
@Data
public class BaseEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 排序字段白名单正则：仅允许字母开头，含字母数字下划点和表别名前缀（如 pa.create_time） */
    private static final Pattern ORDER_BY_PATTERN = Pattern.compile("^[a-zA-Z][a-zA-Z0-9_.]{0,63}$");
    /** 排序方向白名单：仅允许 asc/desc（不区分大小写） */
    private static final Pattern IS_ASC_PATTERN = Pattern.compile("^(?i)(asc|desc)$");

    /**
     * 搜索值
     */
    @JsonIgnore
    @TableField(exist = false)
    private String searchValue;

    /**
     * 创建者
     */
    private String createBy;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /**
     * 更新者
     */
    private String updateBy;

    /**
     * 更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    /**
     * 备注
     */
    private String remark;

    /**
     * 删除标记（0=存在 2=删除）
     * <p>
     * 与 sys_user / sys_dept / sys_role / portal_user 已有的 del_flag 字段保持一致。
     * MyBatis-Plus 自动处理：
     * - SELECT 自动追加 WHERE del_flag = '0'
     * - deleteById / deleteBatchIds 自动转为 UPDATE SET del_flag = '2'
     * <p>
     * 注意：仅对继承 BaseEntity 的实体生效；关联表（PortalLike / PortalBookmark 等）
     * 不继承 BaseEntity，保持物理删除（toggle 语义：取消点赞 = 删除关联记录）。
     */
    @TableLogic
    @TableField(value = "del_flag")
    private String delFlag;

    /**
     * 请求参数
     */
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @TableField(exist = false)
    private Map<String, Object> params;

    public String getSearchValue() {
        return searchValue;
    }

    public String getCreateBy() {
        return createBy;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public String getUpdateBy() {
        return updateBy;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public String getRemark() {
        return remark;
    }

    public Map<String, Object> getParams() {
        if (params == null) {
            params = new HashMap<>();
        }
        // 安全校验：拦截 orderByColumn / isAsc 的 SQL 注入
        sanitizeOrderByParam("orderByColumn");
        sanitizeOrderByParam("isAsc");
        return params;
    }

    /**
     * 校验 params 中的排序参数，不合法直接移除（防止 ${} 拼接注入）
     */
    private void sanitizeOrderByParam(String key) {
        Object val = params.get(key);
        if (val == null) {
            return;
        }
        String strVal = String.valueOf(val).trim();
        boolean valid;
        if ("isAsc".equals(key)) {
            valid = IS_ASC_PATTERN.matcher(strVal).matches();
        } else {
            valid = ORDER_BY_PATTERN.matcher(strVal).matches();
        }
        if (!valid) {
            params.remove(key);
        }
    }

}
