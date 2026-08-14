package com.moyun.portal.judge;

/**
 * OJ 判题状态枚举（v6.3 OJ 判题系统）
 * <p>
 * 与力扣/牛客主流约定一致，前端按状态展示对应徽章。
 *
 * @author moyun
 */
public enum JudgeStatus {

    /** Accepted 通过 */
    ACCEPTED("AC", "Accepted"),

    /** Wrong Answer 答案错误 */
    WRONG_ANSWER("WA", "Wrong Answer"),

    /** Time Limit Exceeded 超时 */
    TIME_LIMIT_EXCEEDED("TLE", "Time Limit Exceeded"),

    /** Memory Limit Exceeded 超内存（当前实现以配置上限为准） */
    MEMORY_LIMIT_EXCEEDED("MLE", "Memory Limit Exceeded"),

    /** Runtime Error 运行时错误（异常/退出码非 0） */
    RUNTIME_ERROR("RE", "Runtime Error"),

    /** Compile Error 编译失败（编译型语言编译阶段失败） */
    COMPILE_ERROR("CE", "Compile Error"),

    /** System Error 判题机异常（沙箱不可用等） */
    SYSTEM_ERROR("SE", "System Error"),

    /** Pending 待判题（异步队列中，当前同步实现不会返回此态） */
    PENDING("PENDING", "Pending");

    /** 短码（持久化到 submission.status 字段） */
    private final String code;

    /** 完整展示名（前端展示用） */
    private final String displayName;

    JudgeStatus(String code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    public String getCode() {
        return code;
    }

    public String getDisplayName() {
        return displayName;
    }

    /** 是否通过 */
    public boolean isAccepted() {
        return this == ACCEPTED;
    }

    /** 是否判题终结态（非 pending） */
    public boolean isFinal() {
        return this != PENDING;
    }

    /**
     * 根据短码或历史 status 字符串解析为枚举值，未识别返回 PENDING
     * <p>
     * 兼容两种来源：
     * 1) OJ 判题写入的短码（AC/WA/TLE/MLE/RE/CE/SE/PENDING）；
     * 2) 历史 submission.status（accepted/wrong_answer/time_limit/...）。
     */
    public static JudgeStatus fromCode(String code) {
        if (code == null) return PENDING;
        switch (code) {
            case "AC":
            case "accepted":
                return ACCEPTED;
            case "WA":
            case "wrong_answer":
                return WRONG_ANSWER;
            case "TLE":
            case "time_limit":
                return TIME_LIMIT_EXCEEDED;
            case "MLE":
            case "memory_limit":
                return MEMORY_LIMIT_EXCEEDED;
            case "RE":
            case "runtime_error":
                return RUNTIME_ERROR;
            case "CE":
            case "compile_error":
                return COMPILE_ERROR;
            case "SE":
                return SYSTEM_ERROR;
            case "PENDING":
            case "pending":
                return PENDING;
            default:
                return PENDING;
        }
    }
}
