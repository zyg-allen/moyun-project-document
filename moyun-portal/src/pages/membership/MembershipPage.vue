<template>
  <div class="min-h-screen bg-theme-bg">
    <div class="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 py-6 sm:py-8">
      <!-- 页头 -->
      <div class="mb-6">
        <h1 class="text-2xl sm:text-3xl font-bold text-theme-text mb-1">墨韵会员</h1>
        <p class="meta-text text-theme-text-secondary">
          一份会员，解锁语音面试、深度面试报告与简历深度优化等全部高级功能，续费顺延不折损。
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
          <span class="text-3xl">{{ vip.isVip ? '👑' : (statusError ? '⚠️' : '🔒') }}</span>
          <div>
            <!-- 清单 P2：状态查询失败与"确实未开通"必须区分，避免把加载失败说成没开通 -->
            <p class="font-bold text-lg" :class="vip.isVip ? 'text-amber-200' : 'text-theme-text'">
              {{ vip.isVip ? vip.tierName + ' · 生效中' : (statusError ? '会员状态加载失败' : '尚未开通会员') }}
            </p>
            <p class="meta-text" :class="vip.isVip ? 'text-slate-300' : 'text-theme-text-secondary'">
              {{ vip.isVip
                ? (vip.expireTime ? '权益有效期至 ' + formatDay(vip.expireTime) : '永久有效')
                : (statusError ? '暂时无法获取你的会员状态，请稍后重试' : '部分功能可免费体验，开通后解锁全部高级功能') }}
            </p>
            <button
              v-if="statusError && !vip.isVip"
              class="meta-text mt-1 underline"
              style="color: var(--theme-primary);"
              @click="reloadStatus"
            >重试</button>
          </div>
        </div>
        <!-- 当前权益用量（含 free 档免费额度） -->
        <div v-if="vip.benefits?.length" class="grid grid-cols-1 sm:grid-cols-2 gap-2 mt-4">
          <div v-for="b in vip.benefits" :key="b.code"
               class="flex items-center justify-between gap-2 rounded-lg px-3 py-1.5"
               :class="vip.isVip ? 'bg-white/10' : 'bg-theme-bg'">
            <span class="meta-text" :class="vip.isVip ? 'text-slate-200' : 'text-theme-text'">{{ b.name }}</span>
            <span class="meta-text font-semibold" :class="vip.isVip ? 'text-amber-200' : 'text-theme-primary'">
              {{ b.left === null ? '不限次' : `剩余 ${b.left} 次` }}
            </span>
          </div>
        </div>
      </div>

      <!--
        清单 P2：底部订阅栏 fixed bottom-0（py-3 + 两行文案约 70px），而内容区底部没有预留等高内边距，
        最后一个等级卡会被固定栏压住。这里在内容末尾留出安全间距。
      -->
      <div class="h-24" aria-hidden="true"></div>

      <!-- 等级选择 -->
      <h2 class="section-title mb-3">选择会员等级</h2>
      <div class="grid grid-cols-1 sm:grid-cols-2 gap-4 mb-8">
        <button
          v-for="t in tiers"
          :key="t.tierCode"
          class="relative text-left rounded-2xl border-2 p-5 transition-all hover:-translate-y-0.5 hover:shadow-lg"
          :class="!isSellable(t)
            ? 'opacity-60 cursor-not-allowed border-theme-border bg-theme-surface'
            : (selectedCode === t.tierCode
              ? 'border-theme-primary bg-theme-primary/5 shadow-md'
              : 'border-theme-border bg-theme-surface')"
          :disabled="!isSellable(t)"
          @click="selectTier(t)"
        >
          <span
            v-if="t.popular"
            class="absolute top-3 right-3 inline-flex items-center px-2 py-0.5 rounded-full text-xs font-bold text-white bg-amber-500"
          >推荐</span>
          <p class="font-bold text-lg text-theme-text mb-0.5">{{ t.tierName }}</p>
          <p class="meta-text text-theme-text-secondary mb-3">
            {{ t.durationDays == null ? '暂不可购买（未配置有效期）'
              : (t.durationDays < 0 ? '永久有效' : t.durationDays + ' 天') }}
          </p>
          <p class="flex items-baseline gap-2">
            <span class="text-3xl font-bold text-theme-primary">¥{{ t.price }}</span>
            <span v-if="t.originalPrice" class="meta-text text-theme-text-secondary line-through">¥{{ t.originalPrice }}</span>
          </p>
          <p v-if="t.description" class="meta-text text-theme-text-secondary mt-2">{{ t.description }}</p>
          <!-- 等级权益清单 -->
          <div class="mt-3 space-y-1">
            <div v-for="b in t.benefits" :key="b.code" class="flex items-center gap-1.5">
              <span class="text-green-500 font-bold text-xs">✓</span>
              <span class="meta-text text-theme-text">{{ benefitText(b) }}</span>
            </div>
          </div>
        </button>
      </div>
      <!-- 失败态（清单 P2）：必须排在"暂无在售等级"之前 -->
      <div v-if="tiersError && !loading" class="text-center py-8">
        <p class="meta-text text-theme-text-secondary mb-3">会员等级加载失败</p>
        <button
          class="px-4 py-2 rounded-xl text-sm font-medium"
          style="background-color: var(--theme-primary); color: white;"
          @click="reloadTiers"
        >重试</button>
      </div>
      <p v-else-if="!tiers.length && !loading" class="text-center meta-text text-theme-text-secondary py-8">
        暂无在售等级
      </p>

      <!-- 订阅按钮 -->
      <div class="fixed bottom-0 inset-x-0 border-t border-theme-border bg-theme-surface/95 backdrop-blur">
        <div class="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 py-3 flex items-center gap-4">
          <div class="flex-1 min-w-0">
            <p class="meta-text text-theme-text-secondary">已选：{{ selectedTier?.tierName || '未选择' }}</p>
            <p v-if="selectedTier" class="font-bold text-theme-text">
              合计 <span class="text-xl text-theme-primary">¥{{ selectedTier.price }}</span>
            </p>
          </div>
          <button
            class="shrink-0 inline-flex items-center gap-2 px-6 py-3 rounded-xl font-semibold shadow-theme-sm transition hover:opacity-90 disabled:opacity-50 disabled:cursor-not-allowed bg-theme-primary text-theme-on-primary"
            :disabled="submitting || !selectedCode"
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
  getVipTiers,
  getVipStatus,
  subscribeVip,
  type VipTierVO,
  type VipStatusVO,
  type VipBenefitItemVO,
} from '@/api/vip';
import { useToast } from '@/composables/useToast';
import { useAuth } from '@/composables/useAuth';

const router = useRouter();
const toast = useToast();
const { requireAuth } = useAuth();

useHead({
  title: '墨韵会员 - 墨韵门户',
  meta: [{ name: 'robots', content: 'noindex,nofollow' }],
});

// 首屏即进入加载态，避免"空态一闪"（清单 P2）
const loading = ref(true);
const tiers = ref<VipTierVO[]>([]);
const selectedCode = ref<string | null>(null);
const submitting = ref(false);
const vip = ref<Partial<VipStatusVO>>({ isVip: false });

const selectedTier = computed(() => tiers.value.find((t) => t.tierCode === selectedCode.value) || null);

/** 权益文案：数字额度 + 周期；unlimited 不限次 */
function benefitText(b: VipBenefitItemVO) {
  if (b.value === 'unlimited') return `${b.name}：不限次`;
  const period = b.period === 'month' ? '/月' : b.period === 'day' ? '/天' : b.period === 'year' ? '/年' : '';
  return `${b.name}：${b.value} 次${period}`;
}

/**
 * 会员状态拉取失败标记（清单 P2）。
 *
 * <p>原先 `getVipStatus().catch(() => null)` 静默兜底，而 `vip` 初值是 `{isVip:false}`
 * ⇒ 失败后状态卡直接渲染"尚未开通会员 / 部分功能可免费体验"，**把"没查到"说成了"没开通"**，
 * 且权益用量区（v-if="vip.benefits?.length"）整块消失。</p>
 */
const statusError = ref(false);

/**
 * 等级列表加载失败（清单 P2）。
 *
 * <p>原先 `getVipTiers()` 失败只 toast，页面落到「暂无在售等级」——
 * 与"确实没有在售等级"共用同一段文案，且**没有任何重试入口**（同仓库排行榜/日历等页都有重试）。
 * 另外 loading 初值为 false、onMounted 才置 true，首屏会先闪一下空态，这里一并改为初值 true。</p>
 */
const tiersError = ref(false);

onMounted(async () => {
  loading.value = true;
  try {
    const [tiersRes, statusRes] = await Promise.all([
      getVipTiers(),
      getVipStatus().catch(() => {
        statusError.value = true;
        return null;
      }),
    ]);
    // 售卖页只展示付费等级（free 档的免费额度已在状态卡体现）
    tiers.value = (tiersRes.data?.records || []).filter((t) => t.tierCode !== 'free' && t.price > 0);
    if (tiers.value.length) {
      const popular = tiers.value.find((t) => t.popular);
      selectedCode.value = (popular || tiers.value[0]).tierCode;
    }
    if (statusRes?.data) vip.value = statusRes.data;
  } catch (e) {
    tiersError.value = true;
    toast.error((e as Error).message || '加载失败');
  } finally {
    loading.value = false;
  }
});

/** 重新拉取会员状态（失败重试用） */
async function reloadStatus() {
  statusError.value = false;
  try {
    const res = await getVipStatus();
    if (res.data) vip.value = res.data;
  } catch {
    statusError.value = true;
  }
}

/** 重新加载在售等级（失败重试用） */
async function reloadTiers() {
  tiersError.value = false;
  loading.value = true;
  try {
    const res = await getVipTiers();
    tiers.value = (res.data?.records || []).filter((t) => t.tierCode !== 'free' && t.price > 0);
    if (tiers.value.length) {
      const popular = tiers.value.find((t) => t.popular);
      selectedCode.value = (popular || tiers.value[0]).tierCode;
    }
  } catch (e) {
    tiersError.value = true;
    toast.error((e as Error).message || '加载失败');
  } finally {
    loading.value = false;
  }
}

function formatDay(t: string | null) {
  if (!t) return '';
  return String(t).replace('T', ' ').substring(0, 10);
}

function genClientUuid() {
  return 'vip-' + Date.now() + '-' + Math.random().toString(36).substring(2, 10);
}

/** 等级是否可售：必须已配置有效期。后台未填 duration_days 的等级不可下单（后端 subscribe 亦会拒绝） */
function isSellable(t: VipTierVO): boolean {
  return t.durationDays !== null && t.durationDays !== undefined;
}

function selectTier(t: VipTierVO) {
  if (!isSellable(t)) {
    toast.warning('该等级暂未配置有效期，暂不可购买');
    return;
  }
  selectedCode.value = t.tierCode;
}

async function doSubscribe() {
  if (!requireAuth('/membership')) return;
  if (!selectedCode.value) {
    toast.warning('请选择会员等级');
    return;
  }
  const chosen = selectedTier.value;
  if (chosen && !isSellable(chosen)) {
    toast.error('该等级暂未配置有效期，暂不可购买');
    return;
  }
  if (submitting.value) return;
  submitting.value = true;
  try {
    const res = await subscribeVip({
      tierCode: selectedCode.value,
      clientUuid: genClientUuid(),
    });
    const d = res.data;
    if (d?.payNo) {
      // 跳通用收银台（公共通道，与打赏/付费阅读同构）
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
