<template>
  <div class="min-h-screen py-8 px-4" style="background-color: var(--theme-bg);">
    <div class="max-w-lg mx-auto">
      <!-- 顶部返回 -->
      <div class="flex items-center justify-between mb-6">
        <button
          class="flex items-center gap-2 text-sm transition-colors"
          style="color: var(--theme-text-secondary);"
          @click="goBack"
        >
          <ArrowLeft class="w-4 h-4" />
          返回
        </button>
        <h1 class="text-lg font-semibold" style="color: var(--theme-text);">收银台</h1>
        <span class="w-12"></span>
      </div>

      <!-- 加载中 -->
      <div v-if="loading" class="rounded-2xl border p-10 text-center" style="background-color: var(--theme-surface); border-color: var(--theme-border);">
        <div class="inline-block w-8 h-8 border-2 rounded-full animate-spin"
             style="border-color: var(--theme-border); border-top-color: var(--theme-primary);"></div>
        <p class="mt-4 text-sm" style="color: var(--theme-text-secondary);">正在创建订单…</p>
      </div>

      <!-- 错误态 -->
      <div v-else-if="error" class="rounded-2xl border p-10 text-center" style="background-color: var(--theme-surface); border-color: var(--theme-border);">
        <AlertCircle class="w-10 h-10 mx-auto mb-3" style="color: var(--theme-danger);" />
        <p class="text-sm mb-4" style="color: var(--theme-text-secondary);">{{ error }}</p>
        <button
          class="px-6 py-2 rounded-lg text-sm font-medium text-white transition-opacity hover:opacity-90"
          style="background-color: var(--theme-primary);"
          @click="goBack"
        >
          返回
        </button>
      </div>

      <!-- 支付成功态 -->
      <div v-else-if="paid" class="rounded-2xl border p-10 text-center" style="background-color: var(--theme-surface); border-color: var(--theme-border);">
        <div class="w-14 h-14 rounded-full flex items-center justify-center mx-auto mb-4"
             style="background-color: rgba(34, 197, 94, 0.1);">
          <CheckCircle class="w-8 h-8" style="color: #22c55e;" />
        </div>
        <h2 class="text-lg font-semibold mb-1" style="color: var(--theme-text);">支付成功</h2>
        <p class="text-sm mb-2" style="color: var(--theme-text-secondary);">
          {{ amountYuan }} 元已支付{{ settled ? '，分账已完成' : '' }}
        </p>
        <p class="text-xs mb-6" style="color: var(--theme-text-secondary);">支付单号：{{ payNo }}</p>
        <div class="flex gap-3 justify-center">
          <button
            class="px-6 py-2 rounded-lg text-sm font-medium text-white transition-opacity hover:opacity-90"
            style="background-color: var(--theme-primary);"
            @click="goBack"
          >
            完成并返回
          </button>
          <button
            class="px-6 py-2 rounded-lg text-sm font-medium border transition-colors"
            style="color: var(--theme-text); border-color: var(--theme-border);"
            @click="router.push('/pay/wallet')"
          >
            查看钱包
          </button>
        </div>
      </div>

      <!-- 待支付态：二维码 -->
      <div v-else class="rounded-2xl border overflow-hidden" style="background-color: var(--theme-surface); border-color: var(--theme-border);">
        <div class="px-6 pt-6 pb-5 text-center border-b" style="border-color: var(--theme-border);">
          <p class="text-xs mb-1" style="color: var(--theme-text-secondary);">应付金额</p>
          <p class="text-4xl font-bold" style="color: var(--theme-text);">
            <span class="text-base font-normal mr-1">¥</span>{{ amountYuan }}
          </p>
          <p class="text-xs mt-2" style="color: var(--theme-text-secondary);">
            支付单号：{{ payNo }}
          </p>
        </div>

        <div class="p-6 flex flex-col items-center">
          <div class="p-3 rounded-xl bg-white border" style="border-color: var(--theme-border);">
            <canvas ref="qrCanvasRef" class="block"></canvas>
          </div>
          <p class="text-sm mt-4 flex items-center gap-1.5" style="color: var(--theme-text-secondary);">
            <QrCode class="w-4 h-4" />
            请使用微信扫码完成支付
          </p>
          <p class="text-xs mt-1.5" style="color: var(--theme-text-secondary);">
            <Clock class="w-3 h-3 inline mr-0.5" />订单将在 {{ expireMinutes }} 分钟后自动关闭
          </p>

          <!-- 状态轮询指示 -->
          <p class="text-xs mt-3 flex items-center gap-1.5" style="color: var(--theme-text-secondary);">
            <span class="w-1.5 h-1.5 rounded-full animate-pulse" style="background-color: var(--theme-primary);"></span>
            正在等待支付结果…
          </p>

          <!-- mock 模拟支付按钮 -->
          <button
            v-if="mockEnabled"
            class="mt-5 w-full py-2.5 rounded-lg text-sm font-medium text-white transition-opacity hover:opacity-90"
            style="background: linear-gradient(90deg, #22c55e, #16a34a);"
            @click="handleMockPay"
          >
            模拟支付成功（沙箱演练）
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useToast } from '@/composables/useToast';
import { ref, computed, onMounted, onUnmounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ArrowLeft, CheckCircle, AlertCircle, QrCode, Clock } from 'lucide-vue-next';
import QRCode from 'qrcode';
import { getPayStatus, mockPaySuccess } from '@/api/pay';
import { useUserStore } from '@/stores/user';

const toast = useToast();

const route = useRoute();
const router = useRouter();
const userStore = useUserStore();

const qrCanvasRef = ref<HTMLCanvasElement | null>(null);
const loading = ref(true);
const error = ref('');
const payNo = ref('');
const codeUrl = ref('');
const amountYuan = ref('0.00');
const expireMinutes = ref(30);
const mockEnabled = ref(false);
const paid = ref(false);
const settled = ref(false);

let pollTimer: ReturnType<typeof setInterval> | null = null;

const goBack = () => {
  if (window.history.length > 1) {
    router.back();
  } else {
    router.push('/');
  }
};

const renderQr = async (text: string) => {
  if (!qrCanvasRef.value || !text) return;
  await QRCode.toCanvas(qrCanvasRef.value, text, {
    width: 200,
    margin: 1,
    color: { dark: '#000000', light: '#ffffff' },
  });
};

const pollStatus = async () => {
  if (!payNo.value) return;
  try {
    const res = await getPayStatus(payNo.value);
    const data = res.data;
    if (!data) return;
    mockEnabled.value = !!data.mockEnabled;
    if (data.status === 'PAID') {
      paid.value = true;
      stopPolling();
      toast.success('支付成功');
    } else if (data.status === 'SETTLED') {
      paid.value = true;
      settled.value = true;
      stopPolling();
      toast.success('支付成功，分账已完成');
    } else if (data.status === 'CLOSED') {
      stopPolling();
      error.value = '订单已关闭（超时未支付或已手动关单）';
    }
  } catch {
    // 轮询失败静默重试
  }
};

const stopPolling = () => {
  if (pollTimer) {
    clearInterval(pollTimer);
    pollTimer = null;
  }
};

const handleMockPay = async () => {
  try {
    await mockPaySuccess(payNo.value);
    await pollStatus();
  } catch (e) {
    toast.error(e instanceof Error ? e.message : '模拟支付失败');
  }
};

onMounted(async () => {
  if (!userStore.isAuthenticated) {
    router.replace('/login?redirect=' + encodeURIComponent(route.fullPath));
    return;
  }
  const no = String(route.query.payNo || '');
  const code = String(route.query.codeUrl || '');
  const amt = String(route.query.amount || '0');
  const expire = String(route.query.expireMinutes || '30');
  const mock = String(route.query.mockEnabled || '');
  if (!no) {
    error.value = '缺少支付单号，请从打赏入口重新发起';
    loading.value = false;
    return;
  }
  payNo.value = no;
  codeUrl.value = code;
  amountYuan.value = Number(amt).toFixed(2);
  expireMinutes.value = Number(expire) || 30;
  mockEnabled.value = mock === 'true' || mock === '1';
  loading.value = false;

  // 渲染二维码
  if (codeUrl.value) {
    setTimeout(() => renderQr(codeUrl.value), 50);
  }

  // 轮询支付状态（3s）
  await pollStatus();
  pollTimer = setInterval(pollStatus, 3000);
});

onUnmounted(stopPolling);
</script>
