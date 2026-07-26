<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useHead } from '@vueuse/head';
import {
  Building2, FileText, ListChecks,
  Loader2, ChevronRight,
} from 'lucide-vue-next';
import LazyImage from '@/components/LazyImage.vue';
import SiteFooter from '@/components/SiteFooter.vue';
import Breadcrumb from '@/components/Breadcrumb.vue';
import Empty from '@/components/Empty.vue';
import Pagination from '@/components/Pagination.vue';
import { generateSeo } from '@/utils/seo';
import { getSafeAvatar } from '@/utils/avatar';
import { formatRelativeTime } from '@/utils/date';
import {
  getCompanyDetail, getCompanyQuestions, getCompanyExperiences,
} from '@/api/company';
import type {
  InterviewCompanyVO, InterviewQuestionVO, InterviewExperienceVO,
} from '@/types/api';

const route = useRoute();
const router = useRouter();

const companyId = computed(() => route.params.id as string);

const loading = ref(false);
const error = ref<string | null>(null);
const company = ref<InterviewCompanyVO | null>(null);

type TabKey = 'questions' | 'experiences';
const activeTab = ref<TabKey>('questions');

// 各 Tab 数据
const questions = ref<InterviewQuestionVO[]>([]);
const experiences = ref<InterviewExperienceVO[]>([]);
const tabLoading = ref(false);

// 分页（每个 Tab 独立）
const page = ref(1);
const pageSize = 10;
const total = ref(0);
const totalPages = computed(() => Math.max(1, Math.ceil(total.value / pageSize)));

const tabs: { key: TabKey; label: string; icon: typeof ListChecks }[] = [
  { key: 'questions', label: '公司题目', icon: ListChecks },
  { key: 'experiences', label: '面经', icon: FileText },
];

useHead(computed(() => generateSeo({
  title: company.value?.name ? `${company.value.name} - 公司主页` : '公司主页',
  description: company.value?.description || company.value?.industry || '聚合公司题目与面经',
  keywords: ['公司主页', company.value?.name || '墨韵'].filter(Boolean) as string[],
  canonicalPath: `/interview/company/${companyId.value}`,
})));

// 面包屑
const breadcrumbs = computed(() => [
  { label: '面试指南', path: '/interview' },
  { label: '公司' },
]);

onMounted(() => {
  loadCompany();
});

watch(companyId, (newId, oldId) => {
  if (newId && newId !== oldId) {
    company.value = null;
    activeTab.value = 'questions';
    page.value = 1;
    loadCompany();
  }
});

watch(activeTab, () => {
  page.value = 1;
  loadTabData();
});

async function loadCompany() {
  loading.value = true;
  error.value = null;
  try {
    const res = await getCompanyDetail(companyId.value);
    if (res.code === 200 && res.data) {
      company.value = res.data;
      await loadTabData();
    } else {
      error.value = res.message || '公司不存在或已下架';
    }
  } catch (err) {
    const e = err as { message?: string };
    error.value = e?.message || '加载公司信息失败，请稍后重试';
  } finally {
    loading.value = false;
  }
}

async function loadTabData() {
  if (!company.value) return;
  tabLoading.value = true;
  try {
    if (activeTab.value === 'questions') {
      const res = await getCompanyQuestions(companyId.value, { pageNum: page.value, pageSize });
      questions.value = res.data?.list || [];
      total.value = res.data?.total || 0;
    } else if (activeTab.value === 'experiences') {
      const res = await getCompanyExperiences(company.value.name, { pageNum: page.value, pageSize });
      experiences.value = res.data?.list || [];
      total.value = res.data?.total || 0;
    }
  } catch (err) {
    // 静默失败，保持列表为空
    const e = err as { message?: string };
    error.value = e?.message || null;
  } finally {
    tabLoading.value = false;
  }
}

function onPageChange(p: number) {
  page.value = p;
  loadTabData();
}

function goQuestion(q: InterviewQuestionVO) {
  router.push(`/interview/question/${q.id}`);
}
function goExperience(e: InterviewExperienceVO) {
  router.push(`/interview/experience/${e.id}`);
}
</script>

<template>
  <div class="min-h-screen flex flex-col" style="background-color: var(--theme-bg);">
    <!-- 吸顶面包屑栏 -->
    <div class="border-b sticky top-0 z-30 backdrop-blur-sm py-3" style="background-color: var(--theme-surface); border-color: var(--theme-border);">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 flex items-center justify-between gap-4">
        <Breadcrumb :items="breadcrumbs" />
      </div>
    </div>

    <main class="flex-1 py-6">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <!-- 加载中 -->
        <div v-if="loading" class="text-center py-20">
          <Loader2 class="w-8 h-8 animate-spin mx-auto" style="color: var(--theme-primary);" />
          <p class="mt-3 text-sm" style="color: var(--theme-text-secondary);">加载中...</p>
        </div>

        <!-- 错误 -->
        <div v-else-if="error && !company" class="rounded-xl p-10 text-center" style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);">
          <Building2 class="w-12 h-12 mx-auto mb-3 opacity-40" style="color: var(--theme-text-secondary);" />
          <p class="mb-4" style="color: var(--theme-text-secondary);">{{ error }}</p>
          <button @click="router.push('/interview')" class="px-4 py-2 text-white rounded-lg text-sm transition hover:opacity-90" style="background-color: var(--theme-primary);">
            返回面试指南
          </button>
        </div>

        <template v-else-if="company">
          <!-- 公司信息卡 -->
          <section class="rounded-2xl shadow-sm p-6 mb-6 flex flex-col sm:flex-row items-start gap-5" style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);">
            <div class="w-20 h-20 rounded-2xl overflow-hidden flex-shrink-0 flex items-center justify-center" style="background-color: var(--theme-bg);">
              <LazyImage
                v-if="company.logo"
                :src="getSafeAvatar(company.logo)"
                :alt="company.name"
                class="w-full h-full object-cover"
              />
              <Building2 v-else class="w-10 h-10" style="color: var(--theme-text-secondary);" />
            </div>
            <div class="flex-1 min-w-0">
              <h2 class="text-2xl font-bold flex items-center gap-2" style="color: var(--theme-text);">
                {{ company.name }}
              </h2>
              <div class="flex flex-wrap items-center gap-x-4 gap-y-1 mt-2 text-sm" style="color: var(--theme-text-secondary);">
                <span v-if="company.industry" class="flex items-center gap-1">
                  <Building2 class="w-4 h-4" /> {{ company.industry }}
                </span>
                <span v-if="company.questionCount != null" class="flex items-center gap-1">
                  <ListChecks class="w-4 h-4" /> {{ company.questionCount }} 道题
                </span>
              </div>
              <p v-if="company.description" class="mt-3 text-sm leading-relaxed" style="color: var(--theme-text-secondary);">
                {{ company.description }}
              </p>
            </div>
          </section>

          <!-- Tab 切换 -->
          <div class="flex items-center gap-1 border-b mb-5 overflow-x-auto" style="border-color: var(--theme-border);">
            <button
              v-for="t in tabs"
              :key="t.key"
              @click="activeTab = t.key"
              class="flex items-center gap-1.5 px-4 py-2.5 text-sm font-medium whitespace-nowrap transition border-b-2 -mb-px"
              :style="activeTab === t.key
                ? 'color: var(--theme-primary); border-color: var(--theme-primary);'
                : 'color: var(--theme-text-secondary); border-color: transparent;'"
            >
              <component :is="t.icon" class="w-4 h-4" />
              {{ t.label }}
            </button>
          </div>

          <!-- Tab 内容 -->
          <div v-if="tabLoading" class="text-center py-16">
            <Loader2 class="w-7 h-7 animate-spin mx-auto" style="color: var(--theme-primary);" />
          </div>

          <div v-else>
            <!-- 公司题目 -->
            <template v-if="activeTab === 'questions'">
              <Empty v-if="questions.length === 0" description="该公司暂无关联题目" />
              <div v-else class="space-y-2">
                <div
                  v-for="q in questions"
                  :key="q.id"
                  @click="goQuestion(q)"
                  class="rounded-xl p-4 cursor-pointer transition hover:shadow-md group"
                  style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);"
                >
                  <div class="flex items-center justify-between gap-3">
                    <h3 class="font-medium truncate group-hover:underline" style="color: var(--theme-text);">
                      {{ q.title }}
                    </h3>
                    <ChevronRight class="w-4 h-4 flex-shrink-0 opacity-50" style="color: var(--theme-text-secondary);" />
                  </div>
                  <div class="flex items-center gap-3 mt-1.5 text-xs" style="color: var(--theme-text-secondary);">
                    <span>{{ q.categoryName || '未分类' }}</span>
                    <span>{{ q.submissionCount || 0 }} 次提交</span>
                    <span>通过率 {{ q.acceptanceRate || 0 }}%</span>
                  </div>
                </div>
              </div>
            </template>

            <!-- 面经 -->
            <template v-else-if="activeTab === 'experiences'">
              <Empty v-if="experiences.length === 0" description="该公司暂无面经" />
              <div v-else class="space-y-2">
                <div
                  v-for="e in experiences"
                  :key="e.id"
                  @click="goExperience(e)"
                  class="rounded-xl p-4 cursor-pointer transition hover:shadow-md group"
                  style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);"
                >
                  <h3 class="font-medium truncate group-hover:underline" style="color: var(--theme-text);">
                    {{ e.title }}
                  </h3>
                  <div class="flex items-center gap-3 mt-1.5 text-xs" style="color: var(--theme-text-secondary);">
                    <span v-if="e.position">{{ e.position }}</span>
                    <span v-if="e.year">{{ e.year }}年</span>
                    <span>{{ e.viewCount || 0 }} 阅读</span>
                    <span>{{ formatRelativeTime(e.createTime || '') }}</span>
                  </div>
                </div>
              </div>
            </template>

            <!-- 分页 -->
            <Pagination
              v-if="total > 0"
              :current-page="page"
              :total-pages="totalPages"
              :total-items="total"
              :items-per-page="pageSize"
              @page-change="onPageChange"
            />
          </div>
        </template>
      </div>
    </main>

    <SiteFooter />
  </div>
</template>
