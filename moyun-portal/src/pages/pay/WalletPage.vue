<template>
  <div class="min-h-screen py-8 px-4" style="background-color: var(--theme-bg);">
    <div class="max-w-4xl mx-auto">
      <h1 class="text-2xl font-bold mb-6" style="color: var(--theme-text);">我的钱包</h1>

      <!-- 账户总览 -->
      <div class="rounded-2xl border p-6 mb-6" style="background-color: var(--theme-surface); border-color: var(--theme-border);">
        <div v-if="overviewLoading" class="text-center py-4 text-sm" style="color: var(--theme-text-secondary);">加载中…</div>
        <template v-else-if="overview">
          <div class="grid grid-cols-1 sm:grid-cols-3 gap-4">
            <div>
              <p class="text-xs mb-1" style="color: var(--theme-text-secondary);">可用余额（元）</p>
              <p class="text-3xl font-bold" style="color: var(--theme-text);">{{ overview.balanceYuan?.toFixed(2) ?? '0.00' }}</p>
            </div>
            <div>
              <p class="text-xs mb-1" style="color: var(--theme-text-secondary);">累计收入（元）</p>
              <p class="text-2xl font-semibold" style="color: var(--theme-text);">{{ overview.totalIncomeYuan?.toFixed(2) ?? '0.00' }}</p>
            </div>
            <div>
              <p class="text-xs mb-1" style="color: var(--theme-text-secondary);">累计提现（元）</p>
              <p class="text-2xl font-semibold" style="color: var(--theme-text);">{{ overview.totalWithdrawYuan?.toFixed(2) ?? '0.00' }}</p>
            </div>
          </div>
          <p class="text-xs mt-4" style="color: var(--theme-text-secondary);">
            收到的打赏在支付成功后自动分账入账：扣除平台服务费后的金额计入余额。
          </p>
        </template>
      </div>

      <!-- Tab：流水 / 银行卡 -->
      <div class="flex gap-2 mb-4">
        <button
          v-for="tab in tabs"
          :key="tab.key"
          class="px-4 py-2 rounded-lg text-sm font-medium transition-colors"
          :style="activeTab === tab.key
            ? 'background-color: var(--theme-primary); color: #fff;'
            : 'background-color: var(--theme-surface); color: var(--theme-text); border: 1px solid var(--theme-border);'"
          @click="activeTab = tab.key"
        >
          {{ tab.label }}
        </button>
      </div>

      <!-- 资金流水 -->
      <div v-if="activeTab === 'ledger'" class="rounded-2xl border overflow-hidden" style="background-color: var(--theme-surface); border-color: var(--theme-border);">
        <div v-if="ledgerLoading" class="text-center py-10 text-sm" style="color: var(--theme-text-secondary);">加载中…</div>
        <div v-else-if="ledgerEntries.length === 0" class="text-center py-12 text-sm" style="color: var(--theme-text-secondary);">
          暂无流水记录
        </div>
        <table v-else class="w-full text-sm">
          <thead>
            <tr class="border-b" style="border-color: var(--theme-border); color: var(--theme-text-secondary);">
              <th class="px-4 py-3 text-left font-medium">时间</th>
              <th class="px-4 py-3 text-left font-medium">摘要</th>
              <th class="px-4 py-3 text-right font-medium">金额（元）</th>
              <th class="px-4 py-3 text-right font-medium hidden sm:table-cell">变动后余额（元）</th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="entry in ledgerEntries"
              :key="String(entry.id)"
              class="border-b last:border-b-0"
              style="border-color: var(--theme-border);"
            >
              <td class="px-4 py-3 whitespace-nowrap" style="color: var(--theme-text-secondary);">{{ entry.createTime || '-' }}</td>
              <td class="px-4 py-3" style="color: var(--theme-text);">{{ entry.summary || entry.bizType || '-' }}</td>
              <td class="px-4 py-3 text-right font-medium whitespace-nowrap"
                  :style="{ color: entry.direction === 'credit' ? '#22c55e' : 'var(--theme-text);' }">
                {{ entry.direction === 'credit' ? '+' : '-' }}{{ entry.amountYuan?.toFixed(2) ?? '0.00' }}
              </td>
              <td class="px-4 py-3 text-right whitespace-nowrap hidden sm:table-cell" style="color: var(--theme-text-secondary);">
                {{ entry.balanceAfterYuan != null ? entry.balanceAfterYuan.toFixed(2) : '-' }}
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- 银行卡 -->
      <div v-if="activeTab === 'bankcard'" class="space-y-4">
        <div class="flex justify-end">
          <button
            class="px-4 py-2 rounded-lg text-sm font-medium text-white transition-opacity hover:opacity-90"
            style="background-color: var(--theme-primary);"
            @click="showBindForm = !showBindForm"
          >
            {{ showBindForm ? '收起' : '绑定银行卡' }}
          </button>
        </div>

        <!-- 绑定表单 -->
        <div v-if="showBindForm" class="rounded-2xl border p-5" style="background-color: var(--theme-surface); border-color: var(--theme-border);">
          <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label class="block text-xs mb-1.5" style="color: var(--theme-text-secondary);">持卡人姓名</label>
              <input v-model="bindForm.holderName" type="text" placeholder="与银行卡开户名一致"
                     class="w-full px-3 py-2 rounded-lg text-sm outline-none"
                     style="background-color: var(--theme-bg); border: 1px solid var(--theme-border); color: var(--theme-text);" />
            </div>
            <div>
              <label class="block text-xs mb-1.5" style="color: var(--theme-text-secondary);">银行卡号</label>
              <input v-model="bindForm.cardNo" type="text" maxlength="32" placeholder="仅支持借记卡"
                     class="w-full px-3 py-2 rounded-lg text-sm outline-none"
                     style="background-color: var(--theme-bg); border: 1px solid var(--theme-border); color: var(--theme-text);" />
            </div>
            <div>
              <label class="block text-xs mb-1.5" style="color: var(--theme-text-secondary);">预留手机号</label>
              <input v-model="bindForm.phone" type="text" maxlength="11" placeholder="银行预留手机号"
                     class="w-full px-3 py-2 rounded-lg text-sm outline-none"
                     style="background-color: var(--theme-bg); border: 1px solid var(--theme-border); color: var(--theme-text);" />
            </div>
            <div>
              <label class="block text-xs mb-1.5" style="color: var(--theme-text-secondary);">开户银行</label>
              <input v-model="bindForm.bankName" type="text" placeholder="如：中国工商银行"
                     class="w-full px-3 py-2 rounded-lg text-sm outline-none"
                     style="background-color: var(--theme-bg); border: 1px solid var(--theme-border); color: var(--theme-text);" />
            </div>
          </div>
          <p class="text-xs mt-3" style="color: var(--theme-text-secondary);">
            卡号与手机号将加密存储，任何页面只展示脱敏信息；银行卡仅用于后续提现打款。
          </p>
          <button
            class="mt-4 px-5 py-2 rounded-lg text-sm font-medium text-white transition-opacity hover:opacity-90 disabled:opacity-50"
            style="background-color: var(--theme-primary);"
            :disabled="bindSubmitting"
            @click="handleBind"
          >
            {{ bindSubmitting ? '提交中…' : '确认绑定' }}
          </button>
        </div>

        <!-- 卡列表 -->
        <div v-if="cardLoading" class="text-center py-8 text-sm" style="color: var(--theme-text-secondary);">加载中…</div>
        <div v-else-if="bankCards.length === 0" class="rounded-2xl border py-12 text-center text-sm" style="background-color: var(--theme-surface); border-color: var(--theme-border); color: var(--theme-text-secondary);">
          暂未绑定银行卡
        </div>
        <div v-else class="space-y-3">
          <div
            v-for="card in bankCards"
            :key="String(card.id)"
            class="rounded-2xl border p-5 flex flex-col sm:flex-row sm:items-center gap-3 justify-between"
            style="background-color: var(--theme-surface); border-color: var(--theme-border);"
          >
            <div>
              <div class="flex items-center gap-2 flex-wrap">
                <span class="font-medium" style="color: var(--theme-text);">{{ card.bankName || card.bankCode || '银行卡' }}</span>
                <span
                  class="px-1.5 py-0.5 rounded text-[10px] font-medium"
                  :style="card.isDefault === 1
                    ? 'background-color: var(--theme-primary); color: #fff;'
                    : 'background-color: var(--theme-bg); color: var(--theme-text-secondary);'"
                >
                  {{ card.isDefault === 1 ? '默认' : '备用' }}
                </span>
                <span
                  v-if="card.verifyStatus"
                  class="px-1.5 py-0.5 rounded text-[10px]"
                  :style="card.verifyStatus === 'VERIFIED'
                    ? 'background-color: rgba(34,197,94,0.12); color: #16a34a;'
                    : 'background-color: rgba(234,179,8,0.12); color: #ca8a04;'"
                >
                  {{ card.verifyStatus === 'VERIFIED' ? '已核实' : '待核实' }}
                </span>
              </div>
              <p class="text-sm mt-1.5" style="color: var(--theme-text-secondary);">{{ card.cardNoMasked }}</p>
              <p class="text-xs mt-0.5" style="color: var(--theme-text-secondary);">{{ card.holderName }} · {{ card.phoneMasked }}</p>
            </div>
            <div class="flex gap-2 self-end sm:self-center">
              <button
                v-if="card.isDefault !== 1"
                class="px-3 py-1.5 rounded-lg text-xs border transition-colors"
                style="color: var(--theme-text); border-color: var(--theme-border);"
                @click="handleSetDefault(card)"
              >
                设为默认
              </button>
              <button
                class="px-3 py-1.5 rounded-lg text-xs border transition-colors"
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
  </div>
</template>

<script setup lang="ts">
import { useToast } from '@/composables/useToast';
import { ref, onMounted } from 'vue';
import {
  getAccountOverview,
  getMyLedger,
  getBankCards,
  bindBankCard,
  deleteBankCard,
  setDefaultBankCard,
} from '@/api/pay';
import type { PayAccountOverview, PayLedgerEntry, UserBankCard } from '@/types/api';


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
const bankCards = ref<UserBankCard[]>([]);
const cardLoading = ref(false);
const showBindForm = ref(false);
const bindSubmitting = ref(false);
const bindForm = ref({ holderName: '', cardNo: '', phone: '', bankName: '' });

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

const loadLedger = async () => {
  ledgerLoading.value = true;
  try {
    const res = await getMyLedger({ current: 1, size: 50 });
    ledgerEntries.value = res.data?.records ?? [];
  } catch {
    toast.error('流水加载失败');
  } finally {
    ledgerLoading.value = false;
  }
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
  bindSubmitting.value = true;
  try {
    await bindBankCard({
      holderName: f.holderName.trim(),
      cardNo: f.cardNo,
      phone: f.phone,
      bankName: f.bankName.trim() || undefined,
    });
    toast.success('绑定成功');
    showBindForm.value = false;
    bindForm.value = { holderName: '', cardNo: '', phone: '', bankName: '' };
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
  // 银行卡 tab 懒加载
  if (activeTab.value === 'bankcard') {
    await loadCards();
  }
});
</script>
