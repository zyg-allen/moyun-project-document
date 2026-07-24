<template>
  <div
    v-if="show"
    class="fixed inset-0 z-50 flex items-center justify-center p-4"
    style="background-color: rgba(0, 0, 0, 0.5);"
    @click.self="handleClose"
  >
    <div
      class="w-full max-w-md rounded-2xl shadow-xl"
      style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);"
      role="dialog"
      aria-modal="true"
      aria-labelledby="tip-modal-title"
    >
      <!-- 头部 -->
      <div class="flex items-center justify-between p-5 border-b" style="border-color: var(--theme-border);">
        <h3 id="tip-modal-title" class="text-lg font-bold flex items-center gap-2" style="color: var(--theme-text);">
          <Gift class="w-5 h-5" style="color: var(--theme-primary);" />
          创作鼓励
        </h3>
        <button
          @click="handleClose"
          :disabled="tipping"
          class="p-1 rounded-lg transition hover:opacity-70 disabled:opacity-40"
          style="color: var(--theme-text-secondary);"
          aria-label="关闭"
        >
          <X class="w-5 h-5" />
        </button>
      </div>

      <!-- 内容 -->
      <div class="p-5">
        <!-- 作者信息 -->
        <div class="flex items-center gap-3 mb-5">
          <img
            :src="getSafeAvatar(authorAvatar, authorName)"
            :alt="authorName"
            class="w-10 h-10 rounded-full object-cover"
            loading="lazy"
          />
          <div class="min-w-0">
            <p class="font-medium truncate" style="color: var(--theme-text);">
              {{ authorName || '匿名作者' }}
            </p>
            <p v-if="targetTitle" class="text-xs truncate" style="color: var(--theme-text-secondary);">
              {{ targetTitle }}
            </p>
          </div>
        </div>

        <!-- 快捷积分 -->
        <div class="mb-4">
          <p class="text-sm mb-2" style="color: var(--theme-text-secondary);">选择鼓励积分</p>
          <div class="grid grid-cols-3 gap-2">
            <button
              v-for="amt in presetPoints"
              :key="amt"
              @click="tipAmount = amt"
              class="py-2 rounded-lg text-sm font-medium transition"
              :style="tipAmount === amt
                ? 'background-color: var(--theme-primary); color: white;'
                : 'background-color: var(--theme-accent); color: var(--theme-text);'"
            >
              {{ amt }}
            </button>
          </div>
        </div>

        <!-- 自定义积分 -->
        <div class="mb-4">
          <label for="tip-points-input" class="text-sm mb-2 block" style="color: var(--theme-text-secondary);">自定义积分</label>
          <input
            id="tip-points-input"
            v-model.number="tipAmount"
            type="number"
            min="1"
            step="1"
            placeholder="请输入积分数量"
            class="w-full px-3 py-2 rounded-lg text-sm focus:outline-none"
            style="background-color: var(--theme-bg); color: var(--theme-text); border: 1px solid var(--theme-border);"
          />
        </div>

        <!-- 留言 -->
        <div class="mb-5">
          <label for="tip-message-input" class="text-sm mb-2 block" style="color: var(--theme-text-secondary);">留言（选填）</label>
          <textarea
            id="tip-message-input"
            v-model="tipMessage"
            placeholder="说点什么鼓励一下作者..."
            rows="2"
            maxlength="100"
            class="w-full px-3 py-2 rounded-lg text-sm resize-none focus:outline-none"
            style="background-color: var(--theme-bg); color: var(--theme-text); border: 1px solid var(--theme-border);"
          />
        </div>

        <!-- 确认按钮 -->
        <button
          @click="handleTip"
          :disabled="tipping || !tipAmount || tipAmount <= 0"
          class="w-full py-3 rounded-xl font-medium text-sm transition-colors disabled:opacity-50 flex items-center justify-center gap-2"
          style="background-color: var(--theme-primary); color: white;"
        >
          <svg v-if="tipping" class="animate-spin w-4 h-4" viewBox="0 0 24 24" fill="none">
            <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
            <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
          </svg>
          <Gift v-else class="w-4 h-4" />
          {{ tipping ? '处理中...' : `鼓励 ${Math.floor(Number(tipAmount || 0))} 积分` }}
        </button>
        <p class="text-xs text-center mt-3" style="color: var(--theme-text-secondary);">
          积分将从你的账户扣除，作者将获得同等积分
        </p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue';
import { Gift, X } from 'lucide-vue-next';
import { tipTarget } from '@/api/tip';
import { getSafeAvatar } from '@/utils/avatar';
import type { TipTargetType } from '@/types';

const props = defineProps<{
  show: boolean;
  targetType: TipTargetType;
  targetId: string | number;
  authorAvatar?: string;
  authorName?: string;
  targetTitle?: string;
}>();

const emit = defineEmits<{
  (e: 'close'): void;
  (e: 'success'): void;
  (e: 'error', message: string): void;
}>();

// 快捷积分数（MVP 阶段，积分通过签到/任务获取）
const presetPoints = [10, 50, 100, 500, 1000, 2000];
const tipAmount = ref<number>(50);
const tipMessage = ref('');
const tipping = ref(false);

// 弹窗打开时重置为默认值
watch(() => props.show, (val) => {
  if (val) {
    tipAmount.value = 50;
    tipMessage.value = '';
  }
});

function handleClose() {
  if (tipping.value) return;
  emit('close');
}

async function handleTip() {
  const points = Math.floor(Number(tipAmount.value));
  if (!points || points <= 0) {
    emit('error', '请输入有效的积分数量');
    return;
  }
  tipping.value = true;
  try {
    const res = await tipTarget(props.targetType, props.targetId, {
      amount: points,
      message: tipMessage.value,
    });
    if (res.code === 200) {
      emit('success');
    } else {
      emit('error', res.message || '鼓励失败');
    }
  } catch (err) {
    const e = err as { message?: string };
    emit('error', e?.message || '鼓励失败，请稍后重试');
  } finally {
    tipping.value = false;
  }
}
</script>
