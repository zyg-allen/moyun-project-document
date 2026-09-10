package com.moyun.ext.ai2.constant;

/**
 * AI能力统一接入层错误码定义
 *
 * <p>依据《AI能力统一接入层 — 完整方案文档》V2.0 §8。</p>
 *
 * @author laomao
 * @since 2026-09-09
 */
public interface AiErrorCodes {

    int SUCCESS = 0;

    // 通用错误 (1000-1099)
    int UNKNOWN_ERROR = 1000;
    int TIMEOUT = 1001;
    int RATE_LIMITED = 1002;
    int INVALID_REQUEST = 1003;
    int SCENE_NOT_FOUND = 1004;
    int HANDLER_ERROR = 1005;
    int OUTPUT_MODE_NOT_SUPPORTED = 1006;
    /** 场景未开放通用入口调用（ai_scene_config.open_api=0，仅业务内部链路可用） */
    int SCENE_NOT_OPEN = 1007;

    // AI相关错误 (2000-2099)
    int AI_CALL_FAILED = 2000;
    int AI_PARSE_ERROR = 2001;
    int AI_EMPTY_RESULT = 2002;
    int AI_SAFETY_BLOCKED = 2003;
    int AI_TOKEN_LIMIT_EXCEEDED = 2004;
    int AI_MODEL_UNAVAILABLE = 2005;

    // 业务相关错误 (3000-3099)
    int RESUME_NOT_FOUND = 3000;
    int POSITION_NOT_FOUND = 3001;
    int QUESTION_NOT_FOUND = 3002;
}
