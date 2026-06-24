package com.moyun.portal.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.moyun.core.base.BaseEntity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 帮助中心文章对象 portal_help_article
 *
 * @author moyun
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("portal_help_article")
public class PortalHelpArticle extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /** 文章ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 分类ID */
    @NotNull(message = "分类ID不能为空")
    private Long categoryId;

    /** 问题标题 */
    @NotBlank(message = "问题标题不能为空")
    @Size(min = 0, max = 200, message = "问题标题长度不能超过200个字符")
    private String title;

    /** 答案内容 */
    @NotBlank(message = "答案内容不能为空")
    private String content;

    /** 查看次数 */
    private Integer viewCount;

    /** 点赞次数 */
    private Integer likeCount;

    /** 排序 */
    private Integer sort;

    /** 是否精选：0=否 1=是 */
    private Integer isFeatured;

    /** 状态：published/draft */
    @Size(min = 0, max = 20, message = "状态长度不能超过20个字符")
    private String status;

    public PortalHelpArticle() {}

    public PortalHelpArticle(Long id) {
        this.id = id;
    }
}
