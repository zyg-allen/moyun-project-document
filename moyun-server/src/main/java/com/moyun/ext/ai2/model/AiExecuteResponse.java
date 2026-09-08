package com.moyun.ext.ai2.model;

import com.moyun.ext.ai2.constant.AiErrorCodes;
import lombok.Data;

/**
 * AI统一执行响应
 *
 * <p>所有场景的统一出参。依据《AI能力统一接入层 — 完整方案文档》V2.0 §4.2。</p>
 *
 * @param <T> 场景业务数据类型（见 com.moyun.ext.ai2.model.data 包）
 * @author laomao
 * @since 2026-09-09
 */
@Data
public class AiExecuteResponse<T> {

    /** 请求ID */
    private String requestId;

    /** 场景代码 */
    private String scene;

    /** 0=成功，非0=失败（见 {@link AiErrorCodes}） */
    private Integer code;

    /** 提示消息（失败时为错误信息；clarification 时为追问内容） */
    private String msg;

    /** 耗时（毫秒） */
    private Long elapsedMs;

    /** 业务数据（各场景Data结构） */
    private T data;

    /** 元数据（模型/Token/缓存等） */
    private AiMetadata metadata;

    // ===== 工厂方法 =====

    public static <T> AiExecuteResponse<T> success(T data) {
        AiExecuteResponse<T> resp = new AiExecuteResponse<>();
        resp.setCode(AiErrorCodes.SUCCESS);
        resp.setMsg("success");
        resp.setData(data);
        return resp;
    }

    public static <T> AiExecuteResponse<T> failure(int code, String msg) {
        AiExecuteResponse<T> resp = new AiExecuteResponse<>();
        resp.setCode(code);
        resp.setMsg(msg);
        return resp;
    }

    /**
     * 意图不明确时的追问响应
     */
    public static <T> AiExecuteResponse<T> clarification(String question) {
        AiExecuteResponse<T> resp = new AiExecuteResponse<>();
        resp.setCode(AiErrorCodes.SUCCESS);
        resp.setMsg(question);
        return resp;
    }
}
