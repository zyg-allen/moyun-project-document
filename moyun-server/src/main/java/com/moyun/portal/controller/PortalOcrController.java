package com.moyun.portal.controller;

import com.moyun.common.constant.HttpStatus;
import com.moyun.core.base.AjaxResult;
import com.moyun.core.base.BaseController;
import com.moyun.ext.ai.dto.IdCardOcrResult;
import com.moyun.ext.ai.service.IOcrService;
import com.moyun.portal.util.PortalSecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 门户 OCR 识别 Controller
 *
 * <p>当前仅提供身份证 OCR 识别，用于创作者认证页「自动回填」。
 * 接口要求登录态，且按 {@code side} 单面识别，避免被滥用为通用 OCR 网关。
 *
 * <p>识别逻辑由 {@link IOcrService} 提供，当前为 STUB 实现，待接入真实 OCR API。
 * 详见 {@code OcrServiceImpl} 的 TODO 接入清单。
 *
 * @author moyun
 */
@Slf4j
@Tag(name = "门户 OCR 识别", description = "身份证等证件 OCR 识别接口")
@RestController
@RequestMapping("/portal/ocr")
public class PortalOcrController extends BaseController {

    @Autowired
    private IOcrService ocrService;

    /**
     * 身份证 OCR 识别
     *
     * @param file 身份证图片（jpg/png，建议 &lt; 5MB）
     * @param side 识别面：front=人像面 / back=国徽面
     */
    @Operation(summary = "身份证 OCR 识别", description = "上传身份证图片，识别姓名/号码等字段，自动回填到认证表单")
    @PostMapping("/id-card")
    public AjaxResult recognizeIdCard(
            @Parameter(description = "身份证图片文件") @RequestParam("file") MultipartFile file,
            @Parameter(description = "识别面 front=人像面 / back=国徽面") @RequestParam("side") String side) {
        Long userId = PortalSecurityUtils.getUserId();
        if (userId == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "请先登录");
        }
        if (file == null || file.isEmpty()) {
            return AjaxResult.error("请上传身份证图片");
        }
        if (file.getSize() > 5 * 1024 * 1024) {
            return AjaxResult.error("图片大小不能超过 5MB");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return AjaxResult.error("仅支持图片文件");
        }
        if (!"front".equals(side) && !"back".equals(side)) {
            return AjaxResult.error("side 参数非法，仅支持 front / back");
        }
        IdCardOcrResult result = ocrService.recognizeIdCard(file, side);
        return AjaxResult.success(result);
    }
}
