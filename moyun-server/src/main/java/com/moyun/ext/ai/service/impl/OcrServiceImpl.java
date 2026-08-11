package com.moyun.ext.ai.service.impl;

import com.moyun.ext.ai.dto.IdCardOcrResult;
import com.moyun.ext.ai.service.IOcrService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * OCR 识别服务 - STUB 实现
 *
 * <p>当前实现返回「识别失败」占位结果，仅用于前后端联调。
 *
 * <h3>TODO（接入真实 API 时的接入清单）</h3>
 * <ol>
 *   <li>引入厂商 SDK：阿里云 OCR / 腾讯云 OCR / 百度 AI / PaddleOCR 等，
 *       在 pom.xml 添加对应依赖。</li>
 *   <li>在 {@code application.yml} 添加配置项：
 *       <pre>
 *       ocr:
 *         provider: aliyun   # aliyun / tencent / baidu / paddle
 *         aliyun:
 *           access-key-id: ${OCR_ALIYUN_AK:}
 *           access-key-secret: ${OCR_ALIYUN_SK:}
 *           endpoint: ocr-api.cn-hangzhou.aliyuncs.com
 *         tencent:
 *           secret-id: ${OCR_TENCENT_ID:}
 *           secret-key: ${OCR_TENCENT_KEY:}
 *           region: ap-guangzhou
 *       </pre>
 *   </li>
 *   <li>在 {@code OcrProperties} 配置类（新建）中映射上述配置，
 *       并按 {@code provider} 选择具体实现。</li>
 *   <li>替换下方 {@code recognizeIdCard} 内部逻辑：
 *       <ul>
 *         <li>校验文件类型/大小（建议 &lt;5MB，仅 jpg/png）</li>
 *         <li>调用厂商 SDK 上传图片并解析返回 JSON</li>
 *         <li>映射到 {@link IdCardOcrResult} 字段</li>
 *         <li>对 front 识别出的 {@code idNo} 调用 {@code IdCardUtil.validate}
 *             过滤错误识别</li>
 *         <li>对识别失败/字段为空的情况返回 {@code success=false}
 *             + errorMessage 便于前端 toast</li>
 *         <li>识别原始响应记 DEBUG 日志，敏感字段（idNo/address）出日志时
 *             调用 {@code DesensitizedUtil} 脱敏</li>
 *       </ul>
 *   </li>
 *   <li>调用方限制：仅门户登录用户可调用，且按 side 参数限制单次识别一面，
 *       避免被滥用为通用 OCR 接口。</li>
 *   <li>计费/限流：建议在 {@code PortalOcrController} 上加
 *       {@code @RateLimiter} 或 Redis 计数限流，单用户每日上限 10 次。</li>
 * </ol>
 *
 * <p>替换 STUB 后，删除 {@code recognizeIdCard} 方法体中的
 * {@code "STUB"} 标记和占位返回。
 *
 * @author moyun
 */
@Slf4j
@Service
public class OcrServiceImpl implements IOcrService {

    @Override
    public IdCardOcrResult recognizeIdCard(MultipartFile file, String side) {
        // ====== STUB 开始（待真实 API 替换） ======
        // TODO: 接入真实 OCR API，按上方"接入清单"实施
        IdCardOcrResult result = new IdCardOcrResult();
        result.setSide(side);
        result.setSuccess(false);
        result.setErrorMessage("OCR 服务尚未接入，敬请期待（开发者联调占位返回）");
        log.warn("[OCR-STUB] 识别身份证 side={} fileName={} size={} - 当前为 STUB 实现，未调用真实 API",
                side, file != null ? file.getOriginalFilename() : null,
                file != null ? file.getSize() : -1);
        return result;
        // ====== STUB 结束 ======
    }
}
