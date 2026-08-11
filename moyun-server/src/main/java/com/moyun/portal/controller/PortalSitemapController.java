package com.moyun.portal.controller;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.moyun.common.annotation.Anonymous;
import com.moyun.portal.domain.entity.PortalArticle;
import com.moyun.portal.domain.entity.PortalBook;
import com.moyun.portal.domain.entity.PortalBookList;
import com.moyun.portal.domain.entity.PortalCategory;
import com.moyun.portal.domain.entity.PortalColumn;
import com.moyun.portal.domain.entity.PortalInterviewExperience;
import com.moyun.portal.domain.entity.PortalTopic;
import com.moyun.portal.mapper.PortalArticleMapper;
import com.moyun.portal.mapper.PortalBookListMapper;
import com.moyun.portal.mapper.PortalBookMapper;
import com.moyun.portal.mapper.PortalCategoryMapper;
import com.moyun.portal.mapper.PortalColumnMapper;
import com.moyun.portal.mapper.PortalInterviewExperienceMapper;
import com.moyun.portal.mapper.PortalTopicMapper;

/**
 * 网站地图控制器
 * 动态生成 sitemap.xml，供搜索引擎抓取
 *
 * <p>覆盖范围：
 * <ul>
 *   <li>固定页面：首页 / 分类列表 / 标签列表 / 读书空间 / 面试指南 / 话题列表 / 专栏列表 / 搜索 / 作者 / 帮助页</li>
 *   <li>动态页面：所有已发布文章 / 公开书籍 / 公开书单 / 活跃话题 / 已发布专栏 / 已发布面经</li>
 * </ul>
 *
 * <p>站点域名通过 {@code moyun.portal.domain} 配置项注入，默认占位 {@code https://moyun.example.com}，
 * 生产环境必须通过环境变量 {@code PORTAL_DOMAIN} 覆盖。
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

    @Autowired
    private PortalBookMapper portalBookMapper;

    @Autowired
    private PortalBookListMapper portalBookListMapper;

    @Autowired
    private PortalTopicMapper portalTopicMapper;

    @Autowired
    private PortalColumnMapper portalColumnMapper;

    @Autowired
    private PortalInterviewExperienceMapper portalInterviewExperienceMapper;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * sitemap 协议单文件 URL 数量上限为 50,000，预留 1 个余量用于安全边界
     */
    private static final int MAX_URLS = 49999;

    @Anonymous
    @Operation(summary = "获取网站地图", description = "动态生成 sitemap.xml，包含所有公开页面URL")
    @GetMapping(produces = "application/xml")
    public void generateSitemap(HttpServletResponse response) throws IOException {
        response.setContentType("application/xml;charset=UTF-8");
        response.setHeader("Cache-Control", "public, max-age=3600"); // 缓存1小时

        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");

        int[] counter = {0};

        // 1. 首页
        addUrl(xml, counter, "/", "1.0", "daily");

        // 2. 分类列表页
        addUrl(xml, counter, "/category", "0.9", "daily");

        // 3. 标签列表页
        addUrl(xml, counter, "/tag", "0.8", "weekly");

        // 4. 特殊页面
        addUrl(xml, counter, "/reading", "0.8", "weekly");
        addUrl(xml, counter, "/interview", "0.8", "weekly");

        // 5. 话题 / 专栏 列表页
        addUrl(xml, counter, "/topics", "0.8", "daily");
        addUrl(xml, counter, "/columns", "0.8", "daily");

        // 6. 搜索页
        addUrl(xml, counter, "/search", "0.7", "weekly");

        // 7. 作者列表页
        addUrl(xml, counter, "/authors", "0.7", "weekly");

        // 8. 帮助与信息页
        addUrl(xml, counter, "/help", "0.4", "monthly");
        addUrl(xml, counter, "/about", "0.4", "monthly");
        addUrl(xml, counter, "/agreement", "0.3", "monthly");

        // 9. 所有分类页面（按 slug）
        appendCategories(xml, counter);

        // 10. 所有已发布文章
        appendArticles(xml, counter);

        // 11. 公开书籍（active 状态）
        appendBooks(xml, counter);

        // 12. 公开书单（active 状态且 isPublic=true）
        appendBookLists(xml, counter);

        // 13. 活跃话题
        appendTopics(xml, counter);

        // 14. 已发布专栏
        appendColumns(xml, counter);

        // 15. 已发布面经
        appendInterviewExperiences(xml, counter);

        xml.append("</urlset>");

        PrintWriter writer = response.getWriter();
        writer.write(xml.toString());
        writer.flush();
    }

    private void appendCategories(StringBuilder xml, int[] counter) {
        List<PortalCategory> categories = portalCategoryMapper.selectPortalCategoryList(null);
        if (categories == null) {
            return;
        }
        for (PortalCategory category : categories) {
            if (counter[0] >= MAX_URLS) {
                return;
            }
            if (category.getSlug() != null && !category.getSlug().isEmpty()
                    && "0".equals(category.getStatus())) {
                addUrl(xml, counter, "/category/" + category.getSlug(), "0.8", "daily");
            }
        }
    }

    private void appendArticles(StringBuilder xml, int[] counter) {
        List<PortalArticle> articles = portalArticleMapper.selectPortalArticleList(null);
        if (articles == null) {
            return;
        }
        for (PortalArticle article : articles) {
            if (counter[0] >= MAX_URLS) {
                return;
            }
            if (!"published".equals(article.getStatus())) {
                continue;
            }
            String articleUrl = article.getSlug() != null && !article.getSlug().isEmpty()
                    ? "/article/" + article.getId() + "/" + article.getSlug()
                    : "/article/" + article.getId();
            String lastmod = formatLocalDateTime(article.getUpdateTime());
            addUrl(xml, counter, articleUrl, "0.6", "weekly", lastmod);
        }
    }

    private void appendBooks(StringBuilder xml, int[] counter) {
        List<PortalBook> books = portalBookMapper.selectPortalBookList(null);
        if (books == null) {
            return;
        }
        for (PortalBook book : books) {
            if (counter[0] >= MAX_URLS) {
                return;
            }
            if (!"active".equals(book.getStatus())) {
                continue;
            }
            String lastmod = formatDate(book.getLastUpdateTime());
            addUrl(xml, counter, "/reading/book/" + book.getId(), "0.7", "weekly", lastmod);
        }
    }

    private void appendBookLists(StringBuilder xml, int[] counter) {
        List<PortalBookList> bookLists = portalBookListMapper.selectPortalBookList(null);
        if (bookLists == null) {
            return;
        }
        for (PortalBookList bookList : bookLists) {
            if (counter[0] >= MAX_URLS) {
                return;
            }
            // 仅收录公开且 active 的书单
            if (!"active".equals(bookList.getStatus())) {
                continue;
            }
            if (bookList.getIsPublic() == null || !bookList.getIsPublic()) {
                continue;
            }
            String lastmod = formatLocalDateTime(bookList.getUpdateTime());
            addUrl(xml, counter, "/reading/book-list/" + bookList.getId(), "0.6", "weekly", lastmod);
        }
    }

    private void appendTopics(StringBuilder xml, int[] counter) {
        // PortalTopicMapper 无 selectList 自定义方法，使用 BaseMapper + LambdaQueryWrapper
        LambdaQueryWrapper<PortalTopic> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PortalTopic::getStatus, "active")
                .orderByDesc(PortalTopic::getUpdatedTime);
        List<PortalTopic> topics = portalTopicMapper.selectList(wrapper);
        if (topics == null) {
            return;
        }
        for (PortalTopic topic : topics) {
            if (counter[0] >= MAX_URLS) {
                return;
            }
            String lastmod = formatLocalDateTime(topic.getUpdatedTime());
            addUrl(xml, counter, "/topic/" + topic.getId(), "0.6", "weekly", lastmod);
        }
    }

    private void appendColumns(StringBuilder xml, int[] counter) {
        LambdaQueryWrapper<PortalColumn> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PortalColumn::getStatus, "published")
                .orderByDesc(PortalColumn::getUpdatedTime);
        List<PortalColumn> columns = portalColumnMapper.selectList(wrapper);
        if (columns == null) {
            return;
        }
        for (PortalColumn column : columns) {
            if (counter[0] >= MAX_URLS) {
                return;
            }
            String lastmod = formatLocalDateTime(column.getUpdatedTime());
            addUrl(xml, counter, "/column/" + column.getId(), "0.6", "weekly", lastmod);
        }
    }

    private void appendInterviewExperiences(StringBuilder xml, int[] counter) {
        LambdaQueryWrapper<PortalInterviewExperience> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PortalInterviewExperience::getStatus, "published")
                .orderByDesc(PortalInterviewExperience::getUpdateTime);
        List<PortalInterviewExperience> experiences = portalInterviewExperienceMapper.selectList(wrapper);
        if (experiences == null) {
            return;
        }
        for (PortalInterviewExperience experience : experiences) {
            if (counter[0] >= MAX_URLS) {
                return;
            }
            String lastmod = formatLocalDateTime(experience.getUpdateTime());
            addUrl(xml, counter, "/interview/experience/" + experience.getId(), "0.6", "weekly", lastmod);
        }
    }

    private void addUrl(StringBuilder xml, int[] counter, String path, String priority, String changefreq) {
        addUrl(xml, counter, path, priority, changefreq, LocalDateTime.now().format(DATE_FORMATTER));
    }

    private void addUrl(StringBuilder xml, int[] counter, String path, String priority, String changefreq, String lastmod) {
        if (counter[0] >= MAX_URLS) {
            return;
        }
        xml.append("  <url>\n");
        xml.append("    <loc>").append(domain).append(path).append("</loc>\n");
        xml.append("    <lastmod>").append(lastmod).append("</lastmod>\n");
        xml.append("    <changefreq>").append(changefreq).append("</changefreq>\n");
        xml.append("    <priority>").append(priority).append("</priority>\n");
        xml.append("  </url>\n");
        counter[0]++;
    }

    /**
     * 格式化 LocalDateTime，null 时回退到当前日期
     */
    private String formatLocalDateTime(LocalDateTime time) {
        return time != null ? time.format(DATE_FORMATTER) : LocalDateTime.now().format(DATE_FORMATTER);
    }

    /**
     * 格式化 java.util.Date（PortalBook.lastUpdateTime 等字段），null 时回退到当前日期
     */
    private String formatDate(Date date) {
        if (date == null) {
            return LocalDateTime.now().format(DATE_FORMATTER);
        }
        return date.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate().format(DATE_FORMATTER);
    }
}
