package com.moyun.portal.controller;

import com.moyun.common.annotation.Anonymous;
import com.moyun.portal.domain.entity.PortalArticle;
import com.moyun.portal.domain.entity.PortalCategory;
import com.moyun.portal.mapper.PortalArticleMapper;
import com.moyun.portal.mapper.PortalCategoryMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 网站地图控制器
 * 动态生成 sitemap.xml，供搜索引擎抓取
 *
 * @author moyun
 */
@Tag(name = "网站地图", description = "动态生成SEO网站地图")
@RestController
@RequestMapping("/portal/sitemap")
public class PortalSitemapController {

    @Value("${moyun.portal.domain:https://moyun.example.com}")
    private String domain;

    @Autowired
    private PortalArticleMapper portalArticleMapper;

    @Autowired
    private PortalCategoryMapper portalCategoryMapper;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Anonymous
    @Operation(summary = "获取网站地图", description = "动态生成 sitemap.xml，包含所有公开页面URL")
    @GetMapping(produces = "application/xml")
    public void generateSitemap(HttpServletResponse response) throws IOException {
        response.setContentType("application/xml;charset=UTF-8");
        response.setHeader("Cache-Control", "public, max-age=3600"); // 缓存1小时

        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");

        // 1. 首页
        addUrl(xml, "/", "1.0", "daily");

        // 2. 分类列表页
        addUrl(xml, "/category", "0.9", "daily");

        // 3. 标签列表页
        addUrl(xml, "/tag", "0.8", "weekly");

        // 4. 特殊页面
        addUrl(xml, "/reading", "0.8", "weekly");
        addUrl(xml, "/interview", "0.8", "weekly");

        // 5. 搜索页
        addUrl(xml, "/search", "0.7", "weekly");

        // 6. 作者列表页
        addUrl(xml, "/authors", "0.7", "weekly");

        // 7. 帮助与信息页
        addUrl(xml, "/help", "0.4", "monthly");
        addUrl(xml, "/about", "0.4", "monthly");
        addUrl(xml, "/agreement", "0.3", "monthly");

        // 8. 所有分类页面
        List<PortalCategory> categories = portalCategoryMapper.selectPortalCategoryList(null);
        if (categories != null) {
            for (PortalCategory category : categories) {
                if (category.getSlug() != null && !category.getSlug().isEmpty()) {
                    addUrl(xml, "/category/" + category.getSlug(), "0.8", "daily");
                }
            }
        }

        // 9. 所有已发布文章
        List<PortalArticle> articles = portalArticleMapper.selectPortalArticleList(null);
        if (articles != null) {
            for (PortalArticle article : articles) {
                if ("published".equals(article.getStatus())) {
                    String articleUrl = article.getSlug() != null && !article.getSlug().isEmpty()
                            ? "/article/" + article.getId() + "/" + article.getSlug()
                            : "/article/" + article.getId();
                    String lastmod = article.getUpdateTime() != null
                            ? article.getUpdateTime().format(DATE_FORMATTER)
                            : LocalDateTime.now().format(DATE_FORMATTER);
                    addUrl(xml, articleUrl, "0.6", "weekly", lastmod);
                }
            }
        }

        xml.append("</urlset>");

        PrintWriter writer = response.getWriter();
        writer.write(xml.toString());
        writer.flush();
    }

    private void addUrl(StringBuilder xml, String path, String priority, String changefreq) {
        addUrl(xml, path, priority, changefreq, LocalDateTime.now().format(DATE_FORMATTER));
    }

    private void addUrl(StringBuilder xml, String path, String priority, String changefreq, String lastmod) {
        xml.append("  <url>\n");
        xml.append("    <loc>").append(domain).append(path).append("</loc>\n");
        xml.append("    <lastmod>").append(lastmod).append("</lastmod>\n");
        xml.append("    <changefreq>").append(changefreq).append("</changefreq>\n");
        xml.append("    <priority>").append(priority).append("</priority>\n");
        xml.append("  </url>\n");
    }
}
