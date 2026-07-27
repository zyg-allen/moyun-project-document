<script setup lang="ts">
import { ref, reactive, computed, onMounted, watch, nextTick } from 'vue';
import { useRoute, useRouter, onBeforeRouteLeave } from 'vue-router';
import { useHead } from '@vueuse/head';
import {
  Save, Download, Star, Plus, Trash2, User, Briefcase, GraduationCap,
  Code, FileText, Target, Sparkles, CheckCircle2, XCircle, AlertCircle,
} from 'lucide-vue-next';
import SiteFooter from '@/components/SiteFooter.vue';
import Breadcrumb from '@/components/Breadcrumb.vue';
import { generateSeo } from '@/utils/seo';
import {
  getResumeDetail, saveResume, exportResumePdf, scoreResume, getResumeAiAdvice,
} from '@/api/interview';
import { getToken } from '@/api/client';
import type {
  UserResumeVO, UserResumeJobIntention, UserResumeEducationItem, UserResumeWorkItem,
  UserResumeProjectItem, UserResumeSkillItem, UserResumeScoreItem,
  ResumeAiAdviceVO,
} from '@/types/api';
import { useToast } from '@/composables/useToast';

const route = useRoute();
const router = useRouter();
const toast = useToast();

const editId = computed(() => route.params.id as string | undefined);
const isEdit = computed(() => !!editId.value);

const pageTitle = computed(() => isEdit.value ? '编辑简历' : '创建简历');

// 加载 / 状态
const loadingDetail = ref(false);
const pageError = ref<string | null>(null);
const exporting = ref(false);
const scoring = ref(false);
// v5.9 阶段2：AI 改进建议
const adviceLoading = ref(false);
const aiAdvice = ref<ResumeAiAdviceVO | null>(null);
// 自动保存指示：idle / saving / saved
const saveStatus = ref<'idle' | 'saving' | 'saved'>('idle');
// 保存互斥锁：防止并发保存产生重复创建/后写覆盖
const saving = ref(false);

// 加载完成标记：用于离开页时判断是否有未保存内容
const loaded = ref(false);

// 表单
const form = reactive<UserResumeVO>({
  id: undefined,
  title: '',
  name: '',
  gender: '',
  birthDate: '',
  phone: '',
  email: '',
  avatar: '',
  jobIntention: {
    position: '', city: '', salaryMin: undefined, salaryMax: undefined,
    jobType: '', availableTime: '',
  },
  educations: [],
  works: [],
  projects: [],
  skills: [],
  selfIntro: '',
  score: undefined,
  scoreDetail: [],
  scoredTime: '',
  fileUrl: '',
  exportTime: '',
  status: 'draft',
  versionNo: undefined,
});

useHead(computed(() => generateSeo({
  title: pageTitle.value,
  description: '创建与编辑结构化简历，支持教育、工作、项目经历及技能、AI 评分与 PDF 导出',
  keywords: ['简历编辑', '创建简历', '求职简历', '简历评分', '墨韵'],
  canonicalPath: isEdit.value
    ? `/interview/resume/edit/${editId.value}`
    : '/interview/resume/edit',
  robots: 'noindex,nofollow',
})));

// 动态数组增删辅助
function addEducation() {
  form.educations!.push({
    school: '', major: '', degree: '', startDate: '', endDate: '', description: '',
  } as UserResumeEducationItem);
}
function removeEducation(idx: number) {
  form.educations!.splice(idx, 1);
}
function addWork() {
  form.works!.push({
    company: '', position: '', startDate: '', endDate: '', description: '',
  } as UserResumeWorkItem);
}
function removeWork(idx: number) {
  form.works!.splice(idx, 1);
}
function addProject() {
  form.projects!.push({
    name: '', role: '', startDate: '', endDate: '', description: '', url: '',
  } as UserResumeProjectItem);
}
function removeProject(idx: number) {
  form.projects!.splice(idx, 1);
}
function addSkill() {
  form.skills!.push({ name: '', level: '', category: '' } as UserResumeSkillItem);
}
function removeSkill(idx: number) {
  form.skills!.splice(idx, 1);
}

// 求职意向默认值，避免 null
function ensureJobIntention(): UserResumeJobIntention {
  if (!form.jobIntention) {
    form.jobIntention = {
      position: '', city: '', salaryMin: undefined, salaryMax: undefined,
      jobType: '', availableTime: '',
    };
  }
  return form.jobIntention;
}

// 面包屑
const breadcrumbs = computed(() => [
  { label: '面试指南', path: '/interview' },
  { label: '我的简历', path: '/interview/my/resumes' },
  { label: isEdit.value ? '编辑' : '创建' },
]);

// 进度条百分比
function scorePercent(item: UserResumeScoreItem): number {
  if (!item.maxScore || item.maxScore <= 0) return 0;
  return Math.max(0, Math.min(100, (item.score / item.maxScore) * 100));
}

// 保存：silent 时不显示成功 toast，错误始终提示
async function doSave(silent = false): Promise<boolean> {
  // 互斥锁：已有保存在途时跳过（手动保存提示稍候）
  if (saving.value) {
    if (!silent) toast.error('正在保存中，请稍候');
    return false;
  }
  if (!form.title?.trim()) {
    if (!silent) toast.error('请填写简历标题');
    return false;
  }
  try {
    saving.value = true;
    saveStatus.value = 'saving';
    const res = await saveResume({ ...form });
    if (res.code === 200) {
      if (form.id === undefined || form.id === null || form.id === '') {
        form.id = res.data as string | number;
        // 新建后切换到编辑模式 URL，避免刷新重复创建
        if (!isEdit.value) {
          router.replace(`/interview/resume/edit/${form.id}`);
        }
      }
      saveStatus.value = 'saved';
      if (!silent) toast.success('保存成功');
      // 等待 form.id 变化触发的 watch 执行完（被 saving 标志跳过），
      // 再返回，避免 watch 在 nextTick 把 saveStatus 覆盖回 idle
      await nextTick();
      return true;
    } else {
      saveStatus.value = 'idle';
      toast.error(res.message || '保存失败');
      return false;
    }
  } catch (err: any) {
    saveStatus.value = 'idle';
    toast.error(err?.message || '保存失败，请稍后重试');
    return false;
  } finally {
    saving.value = false;
  }
}

// 手动保存草稿
async function handleSaveDraft() {
  await doSave(false);
}

// 认证下载 PDF（fetch blob + a 标签，避免 window.open 无法携带 token）
async function downloadPdfAuth(url: string) {
  const token = getToken();
  const baseUrl = import.meta.env.VITE_API_BASE_URL || '/api';
  const resp = await fetch(baseUrl + url, {
    headers: token ? { Authorization: `Bearer ${token}` } : {},
  });
  if (!resp.ok) {
    const txt = await resp.text().catch(() => '');
    throw new Error(txt || `下载失败 (${resp.status})`);
  }
  const blob = await resp.blob();
  const a = document.createElement('a');
  a.href = URL.createObjectURL(blob);
  a.download = `resume_${form.id ?? 'export'}.pdf`;
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
  URL.revokeObjectURL(a.href);
}

// 导出 PDF：先保存再导出
async function handleExportPdf() {
  if (exporting.value) return;
  const ok = await doSave(true);
  if (!ok || !form.id) return;
  try {
    exporting.value = true;
    const res = await exportResumePdf(form.id);
    if (res.code === 200 && res.data?.fileUrl) {
      // fileUrl 为认证下载端点，需带 token 下载
      await downloadPdfAuth(res.data.fileUrl);
      toast.success('PDF 导出成功');
      form.fileUrl = res.data.fileUrl;
      form.exportTime = res.data.exportTime || '';
    } else {
      toast.error(res.message || '导出失败，请稍后重试');
    }
  } catch (err: any) {
    toast.error(err?.message || '导出失败，请稍后重试');
  } finally {
    exporting.value = false;
  }
}

// 评分：先保存再评分
async function handleScore() {
  if (scoring.value) return;
  const ok = await doSave(true);
  if (!ok || !form.id) return;
  try {
    scoring.value = true;
    const res = await scoreResume(form.id);
    if (res.code === 200 && res.data) {
      form.score = res.data.score;
      form.scoreDetail = res.data.scoreDetail || [];
      form.scoredTime = res.data.scoredTime || '';
      // 评分变化后清空旧建议，避免展示过期内容
      aiAdvice.value = null;
      toast.success(`评分完成：${form.score} 分`);
      // 滚动到评分面板
      nextTick(() => {
        const el = document.getElementById('resume-score-panel');
        if (el) el.scrollIntoView({ behavior: 'smooth', block: 'start' });
      });
    } else {
      toast.error(res.message || '评分失败，请稍后重试');
    }
  } catch (err: any) {
    toast.error(err?.message || '评分失败，请稍后重试');
  } finally {
    scoring.value = false;
  }
}

// v5.9 阶段2：获取 AI 改进建议（基于当前评分明细 + 岗位匹配度）
async function handleGetAdvice() {
  if (adviceLoading.value) return;
  if (!form.id) {
    toast.error('请先保存简历再获取建议');
    return;
  }
  // 若未评分或内容已变更，先评分（后端也会兜底实时评分，但前端先调用保证一致性）
  if (!form.scoreDetail || form.scoreDetail.length === 0) {
    const ok = await doSave(true);
    if (!ok || !form.id) return;
  }
  try {
    adviceLoading.value = true;
    const res = await getResumeAiAdvice(form.id);
    if (res.code === 200 && res.data) {
      aiAdvice.value = res.data;
      nextTick(() => {
        const el = document.getElementById('resume-advice-panel');
        if (el) el.scrollIntoView({ behavior: 'smooth', block: 'start' });
      });
    } else {
      toast.error(res.message || '生成建议失败，请稍后重试');
    }
  } catch (err: any) {
    toast.error(err?.message || '生成建议失败，请稍后重试');
  } finally {
    adviceLoading.value = false;
  }
}

// 优先级样式映射
const priorityStyle: Record<string, { label: string; class: string }> = {
  high: { label: '高优先级', class: 'bg-red-50 text-red-600 border border-red-200' },
  medium: { label: '中优先级', class: 'bg-amber-50 text-amber-600 border border-amber-200' },
  low: { label: '低优先级', class: 'bg-gray-50 text-gray-600 border border-gray-200' },
};

// 建议类型样式映射
const adviceTypeLabel: Record<string, string> = {
  fill: '补充缺失',
  refine: '优化已有',
  match: '岗位匹配',
};

// 评分等级样式映射
const gradeStyle: Record<string, string> = {
  A: 'bg-green-50 text-green-600 border border-green-200',
  B: 'bg-blue-50 text-blue-600 border border-blue-200',
  C: 'bg-amber-50 text-amber-600 border border-amber-200',
  D: 'bg-red-50 text-red-600 border border-red-200',
};

// 注：已移除表单自动保存。为避免用户中途放弃时产生难以清理的脏数据，
// 简历仅在用户手动点击「保存草稿」「导出PDF」「评分」时才入库。
// 但仍需跟踪"是否有未保存修改"，用于离开页提示：用户编辑后 saveStatus 重置为 idle，
// 保存成功后恢复 saved。加载阶段（loaded=false）与保存流程（saving=true）跳过，
// 避免回填数据 / 保存后 form.id 回填触发误判。
watch(
  form,
  () => {
    if (!loaded.value) return;
    if (saving.value) return;
    saveStatus.value = 'idle';
  },
  { deep: true },
);

// 加载详情；返回是否成功
async function loadDetail(): Promise<boolean> {
  if (!editId.value) return false;
  try {
    loadingDetail.value = true;
    pageError.value = null;
    const res = await getResumeDetail(editId.value);
    if (res.code === 200 && res.data) {
      const d = res.data;
      form.id = d.id;
      form.title = d.title || '';
      form.name = d.name || '';
      form.gender = d.gender || '';
      form.birthDate = d.birthDate || '';
      form.phone = d.phone || '';
      form.email = d.email || '';
      form.avatar = d.avatar || '';
      form.jobIntention = d.jobIntention || {
        position: '', city: '', salaryMin: undefined, salaryMax: undefined,
        jobType: '', availableTime: '',
      };
      form.educations = d.educations || [];
      form.works = d.works || [];
      form.projects = d.projects || [];
      form.skills = d.skills || [];
      form.selfIntro = d.selfIntro || '';
      form.score = d.score;
      form.scoreDetail = d.scoreDetail || [];
      form.scoredTime = d.scoredTime || '';
      form.fileUrl = d.fileUrl || '';
      form.exportTime = d.exportTime || '';
      form.status = d.status || 'draft';
      form.versionNo = d.versionNo;
      // 等待本轮 form 变化触发的 watch 执行完（此时 loaded=false 被跳过），
      // 再标记为已同步，避免回填数据被误判为"有未保存修改"
      await nextTick();
      saveStatus.value = 'saved';
      return true;
    } else {
      pageError.value = res.message || '加载简历失败';
      return false;
    }
  } catch (err: any) {
    pageError.value = err?.message || '加载简历失败，请稍后重试';
    return false;
  } finally {
    loadingDetail.value = false;
  }
}

onMounted(() => {
  if (isEdit.value && editId.value) {
    loadDetail().then((ok) => {
      // 加载成功后标记 loaded，用于离开页时判断未保存内容
      if (ok) nextTick(() => { loaded.value = true; });
    });
  } else {
    nextTick(() => { loaded.value = true; });
  }
});

// 路由 :id 变更时（同组件复用）重新加载详情，避免数据错位
watch(() => route.params.id, (newId, oldId) => {
  if (newId === oldId) return;
  // 仅在切换到另一份已有简历时重新加载；从编辑切回创建（无 id）则重置为空白草稿
  if (!newId) {
    loaded.value = false;
    form.id = undefined;
    form.title = '';
    nextTick(() => { loaded.value = true; });
    return;
  }
  loaded.value = false;
  form.id = undefined;
  loadDetail().then((ok) => {
    if (ok) nextTick(() => { loaded.value = true; });
  });
});

// 离开页面前提示：表单有标题且有内容、且最近未成功保存时
onBeforeRouteLeave((to, from, next) => {
  const hasContent = !!form.title?.trim();
  const unsaved = saveStatus.value !== 'saved' && hasContent && loaded.value;
  if (unsaved && !window.confirm('有未保存的内容，确定离开吗？')) {
    next(false);
  } else {
    next();
  }
});
</script>

<template>
  <div class="min-h-screen flex flex-col" style="background-color: var(--theme-bg);">
    <!-- 吸顶面包屑栏 -->
    <div
      class="border-b sticky top-0 z-30 backdrop-blur-sm py-3"
      style="background-color: var(--theme-surface); border-color: var(--theme-border);"
    >
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 flex items-center justify-between gap-4">
        <Breadcrumb :items="breadcrumbs" />
        <div class="flex items-center gap-2 shrink-0">
          <!-- 保存状态指示（手动保存时显示） -->
          <span
            v-if="saveStatus !== 'idle'"
            class="text-xs hidden sm:inline-flex items-center"
            style="color: var(--theme-text-secondary);"
          >
            <span v-if="saveStatus === 'saving'">保存中...</span>
            <span v-else-if="saveStatus === 'saved'" class="flex items-center">
              <span
                class="w-1.5 h-1.5 rounded-full mr-1"
                style="background-color: var(--theme-primary);"
              ></span>
              已保存
            </span>
          </span>
          <button
            @click="handleSaveDraft"
            class="inline-flex items-center px-3 py-1.5 rounded-lg text-sm transition hover:opacity-90"
            style="background-color: var(--theme-bg); color: var(--theme-text); border: 1px solid var(--theme-border);"
          >
            <Save class="w-4 h-4 mr-1" />
            保存草稿
          </button>
          <button
            @click="handleExportPdf"
            :disabled="exporting"
            class="inline-flex items-center px-3 py-1.5 rounded-lg text-sm transition hover:opacity-90 disabled:opacity-50 disabled:cursor-not-allowed"
            style="background-color: var(--theme-bg); color: var(--theme-text); border: 1px solid var(--theme-border);"
          >
            <Download class="w-4 h-4 mr-1" />
            {{ exporting ? '导出中...' : '导出PDF' }}
          </button>
          <button
            @click="handleScore"
            :disabled="scoring"
            class="inline-flex items-center px-3 py-1.5 rounded-lg text-sm text-white transition hover:opacity-90 disabled:opacity-50 disabled:cursor-not-allowed"
            style="background-color: var(--theme-primary);"
          >
            <Star class="w-4 h-4 mr-1" />
            {{ scoring ? '评分中...' : '评分' }}
          </button>
          <button
            @click="handleGetAdvice"
            :disabled="adviceLoading"
            class="inline-flex items-center px-3 py-1.5 rounded-lg text-sm text-white transition hover:opacity-90 disabled:opacity-50 disabled:cursor-not-allowed"
            style="background: linear-gradient(135deg, var(--theme-primary), color-mix(in srgb, var(--theme-primary) 70%, #7c3aed));"
          >
            <Sparkles class="w-4 h-4 mr-1" />
            {{ adviceLoading ? '生成中...' : 'AI 建议' }}
          </button>
        </div>
      </div>
    </div>

    <!-- 内容区 -->
    <div class="flex-1 py-8">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <!-- 加载详情中 -->
        <div
          v-if="loadingDetail"
          class="rounded-xl border p-12 text-center"
          style="background-color: var(--theme-surface); border-color: var(--theme-border);"
        >
          <div
            class="animate-spin rounded-full h-10 w-10 border-2 mx-auto"
            style="border-color: var(--theme-border); border-top-color: var(--theme-primary);"
          ></div>
          <p class="mt-4 text-sm" style="color: var(--theme-text-secondary);">加载中...</p>
        </div>

        <!-- 加载失败 -->
        <div
          v-else-if="pageError"
          class="rounded-xl border p-8 text-center"
          style="background-color: var(--theme-surface); border-color: var(--theme-border);"
        >
          <p class="mb-4 text-sm" style="color: var(--theme-text);">{{ pageError }}</p>
          <button
            @click="loadDetail"
            class="px-4 py-2 text-white rounded-lg text-sm transition hover:opacity-90"
            style="background-color: var(--theme-primary);"
          >
            重试
          </button>
        </div>

        <template v-else>
          <!-- 评分面板 -->
          <div
            v-if="form.scoreDetail && form.scoreDetail.length > 0"
            id="resume-score-panel"
            class="rounded-xl border p-6 mb-6"
            style="background-color: var(--theme-surface); border-color: var(--theme-border);"
          >
            <div class="flex items-center justify-between mb-4">
              <h3 class="text-base font-semibold flex items-center" style="color: var(--theme-text);">
                <Star class="w-4 h-4 mr-2" style="color: var(--theme-primary);" />
                AI 评分详情
              </h3>
              <div class="flex items-baseline gap-1">
                <span class="text-3xl font-bold" style="color: var(--theme-primary);">{{ form.score ?? 0 }}</span>
                <span class="text-sm" style="color: var(--theme-text-secondary);">分</span>
              </div>
            </div>
            <div class="space-y-3">
              <div v-for="(item, idx) in form.scoreDetail" :key="idx">
                <div class="flex items-center justify-between text-sm mb-1">
                  <span style="color: var(--theme-text);">{{ item.item }}</span>
                  <span style="color: var(--theme-text-secondary);">
                    {{ item.score }} / {{ item.maxScore }}
                  </span>
                </div>
                <div class="w-full h-2 rounded-full" style="background-color: var(--theme-bg);">
                  <div
                    class="h-2 rounded-full transition-all"
                    :style="{ width: scorePercent(item) + '%', backgroundColor: 'var(--theme-primary)' }"
                  ></div>
                </div>
                <p v-if="item.message" class="text-xs mt-1" style="color: var(--theme-text-secondary);">
                  {{ item.message }}
                </p>
                <!-- v5.9 阶段2：岗位匹配度子项明细 -->
                <div
                  v-if="item.subItems && item.subItems.length > 0"
                  class="mt-2 flex flex-wrap gap-1.5"
                >
                  <span
                    v-for="sub in item.subItems"
                    :key="sub.name"
                    class="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-xs"
                    :style="sub.hit
                      ? { backgroundColor: 'rgba(22,163,74,0.1)', color: '#16a34a', border: '1px solid rgba(22,163,74,0.2)' }
                      : { backgroundColor: 'rgba(239,68,68,0.08)', color: '#ef4444', border: '1px solid rgba(239,68,68,0.2)' }"
                    :title="sub.message"
                  >
                    <CheckCircle2 v-if="sub.hit" class="w-3 h-3" />
                    <XCircle v-else class="w-3 h-3" />
                    {{ sub.name }}
                  </span>
                </div>
              </div>
            </div>
          </div>

          <!-- v5.9 阶段2：AI 改进建议面板 -->
          <div
            v-if="aiAdvice"
            id="resume-advice-panel"
            class="rounded-xl border p-6 mb-6"
            style="background: linear-gradient(135deg, var(--theme-surface), color-mix(in srgb, var(--theme-primary) 6%, var(--theme-surface))); border-color: var(--theme-border);"
          >
            <!-- 标题行 -->
            <div class="flex items-center justify-between mb-4">
              <h3 class="text-base font-semibold flex items-center" style="color: var(--theme-text);">
                <Sparkles class="w-4 h-4 mr-2" style="color: var(--theme-primary);" />
                AI 改进建议
              </h3>
              <div class="flex items-center gap-2">
                <span
                  v-if="aiAdvice.grade"
                  class="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium"
                  :class="gradeStyle[aiAdvice.grade] || gradeStyle.D"
                >
                  等级 {{ aiAdvice.grade }}
                </span>
                <span
                  v-if="aiAdvice.aiPowered === false"
                  class="text-xs px-2 py-0.5 rounded-full"
                  style="background-color: var(--theme-bg); color: var(--theme-text-secondary);"
                >规则化生成</span>
              </div>
            </div>

            <!-- 整体总结 -->
            <div
              v-if="aiAdvice.summary"
              class="rounded-lg p-3 mb-4 text-sm leading-relaxed"
              style="background-color: var(--theme-bg); color: var(--theme-text);"
            >
              {{ aiAdvice.summary }}
            </div>

            <!-- 缺失技能提示 -->
            <div
              v-if="aiAdvice.missingSkills && aiAdvice.missingSkills.length > 0"
              class="mb-4 rounded-lg p-3"
              style="background-color: rgba(239,68,68,0.06); border: 1px solid rgba(239,68,68,0.15);"
            >
              <div class="text-xs font-medium mb-2 flex items-center" style="color: #ef4444;">
                <AlertCircle class="w-3.5 h-3.5 mr-1" />
                岗位必备技能缺失（{{ aiAdvice.missingSkills.length }} 项）
              </div>
              <div class="flex flex-wrap gap-1.5">
                <span
                  v-for="skill in aiAdvice.missingSkills"
                  :key="skill"
                  class="px-2 py-0.5 rounded-full text-xs"
                  style="background-color: rgba(239,68,68,0.08); color: #ef4444; border: 1px solid rgba(239,68,68,0.2);"
                >{{ skill }}</span>
              </div>
            </div>

            <!-- 建议列表 -->
            <div v-if="aiAdvice.advices && aiAdvice.advices.length > 0" class="space-y-3">
              <div
                v-for="(advice, idx) in aiAdvice.advices"
                :key="idx"
                class="rounded-lg p-3"
                style="background-color: var(--theme-bg);"
              >
                <div class="flex items-center gap-2 mb-1.5 flex-wrap">
                  <span class="text-xs font-medium" style="color: var(--theme-text);">
                    {{ advice.dimension || '综合' }}
                  </span>
                  <span
                    v-if="advice.priority && priorityStyle[advice.priority]"
                    class="text-xs px-1.5 py-0.5 rounded"
                    :class="priorityStyle[advice.priority].class"
                  >
                    {{ priorityStyle[advice.priority].label }}
                  </span>
                  <span
                    v-if="advice.type && adviceTypeLabel[advice.type]"
                    class="text-xs px-1.5 py-0.5 rounded"
                    style="background-color: var(--theme-surface); color: var(--theme-text-secondary); border: 1px solid var(--theme-border);"
                  >
                    {{ adviceTypeLabel[advice.type] }}
                  </span>
                </div>
                <p class="text-sm leading-relaxed" style="color: var(--theme-text-secondary);">
                  {{ advice.content }}
                </p>
              </div>
            </div>
            <p v-else class="text-sm text-center py-4" style="color: var(--theme-text-secondary);">
              <CheckCircle2 class="w-5 h-5 inline mr-1" style="color: #16a34a;" />
              各维度得分率良好，暂无改进建议
            </p>
          </div>

          <!-- 1. 简历标题 -->
          <div
            class="rounded-xl border p-6 mb-5"
            style="background-color: var(--theme-surface); border-color: var(--theme-border);"
          >
            <h3 class="text-base font-semibold mb-4 flex items-center" style="color: var(--theme-text);">
              <FileText class="w-4 h-4 mr-2" style="color: var(--theme-primary);" />
              简历标题
            </h3>
            <input
              v-model="form.title"
              type="text"
              placeholder="例如：张三 - 前端开发工程师简历"
              maxlength="100"
              class="w-full px-3 py-2 rounded-lg text-sm focus:outline-none"
              style="background-color: var(--theme-bg); border: 1px solid var(--theme-border); color: var(--theme-text);"
            />
          </div>

          <!-- 2. 基本信息 -->
          <div
            class="rounded-xl border p-6 mb-5"
            style="background-color: var(--theme-surface); border-color: var(--theme-border);"
          >
            <h3 class="text-base font-semibold mb-4 flex items-center" style="color: var(--theme-text);">
              <User class="w-4 h-4 mr-2" style="color: var(--theme-primary);" />
              基本信息
            </h3>
            <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                <label class="block text-sm font-medium mb-1.5" style="color: var(--theme-text);">姓名</label>
                <input
                  v-model="form.name"
                  type="text"
                  placeholder="请输入姓名"
                  class="w-full px-3 py-2 rounded-lg text-sm focus:outline-none"
                  style="background-color: var(--theme-bg); border: 1px solid var(--theme-border); color: var(--theme-text);"
                />
              </div>
              <div>
                <label class="block text-sm font-medium mb-1.5" style="color: var(--theme-text);">性别</label>
                <select
                  v-model="form.gender"
                  class="w-full px-3 py-2 rounded-lg text-sm focus:outline-none"
                  style="background-color: var(--theme-bg); border: 1px solid var(--theme-border); color: var(--theme-text);"
                >
                  <option value="">请选择</option>
                  <option value="男">男</option>
                  <option value="女">女</option>
                  <option value="保密">保密</option>
                </select>
              </div>
              <div>
                <label class="block text-sm font-medium mb-1.5" style="color: var(--theme-text);">出生日期</label>
                <input
                  v-model="form.birthDate"
                  type="date"
                  class="w-full px-3 py-2 rounded-lg text-sm focus:outline-none"
                  style="background-color: var(--theme-bg); border: 1px solid var(--theme-border); color: var(--theme-text);"
                />
              </div>
              <div>
                <label class="block text-sm font-medium mb-1.5" style="color: var(--theme-text);">手机</label>
                <input
                  v-model="form.phone"
                  type="text"
                  placeholder="请输入手机号"
                  class="w-full px-3 py-2 rounded-lg text-sm focus:outline-none"
                  style="background-color: var(--theme-bg); border: 1px solid var(--theme-border); color: var(--theme-text);"
                />
              </div>
              <div class="md:col-span-2">
                <label class="block text-sm font-medium mb-1.5" style="color: var(--theme-text);">邮箱</label>
                <input
                  v-model="form.email"
                  type="email"
                  placeholder="请输入邮箱"
                  class="w-full px-3 py-2 rounded-lg text-sm focus:outline-none"
                  style="background-color: var(--theme-bg); border: 1px solid var(--theme-border); color: var(--theme-text);"
                />
              </div>
            </div>
          </div>

          <!-- 3. 求职意向 -->
          <div
            class="rounded-xl border p-6 mb-5"
            style="background-color: var(--theme-surface); border-color: var(--theme-border);"
          >
            <h3 class="text-base font-semibold mb-4 flex items-center" style="color: var(--theme-text);">
              <Target class="w-4 h-4 mr-2" style="color: var(--theme-primary);" />
              求职意向
            </h3>
            <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                <label class="block text-sm font-medium mb-1.5" style="color: var(--theme-text);">期望职位</label>
                <input
                  v-model="ensureJobIntention().position"
                  type="text"
                  placeholder="例如：前端开发工程师"
                  class="w-full px-3 py-2 rounded-lg text-sm focus:outline-none"
                  style="background-color: var(--theme-bg); border: 1px solid var(--theme-border); color: var(--theme-text);"
                />
              </div>
              <div>
                <label class="block text-sm font-medium mb-1.5" style="color: var(--theme-text);">期望城市</label>
                <input
                  v-model="ensureJobIntention().city"
                  type="text"
                  placeholder="例如：北京"
                  class="w-full px-3 py-2 rounded-lg text-sm focus:outline-none"
                  style="background-color: var(--theme-bg); border: 1px solid var(--theme-border); color: var(--theme-text);"
                />
              </div>
              <div>
                <label class="block text-sm font-medium mb-1.5" style="color: var(--theme-text);">最低薪资（K）</label>
                <input
                  v-model.number="ensureJobIntention().salaryMin"
                  type="number"
                  min="0"
                  placeholder="例如：15"
                  class="w-full px-3 py-2 rounded-lg text-sm focus:outline-none"
                  style="background-color: var(--theme-bg); border: 1px solid var(--theme-border); color: var(--theme-text);"
                />
              </div>
              <div>
                <label class="block text-sm font-medium mb-1.5" style="color: var(--theme-text);">最高薪资（K）</label>
                <input
                  v-model.number="ensureJobIntention().salaryMax"
                  type="number"
                  min="0"
                  placeholder="例如：25"
                  class="w-full px-3 py-2 rounded-lg text-sm focus:outline-none"
                  style="background-color: var(--theme-bg); border: 1px solid var(--theme-border); color: var(--theme-text);"
                />
              </div>
              <div>
                <label class="block text-sm font-medium mb-1.5" style="color: var(--theme-text);">工作性质</label>
                <select
                  v-model="ensureJobIntention().jobType"
                  class="w-full px-3 py-2 rounded-lg text-sm focus:outline-none"
                  style="background-color: var(--theme-bg); border: 1px solid var(--theme-border); color: var(--theme-text);"
                >
                  <option value="">请选择</option>
                  <option value="全职">全职</option>
                  <option value="兼职">兼职</option>
                  <option value="实习">实习</option>
                </select>
              </div>
              <div>
                <label class="block text-sm font-medium mb-1.5" style="color: var(--theme-text);">到岗时间</label>
                <input
                  v-model="ensureJobIntention().availableTime"
                  type="text"
                  placeholder="例如：随时 / 1个月内"
                  class="w-full px-3 py-2 rounded-lg text-sm focus:outline-none"
                  style="background-color: var(--theme-bg); border: 1px solid var(--theme-border); color: var(--theme-text);"
                />
              </div>
            </div>
          </div>

          <!-- 4. 教育经历 -->
          <div
            class="rounded-xl border p-6 mb-5"
            style="background-color: var(--theme-surface); border-color: var(--theme-border);"
          >
            <div class="flex items-center justify-between mb-4">
              <h3 class="text-base font-semibold flex items-center" style="color: var(--theme-text);">
                <GraduationCap class="w-4 h-4 mr-2" style="color: var(--theme-primary);" />
                教育经历
              </h3>
              <button
                @click="addEducation"
                class="inline-flex items-center px-3 py-1.5 rounded-lg text-xs text-white transition hover:opacity-90"
                style="background-color: var(--theme-primary);"
              >
                <Plus class="w-3.5 h-3.5 mr-1" />
                添加
              </button>
            </div>
            <div v-if="form.educations!.length === 0" class="text-sm py-3 text-center" style="color: var(--theme-text-secondary);">
              暂无教育经历，点击右上角添加
            </div>
            <div v-else class="space-y-4">
              <div
                v-for="(edu, idx) in form.educations"
                :key="'edu-' + idx"
                class="rounded-lg p-4 relative"
                style="background-color: var(--theme-bg); border: 1px solid var(--theme-border);"
              >
                <div class="grid grid-cols-1 md:grid-cols-2 gap-3">
                  <div>
                    <label class="block text-sm font-medium mb-1.5" style="color: var(--theme-text);">学校</label>
                    <input v-model="edu.school" type="text" placeholder="学校名称" class="w-full px-3 py-2 rounded-lg text-sm focus:outline-none" style="background-color: var(--theme-surface); border: 1px solid var(--theme-border); color: var(--theme-text);" />
                  </div>
                  <div>
                    <label class="block text-sm font-medium mb-1.5" style="color: var(--theme-text);">专业</label>
                    <input v-model="edu.major" type="text" placeholder="专业" class="w-full px-3 py-2 rounded-lg text-sm focus:outline-none" style="background-color: var(--theme-surface); border: 1px solid var(--theme-border); color: var(--theme-text);" />
                  </div>
                  <div>
                    <label class="block text-sm font-medium mb-1.5" style="color: var(--theme-text);">学历</label>
                    <select v-model="edu.degree" class="w-full px-3 py-2 rounded-lg text-sm focus:outline-none" style="background-color: var(--theme-surface); border: 1px solid var(--theme-border); color: var(--theme-text);">
                      <option value="">请选择</option>
                      <option value="大专">大专</option>
                      <option value="本科">本科</option>
                      <option value="硕士">硕士</option>
                      <option value="博士">博士</option>
                    </select>
                  </div>
                  <div class="grid grid-cols-2 gap-3">
                    <div>
                      <label class="block text-sm font-medium mb-1.5" style="color: var(--theme-text);">开始</label>
                      <input v-model="edu.startDate" type="month" class="w-full px-3 py-2 rounded-lg text-sm focus:outline-none" style="background-color: var(--theme-surface); border: 1px solid var(--theme-border); color: var(--theme-text);" />
                    </div>
                    <div>
                      <label class="block text-sm font-medium mb-1.5" style="color: var(--theme-text);">结束</label>
                      <input v-model="edu.endDate" type="month" class="w-full px-3 py-2 rounded-lg text-sm focus:outline-none" style="background-color: var(--theme-surface); border: 1px solid var(--theme-border); color: var(--theme-text);" />
                    </div>
                  </div>
                  <div class="md:col-span-2">
                    <label class="block text-sm font-medium mb-1.5" style="color: var(--theme-text);">经历描述</label>
                    <textarea v-model="edu.description" rows="2" placeholder="主修课程、荣誉、绩点等" class="w-full px-3 py-2 rounded-lg text-sm focus:outline-none resize-y" style="background-color: var(--theme-surface); border: 1px solid var(--theme-border); color: var(--theme-text);"></textarea>
                  </div>
                </div>
                <button
                  @click="removeEducation(idx)"
                  class="absolute -top-2 -right-2 w-6 h-6 rounded-full flex items-center justify-center text-white shadow"
                  style="background-color: #ef4444;"
                  aria-label="删除该教育经历"
                >
                  <Trash2 class="w-3 h-3" />
                </button>
              </div>
            </div>
          </div>

          <!-- 5. 工作经历 -->
          <div
            class="rounded-xl border p-6 mb-5"
            style="background-color: var(--theme-surface); border-color: var(--theme-border);"
          >
            <div class="flex items-center justify-between mb-4">
              <h3 class="text-base font-semibold flex items-center" style="color: var(--theme-text);">
                <Briefcase class="w-4 h-4 mr-2" style="color: var(--theme-primary);" />
                工作经历
              </h3>
              <button
                @click="addWork"
                class="inline-flex items-center px-3 py-1.5 rounded-lg text-xs text-white transition hover:opacity-90"
                style="background-color: var(--theme-primary);"
              >
                <Plus class="w-3.5 h-3.5 mr-1" />
                添加
              </button>
            </div>
            <div v-if="form.works!.length === 0" class="text-sm py-3 text-center" style="color: var(--theme-text-secondary);">
              暂无工作经历，点击右上角添加
            </div>
            <div v-else class="space-y-4">
              <div
                v-for="(w, idx) in form.works"
                :key="'work-' + idx"
                class="rounded-lg p-4 relative"
                style="background-color: var(--theme-bg); border: 1px solid var(--theme-border);"
              >
                <div class="grid grid-cols-1 md:grid-cols-2 gap-3">
                  <div>
                    <label class="block text-sm font-medium mb-1.5" style="color: var(--theme-text);">公司</label>
                    <input v-model="w.company" type="text" placeholder="公司名称" class="w-full px-3 py-2 rounded-lg text-sm focus:outline-none" style="background-color: var(--theme-surface); border: 1px solid var(--theme-border); color: var(--theme-text);" />
                  </div>
                  <div>
                    <label class="block text-sm font-medium mb-1.5" style="color: var(--theme-text);">职位</label>
                    <input v-model="w.position" type="text" placeholder="职位" class="w-full px-3 py-2 rounded-lg text-sm focus:outline-none" style="background-color: var(--theme-surface); border: 1px solid var(--theme-border); color: var(--theme-text);" />
                  </div>
                  <div class="grid grid-cols-2 gap-3 md:col-span-2">
                    <div>
                      <label class="block text-sm font-medium mb-1.5" style="color: var(--theme-text);">开始</label>
                      <input v-model="w.startDate" type="month" class="w-full px-3 py-2 rounded-lg text-sm focus:outline-none" style="background-color: var(--theme-surface); border: 1px solid var(--theme-border); color: var(--theme-text);" />
                    </div>
                    <div>
                      <label class="block text-sm font-medium mb-1.5" style="color: var(--theme-text);">结束</label>
                      <input v-model="w.endDate" type="month" class="w-full px-3 py-2 rounded-lg text-sm focus:outline-none" style="background-color: var(--theme-surface); border: 1px solid var(--theme-border); color: var(--theme-text);" />
                    </div>
                  </div>
                  <div class="md:col-span-2">
                    <label class="block text-sm font-medium mb-1.5" style="color: var(--theme-text);">工作描述</label>
                    <textarea v-model="w.description" rows="2" placeholder="主要职责与成果" class="w-full px-3 py-2 rounded-lg text-sm focus:outline-none resize-y" style="background-color: var(--theme-surface); border: 1px solid var(--theme-border); color: var(--theme-text);"></textarea>
                  </div>
                </div>
                <button
                  @click="removeWork(idx)"
                  class="absolute -top-2 -right-2 w-6 h-6 rounded-full flex items-center justify-center text-white shadow"
                  style="background-color: #ef4444;"
                  aria-label="删除该工作经历"
                >
                  <Trash2 class="w-3 h-3" />
                </button>
              </div>
            </div>
          </div>

          <!-- 6. 项目经历 -->
          <div
            class="rounded-xl border p-6 mb-5"
            style="background-color: var(--theme-surface); border-color: var(--theme-border);"
          >
            <div class="flex items-center justify-between mb-4">
              <h3 class="text-base font-semibold flex items-center" style="color: var(--theme-text);">
                <Code class="w-4 h-4 mr-2" style="color: var(--theme-primary);" />
                项目经历
              </h3>
              <button
                @click="addProject"
                class="inline-flex items-center px-3 py-1.5 rounded-lg text-xs text-white transition hover:opacity-90"
                style="background-color: var(--theme-primary);"
              >
                <Plus class="w-3.5 h-3.5 mr-1" />
                添加
              </button>
            </div>
            <div v-if="form.projects!.length === 0" class="text-sm py-3 text-center" style="color: var(--theme-text-secondary);">
              暂无项目经历，点击右上角添加
            </div>
            <div v-else class="space-y-4">
              <div
                v-for="(p, idx) in form.projects"
                :key="'proj-' + idx"
                class="rounded-lg p-4 relative"
                style="background-color: var(--theme-bg); border: 1px solid var(--theme-border);"
              >
                <div class="grid grid-cols-1 md:grid-cols-2 gap-3">
                  <div>
                    <label class="block text-sm font-medium mb-1.5" style="color: var(--theme-text);">项目名称</label>
                    <input v-model="p.name" type="text" placeholder="项目名称" class="w-full px-3 py-2 rounded-lg text-sm focus:outline-none" style="background-color: var(--theme-surface); border: 1px solid var(--theme-border); color: var(--theme-text);" />
                  </div>
                  <div>
                    <label class="block text-sm font-medium mb-1.5" style="color: var(--theme-text);">担任角色</label>
                    <input v-model="p.role" type="text" placeholder="例如：前端负责人" class="w-full px-3 py-2 rounded-lg text-sm focus:outline-none" style="background-color: var(--theme-surface); border: 1px solid var(--theme-border); color: var(--theme-text);" />
                  </div>
                  <div class="grid grid-cols-2 gap-3 md:col-span-2">
                    <div>
                      <label class="block text-sm font-medium mb-1.5" style="color: var(--theme-text);">开始</label>
                      <input v-model="p.startDate" type="month" class="w-full px-3 py-2 rounded-lg text-sm focus:outline-none" style="background-color: var(--theme-surface); border: 1px solid var(--theme-border); color: var(--theme-text);" />
                    </div>
                    <div>
                      <label class="block text-sm font-medium mb-1.5" style="color: var(--theme-text);">结束</label>
                      <input v-model="p.endDate" type="month" class="w-full px-3 py-2 rounded-lg text-sm focus:outline-none" style="background-color: var(--theme-surface); border: 1px solid var(--theme-border); color: var(--theme-text);" />
                    </div>
                  </div>
                  <div class="md:col-span-2">
                    <label class="block text-sm font-medium mb-1.5" style="color: var(--theme-text);">项目链接</label>
                    <input v-model="p.url" type="text" placeholder="例如：https://github.com/..." class="w-full px-3 py-2 rounded-lg text-sm focus:outline-none" style="background-color: var(--theme-surface); border: 1px solid var(--theme-border); color: var(--theme-text);" />
                  </div>
                  <div class="md:col-span-2">
                    <label class="block text-sm font-medium mb-1.5" style="color: var(--theme-text);">项目描述</label>
                    <textarea v-model="p.description" rows="3" placeholder="技术栈、职责与成果" class="w-full px-3 py-2 rounded-lg text-sm focus:outline-none resize-y" style="background-color: var(--theme-surface); border: 1px solid var(--theme-border); color: var(--theme-text);"></textarea>
                  </div>
                </div>
                <button
                  @click="removeProject(idx)"
                  class="absolute -top-2 -right-2 w-6 h-6 rounded-full flex items-center justify-center text-white shadow"
                  style="background-color: #ef4444;"
                  aria-label="删除该项目经历"
                >
                  <Trash2 class="w-3 h-3" />
                </button>
              </div>
            </div>
          </div>

          <!-- 7. 技能列表 -->
          <div
            class="rounded-xl border p-6 mb-5"
            style="background-color: var(--theme-surface); border-color: var(--theme-border);"
          >
            <div class="flex items-center justify-between mb-4">
              <h3 class="text-base font-semibold flex items-center" style="color: var(--theme-text);">
                <Star class="w-4 h-4 mr-2" style="color: var(--theme-primary);" />
                技能列表
              </h3>
              <button
                @click="addSkill"
                class="inline-flex items-center px-3 py-1.5 rounded-lg text-xs text-white transition hover:opacity-90"
                style="background-color: var(--theme-primary);"
              >
                <Plus class="w-3.5 h-3.5 mr-1" />
                添加
              </button>
            </div>
            <div v-if="form.skills!.length === 0" class="text-sm py-3 text-center" style="color: var(--theme-text-secondary);">
              暂无技能，点击右上角添加
            </div>
            <div v-else class="space-y-3">
              <div
                v-for="(s, idx) in form.skills"
                :key="'skill-' + idx"
                class="grid grid-cols-1 md:grid-cols-3 gap-3 relative rounded-lg p-3"
                style="background-color: var(--theme-bg); border: 1px solid var(--theme-border);"
              >
                <div>
                  <label class="block text-sm font-medium mb-1.5" style="color: var(--theme-text);">技能名称</label>
                  <input v-model="s.name" type="text" placeholder="例如：Vue" class="w-full px-3 py-2 rounded-lg text-sm focus:outline-none" style="background-color: var(--theme-surface); border: 1px solid var(--theme-border); color: var(--theme-text);" />
                </div>
                <div>
                  <label class="block text-sm font-medium mb-1.5" style="color: var(--theme-text);">熟练度</label>
                  <select v-model="s.level" class="w-full px-3 py-2 rounded-lg text-sm focus:outline-none" style="background-color: var(--theme-surface); border: 1px solid var(--theme-border); color: var(--theme-text);">
                    <option value="">请选择</option>
                    <option value="了解">了解</option>
                    <option value="一般">一般</option>
                    <option value="熟练">熟练</option>
                    <option value="精通">精通</option>
                  </select>
                </div>
                <div>
                  <label class="block text-sm font-medium mb-1.5" style="color: var(--theme-text);">分类</label>
                  <input v-model="s.category" type="text" placeholder="例如：前端框架" class="w-full px-3 py-2 rounded-lg text-sm focus:outline-none" style="background-color: var(--theme-surface); border: 1px solid var(--theme-border); color: var(--theme-text);" />
                </div>
                <button
                  @click="removeSkill(idx)"
                  class="absolute -top-2 -right-2 w-6 h-6 rounded-full flex items-center justify-center text-white shadow"
                  style="background-color: #ef4444;"
                  aria-label="删除该技能"
                >
                  <Trash2 class="w-3 h-3" />
                </button>
              </div>
            </div>
          </div>

          <!-- 8. 自我介绍 -->
          <div
            class="rounded-xl border p-6 mb-6"
            style="background-color: var(--theme-surface); border-color: var(--theme-border);"
          >
            <h3 class="text-base font-semibold mb-4 flex items-center" style="color: var(--theme-text);">
              <FileText class="w-4 h-4 mr-2" style="color: var(--theme-primary);" />
              自我介绍
            </h3>
            <textarea
              v-model="form.selfIntro"
              rows="6"
              placeholder="简要介绍自己的优势、职业规划与兴趣方向..."
              class="w-full px-3 py-2 rounded-lg text-sm focus:outline-none resize-y"
              style="background-color: var(--theme-bg); border: 1px solid var(--theme-border); color: var(--theme-text);"
            ></textarea>
          </div>

          <!-- 底部操作按钮 -->
          <div class="flex flex-col sm:flex-row gap-3">
            <button
              @click="handleSaveDraft"
              class="flex-1 flex items-center justify-center px-5 py-2.5 rounded-lg text-sm font-medium transition hover:opacity-90"
              style="background-color: var(--theme-bg); color: var(--theme-text); border: 1px solid var(--theme-border);"
            >
              <Save class="w-4 h-4 mr-2" />
              保存草稿
            </button>
            <button
              @click="handleExportPdf"
              :disabled="exporting"
              class="flex-1 flex items-center justify-center px-5 py-2.5 rounded-lg text-sm font-medium transition hover:opacity-90 disabled:opacity-50 disabled:cursor-not-allowed"
              style="background-color: var(--theme-bg); color: var(--theme-text); border: 1px solid var(--theme-border);"
            >
              <Download class="w-4 h-4 mr-2" />
              {{ exporting ? '导出中...' : '导出PDF' }}
            </button>
            <button
              @click="handleScore"
              :disabled="scoring"
              class="flex-1 flex items-center justify-center px-5 py-2.5 rounded-lg text-sm font-medium text-white transition hover:opacity-90 disabled:opacity-50 disabled:cursor-not-allowed"
              style="background-color: var(--theme-primary);"
            >
              <Star class="w-4 h-4 mr-2" />
              {{ scoring ? '评分中...' : 'AI 评分' }}
            </button>
            <button
              @click="handleGetAdvice"
              :disabled="adviceLoading"
              class="flex-1 flex items-center justify-center px-5 py-2.5 rounded-lg text-sm font-medium text-white transition hover:opacity-90 disabled:opacity-50 disabled:cursor-not-allowed"
              style="background: linear-gradient(135deg, var(--theme-primary), color-mix(in srgb, var(--theme-primary) 70%, #7c3aed));"
            >
              <Sparkles class="w-4 h-4 mr-2" />
              {{ adviceLoading ? '生成中...' : 'AI 建议' }}
            </button>
          </div>
        </template>
      </div>
    </div>

    <SiteFooter />
  </div>
</template>
