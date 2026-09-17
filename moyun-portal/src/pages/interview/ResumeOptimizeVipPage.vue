<template>
  <div class="min-h-screen bg-theme-bg">
    <div class="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 py-6 sm:py-8">
      <!-- 页头 -->
      <div class="mb-6">
        <h1 class="text-2xl sm:text-3xl font-bold text-theme-text mb-1">简历优化会员</h1>
        <p class="meta-text text-theme-text-secondary">
          解锁简历深度优化：AI 逐项建议、前后对比、采纳保存新版本，订阅即刻生效、续费顺延不折损。
        </p>
      </div>

      <!-- 会员状态卡 -->
      <div
        class="rounded-2xl border p-5 sm:p-6 mb-6 shadow-sm"
        :class="vip.isVip
          ? 'bg-gradient-to-br from-slate-800 to-slate-600 border-transparent text-white'
          : 'bg-theme-surface border-theme-border'"
      >
        <div class="flex items-center gap-3">
          <span class="text-3xl">{{ vip.isVip ? '👑' : '🔒' }}</span>
          <div>
            <p class="font-bold text-lg" :class="vip.isVip ? 'text-amber-200' : 'text-theme-text'">
              {{ vip.isVip ? '会员生效中' : '尚未开通会员' }}
            </p>
            <p class="meta-text" :class="vip.isVip ? 'text-slate-300' : 'text-theme-text-secondary'">
              {{ vip.isVip ? '权益有效期至 ' + formatDay(vip.vipExpire) : '开通后解锁简历深度优化全部能力' }}
            </p>
          </div>
        </div>
      </div>

      <!-- 权益清单 -->
      <div class="rounded-2xl border border-theme-border bg-theme-surface p-5 sm:p-6 mb-6 shadow-sm">
        <h2 class="card-title mb-3">会员权益</h2>
        <div class="grid grid-cols-1 sm:grid-cols-2 gap-2.5">
          <div v-for="b in benefits" :key="b" class="flex items-center gap-2">
            <span class="text-green-500 font-bold">✓</span>
            <span class="meta-text text-theme-text">{{ b }}</span>
          </div>
        </div>
        <p class="meta-text text-theme-text-secondary mt-3">
          注：岗位匹配评分、AI 实时辅助编辑等基础功能无需会员；订阅即刻生效、续费顺延不折损。
        </p>
      </div>

      <!-- 套餐选择 -->
      <h2 class="section-title mb-3">选择套餐</h2>
      <div class="grid grid-cols-1 sm:grid-cols-2 gap-4 mb-8">
        <button
          v-for="p in packages"
          :key="p.id"
          class="relative text-left rounded-2xl border-2 p-5 transition-all hover:-translate-y-0.5 hover:shadow-lg"
          :class="selectedId === p.id
            ? 'border-theme-primary bg-theme-primary/5 shadow-md'
            : 'border-theme-border bg-theme-surface'"
          @click="selectedId = p.id"
        >
          <span
            v-if="p.popular"
            class="absolute top-3 right-3 inline-flex items-center px-2 py-0.5 rounded-full text-xs font-bold text-white bg-amber-500"
          >推荐</span>
          <p class="font-bold text-lg text-theme-text mb-0.5">{{ p.name }}</p>
          <p class="meta-text text-theme-text-secondary mb-3">{{ p.durationDays }} 天</p>
          <p class="flex items-baseline gap-2">
            <span class="text-3xl font-bold text-theme-primary">¥{{ p.price }}</span>
            <span v-if="p.originalPrice" class="meta-text text-theme-text-secondary line-through">¥{{ p.originalPrice }}</span>
          </p>
          <p v-if="p.description" class="meta-text text-theme-text-secondary mt-2">{{ p.description }}</p>
        </button>
      </div>
      <p v-if="!packages.length && !loading" class="text-center meta-text text-theme-text-secondary py-8">
        暂无在售套餐
      </p>

      <!-- 订阅按钮 -->
      <div class="fixed bottom-0 inset-x-0 border-t border-theme-border bg-theme-surface/95 backdrop-blur">
        <div class="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 py-3 flex items-center gap-4">
          <div class="flex-1 min-w-0">
            <p class="meta-text text-theme-text-secondary">已选：{{ selectedName || '未选择' }}</p>
            <p v-if="selectedAmount != null" class="font-bold text-theme-text">
              合计 <span class="text-xl text-theme-primary">¥{{ selectedAmount }}</span>
            </p>
          </div>
          <button
            class="shrink-0 inline-flex items-center gap-2 px-6 py-3 rounded-xl font-semibold shadow-theme-sm transition hover:opacity-90 disabled:opacity-50 disabled:cursor-not-allowed bg-theme-primary text-theme-on-primary"
            :disabled="submitting || !selectedId"
            @click="doSubscribe"
          >
            <span v-if="submitting">下单中…</span>
            <template v-else>
              <CreditCard class="w-4 h-4" />
              立即订阅（微信支付）
            </template>
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { useHead } from '@vueuse/head';
import { CreditCard } from 'lucide-vue-next';
import {
  getResumeOptimizeVipPackages,
  getResumeOptimizeVipStatus,
  subscribeResumeOptimizeVip,
  type ResumeOptimizeVipPackageVO,
} from '@/api/resumeOptimizeVip';
import { useToast } from '@/composables/useToast';
import { useAuth } from '@/composables/useAuth';

const router = useRouter();
const toast = useToast();
const { requireAuth } = useAuth();

useHead({
  title: '简历优化会员 - 墨韵门户',
  meta: [{ name: 'robots', content: 'noindex,nofollow' }],
});

const benefits = ['简历深度优化不限次（非会员可免费体验2次）', 'AI 逐项优化建议', '优化前后对比', '采纳建议生成新版本'];

const loading = ref(false);
const packages = ref<ResumeOptimizeVipPackageVO[]>([]);
const selectedId = ref<number | null>(null);
const submitting = ref(false);
const vip = ref<{ isVip: boolean; vipExpire: string | null }>({ isVip: false, vipExpire: null });

const selectedPkg = computed(() => packages.value.find((p) => p.id === selectedId.value) || null);
const selectedName = computed(() => selectedPkg.value?.name || '');
const selectedAmount = computed(() => selectedPkg.value?.price ?? null);

onMounted(async () => {
  loading.value = true;
  try {
    const [pkgRes, statusRes] = await Promise.all([
      getResumeOptimizeVipPackages(),
      getResumeOptimizeVipStatus().catch(() => null),
    ]);
    packages.value = pkgRes.data?.records || [];
    if (packages.value.length) {
      const popular = packages.value.find((p) => p.popular);
      selectedId.value = (popular || packages.value[0]).id;
    }
    if (statusRes?.data) vip.value = statusRes.data;
  } catch (e) {
    toast.error((e as Error).message || '加载失败');
  } finally {
    loading.value = false;
  }
});

function formatDay(t: string | null) {
  if (!t) return '';
  return String(t).replace('T', ' ').substring(0, 10);
}

function genClientUuid() {
  return 'rvip-' + Date.now() + '-' + Math.random().toString(36).substring(2, 10);
}

async function doSubscribe() {
  if (!requireAuth('/interview/resume/vip')) return;
  if (!selectedId.value) {
    toast.warning('请选择套餐');
    return;
  }
  if (submitting.value) return;
  submitting.value = true;
  try {
    const res = await subscribeResumeOptimizeVip({
      packageId: selectedId.value,
      clientUuid: genClientUuid(),
    });
    const d = res.data;
    if (d?.payNo) {
      // 跳通用收银台（v11.83 公共通道，与面试会员/记账VIP同构）
      router.push({
        path: '/pay/cashier',
        query: {
          payNo: d.payNo,
          codeUrl: d.codeUrl || '',
          amount: String(d.amount ?? ''),
          expireMinutes: '30',
          mockEnabled: String(d.mockEnabled ?? false),
        },
      });
    } else {
      toast.error('下单失败');
    }
  } catch (e) {
    toast.error((e as Error).message || '下单失败，请稍后重试');
  } finally {
    submitting.value = false;
  }
}
</script>
