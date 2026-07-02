package com.moyun.portal.controller;

import com.moyun.common.annotation.Anonymous;
import com.moyun.common.constant.HttpStatus;
import com.moyun.core.base.AjaxResult;
import com.moyun.core.base.BaseController;
import com.moyun.core.base.page.PageDomain;
import com.moyun.ext.cms.domain.query.ColumnQuery;
import com.moyun.ext.cms.domain.vo.ColumnArticleSortItem;
import com.moyun.ext.cms.domain.vo.ColumnVO;
import com.moyun.ext.cms.service.IColumnService;
import com.moyun.portal.util.PortalSecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 专栏/连载 Controller（门户端，创作者天堂核心）
 * <p>
 * 公开接口：列表、详情；其余接口均需登录。
 *
 * @author moyun
 */
@Tag(name = "专栏/连载", description = "专栏创建、文章目录编排、订阅、完结控制")
@RestController
@RequestMapping("/portal/column")
public class PortalColumnController extends BaseController {

    @Autowired
    private IColumnService columnService;

    private Long currentUserId() {
        return PortalSecurityUtils.getUserId();
    }

    // ==================== 公开接口 ====================

    @Operation(summary = "专栏列表", description = "公开分页查询已发布专栏（含作者信息）")
    @GetMapping("/list")
    @Anonymous
    public AjaxResult list(ColumnQuery query) {
        return AjaxResult.success(columnService.listColumns(query));
    }

    @Operation(summary = "专栏详情", description = "公开查询专栏详情（含作者信息、文章目录、当前用户是否订阅）")
    @GetMapping("/{id}")
    @Anonymous
    public AjaxResult detail(@PathVariable("id") Long id) {
        ColumnVO vo = columnService.getColumnDetail(id, currentUserId());
        if (vo == null) {
            return AjaxResult.error("专栏不存在");
        }
        return AjaxResult.success(vo);
    }

    // ==================== 专栏管理（需登录） ====================

    @Operation(summary = "创建/修改专栏", description = "id 为空时创建（校验专栏数量上限），非空时修改（校验归属）")
    @PostMapping("/save")
    public AjaxResult save(@RequestBody ColumnVO vo) {
        Long userId = currentUserId();
        if (userId == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "登录已过期，请重新登录");
        }
        return AjaxResult.success(columnService.saveColumn(vo, userId));
    }

    @Operation(summary = "完结/恢复连载", description = "切换 is_finished 状态，仅作者本人")
    @PutMapping("/{id}/finish")
    public AjaxResult toggleFinish(@PathVariable("id") Long id) {
        Long userId = currentUserId();
        if (userId == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "登录已过期，请重新登录");
        }
        return AjaxResult.success(columnService.toggleFinish(id, userId));
    }

    @Operation(summary = "删除专栏", description = "仅作者本人，级联删除关联与订阅")
    @DeleteMapping("/{id}")
    public AjaxResult delete(@PathVariable("id") Long id) {
        Long userId = currentUserId();
        if (userId == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "登录已过期，请重新登录");
        }
        return AjaxResult.success(columnService.deleteColumn(id, userId));
    }

    @Operation(summary = "切换订阅", description = "订阅/取消订阅（toggle），返回操作后的订阅状态")
    @PostMapping("/{id}/subscribe")
    public AjaxResult toggleSubscribe(@PathVariable("id") Long id) {
        Long userId = currentUserId();
        if (userId == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "登录已过期，请重新登录");
        }
        return AjaxResult.success(columnService.toggleSubscribe(id, userId));
    }

    // ==================== 我的专栏（需登录） ====================

    @Operation(summary = "我创建的专栏", description = "分页查询当前用户创建的专栏（含所有状态）")
    @GetMapping("/my/list")
    public AjaxResult myColumns(PageDomain query) {
        Long userId = currentUserId();
        if (userId == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "登录已过期，请重新登录");
        }
        return AjaxResult.success(columnService.listMyColumns(userId, query));
    }

    @Operation(summary = "我订阅的专栏", description = "分页查询当前用户订阅的专栏")
    @GetMapping("/my/subscribed")
    public AjaxResult mySubscribed(PageDomain query) {
        Long userId = currentUserId();
        if (userId == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "登录已过期，请重新登录");
        }
        return AjaxResult.success(columnService.listSubscribedColumns(userId, query));
    }

    // ==================== 专栏文章管理（需登录） ====================

    @Operation(summary = "加入文章", description = "将文章加入专栏（校验专栏归属、文章归属）")
    @PostMapping("/{columnId}/article/{articleId}")
    public AjaxResult addArticle(@PathVariable("columnId") Long columnId,
                                 @PathVariable("articleId") Long articleId) {
        Long userId = currentUserId();
        if (userId == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "登录已过期，请重新登录");
        }
        return AjaxResult.success(columnService.addArticle(columnId, articleId, userId));
    }

    @Operation(summary = "移出文章", description = "将文章移出专栏（仅作者本人）")
    @DeleteMapping("/{columnId}/article/{articleId}")
    public AjaxResult removeArticle(@PathVariable("columnId") Long columnId,
                                    @PathVariable("articleId") Long articleId) {
        Long userId = currentUserId();
        if (userId == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "登录已过期，请重新登录");
        }
        return AjaxResult.success(columnService.removeArticle(columnId, articleId, userId));
    }

    @Operation(summary = "批量排序", description = "批量调整专栏内文章顺序（id=文章ID, sortOrder=新顺序）")
    @PutMapping("/{columnId}/sort")
    public AjaxResult sortArticles(@PathVariable("columnId") Long columnId,
                                   @RequestBody List<ColumnArticleSortItem> list) {
        Long userId = currentUserId();
        if (userId == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "登录已过期，请重新登录");
        }
        return AjaxResult.success(columnService.sortArticles(columnId, list, userId));
    }
}
