package com.moyun.ext.ai2.handler;

import com.moyun.ext.ai.entity.AiSceneConfig;
import com.moyun.ext.ai.exception.BusinessException;
import com.moyun.ext.ai.exception.ErrorCode;
import com.moyun.ext.ai2.model.AiExecuteRequest;
import com.moyun.ext.ai2.model.AiExecuteResponse;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * AI场景处理器接口（统一接入层SPI）
 *
 * <p>每个AI场景实现一个Handler，由 {@code AiSceneRegistry} 按 sceneCode 注册路由。
 * 依据《AI能力统一接入层 — 完整方案文档》V2.0 §5.1。</p>
 *
 * <p>新增场景三步：1）实现本接口并注册为Spring Bean；2）ai_scene_config 表插入场景配置行；
 * 3）调用统一入口 POST /api/ai/execute。核心编排代码（网关）零改动。</p>
 *
 * @author laomao
 * @since 2026-09-09
 */
public interface AiSceneHandler {

    /**
     * 场景代码（用于注册，须与 ai_scene_config.scene_code 一致）
     */
    String getSceneCode();

    /**
     * 支持的输出模式：sync / stream / both
     */
    default String getSupportedOutputMode() {
        return "sync";
    }

    /**
     * 同步执行
     */
    default AiExecuteResponse<?> execute(AiExecuteRequest request) {
        throw new BusinessException(ErrorCode.SYSTEM_ERROR, "场景 [" + getSceneCode() + "] 未实现同步执行");
    }

    /**
     * 同步执行（带场景配置，v11.48）
     * <p>网关统一调用此方法：Handler 优先读 config 的提示词模板/输出结构（管理页改配置即时生效），
     * 缺省回落子类默认值。默认实现转发到无 config 版本保持兼容。</p>
     */
    default AiExecuteResponse<?> execute(AiExecuteRequest request, AiSceneConfig config) {
        return execute(request);
    }

    /**
     * 流式执行（SSE）
     */
    default void executeStream(AiExecuteRequest request, SseEmitter emitter) {
        throw new BusinessException(ErrorCode.SYSTEM_ERROR, "场景 [" + getSceneCode() + "] 未实现流式执行");
    }

    /**
     * 验证请求（网关在执行前调用）
     */
    default void validate(AiExecuteRequest request) {
        if (request.getInput() == null || request.getInput().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "输入参数不能为空");
        }
    }

    /**
     * 构建系统提示词（默认读取场景配置模板并渲染占位符）
     */
    default String buildSystemPrompt(AiExecuteRequest request, AiSceneConfig config) {
        return null;
    }

    /**
     * 构建用户提示词（默认读取场景配置模板并渲染占位符）
     */
    default String buildUserPrompt(AiExecuteRequest request, AiSceneConfig config) {
        return null;
    }
}
