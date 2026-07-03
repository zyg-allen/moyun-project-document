package com.moyun.portal.controller;

import com.moyun.common.annotation.Anonymous;
import com.moyun.common.constant.HttpStatus;
import com.moyun.core.base.AjaxResult;
import com.moyun.core.base.BaseController;
import com.moyun.core.base.page.PageDomain;
import com.moyun.ext.cms.domain.query.CircleQuery;
import com.moyun.ext.cms.domain.vo.CircleListItemVO;
import com.moyun.ext.cms.domain.vo.CirclePostVO;
import com.moyun.ext.cms.domain.vo.CircleVO;
import com.moyun.ext.cms.service.ICircleService;
import com.moyun.portal.util.PortalSecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 圈子/兴趣小组 Controller（门户端，社交深化与商业化 4.1）
 * <p>
 * 公开接口：列表、详情、帖子流；其余接口均需登录。
 *
 * @author moyun
 */
@Tag(name = "圈子/兴趣小组", description = "圈子创建、加入/退出、发帖、成员管理")
@RestController
@RequestMapping("/portal/circle")
public class PortalCircleController extends BaseController {

    @Autowired
    private ICircleService circleService;

    private Long currentUserId() {
        return PortalSecurityUtils.getUserId();
    }

    // ==================== 公开接口 ====================

    @Operation(summary = "圈子列表", description = "公开分页查询已启用圈子（含圈主信息）")
    @GetMapping("/list")
    @Anonymous
    public AjaxResult list(CircleQuery query) {
        return AjaxResult.success(circleService.listCircles(query));
    }

    @Operation(summary = "圈子详情", description = "公开查询圈子详情（含圈主信息、成员前 N、当前用户视角）")
    @GetMapping("/{id}")
    @Anonymous
    public AjaxResult detail(@PathVariable("id") Long id) {
        CircleVO vo = circleService.getCircleDetail(id, currentUserId());
        if (vo == null) {
            return AjaxResult.error("圈子不存在");
        }
        return AjaxResult.success(vo);
    }

    @Operation(summary = "圈子帖子流", description = "公开分页查询圈子帖子（仅 active，含作者信息）")
    @GetMapping("/{id}/posts")
    @Anonymous
    public AjaxResult posts(@PathVariable("id") Long id, PageDomain query) {
        return AjaxResult.success(circleService.listCirclePosts(id, query));
    }

    // ==================== 圈子管理（需登录） ====================

    @Operation(summary = "创建圈子", description = "创建圈子，创建者自动成为圈主")
    @PostMapping("/save")
    public AjaxResult save(@RequestBody CircleVO vo) {
        Long userId = currentUserId();
        if (userId == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "登录已过期，请重新登录");
        }
        return AjaxResult.success(circleService.createCircle(vo, userId));
    }

    @Operation(summary = "修改圈子", description = "修改圈子信息（仅圈主本人）")
    @PutMapping("/{id}")
    public AjaxResult update(@PathVariable("id") Long id, @RequestBody CircleVO vo) {
        Long userId = currentUserId();
        if (userId == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "登录已过期，请重新登录");
        }
        vo.setId(id);
        return AjaxResult.success(circleService.updateCircle(vo, userId));
    }

    @Operation(summary = "删除圈子", description = "解散圈子（仅圈主本人，级联删除成员与帖子）")
    @DeleteMapping("/{id}")
    public AjaxResult delete(@PathVariable("id") Long id) {
        Long userId = currentUserId();
        if (userId == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "登录已过期，请重新登录");
        }
        return AjaxResult.success(circleService.deleteCircle(id, userId));
    }

    @Operation(summary = "加入圈子", description = "加入圈子（幂等，原子更新成员数）")
    @PostMapping("/{id}/join")
    public AjaxResult join(@PathVariable("id") Long id) {
        Long userId = currentUserId();
        if (userId == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "登录已过期，请重新登录");
        }
        return AjaxResult.success(circleService.joinCircle(id, userId));
    }

    @Operation(summary = "退出圈子", description = "退出圈子（圈主不可退出）")
    @PostMapping("/{id}/leave")
    public AjaxResult leave(@PathVariable("id") Long id) {
        Long userId = currentUserId();
        if (userId == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "登录已过期，请重新登录");
        }
        return AjaxResult.success(circleService.leaveCircle(id, userId));
    }

    // ==================== 帖子管理（需登录） ====================

    @Operation(summary = "发帖", description = "在圈子内发帖（需为圈子成员）")
    @PostMapping("/{id}/post")
    public AjaxResult createPost(@PathVariable("id") Long id, @RequestBody CirclePostVO post) {
        Long userId = currentUserId();
        if (userId == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "登录已过期，请重新登录");
        }
        return AjaxResult.success(circleService.createPost(id, post, userId));
    }
}
