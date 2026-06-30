package com.moyun.portal.controller;

import java.util.List;
import java.util.concurrent.TimeUnit;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.moyun.common.annotation.Log;
import com.moyun.common.constant.HttpStatus;
import com.moyun.common.enums.BusinessType;
import com.moyun.core.base.AjaxResult;
import com.moyun.core.base.BaseController;
import com.moyun.core.config.redis.RedisCache;
import com.moyun.portal.domain.dto.PortalTagBindDTO;
import com.moyun.portal.domain.entity.PortalTag;
import com.moyun.portal.domain.query.TagQuery;
import com.moyun.portal.service.IPortalTagService;
import com.moyun.portal.util.PortalSecurityUtils;

/**
 * 门户标签 Controller
 * <p>
 * 清理说明：本类于本次重构中移除 8 个前端未调用的死接口
 * （list / unbindTags / getTagsByEntity / export / getInfo / add / edit / remove），
 * 仅保留前端实际在用的 5 个接口（getHotTags / searchTags / getRecommendTags / createTag / bindTags）。
 * 对应 Service / Mapper / XML 方法保持不变，仍可被其他 Controller（如 PortalArticleController）内部调用。
 */
@Slf4j
@Tag(name = "门户标签", description = "门户标签的增删改查操作接口")
@RestController
@RequestMapping("/portal/tag")
public class PortalTagController extends BaseController {

    /** 推荐标签 Redis 缓存键（结果与 title/category 无关，使用单一键） */
    private static final String RECOMMEND_CACHE_KEY = "portal:tag:recommend:top10";
    private static final int RECOMMEND_CACHE_MINUTES = 10;

    @Autowired
    private IPortalTagService portalTagService;

    @Autowired
    private RedisCache redisCache;

    @Operation(summary = "获取热门标签", description = "按引用次数排行获取热门标签，支持按 module 过滤")
    @GetMapping("/hot")
    public AjaxResult getHotTags(
            @RequestParam(required = false) String module,
            @RequestParam(defaultValue = "20") Integer limit) {
        return success(portalTagService.getHotTags(module, limit));
    }

    @Operation(summary = "搜索标签", description = "根据关键词搜索标签")
    @GetMapping("/search")
    public AjaxResult searchTags(@RequestParam String keyword) {
        TagQuery query = new TagQuery();
        query.setName(keyword);
        List<PortalTag> list = portalTagService.selectPortalTagList(query);
        return success(list);
    }

    @Operation(summary = "获取推荐标签", description = "根据标题和分类推荐标签，结果缓存 10 分钟")
    @GetMapping("/recommend")
    public AjaxResult getRecommendTags(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String category) {
        // 构建动态缓存键：根据查询条件组合（title/category 影响结果）
        String cacheKey = RECOMMEND_CACHE_KEY;
        if (StringUtils.hasText(category)) {
            cacheKey = cacheKey + ":cat:" + category;
        } else if (StringUtils.hasText(title)) {
            cacheKey = cacheKey + ":t:" + title.hashCode();
        }
        List<PortalTag> cached = redisCache.getCacheObject(cacheKey);
        if (cached != null) {
            return success(cached);
        }
        TagQuery query = new TagQuery();
        query.setStatus("0");
        // 标题模糊匹配标签名
        if (StringUtils.hasText(title)) {
            query.setName(title.trim());
        }
        // 分类关联：筛选该分类下文章使用过的标签
        if (StringUtils.hasText(category)) {
            try {
                query.setCategoryId(Long.valueOf(category.trim()));
            } catch (NumberFormatException e) {
                log.warn("标签推荐分类参数格式错误：category={}", category);
            }
        }
        List<PortalTag> list = portalTagService.selectPortalTagList(query);
        if (list.size() > 10) {
            list = list.subList(0, 10);
        }
        redisCache.setCacheObject(cacheKey, list, RECOMMEND_CACHE_MINUTES, TimeUnit.MINUTES);
        return success(list);
    }

    @Operation(summary = "创建标签", description = "创建新标签（需登录）")
    @PostMapping("/create")
    public AjaxResult createTag(@RequestBody PortalTag portalTag) {
        if (PortalSecurityUtils.getUserId() == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "请先登录");
        }
        int rows = portalTagService.insertPortalTag(portalTag);
        if (rows > 0) {
            redisCache.deleteObject(RECOMMEND_CACHE_KEY);
        }
        return toAjax(rows);
    }

    @Operation(summary = "绑定标签到实体", description = "根据 entityType/entityId 绑定标签，支持按 id 或名称自动创建（需登录）")
    @Log(title = "门户标签-绑定", businessType = BusinessType.UPDATE)
    @PostMapping("/bind")
    public AjaxResult bindTags(@Validated @RequestBody PortalTagBindDTO dto) {
        if (PortalSecurityUtils.getUserId() == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "请先登录");
        }
        portalTagService.bindTags(
            dto.getEntityType(),
            dto.getEntityId(),
            dto.getTagIds() == null ? java.util.Collections.emptyList() : dto.getTagIds(),
            dto.getTagNames() == null ? java.util.Collections.emptyList() : dto.getTagNames(),
            dto.getModule()
        );
        return success();
    }
}
