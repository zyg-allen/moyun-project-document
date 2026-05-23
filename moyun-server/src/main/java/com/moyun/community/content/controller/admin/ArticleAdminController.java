package com.moyun.community.content.controller.admin;

import com.moyun.community.content.entity.Article;
import com.moyun.community.content.model.dto.ArticlePublishDto;
import com.moyun.community.content.model.vo.ArticleListVo;
import com.moyun.community.content.service.ArticleService;
import com.moyun.common.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/article")
@RequiredArgsConstructor
public class ArticleAdminController {

    private final ArticleService articleService;

    @GetMapping("/page")
    @PreAuthorize("@ss.hasPermi('content:article:list')")
    public ResponseEntity<List<ArticleListVo>> getArticlePage(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String articleType,
            @RequestParam(required = false) Long categoryId) {
        return ResponseEntity.ok(articleService.getPendingArticles());
    }

    @GetMapping("/pending/page")
    @PreAuthorize("@ss.hasPermi('content:audit:list')")
    public ResponseEntity<List<ArticleListVo>> getPendingArticles() {
        return ResponseEntity.ok(articleService.getPendingArticles());
    }

    @PostMapping("/audit/approve")
    @PreAuthorize("@ss.hasPermi('content:audit:approve')")
    public ResponseEntity<Map<String, Boolean>> approveArticle(
            @RequestParam Long articleId,
            @RequestParam(required = false) String reason) {
        boolean result = articleService.approveArticle(articleId, SecurityUtils.getUserId(), reason);
        return ResponseEntity.ok(Map.of("success", result));
    }

    @PostMapping("/audit/reject")
    @PreAuthorize("@ss.hasPermi('content:audit:reject')")
    public ResponseEntity<Map<String, Boolean>> rejectArticle(
            @RequestParam Long articleId,
            @RequestParam(required = false) String reason) {
        boolean result = articleService.rejectArticle(articleId, SecurityUtils.getUserId(), reason);
        return ResponseEntity.ok(Map.of("success", result));
    }

    @PutMapping("/offline/{id}")
    @PreAuthorize("@ss.hasPermi('content:article:offline')")
    public ResponseEntity<Map<String, Boolean>> offlineArticle(@PathVariable Long id) {
        boolean result = articleService.rejectArticle(id, SecurityUtils.getUserId(), "管理员下架");
        return ResponseEntity.ok(Map.of("success", result));
    }

    @DeleteMapping("/delete/{id}")
    @PreAuthorize("@ss.hasPermi('content:article:delete')")
    public ResponseEntity<Map<String, Boolean>> deleteArticle(@PathVariable Long id) {
        boolean result = articleService.deleteArticle(id, SecurityUtils.getUserId());
        return ResponseEntity.ok(Map.of("success", result));
    }
}
