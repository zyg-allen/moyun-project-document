package com.moyun.portal.controller;

import com.moyun.common.constant.HttpStatus;
import com.moyun.core.base.AjaxResult;
import com.moyun.core.base.BaseController;
import com.moyun.ext.cms.domain.vo.StudyPlanVO;
import com.moyun.ext.cms.service.IStudyPlanService;
import com.moyun.portal.util.PortalSecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 学习计划与目标 Controller（任务 3.2，门户端）
 * <p>
 * 所有接口均需登录。
 *
 * @author moyun
 */
@Tag(name = "学习计划", description = "学习计划创建、修改、进度跟踪、日志记录")
@RestController
@RequestMapping("/portal/learn/plan")
public class PortalStudyPlanController extends BaseController {

    @Autowired
    private IStudyPlanService studyPlanService;

    private Long currentUserId() {
        return PortalSecurityUtils.getUserId();
    }

    @Operation(summary = "创建/修改学习计划", description = "id 为空时创建（校验计划数量上限），非空时修改（校验归属）")
    @PostMapping("/save")
    public AjaxResult save(@RequestBody StudyPlanVO vo) {
        Long userId = currentUserId();
        if (userId == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "登录已过期，请重新登录");
        }
        return AjaxResult.success(studyPlanService.savePlan(vo, userId));
    }

    @Operation(summary = "我的学习计划", description = "分页查询当前用户的学习计划（含进度统计）")
    @GetMapping("/my")
    public AjaxResult myPlans(@RequestParam(required = false) String status,
                              @RequestParam(defaultValue = "1") Integer pageNum,
                              @RequestParam(defaultValue = "10") Integer pageSize) {
        Long userId = currentUserId();
        if (userId == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "登录已过期，请重新登录");
        }
        return AjaxResult.success(studyPlanService.listMyPlans(userId, status, pageNum, pageSize));
    }

    @Operation(summary = "计划进度", description = "查询单个计划进度（含累计完成数、今日完成数、连续打卡）")
    @GetMapping("/{id}/progress")
    public AjaxResult progress(@PathVariable("id") Long id) {
        Long userId = currentUserId();
        if (userId == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "登录已过期，请重新登录");
        }
        return AjaxResult.success(studyPlanService.getPlanProgress(id, userId));
    }

    @Operation(summary = "记录今日完成数", description = "增量记录今日完成数（delta 可为负，结果不低于 0）")
    @PostMapping("/{id}/progress")
    public AjaxResult recordProgress(@PathVariable("id") Long id,
                                     @RequestParam(defaultValue = "1") int delta) {
        Long userId = currentUserId();
        if (userId == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "登录已过期，请重新登录");
        }
        return AjaxResult.success(studyPlanService.recordTodayProgress(id, userId, delta));
    }

    @Operation(summary = "切换计划状态", description = "状态：active/completed/abandoned")
    @PutMapping("/{id}/status")
    public AjaxResult changeStatus(@PathVariable("id") Long id,
                                    @RequestParam String status) {
        Long userId = currentUserId();
        if (userId == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "登录已过期，请重新登录");
        }
        return AjaxResult.success(studyPlanService.changeStatus(id, userId, status));
    }

    @Operation(summary = "删除计划", description = "仅作者本人，级联删除进度日志")
    @DeleteMapping("/{id}")
    public AjaxResult delete(@PathVariable("id") Long id) {
        Long userId = currentUserId();
        if (userId == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "登录已过期，请重新登录");
        }
        return AjaxResult.success(studyPlanService.deletePlan(id, userId));
    }

    @Operation(summary = "基于画像自动生成学习计划",
            description = "v5.9 阶段3：根据用户画像快照（薄弱点 + 岗位必备技能）自动生成针对性学习计划。"
                    + "自动去重：跳过已存在同 targetCategory 的 active 计划；受计划数量上限限制。"
                    + "无画像或无薄弱点时返回空列表。")
    @PostMapping("/auto-generate")
    public AjaxResult autoGenerate() {
        Long userId = currentUserId();
        if (userId == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "登录已过期，请重新登录");
        }
        List<StudyPlanVO> generated = studyPlanService.generatePlansFromProfile(userId);
        return AjaxResult.success(generated);
    }
}
