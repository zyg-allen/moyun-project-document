<script setup lang="ts">
import { ref, watch } from 'vue';
import { Flag, X } from 'lucide-vue-next';
import { useToast } from '@/composables/useToast';
import { useAuth } from '@/composables/useAuth';
import { submitContentReport, type ReportType, type ReportTargetType } from '@/api/report';

interface Props {
  /** 双向绑定：是否显示 */
  modelValue: boolean;
  /** 举报目标类型：comment/article/user */
  targetType: ReportTargetType;
  /** 举报目标ID */
  targetId: string | number;
  /** 目标描述（用于弹窗标题展示，如"评论"/"文章"） */
  targetLabel?: string;
}

const props = withDefaults(defineProps<Props>(), {
  targetLabel: '内容',
});

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void;
  (e: 'success'): void;
}>();

const toast = useToast();
const { isAuthenticated, requireAuth } = useAuth();

const reportType = ref<ReportType>('spam');
const description = ref('');
const submitting = ref(false);

const reportTypeOptions: { value: ReportType; label: string }[] = [
  { value: 'spam', label: '垃圾内容' },
  { value: 'inappropriate', label: '不当内容' },
  { value: 'infringement', label: '侵权内容' },
  { value: 'fraud', label: '欺诈行为' },
  { value: 'other', label: '其他问题' },
];

// 打开弹窗时重置表单
watch(
  () => props.modelValue,
  (visible) => {
    if (visible) {
      reportType.value = 'spam';
      description.value = '';
      submitting.value = false;
    }
  },
);

function close() {
  if (submitting.value) return;
  emit('update:modelValue', false);
}

async function handleSubmit() {
  if (!isAuthenticated()) {
    toast.warning('请先登录后再举报');
    requireAuth();
    return;
  }
  if (!description.value.trim()) {
    toast.warning('请描述举报原因');
    return;
  }
  submitting.value = true;
  try {
    const res = await submitContentReport(
      props.targetType,
      props.targetId,
      reportType.value,
      description.value.trim(),
    );
    if (res.code === 200) {
      toast.success('举报已提交，我们会尽快处理');
      emit('success');
      emit('update:modelValue', false);
    } else {
      toast.error(res.message || '提交失败');
    }
  } catch (err) {
    const e = err as { message?: string };
    toast.error(e?.message || '提交失败，请稍后重试');
  } finally {
    submitting.value = false;
  }
}
</script>

<template>
  <Teleport to="body">
    <Transition name="report-fade">
      <div
        v-if="modelValue"
        class="fixed inset-0 z-50 flex items-center justify-center p-4"
        role="dialog"
        aria-modal="true"
        aria-labelledby="report-dialog-title"
      >
        <!-- 遮罩 -->
        <div
          class="absolute inset-0 bg-black/50"
          style="background-color: rgba(0, 0, 0, 0.5);"
          @click="close"
        />
        <!-- 弹窗主体 -->
        <div
          class="relative w-full max-w-md rounded-2xl shadow-xl"
          style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);"
        >
          <!-- 头部 -->
          <div class="flex items-center justify-between p-5 border-b" style="border-color: var(--theme-border);">
            <div class="flex items-center gap-2">
              <Flag class="w-5 h-5" style="color: var(--theme-primary);" aria-hidden="true" />
              <h3 id="report-dialog-title" class="text-lg font-bold" style="color: var(--theme-text);">
                举报{{ targetLabel }}
              </h3>
            </div>
            <button
              @click="close"
              class="p-1 rounded-full transition-colors"
              style="color: var(--theme-text-secondary);"
              aria-label="关闭"
            >
              <X class="w-5 h-5" />
            </button>
          </div>

          <!-- 表单 -->
          <div class="p-5 space-y-4">
            <div>
              <label class="block text-sm font-medium mb-2" style="color: var(--theme-text);">
                举报理由 <span style="color: var(--theme-primary);">*</span>
              </label>
              <select
                v-model="reportType"
                class="w-full px-3 py-2 rounded-lg border text-sm focus:outline-none focus:ring-2"
                style="border-color: var(--theme-border); background-color: var(--theme-bg); color: var(--theme-text);"
              >
                <option v-for="opt in reportTypeOptions" :key="opt.value" :value="opt.value">
                  {{ opt.label }}
                </option>
              </select>
            </div>
            <div>
              <label class="block text-sm font-medium mb-2" style="color: var(--theme-text);">
                问题描述 <span style="color: var(--theme-primary);">*</span>
              </label>
              <textarea
                v-model="description"
                rows="4"
                maxlength="2000"
                placeholder="请详细描述违规情况，帮助我们快速处理..."
                class="w-full px-3 py-2 rounded-lg border text-sm resize-none focus:outline-none focus:ring-2"
                style="border-color: var(--theme-border); background-color: var(--theme-bg); color: var(--theme-text);"
              />
            </div>
          </div>

          <!-- 底部按钮 -->
          <div class="flex justify-end gap-3 p-5 border-t" style="border-color: var(--theme-border);">
            <button
              @click="close"
              :disabled="submitting"
              class="px-5 py-2 rounded-full font-medium text-sm transition-colors disabled:opacity-50"
              style="color: var(--theme-text-secondary);"
            >
              取消
            </button>
            <button
              @click="handleSubmit"
              :disabled="submitting || !description.trim()"
              class="px-5 py-2 rounded-full font-medium text-sm transition-colors disabled:opacity-50 flex items-center gap-2"
              style="background-color: var(--theme-primary); color: white;"
            >
              <svg v-if="submitting" class="animate-spin w-4 h-4" viewBox="0 0 24 24" fill="none">
                <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" />
                <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
              </svg>
              {{ submitting ? '提交中...' : '提交举报' }}
            </button>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
.report-fade-enter-active,
.report-fade-leave-active {
  transition: opacity 0.2s ease;
}
.report-fade-enter-from,
.report-fade-leave-to {
  opacity: 0;
}
</style>
