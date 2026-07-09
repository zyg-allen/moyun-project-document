<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useHead } from '@vueuse/head';
import {
  Briefcase, MapPin, Search, ArrowLeft, Loader2, Building2, ChevronRight,
} from 'lucide-vue-next';
import LazyImage from '@/components/LazyImage.vue';
import SiteFooter from '@/components/SiteFooter.vue';
import Empty from '@/components/Empty.vue';
import Pagination from '@/components/Pagination.vue';
import { generateSeo } from '@/utils/seo';
import { getSafeAvatar } from '@/utils/avatar';
import { formatRelativeTime } from '@/utils/date';
import { getJobList } from '@/api/job';
import type { JobListItemVO, JobQuery } from '@/types/api';

const route = useRoute();
const router = useRouter();

// 筛选
const keyword = ref((route.query.keyword as string) || '');
const searchInput = ref(keyword.value);
const cityInput = ref((route.query.city as string) || '');
const activeCity = ref((route.query.city as string) || '');
const activeExperience = ref((route.query.experience as string) || '');
const activeEducation = ref((route.query.education as string) || '');

// 分页
const page = ref<number>(parseInt(route.query.page as string) || 1);
const pageSize = 10;
const total = ref(0);
const totalPages = computed(() => Math.max(1, Math.ceil(total.value / pageSize)));

// 数据
const loading = ref(false);
const error = ref<string | null>(null);
const jobs = ref<JobListItemVO[]>([]);

const experienceOptions = ['不限', '应届', '1-3年', '3-5年', '5-10年', '10年以上'];
const educationOptions = ['不限', '大专', '本科', '硕士', '博士'];

useHead(computed(() => generateSeo({
  title: '在招职位',
  description: '墨韵智库职位广场，聚合优质公司在招职位，助你完成求职闭环',
  canonicalPath: '/interview/jobs',
})));

onMounted(() => {
  loadJobs();
});

watch(page, () => loadJobs());

async function loadJobs() {
  loading.value = true;
  error.value = null;
  try {
    const params: JobQuery = {
      pageNum: page.value,
      pageSize,
    };
    if (keyword.value) params.keyword = keyword.value;
    if (activeCity.value) params.city = activeCity.value;
    if (activeExperience.value && activeExperience.value !== '不限') params.experience = activeExperience.value;
    if (activeEducation.value && activeEducation.value !== '不限') params.education = activeEducation.value;
    const res = await getJobList(params);
    jobs.value = res.data?.list || [];
    total.value = res.data?.total || 0;
  } catch (err) {
    const e = err as { message?: string };
    error.value = e?.message || '加载职位失败，请稍后重试';
  } finally {
    loading.value = false;
  }
}

function handleSearch() {
  keyword.value = searchInput.value.trim();
  activeCity.value = cityInput.value.trim();
  page.value = 1;
  loadJobs();
}

function selectExperience(opt: string) {
  activeExperience.value = opt === '不限' ? '' : opt;
  page.value = 1;
  loadJobs();
}

function selectEducation(opt: string) {
  activeEducation.value = opt === '不限' ? '' : opt;
  page.value = 1;
  loadJobs();
}

function onPageChange(p: number) {
  page.value = p;
}

function goJob(j: JobListItemVO) {
  router.push(`/interview/jobs/${j.id}`);
}

function goCompany(j: JobListItemVO) {
  if (j.companyId) {
    router.push(`/interview/company/${j.companyId}`);
  }
}

function salaryText(j: JobListItemVO): string {
  const min = j.salaryMin;
  const max = j.salaryMax;
  if (min == null && max == null) return '薪资面议';
  if (min != null && max != null) return `${min}k-${max}k`;
  if (min != null) return `${min}k起`;
  return `${max}k以内`;
}
</script>

<template>
  <div class="min-h-screen flex flex-col" style="background-color: var(--theme-bg);">
    <header class="sticky top-0 z-10 backdrop-blur" style="background-color: var(--theme-surface); border-bottom: 1px solid var(--theme-border);">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-3 flex items-center gap-3">
        <button @click="router.back()" class="p-1.5 rounded-lg transition hover:bg-black/5" style="color: var(--theme-text-secondary);">
          <ArrowLeft class="w-5 h-5" />
        </button>
        <h1 class="text-base font-semibold flex items-center gap-2" style="color: var(--theme-text);">
          <Briefcase class="w-5 h-5" /> 在招职位
        </h1>
      </div>
    </header>

    <main class="flex-1 py-6">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <!-- 搜索栏 -->
        <div class="rounded-xl shadow-sm p-4 mb-5" style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);">
          <div class="flex flex-col sm:flex-row gap-2">
            <div class="flex-1 flex items-center gap-2 px-3 py-2 rounded-lg" style="background-color: var(--theme-bg); border: 1px solid var(--theme-border);">
              <Search class="w-4 h-4 flex-shrink-0" style="color: var(--theme-text-secondary);" />
              <input
                v-model="searchInput"
                @keyup.enter="handleSearch"
                type="text"
                placeholder="搜索职位名称"
                class="flex-1 bg-transparent outline-none text-sm"
                style="color: var(--theme-text);"
              />
            </div>
            <div class="flex-1 flex items-center gap-2 px-3 py-2 rounded-lg" style="background-color: var(--theme-bg); border: 1px solid var(--theme-border);">
              <MapPin class="w-4 h-4 flex-shrink-0" style="color: var(--theme-text-secondary);" />
              <input
                v-model="cityInput"
                @keyup.enter="handleSearch"
                type="text"
                placeholder="城市"
                class="flex-1 bg-transparent outline-none text-sm"
                style="color: var(--theme-text);"
              />
            </div>
            <button
              @click="handleSearch"
              class="px-5 py-2 text-white rounded-lg text-sm font-medium transition hover:opacity-90"
              style="background-color: var(--theme-primary);"
            >
              搜索
            </button>
          </div>

          <!-- 经验/学历筛选 -->
          <div class="mt-3 space-y-2">
            <div class="flex items-center gap-2 flex-wrap text-xs">
              <span class="opacity-70" style="color: var(--theme-text-secondary);">经验：</span>
              <button
                v-for="opt in experienceOptions"
                :key="opt"
                @click="selectExperience(opt)"
                class="px-2.5 py-1 rounded-full transition"
                :style="(opt === '不限' ? !activeExperience : activeExperience === opt)
                  ? 'background-color: var(--theme-primary); color: white;'
                  : 'background-color: var(--theme-bg); color: var(--theme-text-secondary); border: 1px solid var(--theme-border);'"
              >
                {{ opt }}
              </button>
            </div>
            <div class="flex items-center gap-2 flex-wrap text-xs">
              <span class="opacity-70" style="color: var(--theme-text-secondary);">学历：</span>
              <button
                v-for="opt in educationOptions"
                :key="opt"
                @click="selectEducation(opt)"
                class="px-2.5 py-1 rounded-full transition"
                :style="(opt === '不限' ? !activeEducation : activeEducation === opt)
                  ? 'background-color: var(--theme-primary); color: white;'
                  : 'background-color: var(--theme-bg); color: var(--theme-text-secondary); border: 1px solid var(--theme-border);'"
              >
                {{ opt }}
              </button>
            </div>
          </div>
        </div>

        <!-- 加载 -->
        <div v-if="loading" class="text-center py-16">
          <Loader2 class="w-8 h-8 animate-spin mx-auto" style="color: var(--theme-primary);" />
          <p class="mt-3 text-sm" style="color: var(--theme-text-secondary);">加载中...</p>
        </div>

        <!-- 错误 -->
        <div v-else-if="error" class="rounded-xl p-8 text-center" style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);">
          <p class="mb-4" style="color: var(--theme-primary);">{{ error }}</p>
          <button @click="loadJobs" class="px-4 py-2 text-white rounded-lg text-sm transition hover:opacity-90" style="background-color: var(--theme-primary);">
            重试
          </button>
        </div>

        <template v-else>
          <Empty v-if="jobs.length === 0" description="暂无符合条件的职位" />
          <div v-else class="space-y-3">
            <div
              v-for="j in jobs"
              :key="j.id"
              @click="goJob(j)"
              class="rounded-xl p-4 cursor-pointer transition hover:shadow-md group"
              style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);"
            >
              <div class="flex items-start justify-between gap-3">
                <div class="flex-1 min-w-0">
                  <h3 class="font-semibold text-base truncate group-hover:underline" style="color: var(--theme-text);">
                    {{ j.title }}
                  </h3>
                  <div class="flex items-center gap-3 mt-1.5 text-xs" style="color: var(--theme-text-secondary);">
                    <span v-if="j.city" class="flex items-center gap-1"><MapPin class="w-3.5 h-3.5" />{{ j.city }}</span>
                    <span v-if="j.experience">{{ j.experience }}</span>
                    <span v-if="j.education">{{ j.education }}</span>
                    <span>{{ formatRelativeTime(j.createdTime || '') }}</span>
                  </div>
                </div>
                <span class="text-sm font-semibold flex-shrink-0" style="color: var(--theme-primary);">
                  {{ salaryText(j) }}
                </span>
              </div>
              <div v-if="j.companyName" class="flex items-center gap-2 mt-3 pt-3" style="border-top: 1px solid var(--theme-border);">
                <div class="w-6 h-6 rounded overflow-hidden flex-shrink-0 flex items-center justify-center" style="background-color: var(--theme-bg);">
                  <LazyImage v-if="j.companyLogo" :src="getSafeAvatar(j.companyLogo)" :alt="j.companyName" class="w-full h-full object-cover" />
                  <Building2 v-else class="w-4 h-4" style="color: var(--theme-text-secondary);" />
                </div>
                <span class="text-xs" style="color: var(--theme-text-secondary);">{{ j.companyName }}</span>
                <ChevronRight class="w-3.5 h-3.5 ml-auto opacity-40" style="color: var(--theme-text-secondary);" />
              </div>
            </div>
          </div>

          <Pagination
            v-if="total > 0"
            :current-page="page"
            :total-pages="totalPages"
            :total-items="total"
            :items-per-page="pageSize"
            @page-change="onPageChange"
          />
        </template>
      </div>
    </main>

    <SiteFooter />
  </div>
</template>
