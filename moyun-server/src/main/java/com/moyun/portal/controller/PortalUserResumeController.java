package com.moyun.portal.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moyun.common.config.RuoYiConfig;
import com.moyun.common.constant.Constants;
import com.moyun.common.constant.HttpStatus;
import com.moyun.core.base.AjaxResult;
import com.moyun.core.base.BaseController;
import com.moyun.ext.cms.domain.query.UserResumeQuery;
import com.moyun.ext.cms.domain.vo.UserResumeVO;
import com.moyun.ext.cms.service.AiTaskService;
import com.moyun.ext.cms.service.IUserResumeService;
import com.moyun.ext.cms.service.ResumeParseTaskHandler;
import com.moyun.ext.file.domain.entity.SysFile;
import com.moyun.ext.file.service.ISysFileService;
import com.moyun.portal.util.PortalSecurityUtils;
import com.moyun.util.bean.PageUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * 用户简历 Controller（门户端，面试空间第2期）
 * <p>
 * 支持结构化简历录入、草稿保存、版本历史、PDF 导出、规则评分。
 * 所有接口均需登录，仅作者可访问/修改自己的简历。
 *
 * @author moyun
 */
@Tag(name = "用户简历", description = "用户简历录入、版本、导出、评分")
@RestController
@RequestMapping("/portal/interview/resume/user")
public class PortalUserResumeController extends BaseController {

    @Autowired
    private IUserResumeService userResumeService;

    @Autowired
    private com.moyun.ext.cms.service.ResumeParseService resumeParseService;

    /** v10.23：附件文件存储（上传接口快速路径：同步保存源文件） */
    @Autowired
    private ISysFileService sysFileService;

    /** v10.23：通用 AI 异步任务服务（LLM 解析改为异步任务） */
    @Autowired
    private AiTaskService aiTaskService;

    private Long currentUserId() {
        return PortalSecurityUtils.getUserId();
    }

    @Operation(summary = "我的简历列表", description = "分页查询当前用户的简历（草稿/已发布）")
    @GetMapping("/list")
    public AjaxResult getMyResumeList(UserResumeQuery query) {
        Long userId = currentUserId();
        if (userId == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "登录已过期，请重新登录");
        }
        Page<UserResumeVO> page = PageUtils.buildPage(query);
        return AjaxResult.success(userResumeService.selectMyResumePage(page, userId, query));
    }

    @Operation(summary = "解析简历附件（v10.12；v10.23 改异步任务化）",
            description = "快速路径：校验文件 → 保存源文件 → 创建附件简历草稿记录（不调 LLM）→ 提交 resume_parse 异步任务。"
                    + "返回 {resumeId, taskId, fileName}，前端通过 GET /portal/ai/task/{taskId} 轮询解析结果。")
    @PostMapping(value = "/parse", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public AjaxResult parseAttachment(@org.springframework.web.bind.annotation.RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        Long userId = currentUserId();
        if (userId == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "登录已过期，请重新登录");
        }
        try {
            // 1. 保存源文件 + 2. 创建附件简历记录（同步快操作，内部校验文件大小/类型）
            SysFile sysFile = sysFileService.uploadFileForPortal(file, "resume_attachment", null);
            String fileUrl = sysFile.getFileUrl();
            Long resumeId = resumeParseService.prepareAttachmentResume(userId, file, fileUrl);

            // 3. 提交 AI 异步解析任务（LLM 部分耗时，交给后台线程池）
            String originalName = file.getOriginalFilename();
            Map<String, Object> bizRef = new HashMap<>();
            bizRef.put("resumeId", resumeId);
            bizRef.put("fileUrl", fileUrl);
            bizRef.put("fileName", originalName);
            Long taskId = aiTaskService.submitTask(userId, ResumeParseTaskHandler.TASK_TYPE, bizRef);

            // 4. 立即返回，前端轮询任务进度
            Map<String, Object> data = new HashMap<>();
            data.put("resumeId", resumeId);
            data.put("taskId", taskId);
            data.put("fileName", originalName);
            return AjaxResult.success(data);
        } catch (RuntimeException e) {
            return AjaxResult.error(e.getMessage());
        }
    }

    @Operation(summary = "简历详情", description = "查询指定简历详情（仅作者可访问）")
    @GetMapping("/{id:[0-9]+}")
    public AjaxResult getResumeDetail(@PathVariable("id") Long id) {
        Long userId = currentUserId();
        if (userId == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "登录已过期，请重新登录");
        }
        UserResumeVO vo = userResumeService.selectResumeDetail(id, userId);
        if (vo == null) {
            return AjaxResult.error("简历不存在");
        }
        return AjaxResult.success(vo);
    }

    @Operation(summary = "保存简历", description = "新增或更新简历内容（id 为空时新增）")
    @PostMapping("/save")
    public AjaxResult saveResume(@RequestBody UserResumeVO vo) {
        Long userId = currentUserId();
        if (userId == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "登录已过期，请重新登录");
        }
        Long id = userResumeService.saveResume(vo, userId);
        return AjaxResult.success(id);
    }

    @Operation(summary = "删除简历", description = "仅作者可删除自己的简历")
    @DeleteMapping("/{id:[0-9]+}")
    public AjaxResult deleteResume(@PathVariable("id") Long id) {
        Long userId = currentUserId();
        if (userId == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "登录已过期，请重新登录");
        }
        return AjaxResult.success(userResumeService.deleteResume(id, userId));
    }

    @Operation(summary = "复制为新版本", description = "基于现有简历复制为新版本（versionNo 自增）")
    @PostMapping("/{id:[0-9]+}/copy")
    public AjaxResult copyResume(@PathVariable("id") Long id) {
        Long userId = currentUserId();
        if (userId == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "登录已过期，请重新登录");
        }
        return AjaxResult.success(userResumeService.copyResumeAsNewVersion(id, userId));
    }

    @Operation(summary = "简历版本历史", description = "查询某简历的全部历史版本")
    @GetMapping("/{id:[0-9]+}/versions")
    public AjaxResult getVersionHistory(@PathVariable("id") Long id) {
        Long userId = currentUserId();
        if (userId == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "登录已过期，请重新登录");
        }
        return AjaxResult.success(userResumeService.selectVersionHistory(id, userId));
    }

    @Operation(summary = "导出 PDF", description = "将简历渲染为 PDF 并返回文件 URL")
    @PostMapping("/{id:[0-9]+}/export")
    public AjaxResult exportPdf(@PathVariable("id") Long id) {
        Long userId = currentUserId();
        if (userId == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "登录已过期，请重新登录");
        }
        return AjaxResult.success(userResumeService.exportResumePdf(id, userId));
    }

    @Operation(summary = "规则评分", description = "对简历进行规则评分，返回评分明细（含岗位匹配度子项）")
    @PostMapping("/{id:[0-9]+}/score")
    public AjaxResult scoreResume(@PathVariable("id") Long id) {
        Long userId = currentUserId();
        if (userId == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "登录已过期，请重新登录");
        }
        return AjaxResult.success(userResumeService.scoreResume(id, userId));
    }

    @Operation(summary = "AI 改进建议", description = "基于评分明细与岗位匹配度生成改进建议（当前规则化，后期接入 AI 模型）")
    @PostMapping("/{id:[0-9]+}/ai-advice")
    public AjaxResult getAiAdvice(@PathVariable("id") Long id) {
        Long userId = currentUserId();
        if (userId == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "登录已过期，请重新登录");
        }
        return AjaxResult.success(userResumeService.generateAiAdvice(id, userId));
    }

    @Operation(summary = "更新状态", description = "更新简历状态：draft/published/archived")
    @PutMapping("/{id:[0-9]+}/status")
    public AjaxResult updateStatus(@PathVariable("id") Long id, @RequestBody Map<String, String> body) {
        Long userId = currentUserId();
        if (userId == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "登录已过期，请重新登录");
        }
        String status = body == null ? null : body.get("status");
        return AjaxResult.success(userResumeService.updateStatus(id, userId, status));
    }

    @Operation(summary = "下载简历 PDF", description = "认证下载当前用户导出的简历 PDF 文件流（避免 /profile/** 公开访问泄露隐私）")
    @GetMapping("/file/{id}")
    public ResponseEntity<Resource> downloadPdf(@PathVariable("id") Long id) {
        Long userId = currentUserId();
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String diskUrl = userResumeService.getResumePdfDiskPath(id, userId);
        if (diskUrl == null) {
            return ResponseEntity.notFound().build();
        }
        // 将 URL 形式（/profile/upload/...）转换为磁盘绝对路径（{profile}/upload/...）
        String diskPath;
        if (diskUrl.startsWith(Constants.RESOURCE_PREFIX)) {
            diskPath = RuoYiConfig.getProfile() + diskUrl.substring(Constants.RESOURCE_PREFIX.length());
        } else {
            diskPath = diskUrl;
        }
        File file = new File(diskPath);
        if (!file.exists() || !file.isFile()) {
            return ResponseEntity.notFound().build();
        }
        String encodedName = URLEncoder.encode(file.getName(), StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + encodedName + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(file.length())
                .body(new FileSystemResource(file));
    }

    @Operation(summary = "附件简历列表", description = "查询当前用户的附件简历（source_type=attachment）")
    @GetMapping("/attachments")
    public AjaxResult getAttachmentList() {
        Long userId = currentUserId();
        if (userId == null) return AjaxResult.error(HttpStatus.UNAUTHORIZED, "登录已过期");
        return AjaxResult.success(userResumeService.selectAttachmentList(userId));
    }

    @Operation(summary = "下载附件源文件", description = "下载附件简历的原始上传文件")
    @GetMapping("/{id:[0-9]+}/download-attachment")
    public ResponseEntity<Resource> downloadAttachment(@PathVariable("id") Long id) {
        Long userId = currentUserId();
        if (userId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        UserResumeVO vo = userResumeService.selectResumeDetail(id, userId);
        if (vo == null || !"attachment".equals(vo.getSourceType()) || vo.getSourceFileUrl() == null) {
            return ResponseEntity.notFound().build();
        }
        // 将 URL 转磁盘路径（复用 downloadPdf 的逻辑）
        String diskUrl = vo.getSourceFileUrl();
        String diskPath;
        if (diskUrl.startsWith(Constants.RESOURCE_PREFIX)) {
            diskPath = RuoYiConfig.getProfile() + diskUrl.substring(Constants.RESOURCE_PREFIX.length());
        } else {
            diskPath = diskUrl;
        }
        File f = new File(diskPath);
        if (!f.exists() || !f.isFile()) return ResponseEntity.notFound().build();
        String fileName = vo.getSourceFileName() != null ? vo.getSourceFileName() : f.getName();
        String encoded = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + encoded + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(f.length())
                .body(new FileSystemResource(f));
    }

    @Operation(summary = "附件转在线简历", description = "将附件简历转为在线简历（source_type 改为 online，保留解析数据）")
    @PostMapping("/{id:[0-9]+}/convert-to-online")
    public AjaxResult convertToOnline(@PathVariable("id") Long id) {
        Long userId = currentUserId();
        if (userId == null) return AjaxResult.error(HttpStatus.UNAUTHORIZED, "登录已过期");
        return AjaxResult.success(userResumeService.convertAttachmentToOnline(id, userId));
    }
}

