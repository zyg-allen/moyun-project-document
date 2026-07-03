<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useHead } from '@vueuse/head';
import {
  ArrowLeft, MapPin, Briefcase, Building2,
  Loader2, GraduationCap, Clock,
} from 'lucide-vue-next';
import LazyImage from '@/components/LazyImage.vue';
import SiteFooter from '@/components/SiteFooter.vue';
import { generateSeo } from '@/utils/seo';
import { getSafeAvatar } from '@/utils/avatar';
import { formatRelativeTime } from '@/utils/date';
import { getJobDetail } from '@/api/job';
import type { JobVO } from '@/types/api';

const route = useRoute();
const router = useRouter();

const jobId = computed(() => route.params.id as string);

const loading = ref(false);
const error = ref<string | null>(null);
const job = ref<JobVO | null>(null);

useHead(computed(() => generateSeo({
  title: job.value?.title ? `${job.value.title} - 职位详情` : '职位详情',
  description: job.value?.description?.slice(0, 120) || '职位详情',
  canonicalPath: `/interview/jobs/${jobId.value}`,
})));

onMounted(() => loadDetail());

watch(jobId, (newId, oldId) => {
  if (newId && newId !== oldId) {
    job.value = null;
    loadDetail();
  }
});

async function loadDetail() {
  loading.value = true;
  error.value = null;
  try {
    const res = await getJobDetail(jobId.value);
    if (res.code === 200 && res.data) {
      job.value = res.data;
    } else {
      error.value = res.message || '职位不存在或已下架';
    }
  } catch (err) {
    const e = err as { message?: string };
    error.value = e?.message || '加载职位详情失败';
  } finally {
    loading.value = false;
  }
}

function salaryText(j: JobVO): string {
  const min = j.salaryMin;
  const max = j.salaryMax;
  if (min == null && max == null) return '薪资面议';
  if (min != null && max != null) return `${min}k-${max}k`;
  if (min != null) return `${min}k起`;
  return `${max}k以内`;
}

function goCompany() {
  if (job.value?.companyId) {
    router.push(`/interview/company/${job.value.companyId}`);
  }
}
</script>

<template>
  <div class="min-h-screen flex flex-col" style="background-color: var(--theme-bg);">
    <header class="sticky top-0 z-10 backdrop-blur" style="background-color: var(--theme-surface); border-bottom: 1px solid var(--theme-border);">
      <div class="max-w-4xl mx-auto px-4 py-3 flex items-center gap-3">
        <button @click="router.back()" class="p-1.5 rounded-lg transition hover:bg-black/5" style="color: var(--theme-text-secondary);">
          <ArrowLeft class="w-5 h-5" />
        </button>
        <h1 class="text-base font-semibold truncate" style="color: var(--theme-text);">职位详情</h1>
      </div>
    </header>

    <main class="flex-1 py-6">
      <div class="max-w-4xl mx-auto px-4">
        <div v-if="loading" class="text-center py-20">
          <Loader2 class="w-8 h-8 animate-spin mx-auto" style="color: var(--theme-primary);" />
          <p class="mt-3 text-sm" style="color: var(--theme-text-secondary);">加载中...</p>
        </div>

        <div v-else-if="error && !job" class="rounded-xl p-10 text-center" style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);">
          <Briefcase class="w-12 h-12 mx-auto mb-3 opacity-40" style="color: var(--theme-text-secondary);" />
          <p class="mb-4" style="color: var(--theme-text-secondary);">{{ error }}</p>
          <button @click="router.push('/interview/jobs')" class="px-4 py-2 text-white rounded-lg text-sm transition hover:opacity-90" style="background-color: var(--theme-primary);">
            查看职位列表
          </button>
        </div>

        <template v-else-if="job">
          <!-- 职位头部信息 -->
          <section class="rounded-2xl shadow-sm p-6 mb-5" style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);">
            <div class="flex items-start justify-between gap-4">
              <div class="flex-1 min-w-0">
                <h2 class="text-2xl font-bold" style="color: var(--theme-text);">{{ job.title }}</h2>
                <p class="mt-2 text-xl font-semibold" style="color: var(--theme-primary);">{{ salaryText(job) }}</p>
                <div class="flex flex-wrap items-center gap-x-4 gap-y-1.5 mt-3 text-sm" style="color: var(--theme-text-secondary);">
                  <span v-if="job.city" class="flex items-center gap-1"><MapPin class="w-4 h-4" />{{ job.city }}</span>
                  <span v-if="job.experience" class="flex items-center gap-1"><Briefcase class="w-4 h-4" />{{ job.experience }}</span>
                  <span v-if="job.education" class="flex items-center gap-1"><GraduationCap class="w-4 h-4" />{{ job.education }}</span>
                  <span v-if="job.createdTime" class="flex items-center gap-1"><Clock class="w-4 h-4" />{{ formatRelativeTime(job.createdTime) }}发布</span>
                </div>
              </div>
              <span v-if="job.status === 'open'" class="flex-shrink-0 px-4 py-2 rounded-lg text-sm text-white" style="background-color: var(--theme-primary);">招聘中</span>
              <span v-else class="flex-shrink-0 px-4 py-2 rounded-lg text-sm" style="background-color: var(--theme-accent); color: var(--theme-text-secondary);">已停止招聘</span>
            </div>

            <!-- 公司信息 -->
            <div v-if="job.companyName" class="flex items-center gap-3 mt-5 pt-5" style="border-top: 1px solid var(--theme-border);">
              <div class="w-10 h-10 rounded-lg overflow-hidden flex-shrink-0 flex items-center justify-center" style="background-color: var(--theme-bg);">
                <LazyImage v-if="job.companyLogo" :src="getSafeAvatar(job.companyLogo)" :alt="job.companyName" class="w-full h-full object-cover" />
                <Building2 v-else class="w-5 h-5" style="color: var(--theme-text-secondary);" />
              </div>
              <div class="flex-1 min-w-0">
                <button @click="goCompany" class="font-medium hover:underline truncate" style="color: var(--theme-text);">
                  {{ job.companyName }}
                </button>
                <p v-if="job.companyIndustry" class="text-xs" style="color: var(--theme-text-secondary);">{{ job.companyIndustry }}</p>
              </div>
              <button @click="goCompany" class="text-xs px-3 py-1 rounded-full transition" style="background-color: var(--theme-bg); color: var(--theme-text-secondary); border: 1px solid var(--theme-border);">
                查看主页
              </button>
            </div>
          </section>

          <!-- 职位描述 -->
          <section v-if="job.description" class="rounded-2xl shadow-sm p-6 mb-5" style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);">
            <h3 class="font-semibold mb-3" style="color: var(--theme-text);">职位描述</h3>
            <p class="text-sm leading-relaxed whitespace-pre-wrap" style="color: var(--theme-text-secondary);">{{ job.description }}</p>
          </section>

          <!-- 任职要求 -->
          <section v-if="job.requirement" class="rounded-2xl shadow-sm p-6 mb-5" style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);">
            <h3 class="font-semibold mb-3" style="color: var(--theme-text);">任职要求</h3>
            <p class="text-sm leading-relaxed whitespace-pre-wrap" style="color: var(--theme-text-secondary);">{{ job.requirement }}</p>
          </section>
        </template>
      </div>
    </main>

    <SiteFooter />
  </div>
</template>
