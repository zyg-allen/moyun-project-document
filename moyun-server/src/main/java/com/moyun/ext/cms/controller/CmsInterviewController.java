package com.moyun.ext.cms.controller;

import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.moyun.common.annotation.Log;
import com.moyun.common.enums.BusinessType;
import com.moyun.core.base.AjaxResult;
import com.moyun.core.base.BaseController;
import com.moyun.core.base.dto.ImportResult;
import com.moyun.ext.cms.domain.query.InterviewCompanyQuery;
import com.moyun.ext.cms.domain.query.InterviewQuestionQuery;
import com.moyun.ext.cms.domain.vo.InterviewCategoryVO;
import com.moyun.ext.cms.domain.vo.InterviewCompanyVO;
import com.moyun.ext.cms.domain.vo.InterviewQuestionDetailVO;
import com.moyun.ext.cms.domain.vo.InterviewQuestionVO;
import com.moyun.ext.cms.service.IPortalInterviewService;
import com.moyun.portal.domain.entity.PortalInterviewCategory;
import com.moyun.portal.domain.entity.PortalInterviewCompany;
import com.moyun.portal.domain.entity.PortalInterviewQuestion;
import com.moyun.portal.domain.entity.PortalInterviewSubmission;
import com.moyun.portal.mapper.PortalInterviewSubmissionMapper;
import com.moyun.portal.mapper.PortalUserStatsMapper;
import com.moyun.portal.service.IPortalGrowthService;
import com.moyun.portal.service.IPortalImportTemplateConfigService;
import com.moyun.portal.service.IPortalTagService;
import com.moyun.util.bean.PageUtils;
import com.moyun.util.file.ExcelUtil;

/**
 * CMS面试模块管理Controller
 * <p>
 * 物理拆分后仅承载题库资源相关接口（题目/分类/公司/精选笔记），
 * 面经与评论管理见 {@link CmsInterviewExperienceController}，
 * 简历模板与用户简历见 {@link CmsInterviewResumeController}。
 * 三个 Controller 共享类级 @RequestMapping("/cms/interview")，方法级路径互不冲突。
 *
 * @author moyun
 */
@Tag(name = "CMS面试模块管理", description = "CMS面试模块管理接口")
@RestController
@RequestMapping("/cms/interview")
public class CmsInterviewController extends BaseController {

    @Autowired
    private IPortalInterviewService portalInterviewService;

    @Autowired
    private PortalInterviewSubmissionMapper portalInterviewSubmissionMapper;

    @Autowired
    private IPortalGrowthService portalGrowthService;

    @Autowired
    private PortalUserStatsMapper portalUserStatsMapper;

    @Autowired
    private IPortalTagService portalTagService;

    /**
     * 导入模板字段配置服务（动态模板下载/解析、失败行导出共用）
     */
    @Autowired
    private IPortalImportTemplateConfigService importTemplateConfigService;

    // ========================================================================
    // 题目管理
    // ========================================================================

    @Operation(summary = "获取题目分页列表", description = "根据条件分页查询题目列表")
    @PreAuthorize("@ss.hasPermi('cms:interview:list')")
    @GetMapping("/question/list")
    public AjaxResult listQuestion(InterviewQuestionQuery query) {
        Page<InterviewQuestionVO> page = PageUtils.buildPage(query);
        page = portalInterviewService.selectQuestionPage(page, query, null);
        return success(page);
    }

    @Operation(summary = "获取题目详情", description = "根据题目ID获取题目详细信息")
    @PreAuthorize("@ss.hasPermi('cms:interview:query')")
    @GetMapping("/question/{id}")
    public AjaxResult getQuestion(@Parameter(description = "题目ID") @PathVariable Long id) {
        InterviewQuestionDetailVO detail = portalInterviewService.selectQuestionDetailById(id, null);
        return success(detail);
    }

    @Operation(summary = "新增题目", description = "创建新题目")
    @PreAuthorize("@ss.hasPermi('cms:interview:add')")
    @Log(title = "面试题目", businessType = BusinessType.INSERT)
    @PostMapping("/question")
    public AjaxResult addQuestion(@Validated @RequestBody PortalInterviewQuestion question) {
        return toAjax(portalInterviewService.insertQuestion(question));
    }

    @Operation(summary = "修改题目", description = "更新题目信息")
    @PreAuthorize("@ss.hasPermi('cms:interview:edit')")
    @Log(title = "面试题目", businessType = BusinessType.UPDATE)
    @PutMapping("/question")
    public AjaxResult editQuestion(@Validated @RequestBody PortalInterviewQuestion question) {
        return toAjax(portalInterviewService.updateQuestion(question));
    }

    @Operation(summary = "批量删除题目", description = "批量删除题目")
    @PreAuthorize("@ss.hasPermi('cms:interview:remove')")
    @Log(title = "面试题目", businessType = BusinessType.DELETE)
    @DeleteMapping("/question")
    public AjaxResult removeQuestion(@RequestBody Long[] ids) {
        int result = portalInterviewService.deleteQuestionByIds(ids);
        // 删除成功后解绑标签（同步减少 reference_count）
        if (result > 0) {
            for (Long id : ids) {
                portalTagService.unbindTags("interview_question", id);
            }
        }
        return toAjax(result);
    }

    // ------------------------------------------------------------------
    // 题库导入导出（动态模板版，作为通用导入导出能力的首个落地业务）
    // 流程：下载动态模板 → 填写 → 上传 → 返回 ImportResult（含失败明细） → 下载失败行修正重导
    // ------------------------------------------------------------------

    /**
     * 导出题目列表到 Excel（兜底走 @Excel 注解；动态模板字段配置仅影响"导入模板"，导出保持实体驱动）
     */
    @Operation(summary = "导出题目", description = "按筛选条件导出题目列表到 Excel")
    @PreAuthorize("@ss.hasPermi('cms:interview:export')")
    @Log(title = "面试题目", businessType = BusinessType.EXPORT)
    @PostMapping("/question/export")
    public void exportQuestion(jakarta.servlet.http.HttpServletResponse response, InterviewQuestionQuery query) {
        List<PortalInterviewQuestion> list = portalInterviewService.selectQuestionList(query);
        ExcelUtil<PortalInterviewQuestion> util = new ExcelUtil<>(PortalInterviewQuestion.class);
        util.exportExcel(response, list, "面试题目");
    }

    /**
     * 下载导入模板（动态版）
     * <p>
     * 优先读 portal_import_template_config 中 businessKey=interview_question 的字段配置：
     * 命中 → 按配置生成表头/说明/示例/下拉校验
     * 未命中 → 回退 ExcelUtil.importTemplateExcel（@Excel 注解静态模板）
     */
    @Operation(summary = "下载导入模板", description = "下载题目导入模板（动态字段配置优先，回退 @Excel 注解）")
    @PreAuthorize("@ss.hasPermi('cms:interview:import')")
    @PostMapping("/question/importTemplate")
    public void importQuestionTemplate(jakarta.servlet.http.HttpServletResponse response) throws java.io.IOException {
        List<com.moyun.portal.domain.entity.PortalImportTemplateConfig> configs =
                importTemplateConfigService.selectEnabledByBusinessKey("interview_question");
        if (configs != null && !configs.isEmpty()) {
            com.moyun.util.file.ImportExportHelper.writeDynamicTemplate(
                    response, "面试题目导入模板", "题目模板", configs);
        } else {
            ExcelUtil<PortalInterviewQuestion> util = new ExcelUtil<>(PortalInterviewQuestion.class);
            util.importTemplateExcel(response, "面试题目");
        }
    }

    /**
     * 导入题目数据
     * <p>
     * 使用 ImportExportHelper 解析 Excel 为 List<Map<字段名,值>>，保留原始数据便于失败回导；
     * 调用 service.importQuestions 返回结构化 ImportResult（成功/失败统计 + 失败明细）。
     *
     * @param file Excel 文件（.xlsx）
     * @return ImportResult
     */
    @Operation(summary = "导入题目数据", description = "上传 Excel 文件批量导入题目，返回成功/失败明细")
    @PreAuthorize("@ss.hasPermi('cms:interview:import')")
    @Log(title = "面试题目", businessType = BusinessType.IMPORT)
    @PostMapping("/question/importData")
    public AjaxResult importQuestionData(@org.springframework.web.bind.annotation.RequestParam("file") org.springframework.web.multipart.MultipartFile file) throws java.io.IOException {
        if (file == null || file.isEmpty()) {
            return error("请选择要导入的文件");
        }
        // 读取字段配置（与下载模板一致，建立 columnName→fieldName 映射）
        List<com.moyun.portal.domain.entity.PortalImportTemplateConfig> configs =
                importTemplateConfigService.selectEnabledByBusinessKey("interview_question");
        if (configs == null || configs.isEmpty()) {
            // 未配置动态模板：回退 ExcelUtil.importExcel（@Excel 注解）
            try {
                ExcelUtil<PortalInterviewQuestion> util = new ExcelUtil<>(PortalInterviewQuestion.class);
                List<PortalInterviewQuestion> list = util.importExcel(file.getInputStream());
                // 转换为 Map 行数据（字段名→值）以适配统一 service 入参
                List<Map<String, String>> rows = new java.util.ArrayList<>();
                for (PortalInterviewQuestion q : list) {
                    Map<String, String> row = new java.util.LinkedHashMap<>();
                    row.put("title", q.getTitle() == null ? "" : q.getTitle());
                    row.put("description", q.getDescription() == null ? "" : q.getDescription());
                    row.put("difficulty", q.getDifficulty() == null ? "" : q.getDifficulty());
                    row.put("categoryId", q.getCategoryId() == null ? "" : String.valueOf(q.getCategoryId()));
                    row.put("tags", q.getTags() == null ? "" : q.getTags());
                    row.put("companies", q.getCompanies() == null ? "" : q.getCompanies());
                    row.put("hint", q.getHint() == null ? "" : q.getHint());
                    row.put("solution", q.getSolution() == null ? "" : q.getSolution());
                    row.put("referenceAnswer", q.getReferenceAnswer() == null ? "" : q.getReferenceAnswer());
                    row.put("answerOutline", q.getAnswerOutline() == null ? "" : q.getAnswerOutline());
                    row.put("examinePoints", q.getExaminePoints() == null ? "" : q.getExaminePoints());
                    row.put("scoringCriteria", q.getScoringCriteria() == null ? "" : q.getScoringCriteria());
                    row.put("prerequisiteIds", q.getPrerequisiteIds() == null ? "" : q.getPrerequisiteIds());
                    row.put("questionType", q.getQuestionType() == null ? "" : q.getQuestionType());
                    row.put("sort", q.getSort() == null ? "" : String.valueOf(q.getSort()));
                    row.put("status", q.getStatus() == null ? "" : q.getStatus());
                    rows.add(row);
                }
                ImportResult result = portalInterviewService.importQuestions(rows, getUsername());
                return success(result);
            } catch (Exception e) {
                return error("导入失败：" + e.getMessage());
            }
        }
        // 动态模板：用 ImportExportHelper 解析（保留字段名映射，便于失败回导）
        List<Map<String, String>> rows = com.moyun.util.file.ImportExportHelper.readRows(file.getInputStream(), configs);
        ImportResult result = portalInterviewService.importQuestions(rows, getUsername());
        return success(result);
    }

    /**
     * 下载失败行 Excel（修正后重导）
     * <p>
     * 前端在 ImportResult 弹窗里点"下载失败行"按钮，将 failRows 提交到此接口；
     * 后端按原模板字段配置生成表头 + 失败原因 + 原行号列。
     */
    @Operation(summary = "下载失败行", description = "导出导入失败行 Excel（含失败原因、原行号）供修正后重导")
    @PreAuthorize("@ss.hasPermi('cms:interview:import')")
    @PostMapping("/question/exportFailRows")
    public void exportQuestionFailRows(jakarta.servlet.http.HttpServletResponse response,
                                       @RequestBody ExportFailRowsBody body) throws java.io.IOException {
        List<com.moyun.portal.domain.entity.PortalImportTemplateConfig> configs =
                importTemplateConfigService.selectEnabledByBusinessKey("interview_question");
        if (configs == null || configs.isEmpty()) {
            // 未配置动态模板：无法生成失败行表头，返回错误提示
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("{\"msg\":\"未配置导入模板字段，无法导出失败行\"}");
            return;
        }
        com.moyun.util.file.ImportExportHelper.writeFailRows(
                response, "面试题目失败行", "失败行", configs,
                body.getFailRows() == null ? java.util.Collections.emptyList() : body.getFailRows()
        );
    }

    @lombok.Data
    public static class ExportFailRowsBody {
        @io.swagger.v3.oas.annotations.media.Schema(description = "失败明细列表")
        private java.util.List<ImportResult.FailRow> failRows;
    }

    // ========================================================================
    // 分类管理
    // ========================================================================

    @Operation(summary = "获取分类列表", description = "获取题目分类列表")
    @PreAuthorize("@ss.hasPermi('cms:interview:list')")
    @GetMapping("/category/list")
    public AjaxResult listCategory() {
        List<InterviewCategoryVO> list = portalInterviewService.selectCategoryList();
        return success(list);
    }

    @Operation(summary = "获取分类详情", description = "根据分类ID获取详细信息")
    @PreAuthorize("@ss.hasPermi('cms:interview:query')")
    @GetMapping("/category/{id}")
    public AjaxResult getCategory(@Parameter(description = "分类ID") @PathVariable Long id) {
        return success(portalInterviewService.selectCategoryById(id));
    }

    @Operation(summary = "新增分类", description = "创建新分类")
    @PreAuthorize("@ss.hasPermi('cms:interview:add')")
    @Log(title = "面试分类", businessType = BusinessType.INSERT)
    @PostMapping("/category")
    public AjaxResult addCategory(@Validated @RequestBody PortalInterviewCategory category) {
        return toAjax(portalInterviewService.insertCategory(category));
    }

    @Operation(summary = "修改分类", description = "更新分类信息")
    @PreAuthorize("@ss.hasPermi('cms:interview:edit')")
    @Log(title = "面试分类", businessType = BusinessType.UPDATE)
    @PutMapping("/category")
    public AjaxResult editCategory(@Validated @RequestBody PortalInterviewCategory category) {
        return toAjax(portalInterviewService.updateCategory(category));
    }

    @Operation(summary = "删除分类", description = "删除分类")
    @PreAuthorize("@ss.hasPermi('cms:interview:remove')")
    @Log(title = "面试分类", businessType = BusinessType.DELETE)
    @DeleteMapping("/category/{ids}")
    public AjaxResult removeCategory(@Parameter(description = "分类ID数组") @PathVariable Long[] ids) {
        return toAjax(portalInterviewService.deleteCategoryByIds(ids));
    }

    // ========================================================================
    // 公司标签管理
    // ========================================================================

    @Operation(summary = "获取公司列表", description = "获取公司标签列表")
    @PreAuthorize("@ss.hasPermi('cms:interview:list')")
    @GetMapping("/company/list")
    public AjaxResult listCompany(InterviewCompanyQuery query) {
        List<InterviewCompanyVO> list = portalInterviewService.selectCompanyList(query);
        return success(list);
    }

    @Operation(summary = "获取公司详情", description = "根据公司ID获取详细信息")
    @PreAuthorize("@ss.hasPermi('cms:interview:query')")
    @GetMapping("/company/{id}")
    public AjaxResult getCompany(@Parameter(description = "公司ID") @PathVariable Long id) {
        return success(portalInterviewService.selectCompanyById(id));
    }

    @Operation(summary = "新增公司", description = "创建新公司标签")
    @PreAuthorize("@ss.hasPermi('cms:interview:add')")
    @Log(title = "公司标签", businessType = BusinessType.INSERT)
    @PostMapping("/company")
    public AjaxResult addCompany(@Validated @RequestBody PortalInterviewCompany company) {
        return toAjax(portalInterviewService.insertCompany(company));
    }

    @Operation(summary = "修改公司", description = "更新公司标签信息")
    @PreAuthorize("@ss.hasPermi('cms:interview:edit')")
    @Log(title = "公司标签", businessType = BusinessType.UPDATE)
    @PutMapping("/company")
    public AjaxResult editCompany(@Validated @RequestBody PortalInterviewCompany company) {
        return toAjax(portalInterviewService.updateCompany(company));
    }

    @Operation(summary = "删除公司", description = "删除公司标签")
    @PreAuthorize("@ss.hasPermi('cms:interview:remove')")
    @Log(title = "公司标签", businessType = BusinessType.DELETE)
    @DeleteMapping("/company/{ids}")
    public AjaxResult removeCompany(@Parameter(description = "公司ID数组") @PathVariable Long[] ids) {
        return toAjax(portalInterviewService.deleteCompanyByIds(ids));
    }

    // ========================================================================
    // 精选笔记管理（提交笔记采纳/取消采纳）
    // ========================================================================

    @Operation(summary = "获取提交笔记分页列表", description = "分页查询用户提交的笔记，支持按题目ID和精选状态筛选")
    @PreAuthorize("@ss.hasPermi('cms:interview:list')")
    @GetMapping("/submission/list")
    public AjaxResult listSubmission(
            @RequestParam(required = false) Long questionId,
            @RequestParam(required = false) Boolean isFeatured,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        Page<PortalInterviewSubmission> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<PortalInterviewSubmission> wrapper = new LambdaQueryWrapper<>();
        if (questionId != null) {
            wrapper.eq(PortalInterviewSubmission::getQuestionId, questionId);
        }
        if (isFeatured != null) {
            wrapper.eq(PortalInterviewSubmission::getIsFeatured, isFeatured);
        }
        // 只查询有 note 内容的提交
        wrapper.isNotNull(PortalInterviewSubmission::getNote)
                .ne(PortalInterviewSubmission::getNote, "");
        wrapper.orderByDesc(PortalInterviewSubmission::getCreateTime);
        page = portalInterviewSubmissionMapper.selectPage(page, wrapper);
        return success(page);
    }

    @Operation(summary = "采纳笔记为精选", description = "将用户提交的笔记采纳为精选笔记，触发 note_adopted 成长事件")
    @PreAuthorize("@ss.hasPermi('cms:interview:edit')")
    @Log(title = "精选笔记", businessType = BusinessType.UPDATE)
    @PutMapping("/submission/featured")
    public AjaxResult featureSubmission(@RequestBody Map<String, Object> body) {
        Long id = Long.valueOf(String.valueOf(body.get("id")));
        int rows = portalInterviewSubmissionMapper.updateFeatured(id, true);
        if (rows > 0) {
            PortalInterviewSubmission submission = portalInterviewSubmissionMapper.selectById(id);
            if (submission != null && submission.getUserId() != null) {
                Long userId = submission.getUserId();
                // 触发 note_adopted 成长事件
                portalGrowthService.recordEvent("interview", "note_adopted", userId, "submission", id);
                // 更新用户统计：笔记被精选数 +1
                portalUserStatsMapper.addNoteAdopted(userId, 1);
            }
        }
        return toAjax(rows);
    }

    @Operation(summary = "取消精选笔记", description = "取消笔记的精选状态")
    @PreAuthorize("@ss.hasPermi('cms:interview:edit')")
    @Log(title = "精选笔记", businessType = BusinessType.UPDATE)
    @PutMapping("/submission/unfeatured")
    public AjaxResult unfeatureSubmission(@RequestBody Map<String, Object> body) {
        Long id = Long.valueOf(String.valueOf(body.get("id")));
        int rows = portalInterviewSubmissionMapper.updateFeatured(id, false);
        if (rows > 0) {
            PortalInterviewSubmission submission = portalInterviewSubmissionMapper.selectById(id);
            if (submission != null && submission.getUserId() != null) {
                Long userId = submission.getUserId();
                // 取消精选不扣成长值，只减统计
                portalUserStatsMapper.addNoteAdopted(userId, -1);
            }
        }
        return toAjax(rows);
    }

}
