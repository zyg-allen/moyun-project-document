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

        <!-- 支付方式切换（V11.0：积分 / 微信支付） -->
        <div class="mb-4">
          <p class="text-sm mb-2" style="color: var(--theme-text-secondary);">选择鼓励方式</p>
          <div class="grid grid-cols-2 gap-2">
            <button
              @click="payMode = 'points'"
              class="py-2.5 rounded-lg text-sm font-medium transition flex items-center justify-center gap-1.5"
              :style="payMode === 'points'
                ? 'background-color: var(--theme-primary); color: white;'
                : 'background-color: var(--theme-accent); color: var(--theme-text);'"
            >
              <Coins class="w-4 h-4" />
              积分鼓励
            </button>
            <button
              @click="payMode = 'wechat'"
              class="py-2.5 rounded-lg text-sm font-medium transition flex items-center justify-center gap-1.5"
              :style="payMode === 'wechat'
                ? 'background-color: #22c55e; color: white;'
                : 'background-color: var(--theme-accent); color: var(--theme-text);'"
            >
              <Wallet class="w-4 h-4" />
              微信打赏
            </button>
          </div>
        </div>

        <!-- 快捷积分（积分模式） -->
        <div v-if="payMode === 'points'" class="mb-4">
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

        <!-- 快捷金额（微信模式，元） -->
        <div v-if="payMode === 'wechat'" class="mb-4">
          <p class="text-sm mb-2" style="color: var(--theme-text-secondary);">选择打赏金额（元）</p>
          <div class="grid grid-cols-3 gap-2">
            <button
              v-for="amt in presetAmounts"
              :key="amt"
              @click="wechatAmount = amt"
              class="py-2 rounded-lg text-sm font-medium transition"
              :style="wechatAmount === amt
                ? 'background-color: #22c55e; color: white;'
                : 'background-color: var(--theme-accent); color: var(--theme-text);'"
            >
              ¥{{ amt }}
            </button>
          </div>
        </div>

        <!-- 自定义金额（双模式） -->
        <div class="mb-4">
          <label for="tip-points-input" class="text-sm mb-2 block" style="color: var(--theme-text-secondary);">
            {{ payMode === 'points' ? '自定义积分' : '自定义金额（元，0.01 ~ 10000）' }}
          </label>
          <input
            id="tip-points-input"
            :value="payMode === 'points' ? tipAmount : wechatAmount"
            @input="onCustomAmountInput($event)"
            type="number"
            :min="payMode === 'points' ? 1 : 0.01"
            :step="payMode === 'points' ? 1 : 0.01"
            :placeholder="payMode === 'points' ? '请输入积分数量' : '请输入打赏金额（元）'"
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
          @click="payMode === 'points' ? handleTip() : handleWechatTip()"
          :disabled="tipping || (payMode === 'points'
            ? (!tipAmount || tipAmount <= 0)
            : (!wechatAmount || wechatAmount < 0.01))"
          class="w-full py-3 rounded-xl font-medium text-sm transition-colors disabled:opacity-50 flex items-center justify-center gap-2"
          :style="payMode === 'wechat'
            ? 'background-color: #22c55e; color: white;'
            : 'background-color: var(--theme-primary); color: white;'"
        >
          <svg v-if="tipping" class="animate-spin w-4 h-4" viewBox="0 0 24 24" fill="none">
            <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
            <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
          </svg>
          <Gift v-else class="w-4 h-4" />
          {{ tipping ? '处理中...' : (payMode === 'points'
            ? `鼓励 ${Math.floor(Number(tipAmount || 0))} 积分`
            : `微信打赏 ¥${Number(wechatAmount || 0).toFixed(2)}`) }}
        </button>
        <p class="text-xs text-center mt-3" style="color: var(--theme-text-secondary);">
          {{ payMode === 'points'
            ? '积分将从你的账户扣除，作者将获得同等积分'
            : '微信支付成功后自动分账：作者获得扣除平台服务费后的金额' }}
        </p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue';
import { useRouter } from 'vue-router';
import { Gift, X, Coins, Wallet } from 'lucide-vue-next';
import { tipTarget, createWechatTip } from '@/api/tip';
import { getSafeAvatar } from '@/utils/avatar';
import { requireRealName } from '@/utils/creatorPermission';
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

const router = useRouter();

// 快捷积分数（MVP 阶段，积分通过签到/任务获取）
const presetPoints = [10, 50, 100, 500, 1000, 2000];
// 微信打赏快捷金额（元）
const presetAmounts = [5, 10, 20, 50, 100, 200];
// 支付模式：points=积分鼓励，wechat=微信打赏（V11.0）
const payMode = ref<'points' | 'wechat'>('points');
const tipAmount = ref<number>(50);
const wechatAmount = ref<number>(10);
const tipMessage = ref('');
const tipping = ref(false);

// 弹窗打开时重置为默认值
watch(() => props.show, (val) => {
  if (val) {
    payMode.value = 'points';
    tipAmount.value = 50;
    wechatAmount.value = 10;
    tipMessage.value = '';
  }
});

function onCustomAmountInput(event: Event) {
  const value = Number((event.target as HTMLInputElement).value);
  if (payMode.value === 'points') {
    tipAmount.value = value;
  } else {
    wechatAmount.value = value;
  }
}

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
  // v10.10 实名策略：打赏属积分消费敏感场景，前端先强制实名校验（后端同步兜底）
  if (!(await requireRealName())) return;
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

/**
 * 微信打赏（V11.0 公共支付通道）
 * 下单成功后跳转收银台页（二维码 + 3s 轮询支付状态）
 */
async function handleWechatTip() {
  const amount = Number(wechatAmount.value);
  if (!amount || amount < 0.01) {
    emit('error', '打赏金额不能低于 0.01 元');
    return;
  }
  if (amount > 10000) {
    emit('error', '单笔打赏不可超过 10000 元');
    return;
  }
  // 资金敏感场景：与积分打赏一致，前端先实名校验（后端同步兜底）
  if (!(await requireRealName())) return;
  tipping.value = true;
  try {
    const res = await createWechatTip(props.targetType, props.targetId, {
      amount,
      message: tipMessage.value,
    });
    if (res.code === 200 && res.data?.payNo) {
      const d = res.data;
      emit('close');
      router.push({
        path: '/pay/cashier',
        query: {
          payNo: d.payNo,
          codeUrl: d.codeUrl || '',
          amount: String(d.amount ?? amount),
          expireMinutes: '30',
          mockEnabled: String(d.mockEnabled ?? false),
        },
      });
    } else {
      emit('error', res.message || '下单失败');
    }
  } catch (err) {
    const e = err as { message?: string };
    emit('error', e?.message || '下单失败，请稍后重试');
  } finally {
    tipping.value = false;
  }
}
</script>
