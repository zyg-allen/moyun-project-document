package com.moyun.portal.controller;

import java.util.Map;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.moyun.common.annotation.Anonymous;
import com.moyun.common.constant.HttpStatus;
import com.moyun.core.base.AjaxResult;
import com.moyun.core.base.BaseController;
import com.moyun.core.base.page.PageDomain;
import com.moyun.ext.cms.domain.vo.BookClubActivityVO;
import com.moyun.ext.cms.domain.vo.BookClubRecordVO;
import com.moyun.ext.cms.service.IBookClubService;
import com.moyun.portal.util.PortalSecurityUtils;

/**
 * 共读活动 Controller（门户端）
 * <p>
 * 公开接口（@Anonymous）：活动列表、活动详情、活动记录列表；
 * 需登录接口：加入/退出活动、查询是否已加入、提交/删除记录、点赞。
 *
 * @author moyun
 */
@Tag(name = "共读活动", description = "共读活动相关接口")
@RestController
@RequestMapping("/portal/reading/club")
public class PortalBookClubController extends BaseController {

    @Autowired
    private IBookClubService bookClubService;

    private Long currentUserId() {
        return PortalSecurityUtils.getUserId();
    }

    // ==================== 活动列表 / 详情（公开） ====================

    @Operation(summary = "活动列表", description = "分页查询共读活动列表（含参与人数、记录数聚合）")
    @GetMapping("/list")
    @Anonymous
    public AjaxResult getActivityList(PageDomain pageDomain) {
        // 公开接口；若已登录则同时返回 isJoined 标记
        Page<BookClubActivityVO> page = bookClubService.getActivityList(pageDomain, currentUserId());
        return AjaxResult.success(page);
    }

    @Operation(summary = "活动详情", description = "根据ID获取活动详情（含统计与 isJoined 标记）")
    @GetMapping("/{id}")
    @Anonymous
    public AjaxResult getActivityDetail(@Parameter(description = "活动ID") @PathVariable Long id) {
        BookClubActivityVO vo = bookClubService.getActivityDetail(id, currentUserId());
        if (vo == null) {
            return AjaxResult.error("活动不存在");
        }
        return AjaxResult.success(vo);
    }

    // ==================== 加入 / 退出 / 是否已加入（需登录） ====================

    @Operation(summary = "加入活动", description = "当前登录用户加入指定共读活动")
    @PostMapping("/{id}/join")
    public AjaxResult joinActivity(@Parameter(description = "活动ID") @PathVariable Long id) {
        Long userId = currentUserId();
        if (userId == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "登录已过期，请重新登录");
        }
        return AjaxResult.success(bookClubService.joinActivity(id, userId));
    }

    @Operation(summary = "退出活动", description = "当前登录用户退出指定共读活动")
    @DeleteMapping("/{id}/leave")
    public AjaxResult leaveActivity(@Parameter(description = "活动ID") @PathVariable Long id) {
        Long userId = currentUserId();
        if (userId == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "登录已过期，请重新登录");
        }
        return AjaxResult.success(bookClubService.leaveActivity(id, userId));
    }

    @Operation(summary = "查询是否已加入", description = "查询当前登录用户是否已加入指定活动")
    @GetMapping("/{id}/is-joined")
    public AjaxResult isJoined(@Parameter(description = "活动ID") @PathVariable Long id) {
        Long userId = currentUserId();
        if (userId == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "登录已过期，请重新登录");
        }
        return AjaxResult.success(bookClubService.isJoined(id, userId));
    }

    // ==================== 共读记录（列表公开，提交/删除/点赞需登录） ====================

    @Operation(summary = "活动记录列表", description = "分页查询某活动的共读打卡记录（含作者信息、点赞标记）")
    @GetMapping("/{id}/records")
    @Anonymous
    public AjaxResult getRecords(@Parameter(description = "活动ID") @PathVariable Long id, PageDomain pageDomain) {
        // 公开接口；若已登录则同时返回 isLiked 标记
        Page<BookClubRecordVO> page = bookClubService.listRecords(id, pageDomain, currentUserId());
        return AjaxResult.success(page);
    }

    @Operation(summary = "提交共读记录", description = "提交读后感/摘抄打卡记录，需登录")
    @PostMapping("/{id}/records")
    public AjaxResult submitRecord(@Parameter(description = "活动ID") @PathVariable Long id,
                                   @RequestBody Map<String, Object> body) {
        Long userId = currentUserId();
        if (userId == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "登录已过期，请重新登录");
        }
        String content = body.get("content") == null ? null : String.valueOf(body.get("content"));
        String recordType = body.get("recordType") == null ? null : String.valueOf(body.get("recordType"));
        return AjaxResult.success(bookClubService.submitRecord(id, userId, content, recordType));
    }

    @Operation(summary = "删除共读记录", description = "仅作者可删除自己的打卡记录，需登录")
    @DeleteMapping("/records/{recordId}")
    public AjaxResult deleteRecord(@Parameter(description = "记录ID") @PathVariable Long recordId) {
        Long userId = currentUserId();
        if (userId == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "登录已过期，请重新登录");
        }
        return AjaxResult.success(bookClubService.deleteRecord(recordId, userId));
    }

    @Operation(summary = "切换记录点赞", description = "点赞/取消点赞打卡记录（toggle），需登录")
    @PostMapping("/records/{recordId}/like")
    public AjaxResult toggleRecordLike(@Parameter(description = "记录ID") @PathVariable Long recordId) {
        Long userId = currentUserId();
        if (userId == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "登录已过期，请重新登录");
        }
        return AjaxResult.success(bookClubService.toggleRecordLike(recordId, userId));
    }
}
