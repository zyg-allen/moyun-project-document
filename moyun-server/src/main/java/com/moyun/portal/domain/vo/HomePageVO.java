package com.moyun.portal.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 首页核心文章数据聚合VO（核心模块：轮播、精选、热门、最新）
 *
 * @author moyun
 */
@Data
@Schema(description = "首页核心文章数据VO")
public class HomePageVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "轮播文章列表")
    private List<ArticleVO> carouselArticles;

    @Schema(description = "精选文章列表")
    private List<ArticleVO> featuredArticles;

    @Schema(description = "热门文章列表")
    private List<ArticleVO> hotArticles;

    @Schema(description = "最新文章列表")
    private List<ArticleVO> latestArticles;
}
