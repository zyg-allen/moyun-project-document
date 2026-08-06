package com.moyun.ext.ai.service.impl;

import com.moyun.ext.ai.dto.DiagramChatDTO;
import com.moyun.ext.ai.prompt.ArchitectureAwareness;
import com.moyun.ext.ai.prompt.DiagramSystemPrompt;
import com.moyun.ext.ai.service.DiagramChatService;
import com.moyun.ext.ai.service.LLMService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * 架构图对话服务实现 - V3
 *
 * <p>核心功能：</p>
 * <ul>
 *     <li>V3 (ELK模式): AI 输出语义 JSON，前端 ELK 计算布局</li>
 *     <li>V2 (兼容模式): AI 直接生成 Draw.io XML</li>
 *     <li>支持 display_diagram / edit_diagram / graph_data</li>
 * </ul>
 *
 * @author laomao
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DiagramChatServiceImpl implements DiagramChatService {

    private final LLMService llmService;
    
    /**
     * 是否使用 ELK 模式（V3）
     * true: AI 输出语义 JSON，前端 ELK 计算布局（推荐）
     * false: AI 直接输出完整 XML（旧模式）
     */
    private static final boolean USE_ELK_MODE = true;

    @Override
    public void generateStreamResponse(DiagramChatDTO dto, SseEmitter emitter) {
        // 异步执行，避免阻塞 HTTP 线程
        CompletableFuture.runAsync(() -> {
            try {
                // 0. 参数校验
                if (dto == null || !StringUtils.hasText(dto.getMessage())) {
                    emitter.send(SseEmitter.event()
                            .name("error")
                            .data("请输入您的需求"));
                    emitter.complete();
                    return;
                }
                
                // 1. 构建 Prompt
                String prompt = buildPrompt(dto);
                
                log.info("🎨 开始流式生成架构图, promptLength={}", prompt.length());
                
                // 使用 CountDownLatch 等待流式生成完成
                CountDownLatch latch = new CountDownLatch(1);
                
                // 2. 流式调用 LLM
                llmService.generateStream(
                        prompt,
                        // onToken: 每个 token 推送给前端
                        token -> {
                            try {
                                // 将 token 转换为 JSON 格式，避免换行符导致 SSE 解析问题
                                String jsonData = "{\"content\":\"" + 
                                        token.replace("\\", "\\\\")
                                             .replace("\"", "\\\"")
                                             .replace("\n", "\\n")
                                             .replace("\r", "\\r")
                                        + "\"}";
                                emitter.send(SseEmitter.event()
                                        .name("message")
                                        .data(jsonData));
                            } catch (IOException e) {
                                log.warn("SSE 发送失败: {}", e.getMessage());
                            }
                        },
                        // onComplete: 完成时发送结束信号
                        () -> {
                            try {
                                log.info("🎨 流式生成完成，发送 done 信号");
                                emitter.send(SseEmitter.event()
                                        .name("done")
                                        .data("[DONE]"));
                                emitter.complete();
                            } catch (IOException e) {
                                log.warn("SSE 完成信号发送失败: {}", e.getMessage());
                            } finally {
                                latch.countDown();
                            }
                        },
                        // onError: 错误处理
                        error -> {
                            try {
                                log.error("🎨 流式生成错误: {}", error.getMessage());
                                emitter.send(SseEmitter.event()
                                        .name("error")
                                        .data(error.getMessage()));
                                emitter.completeWithError(error);
                            } catch (IOException e) {
                                log.warn("SSE 错误信号发送失败: {}", e.getMessage());
                            } finally {
                                latch.countDown();
                            }
                        }
                );
                
                // 等待完成（最多 5 分钟）
                boolean completed = latch.await(5, TimeUnit.MINUTES);
                if (!completed) {
                    log.warn("🎨 流式生成超时");
                    emitter.send(SseEmitter.event()
                            .name("error")
                            .data("生成超时"));
                    emitter.complete();
                }
                
            } catch (Exception e) {
                log.error("流式生成失败: {}", e.getMessage(), e);
                try {
                    emitter.send(SseEmitter.event()
                            .name("error")
                            .data("生成失败: " + e.getMessage()));
                    emitter.completeWithError(e);
                } catch (IOException ex) {
                    log.warn("SSE 发送错误失败");
                }
            }
        });
    }

    /**
     * 构建 Prompt
     * 
     * 根据 USE_ELK_MODE 选择不同的提示词版本：
     * - ELK 模式: AI 输出语义 JSON，前端计算布局
     * - 传统模式: AI 直接输出完整 XML
     */
    private String buildPrompt(DiagramChatDTO dto) {
        StringBuilder sb = new StringBuilder();
        
        String userMessage = StringUtils.hasText(dto.getMessage()) ? dto.getMessage() : "请帮我设计一个系统架构";
        
        // 添加对话历史（如果有）
        boolean hasHistory = dto.getHistory() != null && !dto.getHistory().isEmpty();
        if (hasHistory) {
            sb.append("## 💬 对话历史\n\n");
            int count = 0;
            for (DiagramChatDTO.ChatMessage msg : dto.getHistory()) {
                if (count++ > 5) break;  // 最多保留5轮历史
                String role = "user".equals(msg.getRole()) ? "用户" : "助手";
                String content = msg.getContent();
                // 截断过长的内容
                if (content.length() > 300) {
                    content = content.substring(0, 300) + "...";
                }
                // 隐藏图表数据
                content = content.replaceAll("\\[DISPLAY_DIAGRAM\\][\\s\\S]*?\\[/DISPLAY_DIAGRAM\\]", "[图表XML]");
                content = content.replaceAll("\\[EDIT_DIAGRAM\\][\\s\\S]*?\\[/EDIT_DIAGRAM\\]", "[编辑指令]");
                content = content.replaceAll("\\[GRAPH_DATA\\][\\s\\S]*?\\[/GRAPH_DATA\\]", "[图表数据]");
                sb.append("**").append(role).append("**: ").append(content).append("\n\n");
            }
            sb.append("---\n\n");
        }
        
        // 根据模式选择提示词
        String prompt;
        if (USE_ELK_MODE) {
            // V3 ELK 模式：AI 只输出语义 JSON
            log.info("🎨 使用 ELK 模式 (V3+)");
            
            // 架构感知：分析用户需求，生成上下文增强
            String contextEnhancement = ArchitectureAwareness.generateContextPrompt(userMessage);
            if (!contextEnhancement.isEmpty()) {
                sb.append(contextEnhancement);
                sb.append("---\n\n");
            }
            
            prompt = DiagramSystemPrompt.getELKPromptWithContext(null, userMessage);
        } else {
            // V2 传统模式：AI 输出完整 XML
            log.info("🎨 使用传统模式 (V2)");
            String currentXml = dto.getCurrentDiagramXml();
            prompt = DiagramSystemPrompt.getPromptWithContext(currentXml, userMessage);
        }
        
        return sb.toString() + prompt;
    }
}
