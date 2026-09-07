package com.moyun.portal.handler;

import java.net.URI;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moyun.ext.ai.entity.ModelConfig;
import com.moyun.ext.ai.service.ModelConfigService;

/**
 * 语音识别实时流式中继（fun-asr-realtime）—— V10.1 语音面试官
 *
 * <p>浏览器无法直连百炼（API Key 不能下发前端），本处理器作为双向中继：</p>
 * <pre>
 * 浏览器（AudioWorklet PCM 16kHz） --二进制帧--&gt; 本中继 --原样转发--&gt; 百炼 WebSocket
 * 浏览器 &lt;--{"type":"interim"/"final","text":..}-- 本中继 &lt;--result-generated-- 百炼
 * </pre>
 *
 * <p>浏览器端协议：</p>
 * <ul>
 *   <li>连接：ws://host/ws-asr?token=xxx（握手鉴权由 PortalWebSocketAuthInterceptor 完成）</li>
 *   <li>上行二进制帧：PCM 16kHz 16-bit 单声道裸流（无需 WAV 头）</li>
 *   <li>上行文本：{"action":"stop"} 结束识别 / {"action":"abort"} 丢弃中止</li>
 *   <li>下行文本：{"type":"ready"} 任务已启动 /
 *       {"type":"interim","text":..} 中间结果 /
 *       {"type":"final","text":..} 句子定稿 /
 *       {"type":"finished"} 全部结束 /
 *       {"type":"error","message":..} 失败</li>
 * </ul>
 *
 * <p>上游协议（百炼 WebSocket API）：
 * wss://dashscope.aliyuncs.com/api-ws/v1/inference/，
 * run-task → (task-started 后) 连续发送音频帧 → result-generated 增量返回 → finish-task → task-finished。
 * API 文档：https://help.aliyun.com/zh/model-studio/fun-asr-realtime-websocket-api</p>
 *
 * <p>API Key 取值：后台「AI 模块 → 模型配置」model_type='asr' 默认启用项 → moyun.ai.api-key
 * → langchain4j.dashscope.api-key。</p>
 *
 * @author moyun
 */
@Component
public class AsrStreamRelayHandler extends AbstractWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(AsrStreamRelayHandler.class);

    /** 握手 attributes 中存放中继上下文的 key */
    private static final String CTX_ATTR = "asrRelayCtx";

    /** 上游连接建立超时（ms） */
    private static final long UPSTREAM_CONNECT_TIMEOUT_MS = 8_000;

    /** 单次识别最长时长（安全上限，超时自动 finish-task）：10 分钟 */
    private static final long MAX_STREAM_MS = 10 * 60 * 1000L;

    /** task-started 前待转发音频的内存上限（超出丢最旧，防止内存膨胀） */
    private static final long PENDING_AUDIO_LIMIT = 2_000_000L;

    /**
     * 中继会话上下文：一个浏览器会话对应一条上游连接。
     * pendingAudio/taskStarted 等可变状态均以 ctx 为锁同步。
     */
    private static final class RelayContext {
        final WebSocketSession browser;
        final String taskId;
        volatile WebSocketSession upstream;
        /** 上游 task-started 已收到，可直发音频 */
        boolean taskStarted;
        /** finish-task 已发送（停止采集后等待剩余结果） */
        boolean finishRequested;
        /** 浏览器已收到终结通知（finished/error），不再重复通知 */
        volatile boolean doneNotified;
        /** 用户中止：直接关闭上游，不 finish */
        volatile boolean cancelled;
        /** task-started 前缓存的音频帧 */
        final ArrayDeque<byte[]> pendingAudio = new ArrayDeque<>();
        long pendingBytes = 0L;
        ScheduledFuture<?> maxDurationGuard;

        RelayContext(WebSocketSession browser) {
            this.browser = browser;
            this.taskId = UUID.randomUUID().toString().replace("-", "").substring(0, 32);
        }
    }

    /** 上游（百炼）连接的事件处理 */
    private class UpstreamHandler extends AbstractWebSocketHandler {

        private final RelayContext ctx;

        UpstreamHandler(RelayContext ctx) {
            this.ctx = ctx;
        }

        @Override
        public void afterConnectionEstablished(WebSocketSession session) {
            ctx.upstream = session;
            // 连接建立后立即下发 run-task
            sendUpstreamJson(ctx, buildRunTask(ctx.taskId));
        }

        @Override
        protected void handleTextMessage(WebSocketSession session, TextMessage message) {
            try {
                JsonNode root = objectMapper.readTree(message.getPayload());
                String event = root.path("header").path("event").asText("");
                switch (event) {
                    case "task-started" -> {
                        synchronized (ctx) {
                            ctx.taskStarted = true;
                            flushPendingAudio(ctx);
                        }
                        log.info("[ASR-Relay] 任务已启动 taskId={}", ctx.taskId);
                        sendToBrowser(ctx, Map.of("type", "ready"));
                    }
                    case "result-generated" -> {
                        JsonNode sentence = root.path("payload").path("output").path("sentence");
                        String text = sentence.path("text").asText("");
                        if (!text.isBlank()) {
                            // 句子定稿标记：实测 fun-asr-realtime 为 sentence_end（兼容旧协议 is_end）
                            boolean isEnd = sentence.path("sentence_end").asBoolean(false)
                                    || sentence.path("is_end").asBoolean(false);
                            sendToBrowser(ctx, Map.of(
                                    "type", isEnd ? "final" : "interim",
                                    "text", text));
                        }
                    }
                    case "task-finished" -> {
                        log.info("[ASR-Relay] 任务完成 taskId={}", ctx.taskId);
                        notifyDone(ctx, Map.of("type", "finished"));
                    }
                    case "task-failed" -> {
                        String errMsg = root.path("header").path("error_message").asText("语音识别失败");
                        log.warn("[ASR-Relay] 任务失败 taskId={} err={}", ctx.taskId, errMsg);
                        notifyDone(ctx, Map.of("type", "error", "message", errMsg));
                    }
                    default -> log.debug("[ASR-Relay] 忽略上游事件 {} taskId={}", event, ctx.taskId);
                }
            } catch (Exception e) {
                log.error("[ASR-Relay] 解析上游消息失败 taskId={}", ctx.taskId, e);
            }
        }

        @Override
        public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) {
            // 上游意外关闭（未收到 finished/error）：通知浏览器并关闭其会话
            if (!ctx.doneNotified) {
                log.warn("[ASR-Relay] 上游连接关闭 taskId={} status={}", ctx.taskId, closeStatus);
                notifyDone(ctx, Map.of("type", "error", "message", "语音识别连接中断"));
            }
        }

        @Override
        public void handleTransportError(WebSocketSession session, Throwable ex) {
            log.warn("[ASR-Relay] 上游传输错误 taskId={}", ctx.taskId, ex);
        }
    }

    @Autowired(required = false)
    private ModelConfigService modelConfigService;

    @Value("${moyun.ai.api-key:}")
    private String moyunAiApiKey;

    @Value("${langchain4j.dashscope.api-key:}")
    private String dashscopeApiKey;

    /** fun-asr-realtime 上游 WebSocket 地址 */
    @Value("${moyun.ai.asr-realtime-url:wss://dashscope.aliyuncs.com/api-ws/v1/inference/}")
    private String asrRealtimeUrl;

    /** 实时识别模型名 */
    @Value("${moyun.ai.asr-realtime-model:fun-asr-realtime}")
    private String asrRealtimeModel;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final WebSocketClient upstreamClient = new StandardWebSocketClient();

    /** 单线程守护调度器：负责 10 分钟安全上限自动 finish */
    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "asr-relay-guard");
                t.setDaemon(true);
                return t;
            });

    // ==================== 浏览器侧 ====================

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        RelayContext ctx = new RelayContext(session);
        session.getAttributes().put(CTX_ATTR, ctx);
        try {
            String apiKey = resolveApiKey();
            if (apiKey == null || apiKey.isBlank()) {
                log.warn("[ASR-Relay] 未配置 API Key，拒绝连接");
                sendToBrowser(ctx, Map.of("type", "error", "message",
                        "语音识别服务未配置：请在后台模型配置中添加 model_type='asr' 的默认启用项"));
                notifyDone(ctx, Map.of("type", "error", "message", "语音识别服务未配置"));
                closeQuietly(session);
                return;
            }

            WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
            headers.add(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey);
            // doHandshake 异步建立上游连接；run-task 在 UpstreamHandler.afterConnectionEstablished 中发送
            upstreamClient.doHandshake(new UpstreamHandler(ctx), headers, URI.create(asrRealtimeUrl.trim()));
            // 10 分钟安全上限：超时自动 finish-task，防止会话泄漏
            synchronized (ctx) {
                ctx.maxDurationGuard = scheduler.schedule(
                        () -> requestFinish(ctx), MAX_STREAM_MS, TimeUnit.MILLISECONDS);
            }
        } catch (Exception e) {
            log.error("[ASR-Relay] 上游连接建立失败 taskId={}", ctx.taskId, e);
            notifyDone(ctx, Map.of("type", "error", "message", "语音识别服务连接失败"));
        }
    }

    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) {
        RelayContext ctx = (RelayContext) session.getAttributes().get(CTX_ATTR);
        if (ctx == null || ctx.cancelled || ctx.finishRequested) return;

        // payload 底层缓冲可能被容器复用，必须拷贝
        ByteBuffer payload = message.getPayload();
        byte[] copy = new byte[message.getPayloadLength()];
        payload.get(copy);

        synchronized (ctx) {
            if (ctx.upstream != null && ctx.upstream.isOpen() && ctx.taskStarted) {
                sendUpstreamBinary(ctx, copy);
            } else {
                // task-started 前先缓存
                ctx.pendingAudio.addLast(copy);
                ctx.pendingBytes += copy.length;
                while (ctx.pendingBytes > PENDING_AUDIO_LIMIT) {
                    byte[] dropped = ctx.pendingAudio.pollFirst();
                    if (dropped == null) break;
                    ctx.pendingBytes -= dropped.length;
                }
            }
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        RelayContext ctx = (RelayContext) session.getAttributes().get(CTX_ATTR);
        if (ctx == null) return;
        try {
            String action = objectMapper.readTree(message.getPayload()).path("action").asText("");
            switch (action) {
                case "stop" -> requestFinish(ctx);
                case "abort" -> {
                    log.info("[ASR-Relay] 用户中止 taskId={}", ctx.taskId);
                    ctx.cancelled = true;
                    notifyDone(ctx, Map.of("type", "finished"));
                }
                default -> log.debug("[ASR-Relay] 忽略浏览器控制消息 {}", action);
            }
        } catch (Exception e) {
            log.warn("[ASR-Relay] 解析浏览器控制消息失败", e);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) {
        RelayContext ctx = (RelayContext) session.getAttributes().get(CTX_ATTR);
        if (ctx == null) return;
        ctx.cancelled = true;
        closeUpstream(ctx);
        cancelGuard(ctx);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable ex) {
        RelayContext ctx = (RelayContext) session.getAttributes().get(CTX_ATTR);
        if (ctx == null) return;
        log.warn("[ASR-Relay] 浏览器连接错误 taskId={}", ctx.taskId, ex);
        ctx.cancelled = true;
        closeUpstream(ctx);
        cancelGuard(ctx);
    }

    // ==================== 中继内部 ====================

    /** 浏览器请求结束：发送 finish-task，等待上游回吐剩余结果后 task-finished */
    private void requestFinish(RelayContext ctx) {
        synchronized (ctx) {
            if (ctx.finishRequested || ctx.cancelled) return;
            ctx.finishRequested = true;
            if (ctx.upstream != null && ctx.upstream.isOpen()) {
                sendUpstreamJson(ctx, Map.of(
                        "header", Map.of("action", "finish-task", "task_id", ctx.taskId, "streaming", "duplex"),
                        "payload", Map.of("input", Map.of())));
            }
        }
        cancelGuard(ctx);
    }

    /** 通知浏览器终结消息并关闭两侧连接（task-finished / task-failed / 中止共用） */
    private void notifyDone(RelayContext ctx, Map<String, Object> msg) {
        if (ctx.doneNotified) return;
        ctx.doneNotified = true;
        sendToBrowser(ctx, msg);
        closeUpstream(ctx);
        cancelGuard(ctx);
        closeQuietly(ctx.browser);
    }

    /** task-started 后将缓存的音频一次性发往上游 */
    private void flushPendingAudio(RelayContext ctx) {
        while (!ctx.pendingAudio.isEmpty()) {
            byte[] audio = ctx.pendingAudio.pollFirst();
            ctx.pendingBytes -= audio.length;
            sendUpstreamBinary(ctx, audio);
        }
    }

    private void sendUpstreamBinary(RelayContext ctx, byte[] bytes) {
        try {
            ctx.upstream.sendMessage(new BinaryMessage(ByteBuffer.wrap(bytes)));
        } catch (Exception e) {
            log.warn("[ASR-Relay] 上游音频发送失败 taskId={}", ctx.taskId, e);
        }
    }

    private void sendUpstreamJson(RelayContext ctx, Object body) {
        try {
            String json = objectMapper.writeValueAsString(body);
            WebSocketSession upstream = ctx.upstream;
            if (upstream != null && upstream.isOpen()) {
                synchronized (upstream) {
                    upstream.sendMessage(new TextMessage(json));
                }
            }
        } catch (Exception e) {
            log.error("[ASR-Relay] 上游指令发送失败 taskId={}", ctx.taskId, e);
        }
    }

    private void sendToBrowser(RelayContext ctx, Object body) {
        try {
            String json = objectMapper.writeValueAsString(body);
            if (ctx.browser.isOpen()) {
                synchronized (ctx.browser) {
                    ctx.browser.sendMessage(new TextMessage(json));
                }
            }
        } catch (Exception e) {
            log.warn("[ASR-Relay] 浏览器消息发送失败 taskId={}", ctx.taskId, e);
        }
    }

    private Map<String, Object> buildRunTask(String taskId) {
        return Map.of(
                "header", Map.of("action", "run-task", "task_id", taskId, "streaming", "duplex"),
                "payload", Map.of(
                        "task_group", "audio",
                        "task", "asr",
                        "function", "recognition",
                        "model", asrRealtimeModel,
                        "parameters", Map.of("sample_rate", 16000, "format", "pcm"),
                        "input", Map.of()));
    }

    private void closeUpstream(RelayContext ctx) {
        WebSocketSession upstream = ctx.upstream;
        if (upstream != null && upstream.isOpen()) {
            closeQuietly(upstream);
        }
    }

    private void cancelGuard(RelayContext ctx) {
        ScheduledFuture<?> guard = ctx.maxDurationGuard;
        if (guard != null) {
            guard.cancel(false);
        }
    }

    private void closeQuietly(WebSocketSession session) {
        try {
            session.close(CloseStatus.NORMAL);
        } catch (Exception ignored) {
            // no-op
        }
    }

    /** API Key 取值：后台模型配置（asr 默认启用项）→ moyun.ai.api-key → langchain4j.dashscope.api-key */
    private String resolveApiKey() {
        if (modelConfigService != null) {
            try {
                ModelConfig config = modelConfigService.getDefaultAsrConfig();
                if (config != null && config.getApiKey() != null && !config.getApiKey().isBlank()) {
                    return config.getApiKey();
                }
            } catch (Exception e) {
                log.warn("[ASR-Relay] 读取后台模型配置失败，回退 yaml: {}", e.getMessage());
            }
        }
        if (moyunAiApiKey != null && !moyunAiApiKey.isBlank()) {
            return moyunAiApiKey;
        }
        return dashscopeApiKey;
    }
}
