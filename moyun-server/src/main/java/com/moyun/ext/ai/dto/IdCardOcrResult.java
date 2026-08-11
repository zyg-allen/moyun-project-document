package com.moyun.ext.ai.dto;

import lombok.Data;

/**
 * 身份证 OCR 识别结果
 *
 * <p>字段语义：
 * <ul>
 *   <li>人像面（front）：name / gender / nation / birthDate / address / idNo</li>
 *   <li>国徽面（back）：authority / validPeriod</li>
 * </ul>
 * 任一面别识别失败的字段保持 null，由调用方决定如何提示用户。
 *
 * @author moyun
 */
@Data
public class IdCardOcrResult {

    /** 识别面：front=人像面 / back=国徽面 */
    private String side;

    /** 姓名（仅 front） */
    private String name;

    /** 性别：男 / 女（仅 front） */
    private String gender;

    /** 民族：汉 / 回 / 蒙古 等（仅 front） */
    private String nation;

    /** 出生日期：yyyy-MM-dd（仅 front） */
    private String birthDate;

    /** 住址（仅 front） */
    private String address;

    /** 公民身份号码（仅 front，已通过 {@code IdCardUtil} 校验） */
    private String idNo;

    /** 签发机关（仅 back） */
    private String authority;

    /** 有效期限：yyyy.MM.dd-yyyy.MM.dd 或 yyyy.MM.dd-长期（仅 back） */
    private String validPeriod;

    /** 是否识别成功（true=有任意字段被识别出；false=全部字段为空或异常） */
    private boolean success;

    /** 错误信息（识别失败时填入，便于前端提示） */
    private String errorMessage;
}
