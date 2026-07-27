package com.moyun.agent.service;

/**
 * 知识库处理进度管理服务接口
 *
 * <p>基于Redis实现的进度管理，提供：
 * <ul>
 *   <li>实时更新处理进度</li>
 *   <li>刷新页面后进度不丢失</li>
 *   <li>分布式锁防止重复处理</li>
 *   <li>支持应用重启后状态恢复</li>
 * </ul>
 * </p>
 *
 * @author laomao
 */
public interface KnowledgeProcessProgressService {

    /**
     * 更新处理进度
     *
     * @param knowledgeId 知识库ID
     * @param progress 进度百分比（0-100）
     * @param message 进度消息
     * @param currentStep 当前步骤
     */
    void updateProgress(Long knowledgeId, int progress, String message, String currentStep);

    /**
     * 获取处理进度
     *
     * @param knowledgeId 知识库ID
     * @return 进度信息，如果不存在返回null
     */
    ProcessProgress getProgress(Long knowledgeId);

    /**
     * 尝试获取处理锁（防止重复处理）
     *
     * @param knowledgeId 知识库ID
     * @return true-获取成功，false-已有其他任务在处理
     */
    boolean tryLock(Long knowledgeId);

    /**
     * 释放处理锁
     *
     * @param knowledgeId 知识库ID
     */
    void releaseLock(Long knowledgeId);

    /**
     * 检查是否正在处理
     *
     * @param knowledgeId 知识库ID
     * @return true-正在处理，false-未在处理
     */
    boolean isProcessing(Long knowledgeId);

    /**
     * 清除进度信息（处理完成或失败后调用）
     *
     * @param knowledgeId 知识库ID
     */
    void clearProgress(Long knowledgeId);

    /**
     * 进度信息类
     */
    class ProcessProgress implements java.io.Serializable {
        private Long knowledgeId;
        private int progress;
        private String message;
        private String currentStep;
        private long updateTime;

        public Long getKnowledgeId() {
            return knowledgeId;
        }

        public void setKnowledgeId(Long knowledgeId) {
            this.knowledgeId = knowledgeId;
        }

        public int getProgress() {
            return progress;
        }

        public void setProgress(int progress) {
            this.progress = progress;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public String getCurrentStep() {
            return currentStep;
        }

        public void setCurrentStep(String currentStep) {
            this.currentStep = currentStep;
        }

        public long getUpdateTime() {
            return updateTime;
        }

        public void setUpdateTime(long updateTime) {
            this.updateTime = updateTime;
        }
    }
}
