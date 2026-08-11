package com.moyun.ext.ai.service;

import com.moyun.ext.ai.dto.IdCardOcrResult;
import org.springframework.web.multipart.MultipartFile;

/**
 * OCR 识别服务
 *
 * <p>当前为接口约定层，由各厂商 SDK 实现具体识别逻辑。
 * 默认实现 {@code OcrServiceImpl} 为 STUB，仅返回结构化空对象，
 * 便于前端联调；接入真实 API 后替换 STUB 内部逻辑即可。
 *
 * @author moyun
 */
public interface IOcrService {

    /**
     * 身份证 OCR 识别
     *
     * @param file 身份证图片文件（人像面或国徽面）
     * @param side  side=front 识别人像面，side=back 识别国徽面
     * @return 识别结果对象（不为 null，未识别出的字段为 null）
     */
    IdCardOcrResult recognizeIdCard(MultipartFile file, String side);
}
