package com.moyun.community.content.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class HomeVo implements Serializable {

    private static final long serialVersionUID = 1L;

    private List<BannerVo> banners;

    private List<CategoryVo> categories;

    private List<ArticleListVo> recommendArticles;

    private List<ArticleListVo> hotArticles;

    private List<ArticleListVo> latestArticles;

    @Data
    public static class BannerVo implements Serializable {
        private Long bannerId;
        private String title;
        private String imageUrl;
        private String jumpType;
        private String jumpValue;
    }

    @Data
    public static class CategoryVo implements Serializable {
        private Long categoryId;
        private String categoryName;
        private String categoryCode;
        private String categoryType;
        private String icon;
        private String coverImage;
    }
}
