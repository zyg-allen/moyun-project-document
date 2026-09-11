<template>
  <!--
    AI 实时辅助弹窗（v10.18 阶段二独立组件抽离）
    设计文档：docs/简历编辑和优化模块重构设计-20260826.md 阶段二
    职责：纯展示 + 事件回传，调用方负责 openFieldAssist / adoptAssist / autoSaveAfterAdopt
  -->
  <Teleport to="body">
    <div v-if="visible" class="re-advice-mask" @click.self="$emit('close')">
      <div class="re-advice-box" style="max-width: 640px;">
        <div class="re-advice-head">
          <h3>
            <Sparkles class="w-4 h-4" style="color: var(--theme-primary);" />
            AI 实时优化
            <span class="re-advice-grade" style="background: color-mix(in srgb, var(--theme-primary) 10%, transparent); color: var(--theme-primary);">
              {{ targetType === 'work' ? '工作描述' : targetType === 'project' ? '项目描述' : '自我评价' }}
            </span>
          </h3>
          <div class="re-advice-head-actions">
            <button class="re-advice-close" @click="$emit('close')">
              <X class="w-4 h-4" />
            </button>
          </div>
        </div>

        <!-- 原文（对比基准） -->
        <div v-if="originalText" class="re-assist-original">
          <div class="re-assist-label">📌 原始内容</div>
          <p>{{ originalText }}</p>
        </div>

        <!-- 加载中 -->
        <div v-if="loading" class="re-advice-loading">
          <div class="re-loading-spinner"></div>
          <p>AI 正在生成 3 个优化版本...</p>
        </div>

        <!-- 版本列表 -->
        <div v-else class="re-advice-body">
          <div
            v-for="(s, i) in suggestions"
            :key="i"
            class="re-assist-version"
            :class="{ 're-assist-recommend': i === 0 }"
          >
            <div class="re-assist-version-head">
              <span class="re-assist-version-tag">{{ versionLabels[i] || `版本 ${i + 1}` }}</span>
              <span v-if="s.reason" class="re-assist-reason">💡 {{ s.reason }}</span>
            </div>
            <p class="re-assist-text">{{ s.text }}</p>
            <button class="re-assist-adopt-btn" @click="$emit('adopt', s.text)">采纳此版本</button>
          </div>
          <div v-if="!suggestions.length" class="re-empty-tip" style="padding: 24px 0;">
            AI 未生成有效建议，请补充内容后重试
          </div>
          <div class="re-field-hint" style="margin-top: 8px;">
            提示：AI 可能使用 [X%]、[X万] 等占位符标记需你确认的数据，采纳后请替换为真实数值
          </div>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<script setup lang="ts">
import { Sparkles, X } from 'lucide-vue-next';
import type { FieldAssistSuggestion } from '@/api/resumeOptimize';

/**
 * AI 实时辅助弹窗（v10.18 阶段二独立组件）
 * props:
 *   - visible: 弹窗显示
 *   - loading: AI 生成中
 *   - suggestions: 3 个版本建议
 *   - originalText: 原文（对比基准）
 *   - versionLabels: 版本标签数组（默认 ['推荐版本', '量化版本', '简练版本']）
 *   - targetType: 字段类型 work/project/selfIntro，用于标题展示
 * emits:
 *   - close: 关闭弹窗
 *   - adopt(text): 采纳某版本，调用方负责替换字段 + autoSaveAfterAdopt
 */
defineProps<{
  visible: boolean;
  loading: boolean;
  suggestions: FieldAssistSuggestion[];
  originalText: string;
  versionLabels?: string[];
  targetType?: 'work' | 'project' | 'selfIntro' | string;
}>();

defineEmits<{
  (e: 'close'): void;
  (e: 'adopt', text: string): void;
}>();
</script>
