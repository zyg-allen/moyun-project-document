<script setup lang="ts">
import { ref, computed, onMounted, watch, reactive } from 'vue';
import { useRouter } from 'vue-router';
import { useHead } from '@vueuse/head';
import {
  Target, Plus, Pencil, Trash2, CheckCircle2, Loader2,
  AlertCircle, Minus, Flame, X, ListChecks, Sparkles,
} from 'lucide-vue-next';
import SiteFooter from '@/components/SiteFooter.vue';
import Breadcrumb from '@/components/Breadcrumb.vue';
import { generateSeo } from '@/utils/seo';
import {
  getMyStudyPlans, saveStudyPlan, deleteStudyPlan,
  recordPlanProgress, changePlanStatus, autoGeneratePlans,
} from '@/api/learn';
import type { StudyPlanVO, StudyPlanSaveBody } from '@/api/learn';
import { useToast } from '@/composables/useToast';

const router = useRouter();
const toast = useToast();

const loading = ref(false);
const error = ref<string | null>(null);
const plans = ref<StudyPlanVO[]>([]);
const total = ref(0);
const page = ref(1);
const pageSize = 12;
const actionId = ref<number | null>(null);
// v5.9 阶段3：画像生成状态
const generating = ref(false);

type StatusFilter = 'active' | 'completed' | 'abandoned' | '';
const statusFilter = ref<StatusFilter>('');

// 编辑表单
const formOpen = ref(false);
const formLoading = ref(false);
const formError = ref<string | null>(null);
const isEdit = computed(() => form.id != null && form.id > 0);

const form = reactive<StudyPlanSaveBody>({
  id: undefined,
  title: '',
  planType: 'daily_question',
  targetCount: 10,
  targetCategory: '',
  startDate: '',
  endDate: '',
  status: 'active',
});

const totalPages = computed(() => Math.max(1, Math.ceil(total.value / pageSize)));

const breadcrumbs = computed(() => [
  { label: '学习中心', path: '/learn' },
  { label: '学习计划' },
]);

useHead(computed(() => generateSeo({
  title: '学习计划',
  description: '墨韵智库学习计划 - 创建刷题/阅读计划，跟踪进度，记录每日完成',
  keywords: ['学习计划', '刷题计划', '目标', '进度跟踪', '墨韵'],
  canonicalPath: '/learn/plan',
  robots: 'noindex,nofollow',
})));

onMounted(() => {
  loadPlans();
});

watch(page, () => {
  loadPlans();
});

watch(statusFilter, () => {
  if (page.value !== 1) {
    page.value = 1;
  } else {
    loadPlans();
  }
});

async function loadPlans() {
  loading.value = true;
  error.value = null;
  try {
    const params: Record<string, unknown> = { pageNum: page.value, pageSize };
    if (statusFilter.value) params.status = statusFilter.value;
    const res = await getMyStudyPlans(params);
    if (res.code === 200 && res.data) {
      plans.value = res.data.list || [];
      total.value = res.data.total || 0;
    } else {
      error.value = res.message || '加载失败';
    }
  } catch (err) {
    const e = err as { message?: string };
    error.value = e?.message || '加载失败，请稍后重试';
  } finally {
    loading.value = false;
  }
}

function openCreate() {
  Object.assign(form, {
    id: undefined,
    title: '',
    planType: 'daily_question',
    targetCount: 10,
    targetCategory: '',
    startDate: '',
    endDate: '',
    status: 'active',
  });
  formError.value = null;
  formOpen.value = true;
}

// v5.9 阶段3：基于画像自动生成学习计划
async function handleAutoGenerate() {
  if (generating.value) return;
  try {
    generating.value = true;
    const res = await autoGeneratePlans();
    if (res.code === 200 && res.data) {
      const count = res.data.length;
      if (count === 0) {
        toast.info('暂无可生成的计划（画像薄弱点为空或已有同名计划）');
      } else {
        toast.success(`已生成 ${count} 个学习计划（基于你的薄弱点与岗位必备技能）`);
        // 刷新列表
        await loadPlans();
      }
    } else {
      toast.error(res.message || '生成失败，请稍后重试');
    }
  } catch (err: any) {
    toast.error(err?.message || '生成失败，请稍后重试');
  } finally {
    generating.value = false;
  }
}

function openEdit(plan: StudyPlanVO) {
  Object.assign(form, {
    id: plan.id,
    title: plan.title,
    planType: plan.planType || 'custom',
    targetCount: plan.targetCount ?? undefined,
    targetCategory: plan.targetCategory || '',
    startDate: plan.startDate || '',
    endDate: plan.endDate || '',
    status: plan.status,
  });
  formError.value = null;
  formOpen.value = true;
}

function closeForm() {
  if (formLoading.value) return;
  formOpen.value = false;
}

async function submitForm() {
  if (!form.title.trim()) {
    formError.value = '计划标题不能为空';
    return;
  }
  formLoading.value = true;
  formError.value = null;
  try {
    const body: StudyPlanSaveBody = {
      id: form.id,
      title: form.title.trim(),
      planType: form.planType,
      targetCount: form.targetCount || undefined,
      targetCategory: form.targetCategory || undefined,
      startDate: form.startDate || undefined,
      endDate: form.endDate || undefined,
      status: form.status,
    };
    await saveStudyPlan(body);
    formOpen.value = false;
    await loadPlans();
  } catch (err) {
    const e = err as { message?: string };
    formError.value = e?.message || '保存失败';
  } finally {
    formLoading.value = false;
  }
}

async function removePlan(plan: StudyPlanVO) {
  if (!window.confirm(`确定删除计划「${plan.title}」？该操作不可恢复，相关进度日志将一并删除。`)) return;
  actionId.value = plan.id;
  try {
    await deleteStudyPlan(plan.id);
    await loadPlans();
  } catch (err) {
    const e = err as { message?: string };
    error.value = e?.message || '删除失败';
  } finally {
    actionId.value = null;
  }
}

async function incProgress(plan: StudyPlanVO, delta: number) {
  actionId.value = plan.id;
  try {
    const res = await recordPlanProgress(plan.id, delta);
    if (res.code === 200) {
      // 局部更新进度
      plan.todayDoneCount = (res.data as number) ?? plan.todayDoneCount + delta;
      plan.doneCount = Math.max(0, plan.doneCount + delta);
      if (plan.targetCount && plan.targetCount > 0) {
        plan.progressPercent = Math.min(100, Math.round(plan.doneCount * 100 / plan.targetCount));
      }
    }
  } catch (err) {
    const e = err as { message?: string };
    error.value = e?.message || '更新进度失败';
  } finally {
    actionId.value = null;
  }
}

async function toggleStatus(plan: StudyPlanVO, target: string) {
  actionId.value = plan.id;
  try {
    await changePlanStatus(plan.id, target);
    plan.status = target;
  } catch (err) {
    const e = err as { message?: string };
    error.value = e?.message || '状态更新失败';
  } finally {
    actionId.value = null;
  }
}

function gotoPage(p: number) {
  if (p < 1 || p > totalPages.value) return;
  page.value = p;
  window.scrollTo({ top: 0, behavior: 'smooth' });
}

function planTypeText(t: string | null) {
  const map: Record<string, string> = {
    daily_question: '每日刷题',
    weekly_reading: '每周阅读',
    custom: '自定义',
  };
  return (t && map[t]) || '学习计划';
}

function statusText(s: string) {
  const map: Record<string, string> = { active: '进行中', completed: '已完成', abandoned: '已放弃' };
  return map[s] || s;
}

function statusTagColor(s: string) {
  if (s === 'active') return '#10b981';
  if (s === 'completed') return '#3b82f6';
  return '#94a3b8';
}

const statusTabs: { value: StatusFilter; label: string }[] = [
  { value: '', label: '全部' },
  { value: 'active', label: '进行中' },
  { value: 'completed', label: '已完成' },
  { value: 'abandoned', label: '已放弃' },
];
</script>

<template>
  <div class="min-h-screen flex flex-col" style="background-color: var(--theme-bg);">
    <!-- 顶部面包屑栏 -->
    <div class="border-b sticky top-0 z-30 backdrop-blur-sm py-3" style="background-color: var(--theme-surface); border-color: var(--theme-border);">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 flex items-center justify-between gap-4">
        <Breadcrumb :items="breadcrumbs" />
        <div class="flex items-center gap-3">
          <button
            @click="handleAutoGenerate"
            :disabled="generating"
            class="inline-flex items-center text-sm font-medium px-3 py-1.5 rounded-lg text-white transition hover:opacity-90 disabled:opacity-50 disabled:cursor-not-allowed"
            style="background: linear-gradient(135deg, var(--theme-primary), color-mix(in srgb, var(--theme-primary) 70%, #7c3aed));"
            title="根据你的薄弱点与岗位必备技能自动生成学习计划"
          >
            <Sparkles class="w-4 h-4 mr-1" />
            {{ generating ? '生成中...' : '基于画像生成' }}
          </button>
          <button
            @click="openCreate"
            class="inline-flex items-center text-sm font-medium transition hover:opacity-90"
            style="color: var(--theme-primary);"
          >
            <Plus class="w-4 h-4 mr-1" /> 新建计划
          </button>
        </div>
      </div>
    </div>

    <main class="flex-1 max-w-7xl mx-auto w-full px-4 sm:px-6 lg:px-8 py-8">
      <!-- 状态筛选 -->
      <div class="mb-6 flex items-center gap-1 p-1 rounded-lg w-fit" style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);">
        <button
          v-for="tab in statusTabs"
          :key="tab.value"
          @click="statusFilter = tab.value"
          class="px-3 py-1.5 rounded-md text-sm font-medium transition"
          :style="statusFilter === tab.value
            ? 'background-color: var(--theme-primary); color: #fff;'
            : 'color: var(--theme-text-secondary);'"
        >
          {{ tab.label }}
        </button>
      </div>

      <!-- 错误提示 -->
      <div
        v-if="error"
        class="mb-4 rounded-lg p-3 flex items-start text-sm"
        style="background-color: color-mix(in srgb, #ef4444 10%, transparent); color: #ef4444;"
      >
        <AlertCircle class="w-4 h-4 mr-2 mt-0.5 flex-shrink-0" />
        <span>{{ error }}</span>
      </div>

      <!-- 加载态 -->
      <div v-if="loading" class="flex flex-col items-center justify-center py-20">
        <Loader2 class="w-8 h-8 animate-spin" style="color: var(--theme-primary);" />
        <p class="mt-3 text-sm" style="color: var(--theme-text-secondary);">加载中...</p>
      </div>

      <!-- 空态 -->
      <div
        v-else-if="plans.length === 0"
        class="flex flex-col items-center justify-center py-20 rounded-xl border"
        style="border-color: var(--theme-border); background-color: var(--theme-surface);"
      >
        <Target class="w-12 h-12 mb-4 opacity-40" style="color: var(--theme-text-secondary);" />
        <p class="text-sm mb-4" style="color: var(--theme-text-secondary);">
          {{ statusFilter ? '该状态下暂无计划' : '还没有学习计划，开始创建第一个吧' }}
        </p>
        <button
          @click="openCreate"
          class="inline-flex items-center px-5 py-2.5 rounded-lg text-sm font-medium text-white transition hover:opacity-90"
          style="background-color: var(--theme-primary);"
        >
          <Plus class="w-4 h-4 mr-1.5" /> 新建计划
        </button>
      </div>

      <!-- 计划列表 -->
      <div v-else class="grid grid-cols-1 md:grid-cols-2 gap-4">
        <div
          v-for="plan in plans"
          :key="plan.id"
          class="rounded-xl border p-5 transition hover:shadow-md"
          style="background-color: var(--theme-surface); border-color: var(--theme-border);"
        >
          <div class="flex items-start justify-between mb-3">
            <div class="flex items-center min-w-0">
              <span
                class="inline-block px-2 py-0.5 rounded text-xs mr-2 flex-shrink-0"
                style="background-color: color-mix(in srgb, var(--theme-primary) 12%, transparent); color: var(--theme-primary);"
              >
                {{ planTypeText(plan.planType) }}
              </span>
              <h3 class="text-base font-semibold truncate" style="color: var(--theme-text);">{{ plan.title }}</h3>
            </div>
            <span
              class="text-xs px-2 py-0.5 rounded-full flex-shrink-0 ml-2"
              :style="{ color: '#fff', backgroundColor: statusTagColor(plan.status) }"
            >
              {{ statusText(plan.status) }}
            </span>
          </div>

          <!-- 目标信息 -->
          <div class="flex flex-wrap items-center gap-x-4 gap-y-1 text-xs mb-3" style="color: var(--theme-text-secondary);">
            <span v-if="plan.targetCount" class="inline-flex items-center">
              <ListChecks class="w-3.5 h-3.5 mr-1" />
              目标 {{ plan.targetCount }} 题
            </span>
            <span v-if="plan.targetCategory">分类：{{ plan.targetCategory }}</span>
            <span v-if="plan.startDate">{{ plan.startDate }} 起</span>
            <span v-if="plan.endDate">至 {{ plan.endDate }}</span>
          </div>

          <!-- 进度 -->
          <div class="mb-3">
            <div class="flex items-center justify-between text-xs mb-1.5" style="color: var(--theme-text-secondary);">
              <span>已完成 {{ plan.doneCount }} / {{ plan.targetCount || '∞' }}</span>
              <span>{{ plan.progressPercent || 0 }}%</span>
            </div>
            <div class="h-2 rounded-full overflow-hidden" style="background-color: var(--theme-border);">
              <div
                class="h-full rounded-full transition-all"
                :style="{ width: (plan.progressPercent || 0) + '%', backgroundColor: 'var(--theme-primary)' }"
              ></div>
            </div>
          </div>

          <!-- 今日完成与打卡 -->
          <div class="flex items-center justify-between text-xs mb-4" style="color: var(--theme-text-secondary);">
            <span class="inline-flex items-center">
              <Flame class="w-3.5 h-3.5 mr-1" style="color: #f59e0b;" />
              连续 {{ plan.streakDays }} 天
            </span>
            <span>今日完成 {{ plan.todayDoneCount }} 题</span>
          </div>

          <!-- 操作 -->
          <div class="flex items-center justify-between border-t pt-3" style="border-color: var(--theme-border);">
            <!-- 今日打卡（仅 active 可用） -->
            <div v-if="plan.status === 'active'" class="flex items-center gap-2">
              <button
                :disabled="actionId === plan.id"
                @click="incProgress(plan, -1)"
                class="w-7 h-7 rounded-md flex items-center justify-center transition hover:opacity-80 disabled:opacity-40"
                style="border: 1px solid var(--theme-border); color: var(--theme-text-secondary);"
                title="减少今日完成数"
              >
                <Minus class="w-3.5 h-3.5" />
              </button>
              <button
                :disabled="actionId === plan.id"
                @click="incProgress(plan, 1)"
                class="px-3 h-7 rounded-md text-xs font-medium text-white transition hover:opacity-90 disabled:opacity-40 inline-flex items-center"
                style="background-color: var(--theme-primary);"
                title="记录今日完成 +1"
              >
                <Plus class="w-3.5 h-3.5 mr-0.5" /> 今日打卡
              </button>
            </div>
            <div v-else></div>

            <!-- 编辑/状态/删除 -->
            <div class="flex items-center gap-1">
              <button
                v-if="plan.status === 'active'"
                @click="toggleStatus(plan, 'completed')"
                :disabled="actionId === plan.id"
                class="w-7 h-7 rounded-md flex items-center justify-center transition hover:opacity-80"
                style="color: #10b981;"
                title="标记为已完成"
              >
                <CheckCircle2 class="w-4 h-4" />
              </button>
              <button
                v-if="plan.status !== 'active'"
                @click="toggleStatus(plan, 'active')"
                :disabled="actionId === plan.id"
                class="text-xs px-2 h-7 rounded-md transition hover:opacity-80"
                style="color: var(--theme-primary);"
                title="重新激活"
              >
                激活
              </button>
              <button
                @click="openEdit(plan)"
                class="w-7 h-7 rounded-md flex items-center justify-center transition hover:opacity-80"
                style="color: var(--theme-text-secondary);"
                title="编辑"
              >
                <Pencil class="w-3.5 h-3.5" />
              </button>
              <button
                :disabled="actionId === plan.id"
                @click="removePlan(plan)"
                class="w-7 h-7 rounded-md flex items-center justify-center transition hover:opacity-80"
                style="color: #ef4444;"
                title="删除"
              >
                <Trash2 class="w-3.5 h-3.5" />
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- 分页 -->
      <div v-if="total > pageSize" class="mt-8 flex items-center justify-center gap-2">
        <button
          :disabled="page <= 1"
          @click="gotoPage(page - 1)"
          class="px-3 py-1.5 rounded-md text-sm transition disabled:opacity-40"
          style="border: 1px solid var(--theme-border); color: var(--theme-text);"
        >上一页</button>
        <span class="text-sm" style="color: var(--theme-text-secondary);">
          {{ page }} / {{ totalPages }}
        </span>
        <button
          :disabled="page >= totalPages"
          @click="gotoPage(page + 1)"
          class="px-3 py-1.5 rounded-md text-sm transition disabled:opacity-40"
          style="border: 1px solid var(--theme-border); color: var(--theme-text);"
        >下一页</button>
      </div>
    </main>

    <!-- 创建/编辑对话框 -->
    <div
      v-if="formOpen"
      class="fixed inset-0 z-50 flex items-center justify-center p-4"
      style="background-color: rgba(0,0,0,0.5);"
      @click.self="closeForm"
    >
      <div
        class="w-full max-w-lg rounded-xl shadow-xl"
        style="background-color: var(--theme-surface);"
      >
        <div
          class="px-5 py-4 border-b flex items-center justify-between"
          style="border-color: var(--theme-border);"
        >
          <h3 class="text-base font-semibold" style="color: var(--theme-text);">
            {{ isEdit ? '编辑计划' : '新建学习计划' }}
          </h3>
          <button
            @click="closeForm"
            class="transition hover:opacity-70"
            style="color: var(--theme-text-secondary);"
          >
            <X class="w-5 h-5" />
          </button>
        </div>

        <div class="p-5 space-y-4">
          <div v-if="formError" class="rounded-lg p-3 text-sm" style="background-color: color-mix(in srgb, #ef4444 10%, transparent); color: #ef4444;">
            {{ formError }}
          </div>

          <div>
            <label class="block text-sm mb-1.5" style="color: var(--theme-text);">计划标题 *</label>
            <input
              v-model="form.title"
              type="text"
              placeholder="如：每日刷 5 道算法题"
              class="w-full px-3 py-2 rounded-lg text-sm focus:outline-none"
              style="border: 1px solid var(--theme-border); background-color: var(--theme-bg); color: var(--theme-text);"
            />
          </div>

          <div class="grid grid-cols-2 gap-4">
            <div>
              <label class="block text-sm mb-1.5" style="color: var(--theme-text);">计划类型</label>
              <select
                v-model="form.planType"
                class="w-full px-3 py-2 rounded-lg text-sm focus:outline-none"
                style="border: 1px solid var(--theme-border); background-color: var(--theme-bg); color: var(--theme-text);"
              >
                <option value="daily_question">每日刷题</option>
                <option value="weekly_reading">每周阅读</option>
                <option value="custom">自定义</option>
              </select>
            </div>
            <div>
              <label class="block text-sm mb-1.5" style="color: var(--theme-text);">目标数量</label>
              <input
                v-model.number="form.targetCount"
                type="number"
                min="0"
                placeholder="如：100"
                class="w-full px-3 py-2 rounded-lg text-sm focus:outline-none"
                style="border: 1px solid var(--theme-border); background-color: var(--theme-bg); color: var(--theme-text);"
              />
            </div>
          </div>

          <div>
            <label class="block text-sm mb-1.5" style="color: var(--theme-text);">目标分类</label>
            <input
              v-model="form.targetCategory"
              type="text"
              placeholder="如：算法 / 后端 / Java"
              class="w-full px-3 py-2 rounded-lg text-sm focus:outline-none"
              style="border: 1px solid var(--theme-border); background-color: var(--theme-bg); color: var(--theme-text);"
            />
          </div>

          <div class="grid grid-cols-2 gap-4">
            <div>
              <label class="block text-sm mb-1.5" style="color: var(--theme-text);">开始日期</label>
              <input
                v-model="form.startDate"
                type="date"
                class="w-full px-3 py-2 rounded-lg text-sm focus:outline-none"
                style="border: 1px solid var(--theme-border); background-color: var(--theme-bg); color: var(--theme-text);"
              />
            </div>
            <div>
              <label class="block text-sm mb-1.5" style="color: var(--theme-text);">结束日期</label>
              <input
                v-model="form.endDate"
                type="date"
                class="w-full px-3 py-2 rounded-lg text-sm focus:outline-none"
                style="border: 1px solid var(--theme-border); background-color: var(--theme-bg); color: var(--theme-text);"
              />
            </div>
          </div>
        </div>

        <div class="px-5 py-4 border-t flex items-center justify-end gap-2" style="border-color: var(--theme-border);">
          <button
            @click="closeForm"
            :disabled="formLoading"
            class="px-4 py-2 rounded-lg text-sm transition hover:opacity-80"
            style="border: 1px solid var(--theme-border); color: var(--theme-text);"
          >取消</button>
          <button
            @click="submitForm"
            :disabled="formLoading"
            class="px-4 py-2 rounded-lg text-sm font-medium text-white transition hover:opacity-90 disabled:opacity-50 inline-flex items-center"
            style="background-color: var(--theme-primary);"
          >
            <Loader2 v-if="formLoading" class="w-4 h-4 mr-1 animate-spin" />
            {{ isEdit ? '保存修改' : '创建计划' }}
          </button>
        </div>
      </div>
    </div>

    <SiteFooter />
  </div>
</template>
