package com.moyun.portal.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 首页非核心模块VO集合
 * 每个模块独立缓存，独立接口返回
 *
 * @author moyun
 */
public class HomeModuleVO {

    /**
     * 分类简化VO
     */
    @Data
    @Schema(description = "分类简化VO")
    public static class CategorySimpleVO implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        @Schema(description = "分类ID")
        private Long id;

        @Schema(description = "分类名称")
        private String name;

        @Schema(description = "分类别名")
        private String slug;

        @Schema(description = "分类图标")
        private String icon;
    }

    /**
     * 标签简化VO
     */
    @Data
    @Schema(description = "标签简化VO")
    public static class TagSimpleVO implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        @Schema(description = "标签ID")
        private Long id;

        @Schema(description = "标签名称")
        private String name;

        @Schema(description = "标签别名")
        private String slug;
    }

    /**
     * 友情链接简化VO
     */
    @Data
    @Schema(description = "友情链接简化VO")
    public static class FriendLinkSimpleVO implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        @Schema(description = "链接ID")
        private Long id;

        @Schema(description = "链接名称")
        private String name;

        @Schema(description = "链接地址")
        private String url;

        @Schema(description = "链接Logo")
        private String logo;
    }

    /**
     * 作者简化VO
     */
    @Data
    @Schema(description = "作者简化VO")
    public static class AuthorSimpleVO implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        @Schema(description = "作者ID")
        private Long id;

        @Schema(description = "作者用户名")
        private String username;

        @Schema(description = "作者昵称")
        private String nickname;

        @Schema(description = "作者头像")
        private String avatar;

        @Schema(description = "作者简介")
        private String bio;

        @Schema(description = "文章数量")
        private Integer articleCount = 0;

        @Schema(description = "粉丝数量")
        private Integer followerCount = 0;
    }

    /**
     * 分类推荐文章VO
     */
    @Data
    @Schema(description = "分类推荐文章VO")
    public static class CategoryArticlesVO implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        @Schema(description = "分类ID")
        private Long categoryId;

        @Schema(description = "分类名称")
        private String categoryName;

        @Schema(description = "文章列表")
        private List<ArticleVO> articles;
    }
}
