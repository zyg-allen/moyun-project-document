<script setup lang="ts">
import { useToast } from '@/composables/useToast';
import { ref, computed, onMounted } from 'vue';
import { useHead } from '@vueuse/head';
import {
  Wallet,
  TrendingUp,
  ArrowUpFromLine,
  Coins,
  Landmark,
  ReceiptText,
  CreditCard,
  ShieldCheck,
  Plus,
  Loader2,
  BadgeCheck,
  Lock,
  Split,
  Nfc,
} from 'lucide-vue-next';
import Breadcrumb from '@/components/Breadcrumb.vue';
import { generateSeo } from '@/utils/seo';
import {
  getAccountOverview,
  getMyLedger,
  getBankCards,
  bindBankCard,
  deleteBankCard,
  setDefaultBankCard,
} from '@/api/pay';
import { sendSmsCode } from '@/api/sms';
import type { PayAccountOverview, PayLedgerEntry, UserBankCard } from '@/types/api';

useHead(
  generateSeo({
    title: '我的钱包',
    description: '打赏收入、资金流水与提现银行卡管理',
    keywords: ['我的钱包', '打赏收入', '资金流水', '银行卡'],
    type: 'website',
  })
);

const toast = useToast();

const tabs = [
  { key: 'ledger', label: '资金流水' },
  { key: 'bankcard', label: '银行卡' },
] as const;

const activeTab = ref<'ledger' | 'bankcard'>('ledger');
const overview = ref<PayAccountOverview | null>(null);
const overviewLoading = ref(true);
const ledgerEntries = ref<PayLedgerEntry[]>([]);
const ledgerLoading = ref(true);
const ledgerCurrent = ref(1);
const ledgerTotal = ref(0);
const PAGE_SIZE = 20;
const bankCards = ref<UserBankCard[]>([]);
const cardLoading = ref(false);
const showBindForm = ref(false);
const bindSubmitting = ref(false);
const bindForm = ref({ holderName: '', cardNo: '', phone: '', bankName: '', smsCode: '' });
// 短信验证码（V11.1）
const smsSending = ref(false);
const smsCooldown = ref(0);
let smsTimer: ReturnType<typeof setInterval> | null = null;

const hasMoreLedger = computed(() => ledgerEntries.value.length < ledgerTotal.value);

// 本页累计收入合计（当前已加载流水）
const loadedCreditTotal = computed(() =>
  ledgerEntries.value
    .filter((e) => e.direction === 'credit' && e.accountRole === 'USER')
    .reduce((sum, e) => sum + (e.amountYuan ?? 0), 0)
);

const switchTab = (key: 'ledger' | 'bankcard') => {
  activeTab.value = key;
  if (key === 'bankcard' && bankCards.value.length === 0 && !cardLoading.value) {
    loadCards();
  }
};

const bizTypeLabel = (bizType?: string) => {
  const map: Record<string, string> = { tip: '打赏', withdraw: '提现', member: '会员' };
  return map[bizType || ''] || bizType || '-';
};

const formatTime = (time?: string) => {
  if (!time) return '-';
  // 精简到 分
  return time.length >= 16 ? time.slice(0, 16) : time;
};

const handleSendSms = async () => {
  const phone = bindForm.value.phone;
  if (!/^1\d{10}$/.test(phone)) {
    toast.error('请先填写正确的预留手机号');
    return;
  }
  smsSending.value = true;
  try {
    await sendSmsCode(phone, 'bankcard');
    toast.success('验证码已发送，5 分钟内有效');
    smsCooldown.value = 60;
    smsTimer = setInterval(() => {
      smsCooldown.value -= 1;
      if (smsCooldown.value <= 0 && smsTimer) {
        clearInterval(smsTimer);
        smsTimer = null;
      }
    }, 1000);
  } catch (e) {
    toast.error(e instanceof Error ? e.message : '发送失败');
  } finally {
    smsSending.value = false;
  }
};

const loadOverview = async () => {
  try {
    const res = await getAccountOverview();
    overview.value = res.data ?? null;
  } catch {
    toast.error('账户信息加载失败');
  } finally {
    overviewLoading.value = false;
  }
};

const loadLedger = async (append = false) => {
  ledgerLoading.value = true;
  try {
    const res = await getMyLedger({ current: ledgerCurrent.value, size: PAGE_SIZE });
    const records = res.data?.records ?? [];
    ledgerTotal.value = res.data?.total ?? 0;
    if (append) {
      ledgerEntries.value = [...ledgerEntries.value, ...records];
    } else {
      ledgerEntries.value = records;
    }
  } catch {
    toast.error('流水加载失败');
  } finally {
    ledgerLoading.value = false;
  }
};

const loadMoreLedger = async () => {
  ledgerCurrent.value += 1;
  await loadLedger(true);
};

const loadCards = async () => {
  cardLoading.value = true;
  try {
    const res = await getBankCards();
    bankCards.value = res.data?.records ?? [];
  } catch {
    toast.error('银行卡加载失败');
  } finally {
    cardLoading.value = false;
  }
};

const handleBind = async () => {
  const f = bindForm.value;
  if (!f.holderName.trim()) {
    toast.error('请填写持卡人姓名');
    return;
  }
  if (!/^\d{12,32}$/.test(f.cardNo)) {
    toast.error('卡号格式不正确');
    return;
  }
  if (!/^1\d{10}$/.test(f.phone)) {
    toast.error('手机号格式不正确');
    return;
  }
  if (!/^\d{6}$/.test(f.smsCode || '')) {
    toast.error('请填写 6 位短信验证码');
    return;
  }
  bindSubmitting.value = true;
  try {
    await bindBankCard({
      holderName: f.holderName.trim(),
      cardNo: f.cardNo,
      phone: f.phone,
      bankName: f.bankName.trim() || undefined,
      smsCode: f.smsCode,
    });
    toast.success('绑定成功');
    showBindForm.value = false;
    bindForm.value = { holderName: '', cardNo: '', phone: '', bankName: '', smsCode: '' };
    await loadCards();
  } catch (e) {
    toast.error(e instanceof Error ? e.message : '绑定失败');
  } finally {
    bindSubmitting.value = false;
  }
};

const handleSetDefault = async (card: UserBankCard) => {
  try {
    await setDefaultBankCard(card.id);
    toast.success('已设为默认卡');
    await loadCards();
  } catch (e) {
    toast.error(e instanceof Error ? e.message : '设置失败');
  }
};

const handleDeleteCard = async (card: UserBankCard) => {
  if (!window.confirm('确认删除该银行卡？')) return;
  try {
    await deleteBankCard(card.id);
    toast.success('删除成功');
    await loadCards();
  } catch (e) {
    toast.error(e instanceof Error ? e.message : '删除失败');
  }
};

onMounted(async () => {
  await Promise.all([loadOverview(), loadLedger()]);
});
</script>

<template>
  <div class="min-h-screen flex flex-col" style="background-color: var(--theme-bg);">
    <!-- 吸顶面包屑栏（与其他列表页同构） -->
    <div
      class="border-b sticky top-0 z-30 backdrop-blur-sm py-3"
      style="background-color: var(--theme-surface); border-color: var(--theme-border);"
    >
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 flex items-center justify-between gap-4">
        <Breadcrumb :items="[{ label: '我的钱包' }]" />
        <div class="flex items-center gap-1.5 text-xs px-3 py-1 rounded-full hidden sm:flex" style="background-color: var(--theme-accent); color: var(--theme-text-secondary);">
          <ShieldCheck class="w-3.5 h-3.5" style="color: var(--theme-success);" />
          资金操作全程加密
        </div>
      </div>
    </div>

    <div class="py-6 sm:py-8 flex-1">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <!-- 页头 -->
        <div class="flex items-end justify-between mb-6 flex-wrap gap-3">
          <div>
            <h1 class="text-2xl font-bold" style="color: var(--theme-text);">我的钱包</h1>
            <p class="text-sm mt-1" style="color: var(--theme-text-secondary);">打赏收入、资金流水与提现银行卡管理</p>
          </div>
        </div>

        <!-- 总览区：主余额卡 + 统计卡组 -->
        <div class="grid grid-cols-1 lg:grid-cols-[1.2fr_1fr] gap-4 sm:gap-6 mb-8">
          <!-- 余额主卡 -->
          <div class="rounded-2xl p-6 sm:p-8 relative overflow-hidden shadow-sm" style="background-color: var(--theme-primary);">
            <div class="absolute -right-16 -top-24 w-72 h-72 rounded-full" style="background-color: rgba(255,255,255,0.08);"></div>
            <div class="absolute -right-2 -bottom-28 w-56 h-56 rounded-full" style="background-color: rgba(255,255,255,0.06);"></div>
            <div class="absolute right-6 top-6 hidden sm:flex items-center gap-1.5 px-2.5 py-1 rounded-full text-[11px]"
                 style="background-color: rgba(255,255,255,0.18); color: #fff;">
              <BadgeCheck class="w-3.5 h-3.5" />
              实名资金账户
            </div>

            <div v-if="overviewLoading" class="py-10 text-center text-sm" style="color: rgba(255,255,255,0.8);">加载中…</div>
            <div v-else class="relative">
              <div class="flex items-center gap-2 mb-2.5 text-sm" style="color: rgba(255,255,255,0.85);">
                <Wallet class="w-4 h-4" />
                可用余额（元）
              </div>
              <p class="text-4xl sm:text-5xl font-bold tracking-tight tabular-nums" style="color: #fff;">
                ¥{{ overview?.balanceYuan?.toFixed(2) ?? '0.00' }}
              </p>
              <p class="text-xs mt-4 leading-relaxed max-w-sm" style="color: rgba(255,255,255,0.75);">
                收到的打赏在支付成功后自动分账入账，扣除平台服务费后的部分进入余额
              </p>
              <div class="flex flex-wrap gap-2.5 mt-6">
                <button
                  class="inline-flex items-center gap-1.5 px-4 py-2 rounded-lg text-sm font-medium transition-all hover:opacity-90"
                  style="background-color: rgba(255,255,255,0.2); color: #fff; backdrop-filter: blur(4px);"
                  @click="activeTab = 'ledger'"
                >
                  <ReceiptText class="w-4 h-4" />
                  查看流水
                </button>
                <button
                  class="inline-flex items-center gap-1.5 px-4 py-2 rounded-lg text-sm font-medium transition-all hover:opacity-90"
                  style="background-color: #fff; color: var(--theme-primary);"
                  @click="switchTab('bankcard')"
                >
                  <CreditCard class="w-4 h-4" />
                  银行卡管理
                </button>
              </div>
            </div>
          </div>

          <!-- 统计卡组 2x2 -->
          <div class="grid grid-cols-2 gap-4 sm:gap-6">
            <div class="rounded-2xl border p-4 sm:p-5 flex flex-col justify-between transition-shadow hover:shadow-md"
                 style="background-color: var(--theme-surface); border-color: var(--theme-border);">
              <div class="w-9 h-9 rounded-xl flex items-center justify-center mb-3" style="background-color: var(--theme-success-bg);">
                <TrendingUp class="w-[18px] h-[18px]" style="color: var(--theme-success);" />
              </div>
              <div>
                <p class="text-xs mb-1" style="color: var(--theme-text-secondary);">累计收入</p>
                <p class="text-xl sm:text-2xl font-bold tabular-nums" style="color: var(--theme-text);">¥{{ overview?.totalIncomeYuan?.toFixed(2) ?? '0.00' }}</p>
              </div>
            </div>
            <div class="rounded-2xl border p-4 sm:p-5 flex flex-col justify-between transition-shadow hover:shadow-md"
                 style="background-color: var(--theme-surface); border-color: var(--theme-border);">
              <div class="w-9 h-9 rounded-xl flex items-center justify-center mb-3" style="background-color: var(--theme-warning-bg);">
                <ArrowUpFromLine class="w-[18px] h-[18px]" style="color: var(--theme-warning);" />
              </div>
              <div>
                <p class="text-xs mb-1" style="color: var(--theme-text-secondary);">累计提现</p>
                <p class="text-xl sm:text-2xl font-bold tabular-nums" style="color: var(--theme-text);">¥{{ overview?.totalWithdrawYuan?.toFixed(2) ?? '0.00' }}</p>
              </div>
            </div>
            <div class="rounded-2xl border p-4 sm:p-5 flex flex-col justify-between transition-shadow hover:shadow-md"
                 style="background-color: var(--theme-surface); border-color: var(--theme-border);">
              <div class="w-9 h-9 rounded-xl flex items-center justify-center mb-3" style="background-color: var(--theme-info-bg);">
                <Coins class="w-[18px] h-[18px]" style="color: var(--theme-info);" />
              </div>
              <div>
                <p class="text-xs mb-1" style="color: var(--theme-text-secondary);">流水笔数</p>
                <p class="text-xl sm:text-2xl font-bold tabular-nums" style="color: var(--theme-text);">{{ ledgerTotal }}</p>
              </div>
            </div>
            <div class="rounded-2xl border p-4 sm:p-5 flex flex-col justify-between transition-shadow hover:shadow-md"
                 style="background-color: var(--theme-surface); border-color: var(--theme-border);">
              <div class="w-9 h-9 rounded-xl flex items-center justify-center mb-3" style="background-color: var(--theme-accent);">
                <Landmark class="w-[18px] h-[18px]" style="color: var(--theme-primary);" />
              </div>
              <div>
                <p class="text-xs mb-1" style="color: var(--theme-text-secondary);">绑定银行卡</p>
                <p class="text-xl sm:text-2xl font-bold tabular-nums" style="color: var(--theme-text);">{{ bankCards.length }} 张</p>
              </div>
            </div>
          </div>
        </div>

        <!-- Tab 切换 -->
        <div class="flex items-center gap-1 p-1 rounded-xl w-fit mb-5"
             style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);">
          <button
            v-for="tab in tabs"
            :key="tab.key"
            class="flex items-center gap-1.5 px-5 py-2 rounded-lg text-sm font-medium transition-all"
            :style="activeTab === tab.key
              ? 'background-color: var(--theme-primary); color: #fff; box-shadow: 0 1px 3px rgba(0,0,0,0.12);'
              : 'color: var(--theme-text-secondary);'"
            @click="switchTab(tab.key)"
          >
            <ReceiptText v-if="tab.key === 'ledger'" class="w-4 h-4" />
            <CreditCard v-else class="w-4 h-4" />
            {{ tab.label }}
            <span v-if="tab.key === 'bankcard' && bankCards.length > 0"
                  class="px-1.5 py-0.5 rounded-full text-[10px]"
                  :style="activeTab === tab.key ? 'background-color: rgba(255,255,255,0.25); color: #fff;' : 'background-color: var(--theme-accent); color: var(--theme-text-secondary);'">
              {{ bankCards.length }}
            </span>
          </button>
        </div>

        <!-- 资金流水 -->
        <div v-if="activeTab === 'ledger'"
             class="rounded-2xl border overflow-hidden"
             style="background-color: var(--theme-surface); border-color: var(--theme-border);">
          <!-- 流水头部：已加载收入合计 -->
          <div v-if="ledgerEntries.length > 0"
               class="flex items-center justify-between px-5 py-3.5 border-b flex-wrap gap-2"
               style="border-color: var(--theme-border); background-color: var(--theme-accent);">
            <div class="flex items-center gap-2 text-xs" style="color: var(--theme-text-secondary);">
              <Split class="w-3.5 h-3.5" style="color: var(--theme-primary);" />
              每笔打赏自动拆分为平台服务费与作者所得，此处为您的入账视角
            </div>
            <div class="text-xs" style="color: var(--theme-text-secondary);">
              本页收入合计 <span class="font-semibold tabular-nums" style="color: var(--theme-success);">¥{{ loadedCreditTotal.toFixed(2) }}</span>
            </div>
          </div>

          <div v-if="ledgerLoading && ledgerEntries.length === 0" class="py-16 flex flex-col items-center gap-3">
            <Loader2 class="w-6 h-6 animate-spin" style="color: var(--theme-text-secondary);" />
            <span class="text-sm" style="color: var(--theme-text-secondary);">加载中…</span>
          </div>
          <div v-else-if="ledgerEntries.length === 0" class="py-20 flex flex-col items-center gap-3">
            <div class="w-14 h-14 rounded-full flex items-center justify-center" style="background-color: var(--theme-accent);">
              <ReceiptText class="w-7 h-7" style="color: var(--theme-text-secondary);" />
            </div>
            <p class="text-sm" style="color: var(--theme-text-secondary);">暂无流水记录</p>
            <p class="text-xs" style="color: var(--theme-text-secondary);">发布内容获得打赏后，收入将展示在这里</p>
          </div>
          <template v-else>
            <!-- 桌面表格 -->
            <table class="w-full text-sm hidden sm:table">
              <thead>
                <tr style="background-color: var(--theme-accent);">
                  <th class="px-5 py-3.5 text-left text-xs font-semibold" style="color: var(--theme-text-secondary);">时间</th>
                  <th class="px-5 py-3.5 text-left text-xs font-semibold" style="color: var(--theme-text-secondary);">类型</th>
                  <th class="px-5 py-3.5 text-left text-xs font-semibold" style="color: var(--theme-text-secondary);">摘要</th>
                  <th class="px-5 py-3.5 text-right text-xs font-semibold" style="color: var(--theme-text-secondary);">金额（元）</th>
                  <th class="px-5 py-3.5 text-right text-xs font-semibold" style="color: var(--theme-text-secondary);">变动后余额</th>
                </tr>
              </thead>
              <tbody>
                <tr
                  v-for="entry in ledgerEntries"
                  :key="String(entry.id)"
                  class="border-b last:border-b-0 transition-colors hover:bg-black/[0.02]"
                  style="border-color: var(--theme-border);"
                >
                  <td class="px-5 py-3.5 whitespace-nowrap text-xs" style="color: var(--theme-text-secondary);">{{ formatTime(entry.createTime) }}</td>
                  <td class="px-5 py-3.5">
                    <span class="inline-flex px-2 py-0.5 rounded text-[10px] font-medium whitespace-nowrap"
                          :style="entry.bizType === 'tip'
                            ? 'background-color: var(--theme-warning-bg); color: var(--theme-warning);'
                            : 'background-color: var(--theme-info-bg); color: var(--theme-info);'">
                      {{ bizTypeLabel(entry.bizType) }}
                    </span>
                  </td>
                  <td class="px-5 py-3.5 max-w-xs truncate" style="color: var(--theme-text);" :title="entry.summary || ''">
                    {{ entry.summary || '-' }}
                  </td>
                  <td class="px-5 py-3.5 text-right font-semibold whitespace-nowrap tabular-nums"
                      :style="{ color: entry.direction === 'credit' ? 'var(--theme-success)' : 'var(--theme-danger)' }">
                    {{ entry.direction === 'credit' ? '+' : '-' }}{{ entry.amountYuan?.toFixed(2) ?? '0.00' }}
                  </td>
                  <td class="px-5 py-3.5 text-right whitespace-nowrap text-xs tabular-nums" style="color: var(--theme-text-secondary);">
                    {{ entry.balanceAfterYuan != null ? '¥' + entry.balanceAfterYuan.toFixed(2) : '-' }}
                  </td>
                </tr>
              </tbody>
            </table>
            <!-- 移动端卡片式流水 -->
            <div class="sm:hidden divide-y" style="border-color: var(--theme-border);">
              <div v-for="entry in ledgerEntries" :key="'m-' + String(entry.id)" class="px-4 py-3.5">
                <div class="flex items-center justify-between gap-3">
                  <div class="flex items-center gap-2 min-w-0">
                    <span class="inline-flex px-2 py-0.5 rounded text-[10px] font-medium whitespace-nowrap flex-shrink-0"
                          :style="entry.bizType === 'tip'
                            ? 'background-color: var(--theme-warning-bg); color: var(--theme-warning);'
                            : 'background-color: var(--theme-info-bg); color: var(--theme-info);'">
                      {{ bizTypeLabel(entry.bizType) }}
                    </span>
                    <span class="text-xs truncate" style="color: var(--theme-text-secondary);">{{ entry.summary || '-' }}</span>
                  </div>
                  <span class="font-semibold whitespace-nowrap tabular-nums text-sm"
                        :style="{ color: entry.direction === 'credit' ? 'var(--theme-success)' : 'var(--theme-danger)' }">
                    {{ entry.direction === 'credit' ? '+' : '-' }}{{ entry.amountYuan?.toFixed(2) ?? '0.00' }}
                  </span>
                </div>
                <div class="flex items-center justify-between mt-1.5 text-[11px]" style="color: var(--theme-text-secondary);">
                  <span>{{ formatTime(entry.createTime) }}</span>
                  <span v-if="entry.balanceAfterYuan != null">余额 ¥{{ entry.balanceAfterYuan.toFixed(2) }}</span>
                </div>
              </div>
            </div>
            <!-- 加载更多 -->
            <div v-if="hasMoreLedger" class="py-3 text-center border-t" style="border-color: var(--theme-border);">
              <button
                class="text-xs font-medium px-4 py-1.5 rounded-lg transition-colors inline-flex items-center gap-1.5"
                style="color: var(--theme-primary);"
                :disabled="ledgerLoading"
                @click="loadMoreLedger"
              >
                <Loader2 v-if="ledgerLoading" class="w-3.5 h-3.5 animate-spin" />
                {{ ledgerLoading ? '加载中…' : '加载更多' }}
              </button>
            </div>
          </template>
        </div>

        <!-- 银行卡 -->
        <div v-if="activeTab === 'bankcard'" class="space-y-4">
          <div class="flex justify-between items-center flex-wrap gap-3">
            <p class="text-sm" style="color: var(--theme-text-secondary);">提现打款账户，卡号加密存储、仅脱敏展示</p>
            <button
              class="inline-flex items-center gap-1.5 px-4 py-2 rounded-lg text-sm font-medium text-white transition-all hover:opacity-90"
              style="background-color: var(--theme-primary);"
              @click="showBindForm = !showBindForm"
            >
              <Plus class="w-4 h-4" />
              {{ showBindForm ? '收起' : '绑定银行卡' }}
            </button>
          </div>

          <!-- 绑定表单 -->
          <transition name="fade">
            <div v-if="showBindForm" class="rounded-2xl border p-6" style="background-color: var(--theme-surface); border-color: var(--theme-border);">
              <h3 class="text-base font-semibold mb-1" style="color: var(--theme-text);">绑定新银行卡</h3>
              <p class="text-xs mb-5" style="color: var(--theme-text-secondary);">仅支持借记卡，需通过银行预留手机号短信验证</p>
              <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div>
                  <label class="block text-xs mb-1.5" style="color: var(--theme-text-secondary);">持卡人姓名</label>
                  <input v-model="bindForm.holderName" type="text" placeholder="与银行卡开户名一致"
                         class="w-full px-3 py-2.5 rounded-lg text-sm outline-none transition-colors focus:border-[var(--theme-primary)]"
                         style="background-color: var(--theme-bg); border: 1px solid var(--theme-border); color: var(--theme-text);" />
                </div>
                <div>
                  <label class="block text-xs mb-1.5" style="color: var(--theme-text-secondary);">银行卡号</label>
                  <input v-model="bindForm.cardNo" type="text" maxlength="32" placeholder="仅支持借记卡"
                         class="w-full px-3 py-2.5 rounded-lg text-sm outline-none transition-colors focus:border-[var(--theme-primary)]"
                         style="background-color: var(--theme-bg); border: 1px solid var(--theme-border); color: var(--theme-text);" />
                </div>
                <div>
                  <label class="block text-xs mb-1.5" style="color: var(--theme-text-secondary);">预留手机号</label>
                  <input v-model="bindForm.phone" type="text" maxlength="11" placeholder="银行预留手机号"
                         class="w-full px-3 py-2.5 rounded-lg text-sm outline-none transition-colors focus:border-[var(--theme-primary)]"
                         style="background-color: var(--theme-bg); border: 1px solid var(--theme-border); color: var(--theme-text);" />
                </div>
                <div>
                  <label class="block text-xs mb-1.5" style="color: var(--theme-text-secondary);">开户银行</label>
                  <input v-model="bindForm.bankName" type="text" placeholder="如：中国工商银行"
                         class="w-full px-3 py-2.5 rounded-lg text-sm outline-none transition-colors focus:border-[var(--theme-primary)]"
                         style="background-color: var(--theme-bg); border: 1px solid var(--theme-border); color: var(--theme-text);" />
                </div>
                <div class="sm:col-span-2">
                  <label class="block text-xs mb-1.5" style="color: var(--theme-text-secondary);">短信验证码</label>
                  <div class="flex gap-2">
                    <input v-model="bindForm.smsCode" type="text" maxlength="6" placeholder="6 位验证码"
                           class="flex-1 px-3 py-2.5 rounded-lg text-sm outline-none transition-colors focus:border-[var(--theme-primary)]"
                           style="background-color: var(--theme-bg); border: 1px solid var(--theme-border); color: var(--theme-text);" />
                    <button
                      class="px-4 py-2.5 rounded-lg text-sm font-medium whitespace-nowrap transition-all disabled:opacity-50"
                      :style="smsCooldown > 0
                        ? 'background-color: var(--theme-accent); color: var(--theme-text-secondary);'
                        : 'background-color: var(--theme-primary); color: #fff;'"
                      :disabled="smsCooldown > 0 || smsSending"
                      @click="handleSendSms"
                    >
                      {{ smsCooldown > 0 ? `${smsCooldown}s 后重发` : (smsSending ? '发送中…' : '获取验证码') }}
                    </button>
                  </div>
                </div>
              </div>
              <div class="flex items-center gap-2 mt-4 text-xs" style="color: var(--theme-text-secondary);">
                <Lock class="w-3.5 h-3.5 shrink-0" style="color: var(--theme-success);" />
                卡号与手机号将 AES-GCM 加密存储，任何页面只展示脱敏信息；银行卡仅用于后续提现打款
              </div>
              <button
                class="mt-5 px-6 py-2.5 rounded-lg text-sm font-medium text-white transition-all hover:opacity-90 disabled:opacity-50 inline-flex items-center gap-2"
                style="background-color: var(--theme-primary);"
                :disabled="bindSubmitting"
                @click="handleBind"
              >
                <Loader2 v-if="bindSubmitting" class="w-4 h-4 animate-spin" />
                {{ bindSubmitting ? '提交中…' : '确认绑定' }}
              </button>
            </div>
          </transition>

          <!-- 卡列表（卡面造型） -->
          <div v-if="cardLoading" class="rounded-2xl border py-16 flex flex-col items-center gap-3" style="background-color: var(--theme-surface); border-color: var(--theme-border);">
            <Loader2 class="w-6 h-6 animate-spin" style="color: var(--theme-text-secondary);" />
            <span class="text-sm" style="color: var(--theme-text-secondary);">加载中…</span>
          </div>
          <div v-else-if="bankCards.length === 0" class="rounded-2xl border py-16 flex flex-col items-center gap-3" style="background-color: var(--theme-surface); border-color: var(--theme-border);">
            <div class="w-14 h-14 rounded-full flex items-center justify-center" style="background-color: var(--theme-accent);">
              <CreditCard class="w-7 h-7" style="color: var(--theme-text-secondary);" />
            </div>
            <p class="text-sm" style="color: var(--theme-text-secondary);">暂未绑定银行卡</p>
            <p class="text-xs" style="color: var(--theme-text-secondary);">绑定后可发起余额提现（打款功能即将开放）</p>
          </div>
          <div v-else class="grid grid-cols-1 lg:grid-cols-2 gap-4">
            <div
              v-for="card in bankCards"
              :key="String(card.id)"
              class="rounded-2xl border overflow-hidden transition-shadow hover:shadow-md"
              style="background-color: var(--theme-surface); border-color: var(--theme-border);"
            >
              <!-- 卡面 -->
              <div class="relative p-5 overflow-hidden" style="background-color: var(--theme-primary);">
                <div class="absolute -right-10 -top-16 w-44 h-44 rounded-full" style="background-color: rgba(255,255,255,0.1);"></div>
                <div class="absolute -left-8 -bottom-20 w-36 h-36 rounded-full" style="background-color: rgba(255,255,255,0.06);"></div>
                <div class="relative flex items-start justify-between">
                  <div>
                    <div class="flex items-center gap-2">
                      <Landmark class="w-4 h-4" style="color: rgba(255,255,255,0.9);" />
                      <span class="font-semibold" style="color: #fff;">{{ card.bankName || card.bankCode || '银行卡' }}</span>
                    </div>
                    <div class="flex items-center gap-2 mt-4">
                      <div class="w-8 h-6 rounded-sm" style="background-color: rgba(255,255,255,0.35);"></div>
                      <Nfc class="w-4 h-4" style="color: rgba(255,255,255,0.75);" />
                    </div>
                    <p class="mt-3 font-mono tracking-widest text-base" style="color: #fff;">{{ card.cardNoMasked }}</p>
                    <p class="mt-2 text-xs" style="color: rgba(255,255,255,0.8);">{{ card.holderName }}</p>
                  </div>
                  <span v-if="card.isDefault === 1"
                        class="px-2 py-0.5 rounded text-[10px] font-medium shrink-0"
                        style="background-color: rgba(255,255,255,0.25); color: #fff;">
                    默认卡
                  </span>
                </div>
              </div>
              <!-- 卡信息 + 操作 -->
              <div class="p-4 flex items-center justify-between">
                <div class="flex items-center gap-2 text-xs" style="color: var(--theme-text-secondary);">
                  <span class="px-2 py-0.5 rounded font-medium"
                        :style="card.verifyStatus === 'VERIFIED'
                          ? 'background-color: var(--theme-success-bg); color: var(--theme-success);'
                          : 'background-color: var(--theme-warning-bg); color: var(--theme-warning);'">
                    {{ card.verifyStatus === 'VERIFIED' ? '已核实' : '待核实' }}
                  </span>
                  <span>仅用于提现打款</span>
                </div>
                <div class="flex gap-2">
                  <button
                    v-if="card.isDefault !== 1"
                    class="px-3 py-1.5 rounded-lg text-xs border transition-colors hover:opacity-80"
                    style="color: var(--theme-text); border-color: var(--theme-border);"
                    @click="handleSetDefault(card)"
                  >
                    设为默认
                  </button>
                  <button
                    class="px-3 py-1.5 rounded-lg text-xs border transition-colors hover:opacity-80"
                    style="color: var(--theme-danger); border-color: var(--theme-border);"
                    @click="handleDeleteCard(card)"
                  >
                    删除
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 底部：资金安全说明 -->
        <div class="mt-8 grid grid-cols-1 sm:grid-cols-3 gap-4">
          <div class="rounded-xl border p-4 flex items-start gap-3" style="background-color: var(--theme-surface); border-color: var(--theme-border);">
            <div class="w-8 h-8 rounded-lg flex items-center justify-center flex-shrink-0" style="background-color: var(--theme-accent);">
              <Split class="w-4 h-4" style="color: var(--theme-primary);" />
            </div>
            <div>
              <p class="text-sm font-medium" style="color: var(--theme-text);">自动分账</p>
              <p class="text-xs mt-0.5 leading-relaxed" style="color: var(--theme-text-secondary);">每笔打赏按平台费率自动拆分，用户所得实时入账</p>
            </div>
          </div>
          <div class="rounded-xl border p-4 flex items-start gap-3" style="background-color: var(--theme-surface); border-color: var(--theme-border);">
            <div class="w-8 h-8 rounded-lg flex items-center justify-center flex-shrink-0" style="background-color: var(--theme-accent);">
              <Lock class="w-4 h-4" style="color: var(--theme-primary);" />
            </div>
            <div>
              <p class="text-sm font-medium" style="color: var(--theme-text);">加密存储</p>
              <p class="text-xs mt-0.5 leading-relaxed" style="color: var(--theme-text-secondary);">银行卡号与手机号 AES-GCM 加密落库，仅脱敏展示</p>
            </div>
          </div>
          <div class="rounded-xl border p-4 flex items-start gap-3" style="background-color: var(--theme-surface); border-color: var(--theme-border);">
            <div class="w-8 h-8 rounded-lg flex items-center justify-center flex-shrink-0" style="background-color: var(--theme-accent);">
              <BadgeCheck class="w-4 h-4" style="color: var(--theme-primary);" />
            </div>
            <div>
              <p class="text-sm font-medium" style="color: var(--theme-text);">复式记账</p>
              <p class="text-xs mt-0.5 leading-relaxed" style="color: var(--theme-text-secondary);">每笔资金变动双向留痕，收支流水全程可追溯</p>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
