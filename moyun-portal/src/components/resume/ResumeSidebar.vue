<script setup lang="ts">
/**
 * 简历维护页 - 左侧导航栏
 * 对应 vue_resume_spec.md §5.1 ResumeSidebar
 * 锚点导航 + 完善进度 + CTA 入口（立即 AI 优化）
 */
import { computed } from 'vue';
import type { Component } from 'vue';
import { Sparkles } from 'lucide-vue-next';

export interface SidebarSection {
  id: string;
  label: string;
  icon: Component;
  status: 'done' | 'partial' | 'empty';
}

const props = defineProps<{
  sections: SidebarSection[];
  activeId: string;
  progress: { filled: number; total: number; percent: number };
  score?: number;
}>();

const emit = defineEmits<{
  (e: 'navigate', id: string): void;
  (e: 'optimize'): void;
}>();

const progressText = computed(() => `${props.progress.filled}/${props.progress.total} 已完成`);
</script>

<template>
  <aside class="re-sidebar">
    <div class="re-sb-group">
      <div class="re-sb-group-label">简历内容</div>
      <button
        v-for="s in sections"
        :key="s.id"
        type="button"
        class="re-sb-item"
        :class="{ active: s.id === activeId }"
        @click="emit('navigate', s.id)"
      >
        <component :is="s.icon" class="re-sb-icon" />
        <span class="re-sb-label">{{ s.label }}</span>
        <span class="re-sb-dot" :class="s.status"></span>
      </button>
    </div>

    <div class="re-sb-divider"></div>

    <div class="re-sb-progress">
      <div class="re-sb-progress-head">
        <span>完善进度</span>
        <strong>{{ progressText }}</strong>
      </div>
      <div class="re-sb-progress-track">
        <div class="re-sb-progress-fill" :style="{ width: progress.percent + '%' }"></div>
      </div>
    </div>

    <div class="re-sb-cta">
      <div class="re-sb-cta-icon">🚀</div>
      <p v-if="score !== undefined && score > 0">
        当前简历评分 <strong style="color: #d97706">{{ score }} 分</strong>，AI 深度优化可提升 20+ 分
      </p>
      <p v-else>
        完善简历后可进行 AI 深度优化，预计提升 20+ 分
      </p>
      <button type="button" class="re-sb-cta-btn" @click="emit('optimize')">
        <Sparkles class="w-3.5 h-3.5" />
        立即 AI 优化
      </button>
    </div>
  </aside>
</template>

<style scoped>
.re-sidebar {
  width: 220px;
  background: #fff;
  border-right: 1px solid var(--theme-border);
  padding: 20px 0;
  position: sticky;
  top: 72px;
  height: calc(100vh - 72px);
  overflow-y: auto;
  flex-shrink: 0;
}
.re-sidebar::-webkit-scrollbar { width: 4px; }
.re-sidebar::-webkit-scrollbar-thumb { background: var(--theme-border); border-radius: 2px; }

.re-sb-group-label {
  font-size: 10px;
  font-weight: 700;
  color: #9ca3af;
  text-transform: uppercase;
  letter-spacing: 0.8px;
  padding: 0 20px;
  margin-bottom: 6px;
}
.re-sb-item {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  padding: 8px 20px;
  cursor: pointer;
  transition: all 0.15s cubic-bezier(0.4, 0, 0.2, 1);
  font-size: 13px;
  color: #4b5563;
  border: none;
  background: none;
  border-left: 2px solid transparent;
  text-align: left;
}
.re-sb-item:hover { background: #f9fafb; color: var(--theme-text); }
.re-sb-item.active {
  background: #fef2f2;
  color: var(--theme-primary);
  font-weight: 600;
  border-left-color: var(--theme-primary);
}
.re-sb-icon { width: 16px; height: 16px; opacity: 0.7; flex-shrink: 0; }
.re-sb-item.active .re-sb-icon { opacity: 1; }
.re-sb-label { flex: 1; }
.re-sb-dot {
  width: 6px; height: 6px;
  border-radius: 50%;
  flex-shrink: 0;
}
.re-sb-dot.done { background: #10b981; }
.re-sb-dot.partial { background: #f59e0b; }
.re-sb-dot.empty { background: #d1d5db; }

.re-sb-divider { height: 1px; background: #f3f4f6; margin: 12px 16px; }

.re-sb-progress {
  margin: 4px 16px 12px;
  padding: 14px;
  background: #f9fafb;
  border-radius: 10px;
  border: 1px solid var(--theme-border);
}
.re-sb-progress-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
.re-sb-progress-head span { font-size: 11px; color: #6b7280; font-weight: 500; }
.re-sb-progress-head strong { font-size: 13px; color: var(--theme-primary); }
.re-sb-progress-track { height: 4px; background: var(--theme-border); border-radius: 2px; overflow: hidden; }
.re-sb-progress-fill {
  height: 100%;
  background: linear-gradient(90deg, var(--theme-primary), #ef4444);
  border-radius: 2px;
  transition: width 0.6s cubic-bezier(0, 0, 0.2, 1);
}

.re-sb-cta {
  margin: 12px 16px 0;
  padding: 16px;
  background: linear-gradient(135deg, #fef2f2, #fff7ed);
  border-radius: 10px;
  border: 1px solid #fecaca;
}
.re-sb-cta-icon { font-size: 20px; margin-bottom: 8px; }
.re-sb-cta p { font-size: 12px; color: #4b5563; line-height: 1.5; margin-bottom: 10px; }
.re-sb-cta-btn {
  width: 100%;
  padding: 8px;
  background: var(--theme-primary);
  color: #fff;
  border: none;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 5px;
}
.re-sb-cta-btn:hover {
  background: #b91c1c;
  transform: translateY(-1px);
  box-shadow: 0 4px 6px -1px rgba(0,0,0,0.07);
}

@media (max-width: 1024px) {
  .re-sidebar { display: none; }
}
</style>
