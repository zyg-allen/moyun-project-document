package com.moyun.ext.ai.exception;

/**
 * 业务错误码枚举
 *
 * <p>统一定义系统中所有业务错误码，分类如下：
 * <ul>
 *   <li>10xxx - 知识库相关错误</li>
 *   <li>20xxx - 模型配置相关错误</li>
 *   <li>30xxx - 对话相关错误</li>
 *   <li>40xxx - 工作流相关错误</li>
 *   <li>50xxx - 数据源相关错误</li>
 *   <li>60xxx - 用户认证相关错误</li>
 * </ul>
 * </p>
 *
 * @author laomao
 * @since 2025-12-11
 */
public enum ErrorCode {

    // ==================== 通用错误 (1-999) ====================
    SUCCESS(0, "成功"),
    SYSTEM_ERROR(500, "系统内部错误"),
    PARAM_ERROR(400, "参数错误"),
    NOT_FOUND(404, "资源不存在"),

    // ==================== 知识库相关 (10xxx) ====================
    DOCUMENT_NOT_FOUND(10001, "文档不存在"),
    DOCUMENT_PARSE_FAILED(10002, "文档解析失败"),
    DOCUMENT_UPLOAD_FAILED(10003, "文档上传失败"),
    VECTOR_STORE_FAILED(10004, "向量存储失败"),
    VECTOR_DELETE_FAILED(10005, "向量删除失败"),
    UNSUPPORTED_FILE_TYPE(10006, "不支持的文件类型"),
    FILE_TOO_LARGE(10007, "文件大小超出限制"),
    KNOWLEDGE_BASE_NOT_FOUND(10008, "知识库不存在"),
    PDF_CONVERT_FAILED(10009, "PDF转换失败"),
    IMAGE_EXTRACT_FAILED(10010, "图片提取失败"),
    DOCUMENT_PROCESS_FAILED(10011, "文档处理失败"),
    DOCUMENT_NOT_READY(10012, "文档未就绪"),
    KNOWLEDGE_BASE_IN_USE(10013, "知识库正在使用中"),

    // ==================== 模型配置相关 (20xxx) ====================
    MODEL_NOT_FOUND(20001, "模型配置不存在"),
    MODEL_CREATE_FAILED(20002, "模型创建失败"),
    MODEL_CONFIG_INVALID(20003, "模型配置无效"),
    EMBEDDING_MODEL_NOT_CONFIGURED(20004, "未配置Embedding模型"),
    CHAT_MODEL_NOT_CONFIGURED(20005, "未配置对话模型"),
    API_KEY_INVALID(20006, "API密钥无效"),

    // ==================== 对话相关 (30xxx) ====================
    AGENT_NOT_FOUND(30001, "智能体不存在"),
    CONVERSATION_NOT_FOUND(30002, "会话不存在"),
    CHAT_FAILED(30003, "对话处理失败"),
    RAG_RETRIEVAL_FAILED(30004, "RAG检索失败"),
    TOOL_CALL_FAILED(30005, "工具调用失败"),

    // ==================== 工作流相关 (40xxx) ====================
    WORKFLOW_NOT_FOUND(40001, "工作流不存在"),
    WORKFLOW_EXECUTE_FAILED(40002, "工作流执行失败"),
    WORKFLOW_NODE_FAILED(40003, "工作流节点执行失败"),
    WORKFLOW_PARSE_FAILED(40004, "工作流解析失败"),
    WORKFLOW_EXECUTION_TIMEOUT(40005, "工作流执行超时"),
    WORKFLOW_PARALLEL_FAILED(40006, "并行执行失败"),

    // ==================== 数据源相关 (50xxx) ====================
    DATASOURCE_NOT_FOUND(50001, "数据源不存在"),
    DATASOURCE_CONNECT_FAILED(50002, "数据源连接失败"),
    SQL_EXECUTE_FAILED(50003, "SQL执行失败"),
    ES_QUERY_FAILED(50004, "Elasticsearch查询失败"),

    // ==================== 用户认证相关 (60xxx) ====================
    USER_NOT_FOUND(60001, "用户不存在"),
    PASSWORD_ERROR(60002, "密码错误"),
    TOKEN_INVALID(60003, "Token无效"),
    TOKEN_EXPIRED(60004, "Token已过期"),
    NO_PERMISSION(60005, "无权限访问");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
