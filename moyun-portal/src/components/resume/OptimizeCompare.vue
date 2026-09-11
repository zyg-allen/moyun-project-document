<template>
  <!--
    深度优化前后对比组件（v10.18 阶段四独立组件抽离 / v10.20 交互优化）
    设计文档：docs/简历编辑和优化模块重构设计-20260826.md 阶段四
    职责：纯展示 + 事件回传，调用方负责 generateOptimize / toggleAdopted / adoptAll / regenerate / preview
    v10.20 改进：
      1. 采纳后卡片折叠为「已应用到 {{字段}}」状态，避免视觉割裂
      2. 每条建议新增「重新生成」按钮（emit regenerate 事件，调用方拉取 3 版本候选）
      3. 顶部新增「预览采纳结果」按钮（emit preview 事件，step4 内就地展开预览，不跳转）
  -->
  <div class="bg-theme-surface rounded-xl border border-theme-border p-6">
    <div class="flex items-center justify-between flex-wrap gap-3 mb-4">
      <h3 class="font-semibold flex items-center gap-2">
        <GitCompare class="w-4 h-4" style="color: var(--theme-primary);" /> 深度优化 · 前后对比
      </h3>
      <div class="flex gap-2 flex-wrap">
        <button
          v-if="!result"
          class="inline-flex items-center gap-1.5 text-sm px-4 py-2 rounded-lg text-white font-medium disabled:opacity-50"
          style="background: var(--theme-primary);"
          :disabled="optimizing"
          @click="$emit('generate')"
        >
          <Sparkles class="w-4 h-4" /> {{ optimizing ? 'AI 生成中...' : '生成深度优化建议' }}
        </button>
        <template v-else>
          <button class="inline-flex items-center gap-1.5 text-sm px-3 py-2 rounded-lg border border-theme-primary text-theme-primary font-medium" @click="$emit('adoptAll')">
            <CheckCircle2 class="w-4 h-4" /> 全部采纳
          </button>
          <button
            class="inline-flex items-center gap-1.5 text-sm px-3 py-2 rounded-lg border border-theme-border text-theme-text-secondary font-medium"
            :class="{ 'bg-theme-accent': previewVisible }"
            @click="$emit('preview')"
          >
            <Eye class="w-4 h-4" /> {{ previewVisible ? '收起预览' : '预览采纳结果' }}
            <span v-if="adoptedSet.size" class="text-xs px-1.5 py-0.5 rounded-full text-white" style="background: var(--theme-primary);">{{ adoptedSet.size }}</span>
          </button>
          <button class="text-sm px-3 py-2 rounded-lg border border-theme-border text-theme-text-secondary" @click="$emit('generate')" :disabled="optimizing">重新生成</button>
        </template>
      </div>
    </div>

    <p v-if="result?.summary" class="text-sm text-theme-text-secondary bg-theme-accent border border-theme-border rounded-lg p-3 mb-4">{{ result.summary }}</p>

    <div v-if="!result" class="py-8 text-center text-sm text-theme-text-secondary border border-dashed border-theme-border rounded-lg">
      基于「{{ targetPosition }}」的岗位要求，AI 将逐项改写简历内容（STAR 法则 + 量化数据）
    </div>

    <div v-else class="space-y-3">
      <div
        v-for="(item, i) in result.items"
        :key="i"
        class="rounded-lg border transition-all"
        :class="adoptedSet.has(i) ? 'border-theme-success bg-theme-success-bg/30' : 'border-theme-border'"
      >
        <!-- 头部：字段定位 + 操作按钮 -->
        <div class="flex items-center justify-between px-4 py-2.5 bg-theme-bg rounded-t-lg gap-2 flex-wrap">
          <div class="flex items-center gap-2 text-sm font-medium">
            <component :is="adoptedSet.has(i) ? CheckCircle2 : AlertCircle" class="w-4 h-4" :class="adoptedSet.has(i) ? 'text-theme-success' : 'text-theme-text-secondary'" />
            <span>{{ sectionLabel(item) }}</span>
            <span v-if="fieldLabel(item)" class="text-xs text-theme-text-secondary font-normal">· {{ fieldLabel(item) }}</span>
            <span v-if="adoptedSet.has(i)" class="text-xs px-2 py-0.5 rounded-full bg-theme-success-bg text-theme-success">已应用</span>
          </div>
          <div class="flex gap-1.5">
            <button
              v-if="!adoptedSet.has(i)"
              class="text-xs px-2.5 py-1.5 rounded-md font-medium inline-flex items-center gap-1"
              style="background: var(--theme-primary); color: white;"
              @click="$emit('toggle', i)"
            >
              <CheckCircle2 class="w-3 h-3" /> 采纳
            </button>
            <template v-else>
              <button
                class="text-xs px-2.5 py-1.5 rounded-md font-medium inline-flex items-center gap-1 border border-theme-border text-theme-text-secondary"
                :disabled="regeneratingIdx === i"
                @click="$emit('regenerate', i)"
              >
                <RefreshCw class="w-3 h-3" :class="regeneratingIdx === i ? 'animate-spin' : ''" />
                {{ regeneratingIdx === i ? '生成中...' : '重新生成' }}
              </button>
              <button
                class="text-xs px-2.5 py-1.5 rounded-md font-medium bg-theme-accent text-theme-text-secondary inline-flex items-center gap-1"
                @click="$emit('toggle', i)"
              >
                <X class="w-3 h-3" /> 撤销
              </button>
            </template>
          </div>
        </div>

        <!-- 已采纳：折叠为「已应用到 {{字段}}」状态，展示优化后内容作为当前内容 -->
        <div v-if="adoptedSet.has(i)" class="p-4">
          <div class="text-xs text-theme-success mb-1.5 flex items-center gap-1">
            <Sparkles class="w-3 h-3" /> 已写入「{{ sectionLabel(item) }}{{ fieldLabel(item) ? ' · ' + fieldLabel(item) : '' }}」
          </div>
          <p class="text-theme-text leading-relaxed bg-theme-success-bg rounded-lg p-2.5 whitespace-pre-line text-sm">{{ item.optimized }}</p>
          <p v-if="item.reason" class="text-xs text-theme-text-secondary mt-2">💡 {{ item.reason }}</p>
        </div>

        <!-- 未采纳：保留前后对比视图 -->
        <div v-else class="p-4 grid md:grid-cols-2 gap-3 text-sm">
          <div>
            <div class="text-xs text-theme-text-secondary mb-1.5">📌 优化前</div>
            <p class="text-theme-text-secondary leading-relaxed bg-theme-bg rounded-lg p-2.5 whitespace-pre-line">{{ item.original || '（空）' }}</p>
          </div>
          <div>
            <div class="text-xs text-theme-success mb-1.5 flex items-center gap-1"><Sparkles class="w-3 h-3" /> AI 优化后</div>
            <!-- v10.22 阶段四：diff 视图，高亮增删改，更直观看到哪里改了 -->
            <div class="text-theme-text leading-relaxed bg-theme-success-bg rounded-lg p-2.5">
              <DiffView :original="item.original" :optimized="item.optimized" />
            </div>
          </div>
        </div>
        <p v-if="!adoptedSet.has(i) && item.reason" class="px-4 pb-3 text-xs text-theme-text-secondary">💡 {{ item.reason }}</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { GitCompare, Sparkles, CheckCircle2, AlertCircle, RefreshCw, X, Eye } from 'lucide-vue-next';
import type { ResumeDeepOptimizeVO, ResumeOptimizeItem } from '@/types/api';
import DiffView from '@/components/resume/DiffView.vue';

/**
 * 深度优化前后对比组件（v10.18 阶段四独立组件 / v10.20 交互优化）
 * props:
 *   - result: 深度优化结果（含 items[]）
 *   - adoptedSet: 已采纳索引集合
 *   - optimizing: AI 生成中
 *   - targetPosition: 目标岗位文案（空状态占位展示）
 *   - sectionLabelMap: 段落标题映射（key=section, value=中文标签）
 *   - previewVisible: 就地预览面板是否展开（v10.20）
 *   - regeneratingIdx: 正在重新生成的建议索引（-1 表示无，v10.20）
 * emits:
 *   - generate: 生成/重新生成深度优化建议
 *   - toggle(index): 切换采纳状态
 *   - adoptAll: 全部采纳
 *   - regenerate(index): 单字段重新生成（调用方拉取 3 版本候选弹窗）
 *   - preview: 切换就地预览面板（不跳转 step5）
 */
const props = defineProps<{
  result: ResumeDeepOptimizeVO | null;
  adoptedSet: Set<number>;
  optimizing: boolean;
  targetPosition?: string;
  sectionLabelMap?: Record<string, string>;
  previewVisible?: boolean;
  regeneratingIdx?: number;
}>();

defineEmits<{
  (e: 'generate'): void;
  (e: 'toggle', index: number): void;
  (e: 'adoptAll'): void;
  (e: 'regenerate', index: number): void;
  (e: 'preview'): void;
}>();

const DEFAULT_SECTION_LABELS: Record<string, string> = {
  objective: '求职意向',
  education: '教育经历',
  work: '工作经历',
  project: '项目经历',
  skills: '专业技能',
  selfIntro: '自我评价',
};

const FIELD_LABELS: Record<string, string> = {
  position: '岗位',
  description: '描述',
  name: '名称',
};

/**
 * v10.20：section 归一化（与后端 ResumeDeepOptimizeService.normalizeSection 保持一致）
 * 兼容 LLM 返回 works/projects/experience 等变体，保证标题能正确显示为「工作经历/项目经历」
 */
function normalizeSection(raw?: string | null): string {
  if (!raw) return '';
  const s = raw.trim().toLowerCase();
  switch (s) {
    case 'works':
    case 'experience':
    case 'experiences':
    case 'working':
      return 'work';
    case 'projects':
    case 'project_experience':
      return 'project';
    case 'educations':
    case 'education_experience':
      return 'education';
    case 'self_intro':
    case 'selfintro':
    case 'selfintroduction':
    case 'introduction':
    case 'intro':
    case 'summary':
      return 'selfIntro';
    case 'job_intention':
    case 'jobintention':
    case 'intention':
      return 'objective';
    case 'skill':
    case 'skill_list':
    case 'skilllist':
      return 'skills';
    default:
      return s;
  }
}

/** 计算单项标题：section + index（如「工作经历 #2」） */
function sectionLabel(item: ResumeOptimizeItem): string {
  const map = props.sectionLabelMap ?? DEFAULT_SECTION_LABELS;
  // v10.20：归一化后再查表，兼容 LLM 返回变体
  const normalized = normalizeSection(item.section);
  const base = map[normalized] || map[item.section] || item.section;
  if (item.index != null && item.index > 0) {
    return `${base} #${item.index}`;
  }
  return base;
}

/** 字段名标签（如「描述」「岗位」） */
function fieldLabel(item: ResumeOptimizeItem): string {
  if (!item.field) return '';
  return FIELD_LABELS[item.field] || item.field;
}
</script>
