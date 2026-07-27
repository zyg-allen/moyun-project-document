<script setup lang="ts">
import { ref, reactive, computed, onMounted, watch, nextTick } from 'vue';
import { useRoute, useRouter, onBeforeRouteLeave } from 'vue-router';
import { useHead } from '@vueuse/head';
import {
  Save, Download, Star, Plus, Trash2, User, Briefcase, GraduationCap,
  Code, FileText, Target, Sparkles, CheckCircle2, XCircle, AlertCircle,
  Eye, ArrowRight, PenLine,
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

// v5.9 阶段2：Tab 切换（编辑 / 预览 / AI建议 / AI评分）
// 取代原吸顶栏 + 底部 4 按钮重复布局：编辑为默认，预览/建议/评分通过 Tab 进入
type ResumeTab = 'edit' | 'preview' | 'advice' | 'score';
const activeTab = ref<ResumeTab>('edit');

// 已采纳建议的索引集合（避免重复采纳；采纳即将建议内容追加到自我介绍）
const acceptedAdvices = ref<Set<number>>(new Set());

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
      acceptedAdvices.value.clear();
      toast.success(`评分完成：${form.score} 分`);
      // 切换到 AI 评分 Tab 展示结果（取代原滚动到评分面板）
      activeTab.value = 'score';
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
      acceptedAdvices.value.clear();
      // 切换到 AI 建议 Tab 展示结果（取代原滚动到建议面板）
      activeTab.value = 'advice';
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

// v5.9 阶段2：采纳建议 —— 将建议内容追加到自我介绍末尾
// 设计权衡：建议为自然语言文本，无法精准定位到某个表单字段，
// 自我介绍是承载改进点的最合适字段；采纳后切到编辑 Tab 由用户检查并保存
function acceptAdvice(advice: { dimension?: string; content: string; priority?: string }, idx: number) {
  if (acceptedAdvices.value.has(idx)) {
    toast.info('该建议已采纳');
    return;
  }
  const prefix = form.selfIntro?.trim() ? '\n\n' : '';
  const tag = `[${advice.dimension || '改进建议'}] ${advice.content}`;
  form.selfIntro = (form.selfIntro || '') + prefix + tag;
  acceptedAdvices.value.add(idx);
  toast.success('已采纳到自我介绍，请切换到「编辑」检查并保存');
}

// 跳转到学习中心（评分 Tab 的"建立学习计划"入口）
function gotoStudyPlan() {
  router.push('/learn');
}

// 简历完成度计算（用于预览 Tab 展示填写进度）
const resumeCompleteness = computed(() => {
  let filled = 0;
  let total = 8;
  if (form.title?.trim()) filled++;
  if (form.name?.trim()) filled++;
  if (form.jobIntention?.position?.trim() || form.jobIntention?.city?.trim()) filled++;
  if (form.educations && form.educations.length > 0) filled++;
  if (form.works && form.works.length > 0) filled++;
  if (form.projects && form.projects.length > 0) filled++;
  if (form.skills && form.skills.length > 0) filled++;
  if (form.selfIntro?.trim()) filled++;
  return Math.round((filled / total) * 100);
});

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
        <!-- 保存状态指示（操作按钮统一移至 Tab 区，吸顶栏仅保留状态提示） -->
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
        <span v-else class="w-12"></span>
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
          <!-- Tab 切换栏：编辑 / 预览 / AI建议 / AI评分 -->
          <div class="mb-6 flex items-center gap-1 rounded-xl p-1 overflow-x-auto" style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);">
            <button
              @click="activeTab = 'edit'"
              class="flex-1 min-w-[80px] inline-flex items-center justify-center gap-1.5 px-3 py-2 rounded-lg text-sm font-medium transition whitespace-nowrap"
              :style="activeTab === 'edit' ? { backgroundColor: 'var(--theme-primary)', color: '#fff' } : { color: 'var(--theme-text-secondary)' }"
            >
              <PenLine class="w-4 h-4" />
              编辑
            </button>
            <button
              @click="activeTab = 'preview'"
              class="flex-1 min-w-[80px] inline-flex items-center justify-center gap-1.5 px-3 py-2 rounded-lg text-sm font-medium transition whitespace-nowrap"
              :style="activeTab === 'preview' ? { backgroundColor: 'var(--theme-primary)', color: '#fff' } : { color: 'var(--theme-text-secondary)' }"
            >
              <Eye class="w-4 h-4" />
              预览
            </button>
            <button
              @click="activeTab = 'advice'"
              class="flex-1 min-w-[80px] inline-flex items-center justify-center gap-1.5 px-3 py-2 rounded-lg text-sm font-medium transition whitespace-nowrap"
              :style="activeTab === 'advice' ? { backgroundColor: 'var(--theme-primary)', color: '#fff' } : { color: 'var(--theme-text-secondary)' }"
            >
              <Sparkles class="w-4 h-4" />
              AI 建议
            </button>
            <button
              @click="activeTab = 'score'"
              class="flex-1 min-w-[80px] inline-flex items-center justify-center gap-1.5 px-3 py-2 rounded-lg text-sm font-medium transition whitespace-nowrap"
              :style="activeTab === 'score' ? { backgroundColor: 'var(--theme-primary)', color: '#fff' } : { color: 'var(--theme-text-secondary)' }"
            >
              <Star class="w-4 h-4" />
              AI 评分
            </button>
          </div>

          <!-- ==================== 编辑 Tab ==================== -->
          <div v-show="activeTab === 'edit'">
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

          <!-- ==================== 编辑 Tab 结束 ==================== -->
          </div>

          <!-- ==================== 预览 Tab ==================== -->
          <div v-show="activeTab === 'preview'">
            <!-- 预览工具栏：导出 PDF -->
            <div
              class="rounded-xl border p-4 mb-4 flex items-center justify-between flex-wrap gap-3"
              style="background-color: var(--theme-surface); border-color: var(--theme-border);"
            >
              <div class="flex items-center gap-3">
                <span class="text-sm font-medium" style="color: var(--theme-text);">
                  <FileText class="w-4 h-4 inline mr-1" style="color: var(--theme-primary);" />
                  简历完成度 {{ resumeCompleteness }}%
                </span>
                <div class="w-32 h-1.5 rounded-full" style="background-color: var(--theme-bg);">
                  <div
                    class="h-1.5 rounded-full transition-all"
                    :style="{ width: resumeCompleteness + '%', backgroundColor: 'var(--theme-primary)' }"
                  ></div>
                </div>
              </div>
              <button
                @click="handleExportPdf"
                :disabled="exporting"
                class="inline-flex items-center px-4 py-2 rounded-lg text-sm font-medium text-white transition hover:opacity-90 disabled:opacity-50 disabled:cursor-not-allowed"
                style="background-color: var(--theme-primary);"
              >
                <Download class="w-4 h-4 mr-1.5" />
                {{ exporting ? '导出中...' : '导出 PDF' }}
              </button>
            </div>

            <!-- 简历预览（只读渲染） -->
            <div
              class="rounded-xl border p-8"
              style="background-color: var(--theme-surface); border-color: var(--theme-border);"
            >
              <!-- 标题 -->
              <h1 class="text-2xl font-bold mb-1" style="color: var(--theme-text);">
                {{ form.title || '未命名简历' }}
              </h1>
              <p v-if="form.name" class="text-sm mb-4" style="color: var(--theme-text-secondary);">
                {{ form.name }}<span v-if="form.gender"> · {{ form.gender }}</span><span v-if="form.birthDate"> · {{ form.birthDate }}</span>
              </p>
              <div v-if="form.phone || form.email" class="text-xs mb-6 flex flex-wrap gap-4" style="color: var(--theme-text-secondary);">
                <span v-if="form.phone">{{ form.phone }}</span>
                <span v-if="form.email">{{ form.email }}</span>
              </div>

              <hr class="mb-6" style="border-color: var(--theme-border);" />

              <!-- 求职意向 -->
              <section v-if="form.jobIntention && (form.jobIntention.position || form.jobIntention.city)" class="mb-6">
                <h2 class="text-base font-semibold mb-2 flex items-center" style="color: var(--theme-text);">
                  <Target class="w-4 h-4 mr-2" style="color: var(--theme-primary);" />
                  求职意向
                </h2>
                <div class="text-sm grid grid-cols-2 gap-1" style="color: var(--theme-text-secondary);">
                  <span v-if="form.jobIntention.position">期望职位：{{ form.jobIntention.position }}</span>
                  <span v-if="form.jobIntention.city">期望城市：{{ form.jobIntention.city }}</span>
                  <span v-if="form.jobIntention.salaryMin">薪资：{{ form.jobIntention.salaryMin }}K - {{ form.jobIntention.salaryMax }}K</span>
                  <span v-if="form.jobIntention.jobType">性质：{{ form.jobIntention.jobType }}</span>
                  <span v-if="form.jobIntention.availableTime">到岗：{{ form.jobIntention.availableTime }}</span>
                </div>
              </section>

              <!-- 教育经历 -->
              <section v-if="form.educations && form.educations.length > 0" class="mb-6">
                <h2 class="text-base font-semibold mb-2 flex items-center" style="color: var(--theme-text);">
                  <GraduationCap class="w-4 h-4 mr-2" style="color: var(--theme-primary);" />
                  教育经历
                </h2>
                <div v-for="(edu, i) in form.educations" :key="'pe-'+i" class="mb-3 text-sm" style="color: var(--theme-text-secondary);">
                  <div class="font-medium" style="color: var(--theme-text);">
                    {{ edu.school }}<span v-if="edu.major"> · {{ edu.major }}</span><span v-if="edu.degree"> · {{ edu.degree }}</span>
                  </div>
                  <div class="text-xs">{{ edu.startDate }} ~ {{ edu.endDate }}</div>
                  <p v-if="edu.description" class="mt-1 text-xs whitespace-pre-line">{{ edu.description }}</p>
                </div>
              </section>

              <!-- 工作经历 -->
              <section v-if="form.works && form.works.length > 0" class="mb-6">
                <h2 class="text-base font-semibold mb-2 flex items-center" style="color: var(--theme-text);">
                  <Briefcase class="w-4 h-4 mr-2" style="color: var(--theme-primary);" />
                  工作经历
                </h2>
                <div v-for="(w, i) in form.works" :key="'pw-'+i" class="mb-3 text-sm" style="color: var(--theme-text-secondary);">
                  <div class="font-medium" style="color: var(--theme-text);">
                    {{ w.company }}<span v-if="w.position"> · {{ w.position }}</span>
                  </div>
                  <div class="text-xs">{{ w.startDate }} ~ {{ w.endDate }}</div>
                  <p v-if="w.description" class="mt-1 text-xs whitespace-pre-line">{{ w.description }}</p>
                </div>
              </section>

              <!-- 项目经历 -->
              <section v-if="form.projects && form.projects.length > 0" class="mb-6">
                <h2 class="text-base font-semibold mb-2 flex items-center" style="color: var(--theme-text);">
                  <Code class="w-4 h-4 mr-2" style="color: var(--theme-primary);" />
                  项目经历
                </h2>
                <div v-for="(p, i) in form.projects" :key="'pp-'+i" class="mb-3 text-sm" style="color: var(--theme-text-secondary);">
                  <div class="font-medium" style="color: var(--theme-text);">
                    {{ p.name }}<span v-if="p.role"> · {{ p.role }}</span>
                  </div>
                  <div class="text-xs">{{ p.startDate }} ~ {{ p.endDate }}</div>
                  <p v-if="p.description" class="mt-1 text-xs whitespace-pre-line">{{ p.description }}</p>
                </div>
              </section>

              <!-- 技能 -->
              <section v-if="form.skills && form.skills.length > 0" class="mb-6">
                <h2 class="text-base font-semibold mb-2 flex items-center" style="color: var(--theme-text);">
                  <Star class="w-4 h-4 mr-2" style="color: var(--theme-primary);" />
                  技能列表
                </h2>
                <div class="flex flex-wrap gap-2">
                  <span
                    v-for="(s, i) in form.skills"
                    :key="'ps-'+i"
                    class="inline-flex items-center px-2.5 py-1 rounded-full text-xs"
                    style="background-color: var(--theme-bg); color: var(--theme-text-secondary);"
                  >
                    {{ s.name }}<span v-if="s.level" class="ml-1 opacity-70">· {{ s.level }}</span>
                  </span>
                </div>
              </section>

              <!-- 自我介绍 -->
              <section v-if="form.selfIntro" class="mb-2">
                <h2 class="text-base font-semibold mb-2 flex items-center" style="color: var(--theme-text);">
                  <User class="w-4 h-4 mr-2" style="color: var(--theme-primary);" />
                  自我介绍
                </h2>
                <p class="text-sm whitespace-pre-line" style="color: var(--theme-text-secondary);">
                  {{ form.selfIntro }}
                </p>
              </section>
            </div>
          </div>

          <!-- ==================== AI 建议 Tab ==================== -->
          <div v-show="activeTab === 'advice'">
            <!-- 空状态：未生成建议 -->
            <div
              v-if="!aiAdvice"
              class="rounded-xl border p-10 text-center"
              style="background-color: var(--theme-surface); border-color: var(--theme-border);"
            >
              <Sparkles class="w-10 h-10 mx-auto mb-3" style="color: var(--theme-primary); opacity: 0.6;" />
              <p class="text-sm mb-4" style="color: var(--theme-text-secondary);">
                基于当前简历评分与目标岗位匹配度，生成针对性改进建议
              </p>
              <button
                @click="handleGetAdvice"
                :disabled="adviceLoading"
                class="inline-flex items-center px-5 py-2.5 rounded-lg text-sm font-medium text-white transition hover:opacity-90 disabled:opacity-50 disabled:cursor-not-allowed"
                style="background: linear-gradient(135deg, var(--theme-primary), color-mix(in srgb, var(--theme-primary) 70%, #7c3aed));"
              >
                <Sparkles class="w-4 h-4 mr-1.5" />
                {{ adviceLoading ? '生成中...' : '获取 AI 建议' }}
              </button>
            </div>

            <!-- 有数据：左右布局 -->
            <div v-else class="grid grid-cols-1 lg:grid-cols-2 gap-6">
              <!-- 左侧：简历预览（紧凑版） -->
              <div
                class="rounded-xl border p-5 lg:sticky lg:top-20 lg:self-start lg:max-h-[calc(100vh-6rem)] lg:overflow-y-auto"
                style="background-color: var(--theme-surface); border-color: var(--theme-border);"
              >
                <h3 class="text-base font-semibold mb-3 flex items-center" style="color: var(--theme-text);">
                  <FileText class="w-4 h-4 mr-2" style="color: var(--theme-primary);" />
                  简历预览
                </h3>
                <div class="text-sm">
                  <div class="font-semibold mb-1" style="color: var(--theme-text);">{{ form.title || '未命名简历' }}</div>
                  <p v-if="form.name" class="text-xs mb-3" style="color: var(--theme-text-secondary);">
                    {{ form.name }}<span v-if="form.jobIntention?.position"> · 目标：{{ form.jobIntention.position }}</span>
                  </p>
                  <div v-if="form.skills && form.skills.length > 0" class="flex flex-wrap gap-1 mb-3">
                    <span
                      v-for="(s, i) in form.skills"
                      :key="'as-'+i"
                      class="px-1.5 py-0.5 rounded text-[10px]"
                      style="background-color: var(--theme-bg); color: var(--theme-text-secondary);"
                    >{{ s.name }}</span>
                  </div>
                  <p v-if="form.selfIntro" class="text-xs whitespace-pre-line line-clamp-[12]" style="color: var(--theme-text-secondary);">
                    {{ form.selfIntro }}
                  </p>
                </div>
              </div>

              <!-- 右侧：建议列表 -->
              <div>
                <!-- 标题行 + 等级 + 重新生成 -->
                <div class="flex items-center justify-between mb-3 flex-wrap gap-2">
                  <h3 class="text-base font-semibold flex items-center" style="color: var(--theme-text);">
                    <Sparkles class="w-4 h-4 mr-2" style="color: var(--theme-primary);" />
                    AI 改进建议
                    <span
                      v-if="aiAdvice.grade"
                      class="ml-2 inline-flex items-center px-2 py-0.5 rounded text-xs font-medium"
                      :class="gradeStyle[aiAdvice.grade] || gradeStyle.D"
                    >等级 {{ aiAdvice.grade }}</span>
                    <span
                      v-if="aiAdvice.aiPowered === false"
                      class="ml-2 text-xs px-2 py-0.5 rounded-full"
                      style="background-color: var(--theme-bg); color: var(--theme-text-secondary);"
                    >规则化生成</span>
                  </h3>
                  <button
                    @click="handleGetAdvice"
                    :disabled="adviceLoading"
                    class="text-xs px-2.5 py-1 rounded-lg transition disabled:opacity-50"
                    style="background-color: var(--theme-bg); color: var(--theme-text-secondary); border: 1px solid var(--theme-border);"
                  >
                    {{ adviceLoading ? '刷新中...' : '刷新建议' }}
                  </button>
                </div>

                <!-- 整体总结 -->
                <div
                  v-if="aiAdvice.summary"
                  class="rounded-lg p-3 mb-3 text-sm leading-relaxed"
                  style="background-color: var(--theme-bg); color: var(--theme-text);"
                >
                  {{ aiAdvice.summary }}
                </div>

                <!-- 缺失技能提示 -->
                <div
                  v-if="aiAdvice.missingSkills && aiAdvice.missingSkills.length > 0"
                  class="mb-3 rounded-lg p-3"
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

                <!-- 建议列表（带采纳按钮） -->
                <div v-if="aiAdvice.advices && aiAdvice.advices.length > 0" class="space-y-3">
                  <div
                    v-for="(advice, idx) in aiAdvice.advices"
                    :key="idx"
                    class="rounded-lg p-3 relative"
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
                    <p class="text-sm leading-relaxed pr-20" style="color: var(--theme-text-secondary);">
                      {{ advice.content }}
                    </p>
                    <!-- 采纳按钮 -->
                    <button
                      v-if="acceptedAdvices.has(idx)"
                      disabled
                      class="absolute top-3 right-3 text-xs px-2.5 py-1 rounded-lg flex items-center"
                      style="background-color: rgba(22,163,74,0.1); color: #16a34a; border: 1px solid rgba(22,163,74,0.2);"
                    >
                      <CheckCircle2 class="w-3 h-3 mr-1" />
                      已采纳
                    </button>
                    <button
                      v-else
                      @click="acceptAdvice(advice, idx)"
                      class="absolute top-3 right-3 text-xs px-2.5 py-1 rounded-lg transition flex items-center"
                      style="background-color: var(--theme-primary); color: #fff;"
                    >
                      <Plus class="w-3 h-3 mr-1" />
                      采纳
                    </button>
                  </div>
                </div>
                <p v-else class="text-sm text-center py-4" style="color: var(--theme-text-secondary);">
                  <CheckCircle2 class="w-5 h-5 inline mr-1" style="color: #16a34a;" />
                  各维度得分率良好，暂无改进建议
                </p>

                <!-- 采纳后提示 -->
                <div
                  v-if="acceptedAdvices.size > 0"
                  class="mt-4 rounded-lg p-3 text-xs flex items-center justify-between flex-wrap gap-2"
                  style="background-color: rgba(22,163,74,0.06); border: 1px solid rgba(22,163,74,0.2);"
                >
                  <span style="color: #16a34a;">
                    <CheckCircle2 class="w-3.5 h-3.5 inline mr-1" />
                    已采纳 {{ acceptedAdvices.size }} 条建议到「自我介绍」
                  </span>
                  <button
                    @click="activeTab = 'edit'"
                    class="text-xs px-2.5 py-1 rounded-lg"
                    style="background-color: var(--theme-primary); color: #fff;"
                  >
                    去编辑检查
                  </button>
                </div>
              </div>
            </div>
          </div>

          <!-- ==================== AI 评分 Tab ==================== -->
          <div v-show="activeTab === 'score'">
            <!-- 空状态：未评分 -->
            <div
              v-if="!(form.scoreDetail && form.scoreDetail.length > 0)"
              class="rounded-xl border p-10 text-center"
              style="background-color: var(--theme-surface); border-color: var(--theme-border);"
            >
              <Star class="w-10 h-10 mx-auto mb-3" style="color: var(--theme-primary); opacity: 0.6;" />
              <p class="text-sm mb-4" style="color: var(--theme-text-secondary);">
                基于简历完整度、岗位匹配度等维度智能评分，发现个人短板
              </p>
              <button
                @click="handleScore"
                :disabled="scoring"
                class="inline-flex items-center px-5 py-2.5 rounded-lg text-sm font-medium text-white transition hover:opacity-90 disabled:opacity-50 disabled:cursor-not-allowed"
                style="background-color: var(--theme-primary);"
              >
                <Star class="w-4 h-4 mr-1.5" />
                {{ scoring ? '评分中...' : '开始 AI 评分' }}
              </button>
            </div>

            <!-- 评分结果 -->
            <div v-else>
              <!-- 综合评分 -->
              <div
                class="rounded-xl border p-6 mb-4"
                style="background: linear-gradient(135deg, var(--theme-surface), color-mix(in srgb, var(--theme-primary) 6%, var(--theme-surface))); border-color: var(--theme-border);"
              >
                <div class="flex items-center justify-between flex-wrap gap-4">
                  <div class="flex items-center gap-4">
                    <div
                      class="w-20 h-20 rounded-full flex flex-col items-center justify-center"
                      style="background-color: var(--theme-primary); color: #fff;"
                    >
                      <span class="text-2xl font-bold leading-none">{{ form.score ?? 0 }}</span>
                      <span class="text-[10px] mt-1 opacity-90">综合分</span>
                    </div>
                    <div>
                      <h3 class="text-base font-semibold flex items-center" style="color: var(--theme-text);">
                        <Star class="w-4 h-4 mr-2" style="color: var(--theme-primary);" />
                        AI 综合评分
                      </h3>
                      <div class="flex items-center gap-2 mt-1">
                        <span
                          v-if="aiAdvice?.grade"
                          class="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium"
                          :class="gradeStyle[aiAdvice.grade] || gradeStyle.D"
                        >等级 {{ aiAdvice.grade }}</span>
                        <span
                          v-if="form.scoredTime"
                          class="text-xs"
                          style="color: var(--theme-text-secondary);"
                        >{{ form.scoredTime }}</span>
                      </div>
                    </div>
                  </div>
                  <button
                    @click="handleScore"
                    :disabled="scoring"
                    class="text-xs px-3 py-1.5 rounded-lg transition disabled:opacity-50"
                    style="background-color: var(--theme-bg); color: var(--theme-text-secondary); border: 1px solid var(--theme-border);"
                  >
                    {{ scoring ? '重新评分中...' : '重新评分' }}
                  </button>
                </div>
              </div>

              <!-- 各维度评分 -->
              <div
                class="rounded-xl border p-5 mb-4"
                style="background-color: var(--theme-surface); border-color: var(--theme-border);"
              >
                <h3 class="text-sm font-semibold mb-4 flex items-center" style="color: var(--theme-text);">
                  <Target class="w-4 h-4 mr-2" style="color: var(--theme-primary);" />
                  各维度评分明细
                </h3>
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
                        :style="{ width: scorePercent(item) + '%', backgroundColor: scorePercent(item) < 60 ? '#ef4444' : 'var(--theme-primary)' }"
                      ></div>
                    </div>
                    <p v-if="item.message" class="text-xs mt-1" style="color: var(--theme-text-secondary);">
                      {{ item.message }}
                    </p>
                    <!-- 岗位匹配度子项明细 -->
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

              <!-- 行业分析：岗位必备技能缺失 -->
              <div
                v-if="aiAdvice?.missingSkills && aiAdvice.missingSkills.length > 0"
                class="rounded-xl border p-5 mb-4"
                style="background-color: rgba(239,68,68,0.04); border-color: rgba(239,68,68,0.2);"
              >
                <h3 class="text-sm font-semibold mb-3 flex items-center" style="color: #ef4444;">
                  <AlertCircle class="w-4 h-4 mr-2" />
                  行业分析 · 岗位必备技能缺失
                </h3>
                <p class="text-xs mb-3" style="color: var(--theme-text-secondary);">
                  以下技能为目标岗位高频要求，但你的简历中未体现，建议补充或通过学习计划加强。
                </p>
                <div class="flex flex-wrap gap-2">
                  <span
                    v-for="skill in aiAdvice.missingSkills"
                    :key="skill"
                    class="inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-xs"
                    style="background-color: rgba(239,68,68,0.08); color: #ef4444; border: 1px solid rgba(239,68,68,0.2);"
                  >
                    <XCircle class="w-3 h-3" />
                    {{ skill }}
                  </span>
                </div>
              </div>

              <!-- 个人短板：高优先级改进建议 -->
              <div
                v-if="aiAdvice?.advices && aiAdvice.advices.filter(a => a.priority === 'high').length > 0"
                class="rounded-xl border p-5 mb-4"
                style="background-color: var(--theme-surface); border-color: var(--theme-border);"
              >
                <h3 class="text-sm font-semibold mb-3 flex items-center" style="color: var(--theme-text);">
                  <Target class="w-4 h-4 mr-2" style="color: #ef4444;" />
                  个人短板 · 优先改进项
                </h3>
                <div class="space-y-2">
                  <div
                    v-for="(advice, idx) in aiAdvice.advices.filter(a => a.priority === 'high')"
                    :key="'weak-'+idx"
                    class="text-sm rounded-lg p-3"
                    style="background-color: var(--theme-bg);"
                  >
                    <div class="flex items-center gap-2 mb-1">
                      <span class="text-xs font-medium" style="color: var(--theme-text);">{{ advice.dimension || '综合' }}</span>
                      <span class="text-xs px-1.5 py-0.5 rounded bg-red-50 text-red-600 border border-red-200">高优先级</span>
                    </div>
                    <p class="text-xs leading-relaxed" style="color: var(--theme-text-secondary);">
                      {{ advice.content }}
                    </p>
                  </div>
                </div>
              </div>

              <!-- 去学习中心建立学习计划入口 -->
              <div
                class="rounded-xl border p-5 flex items-center justify-between flex-wrap gap-3"
                style="background: linear-gradient(135deg, var(--theme-primary), color-mix(in srgb, var(--theme-primary) 70%, #7c3aed)); color: #fff;"
              >
                <div>
                  <h3 class="text-sm font-semibold flex items-center mb-1">
                    <Sparkles class="w-4 h-4 mr-2" />
                    针对短板生成学习计划
                  </h3>
                  <p class="text-xs opacity-90">基于你的画像与薄弱点，自动生成针对性学习计划，逐项突破</p>
                </div>
                <button
                  @click="gotoStudyPlan"
                  class="inline-flex items-center px-4 py-2 rounded-lg text-sm font-medium transition hover:opacity-90"
                  style="background-color: #fff; color: var(--theme-primary);"
                >
                  去建立学习计划
                  <ArrowRight class="w-4 h-4 ml-1.5" />
                </button>
              </div>
            </div>
          </div>

          <!-- ==================== 底部保存按钮（全局，所有 Tab 可见） ==================== -->
          <div class="mt-6 flex justify-center">
            <button
              @click="handleSaveDraft"
              :disabled="saving"
              class="inline-flex items-center justify-center px-8 py-2.5 rounded-lg text-sm font-medium text-white transition hover:opacity-90 disabled:opacity-50 disabled:cursor-not-allowed"
              style="background-color: var(--theme-primary);"
            >
              <Save class="w-4 h-4 mr-2" />
              {{ saving ? '保存中...' : '保存草稿' }}
            </button>
          </div>
        </template>
      </div>
    </div>

    <SiteFooter />
  </div>
</template>
