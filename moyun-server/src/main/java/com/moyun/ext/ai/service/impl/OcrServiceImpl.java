package com.moyun.ext.ai.service.impl;

import com.moyun.ext.ai.config.OcrProperties;
import com.moyun.ext.ai.dto.IdCardOcrResult;
import com.moyun.ext.ai.service.IOcrService;
import com.moyun.util.string.IdCardUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * OCR 识别服务实现（身份证识别，结构骨架已就位）
 *
 * <p>完整流程：文件校验 → 通道配置检查（未配置走降级）→ 图片读取 →
 * 厂商 API 调用 → 响应字段解析映射 → 身份证号本地校验过滤误识别 →
 * 结果组装（敏感字段日志脱敏）。真实厂商调用点以
 * {@code todo：配置第三方：} 注释标识，接入前降级返回明确失败，
 * 前端回退手动填写表单。
 *
 * <h3>接入清单（生产前完成）</h3>
 * <ol>
 *   <li>pom 引入厂商 SDK：阿里云 OCR（com.aliyun:ocr20191230）/
 *       腾讯云 OCR / 百度 AI / PaddleOCR 等，<b>不加依赖前骨架保持注释形态</b>。</li>
 *   <li>application.yaml 配置 moyun.ocr 段（enabled/provider/aliyun.AccessKey），
 *       AccessKey 生产环境变量注入。</li>
 *   <li>放开 {@code recognizeIdCard} 内 {@code todo：配置第三方：} 标记处的
 *       SDK 骨架代码，按厂商响应结构映射 {@link IdCardOcrResult} 字段。</li>
 * </ol>
 *
 * @author moyun
 */
@Slf4j
@Service
public class OcrServiceImpl implements IOcrService {

    /** 身份证图片大小上限（字节）：5MB */
    private static final long MAX_IMAGE_SIZE = 5 * 1024 * 1024;

    @Autowired
    private OcrProperties ocrProperties;

    @Override
    public IdCardOcrResult recognizeIdCard(MultipartFile file, String side) {
        IdCardOcrResult result = new IdCardOcrResult();
        result.setSide(side);

        // 1. 文件基础校验（Controller 已拦一道，此处兜底）
        if (file == null || file.isEmpty()) {
            result.setErrorMessage("请上传身份证图片");
            return result;
        }
        if (file.getSize() > MAX_IMAGE_SIZE) {
            result.setErrorMessage("图片大小不能超过 5MB");
            return result;
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            result.setErrorMessage("仅支持图片文件");
            return result;
        }
        if (!"front".equals(side) && !"back".equals(side)) {
            result.setErrorMessage("side 参数非法，仅支持 front / back");
            return result;
        }

        // 2. 通道配置检查：未配置走降级路径（明确失败，前端回退手动填写，不伪装识别成功）
        if (isChannelNotConfigured()) {
            log.warn("[ocr] OCR 服务未配置（moyun.ocr.enabled={} 或 AccessKey 缺失/为 todo 占位值），"
                    + "降级返回手动填写模式 side={} fileName={} size={}",
                    ocrProperties.isEnabled(), side, file.getOriginalFilename(), file.getSize());
            result.setErrorMessage("OCR 服务未配置，请手动填写证件信息");
            return result;
        }

        try {
            // 3. 图片读取（厂商 API 通用入参：图片二进制/Base64）
            byte[] imageBytes = file.getBytes();

            // 4. 厂商 API 调用 + 5. 响应解析映射
            // todo：配置第三方：OCR 服务（如阿里云OCR）配置
            // （pom 引入 com.aliyun:ocr20191230 等厂商 SDK 后，放开以下骨架）
            //
            // --- 请求组装（阿里云 RecognizeIdentityCard 示例） ---
            // com.aliyun.ocr20191230.Client client = buildClient(); // 按 OcrProperties.aliyun 初始化，单例复用
            // com.aliyun.ocr20191230.models.RecognizeIdentityCardRequest req =
            //         new com.aliyun.ocr20191230.models.RecognizeIdentityCardRequest()
            //                 .setBody(new java.io.ByteArrayInputStream(imageBytes))
            //                 .setSide("front".equals(side) ? "face" : "back");
            //
            // --- 调用 + 响应解析 ---
            // com.aliyun.ocr20191230.models.RecognizeIdentityCardResponse resp = client.recognizeIdentityCard(req);
            // // 响应结构（front）：data.frontInfo.{ name, gender, nationality, birthDate, address, IDNumber }
            // // 响应结构（back） ：data.backInfo.{ issue, validDate }
            // if ("front".equals(side)) {
            //     result.setName(resp.getBody().getData().getFrontInfo().getName());
            //     result.setGender(resp.getBody().getData().getFrontInfo().getGender());
            //     result.setNation(resp.getBody().getData().getFrontInfo().getNationality());
            //     result.setBirthDate(normalizeBirthDate(resp.getBody().getData().getFrontInfo().getBirthDate()));
            //     result.setAddress(resp.getBody().getData().getFrontInfo().getAddress());
            //     result.setIdNo(resp.getBody().getData().getFrontInfo().getIDNumber());
            // } else {
            //     result.setAuthority(resp.getBody().getData().getBackInfo().getIssue());
            //     result.setValidPeriod(resp.getBody().getData().getBackInfo().getValidDate());
            // }

            // --- 6. 识别结果本地校验（front 面）：身份证号校验位过滤厂商误识别 ---
            // if ("front".equals(side) && result.getIdNo() != null
            //         && IdCardUtil.validate(result.getIdNo()) != null) {
            //     log.warn("[ocr] 识别出的身份证号未通过本地校验，已丢弃（防误识别回填） side={}", side);
            //     result.setIdNo(null);
            // }
            //
            // --- 结果判定：任一字段识别出即 success=true ---
            // result.setSuccess(hasAnyField(result));
            // if (!result.isSuccess()) {
            //     result.setErrorMessage("未识别到有效字段，请更换清晰图片或手动填写");
            // }
            // log.info("[ocr] 识别完成 side={} success={} idNo={}",
            //         side, result.isSuccess(),
            //         result.getIdNo() == null ? null : IdCardUtil.mask(result.getIdNo())); // 身份证号日志脱敏
            // return result;

            // SDK 未接入期间的明确失败（enabled=true 但 SDK 未引入时也不会伪装成功）
            log.warn("[ocr] OCR 厂商 SDK 未接入（见 todo：配置第三方： 标记），识别拒绝 side={} fileName={}",
                    side, file.getOriginalFilename());
            result.setErrorMessage("OCR 服务未接入，请手动填写证件信息");
            return result;
        } catch (Exception e) {
            // 7. 异常处理：读取/调用异常统一转识别失败，不向调用方抛原始异常
            log.error("[ocr] 识别异常 side={} fileName={} err={}",
                    side, file.getOriginalFilename(), e.getMessage(), e);
            result.setErrorMessage("证件识别失败，请稍后重试或手动填写");
            return result;
        }
    }

    /**
     * 通道是否未配置：总开关关闭，或 AccessKey 缺失/为 todo 占位值
     */
    private boolean isChannelNotConfigured() {
        if (!ocrProperties.isEnabled()) {
            return true;
        }
        OcrProperties.Aliyun aliyun = ocrProperties.getAliyun();
        return isBlankOrPlaceholder(aliyun.getAccessKeyId())
                || isBlankOrPlaceholder(aliyun.getAccessKeySecret());
    }

    private boolean isBlankOrPlaceholder(String s) {
        return s == null || s.isBlank() || s.trim().toLowerCase().startsWith("todo");
    }

    /**
     * front/back 是否任一业务字段被识别出
     */
    private boolean hasAnyField(IdCardOcrResult r) {
        return r.getName() != null || r.getGender() != null || r.getNation() != null
                || r.getBirthDate() != null || r.getAddress() != null || r.getIdNo() != null
                || r.getAuthority() != null || r.getValidPeriod() != null;
    }

    /**
     * 厂商出生日期格式归一化（如 19900101 / 1990-01-01 → yyyy-MM-dd）
     */
    private String normalizeBirthDate(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String digits = raw.replaceAll("[^0-9]", "");
        if (digits.length() != 8) {
            return raw;
        }
        return digits.substring(0, 4) + "-" + digits.substring(4, 6) + "-" + digits.substring(6, 8);
    }
}
