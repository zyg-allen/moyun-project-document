<script setup lang="ts">
/**
 * 简历维护页 ResumeEditPage
 * 三栏布局：左侧导航 + 中间表单 + 右侧评分面板 + 底部固定操作栏
 * 对应 vue_resume_spec.md §五 + resume_optimizer_page.html page-resume-edit
 */
import { ref, reactive, computed, onMounted, onBeforeUnmount, watch, nextTick } from 'vue';
import { useRoute, useRouter, onBeforeRouteLeave } from 'vue-router';
import { useHead } from '@vueuse/head';
import {
  Save, Download, Star, Plus, Trash2, User, Briefcase, GraduationCap,
  Code, FileText, Target, Sparkles, CheckCircle2, XCircle, AlertCircle,
  ArrowRight, PenLine, UploadCloud, Terminal, FolderKanban, X,
} from 'lucide-vue-next';
import SiteFooter from '@/components/SiteFooter.vue';
import Breadcrumb from '@/components/Breadcrumb.vue';
import SectionCard from '@/components/resume/SectionCard.vue';
import ResumeSidebar, { type SidebarSection } from '@/components/resume/ResumeSidebar.vue';
import ScorePanel from '@/components/resume/ScorePanel.vue';
import ResumeActionBar from '@/components/resume/ResumeActionBar.vue';
import ResumePreviewModal from '@/components/resume/ResumePreviewModal.vue';
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
const adviceLoading = ref(false);
const aiAdvice = ref<ResumeAiAdviceVO | null>(null);
const saveStatus = ref<'idle' | 'saving' | 'saved'>('idle');
const saving = ref(false);
const loaded = ref(false);

// 弹窗控制：预览弹窗 / AI 建议弹窗
const previewVisible = ref(false);
const adviceVisible = ref(false);

// 已采纳建议索引集合
const acceptedAdvices = ref<Set<number>>(new Set());

// 左侧导航当前高亮项（scroll spy）
const activeSection = ref('sec-personal');
const mainScrollRef = ref<HTMLElement | null>(null);

// 技能输入框（chip cloud 添加）
const skillInput = ref('');

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
  keywords: ['简历编辑', '创建简历', '求职简历', '简历评分', '旭林'],
  canonicalPath: isEdit.value
    ? `/interview/resume/edit/${editId.value}`
    : '/interview/resume/edit',
  robots: 'noindex,nofollow',
})));

// ========== 左侧导航配置 ==========
const sections = computed<SidebarSection[]>(() => [
  { id: 'sec-personal', label: '个人信息', icon: User, status: form.name?.trim() ? 'done' : 'partial' },
  { id: 'sec-objective', label: '求职意向', icon: Target, status: form.jobIntention?.position?.trim() ? 'done' : 'empty' },
  { id: 'sec-education', label: '教育背景', icon: GraduationCap, status: (form.educations?.length ?? 0) > 0 ? 'done' : 'empty' },
  { id: 'sec-work', label: '工作经历', icon: Briefcase, status: (form.works?.length ?? 0) > 0 ? 'done' : 'empty' },
  { id: 'sec-project', label: '项目经历', icon: FolderKanban, status: (form.projects?.length ?? 0) > 0 ? 'done' : 'empty' },
  { id: 'sec-skills', label: '专业技能', icon: Terminal, status: (form.skills?.length ?? 0) > 0 ? 'partial' : 'empty' },
  { id: 'sec-eval', label: '自我评价', icon: PenLine, status: form.selfIntro?.trim() ? 'partial' : 'empty' },
]);

// 完善进度
const progress = computed(() => {
  let filled = 0;
  const total = 7;
  if (form.name?.trim()) filled++;
  if (form.jobIntention?.position?.trim() || form.jobIntention?.city?.trim()) filled++;
  if (form.educations && form.educations.length > 0) filled++;
  if (form.works && form.works.length > 0) filled++;
  if (form.projects && form.projects.length > 0) filled++;
  if (form.skills && form.skills.length > 0) filled++;
  if (form.selfIntro?.trim()) filled++;
  return { filled, total, percent: Math.round((filled / total) * 100) };
});

// 可优化项数（用于右侧洞察提示）
const optimizeCount = computed(() => {
  const s = form.score ?? 0;
  if (s === 0) return 0;
  if (s >= 85) return 0;
  if (s >= 70) return 2;
  return 4;
});

// 面包屑
const breadcrumbs = computed(() => [
  { label: '面试指南', path: '/interview' },
  { label: '我的简历', path: '/interview/my/resumes' },
  { label: isEdit.value ? '编辑' : '创建' },
]);

// 简历完成度（预览弹窗用）
const resumeCompleteness = computed(() => progress.value.percent);

// ========== 动态数组增删 ==========
function addEducation() {
  form.educations!.push({
    school: '', major: '', degree: '', startDate: '', endDate: '', description: '',
  } as UserResumeEducationItem);
}
function removeEducation(idx: number) { form.educations!.splice(idx, 1); }
function addWork() {
  form.works!.push({
    company: '', position: '', startDate: '', endDate: '', description: '',
  } as UserResumeWorkItem);
}
function removeWork(idx: number) { form.works!.splice(idx, 1); }
function addProject() {
  form.projects!.push({
    name: '', role: '', startDate: '', endDate: '', description: '', url: '',
  } as UserResumeProjectItem);
}
function removeProject(idx: number) { form.projects!.splice(idx, 1); }

// ========== 技能 chip cloud ==========
// 数据模型仍为 UserResumeSkillItem[]，UI 用 chip 展示；Enter 添加，点击 chip 切换熟练度，点 x 删除
const SKILL_LEVELS = ['了解', '一般', '熟练', '精通'];
const DEFAULT_LEVEL = '熟练';

function addSkillFromInput() {
  const name = skillInput.value.trim();
  if (!name) return;
  if (form.skills!.some(s => s.name === name)) {
    toast.info('该技能已添加');
    return;
  }
  form.skills!.push({ name, level: DEFAULT_LEVEL, category: '' } as UserResumeSkillItem);
  skillInput.value = '';
}
function onSkillKeydown(e: KeyboardEvent) {
  if (e.key === 'Enter') {
    e.preventDefault();
    addSkillFromInput();
  }
}
function cycleSkillLevel(idx: number) {
  const skill = form.skills![idx];
  if (!skill) return;
  const cur = skill.level || DEFAULT_LEVEL;
  const i = SKILL_LEVELS.indexOf(cur);
  skill.level = SKILL_LEVELS[(i + 1) % SKILL_LEVELS.length];
}
function removeSkill(idx: number) { form.skills!.splice(idx, 1); }

// 求职意向默认值兜底
function ensureJobIntention(): UserResumeJobIntention {
  if (!form.jobIntention) {
    form.jobIntention = {
      position: '', city: '', salaryMin: undefined, salaryMax: undefined,
      jobType: '', availableTime: '',
    };
  }
  return form.jobIntention;
}

// 评分进度条百分比
function scorePercent(item: UserResumeScoreItem): number {
  if (!item.maxScore || item.maxScore <= 0) return 0;
  return Math.max(0, Math.min(100, (item.score / item.maxScore) * 100));
}

// ========== 保存 ==========
async function doSave(silent = false): Promise<boolean> {
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
        if (!isEdit.value) {
          router.replace(`/interview/resume/edit/${form.id}`);
        }
      }
      saveStatus.value = 'saved';
      if (!silent) toast.success('保存成功');
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

function handleSaveDraft() { return doSave(false); }

// 认证下载 PDF
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

async function handleExportPdf() {
  if (exporting.value) return;
  const ok = await doSave(true);
  if (!ok || !form.id) {
    toast.error('请先保存简历再导出');
    return;
  }
  try {
    exporting.value = true;
    const res = await exportResumePdf(form.id);
    if (res.code === 200 && res.data?.fileUrl) {
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

// 评分
async function handleScore() {
  if (scoring.value) return;
  const ok = await doSave(true);
  if (!ok || !form.id) {
    toast.error('请先保存简历再评分');
    return;
  }
  try {
    scoring.value = true;
    const res = await scoreResume(form.id);
    if (res.code === 200 && res.data) {
      form.score = res.data.score;
      form.scoreDetail = res.data.scoreDetail || [];
      form.scoredTime = res.data.scoredTime || '';
      aiAdvice.value = null;
      acceptedAdvices.value.clear();
      toast.success(`评分完成：${form.score} 分`);
    } else {
      toast.error(res.message || '评分失败，请稍后重试');
    }
  } catch (err: any) {
    toast.error(err?.message || '评分失败，请稍后重试');
  } finally {
    scoring.value = false;
  }
}

// AI 建议：打开建议弹窗并加载
async function handleOptimize() {
  // 未评分时先评分
  if (form.score === undefined || form.score === 0) {
    await handleScore();
    if (form.score === undefined || form.score === 0) return;
  }
  adviceVisible.value = true;
  if (!aiAdvice.value) {
    await handleGetAdvice();
  }
}

async function handleGetAdvice() {
  if (adviceLoading.value) return;
  if (!form.id) {
    toast.error('请先保存简历再获取建议');
    return;
  }
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
    } else {
      toast.error(res.message || '生成建议失败，请稍后重试');
    }
  } catch (err: any) {
    toast.error(err?.message || '生成建议失败，请稍后重试');
  } finally {
    adviceLoading.value = false;
  }
}

// 优先级 / 建议类型样式
const priorityStyle: Record<string, { label: string; class: string }> = {
  high: { label: '高优先级', class: 'bg-red-50 text-red-600 border border-red-200' },
  medium: { label: '中优先级', class: 'bg-amber-50 text-amber-600 border border-amber-200' },
  low: { label: '低优先级', class: 'bg-gray-50 text-gray-600 border border-gray-200' },
};
const adviceTypeLabel: Record<string, string> = { fill: '补充缺失', refine: '优化已有', match: '岗位匹配' };
const gradeStyle: Record<string, string> = {
  A: 'bg-green-50 text-green-600 border border-green-200',
  B: 'bg-blue-50 text-blue-600 border border-blue-200',
  C: 'bg-amber-50 text-amber-600 border border-amber-200',
  D: 'bg-red-50 text-red-600 border border-red-200',
};

// 采纳建议 —— 追加到自我介绍
function acceptAdvice(advice: { dimension?: string; content?: string; priority?: string }, idx: number) {
  if (acceptedAdvices.value.has(idx)) {
    toast.info('该建议已采纳');
    return;
  }
  if (!advice.content) {
    toast.error('该建议内容为空，无法采纳');
    return;
  }
  const prefix = form.selfIntro?.trim() ? '\n\n' : '';
  const tag = `[${advice.dimension || '改进建议'}] ${advice.content}`;
  form.selfIntro = (form.selfIntro || '') + prefix + tag;
  acceptedAdvices.value.add(idx);
  toast.success('已采纳到自我介绍');
}

// 撤销（mock：提示用户使用浏览器快捷键）
function handleUndo() {
  toast.info('请使用 Ctrl+Z 撤销输入');
}

function gotoStudyPlan() {
  router.push('/learn');
}

// ========== Scroll Spy ==========
function scrollToSection(id: string) {
  const el = document.getElementById(id);
  if (el) {
    el.scrollIntoView({ behavior: 'smooth', block: 'start' });
  }
}

function handleScrollSpy() {
  // 找到距离视口顶部最近（top 最小且 > 80）的 section
  const ids = sections.value.map(s => s.id);
  let current = activeSection.value;
  for (const id of ids) {
    const el = document.getElementById(id);
    if (!el) continue;
    const rect = el.getBoundingClientRect();
    if (rect.top <= 120) {
      current = id;
    } else {
      break;
    }
  }
  activeSection.value = current;
}

// ========== watch / 生命周期 ==========
watch(
  form,
  () => {
    if (!loaded.value) return;
    if (saving.value) return;
    saveStatus.value = 'idle';
  },
  { deep: true },
);

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
      if (ok) nextTick(() => { loaded.value = true; });
    });
  } else {
    nextTick(() => { loaded.value = true; });
  }
  window.addEventListener('scroll', handleScrollSpy, { passive: true });
});

onBeforeUnmount(() => {
  window.removeEventListener('scroll', handleScrollSpy);
});

// 路由 :id 变更时重新加载
watch(() => route.params.id, (newId, oldId) => {
  if (newId === oldId) return;
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

// 离开页提示
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
  <div class="re-page" style="background-color: var(--theme-bg);">
    <!-- 吸顶栏：面包屑 + 保存状态 -->
    <div class="re-topbar">
      <div class="re-topbar-inner">
        <Breadcrumb :items="breadcrumbs" />
        <div class="re-topbar-actions">
          <!-- 标题输入（紧凑） -->
          <input
            v-model="form.title"
            type="text"
            placeholder="简历标题，如：张三 - Java 工程师简历"
            maxlength="100"
            class="re-title-input"
          />
          <span
            v-if="saveStatus === 'saving'"
            class="re-save-badge saving"
          >保存中...</span>
          <span
            v-else-if="saveStatus === 'saved'"
            class="re-save-badge saved"
          >已保存</span>
        </div>
      </div>
    </div>

    <!-- 加载中 -->
    <div v-if="loadingDetail" class="re-loading">
      <div class="re-loading-spinner"></div>
      <p>加载中...</p>
    </div>

    <!-- 加载失败 -->
    <div v-else-if="pageError" class="re-error">
      <p>{{ pageError }}</p>
      <button @click="loadDetail" class="re-retry-btn">重试</button>
    </div>

    <!-- 三栏布局 -->
    <div v-else class="re-layout">
      <!-- 左侧导航 -->
      <ResumeSidebar
        :sections="sections"
        :active-id="activeSection"
        :progress="progress"
        :score="form.score"
        @navigate="scrollToSection"
        @optimize="handleOptimize"
      />

      <!-- 中间主区：表单 -->
      <main class="re-main">
        <!-- 个人信息 -->
        <SectionCard
          section-id="sec-personal"
          :icon="User"
          icon-color="red"
          title="个人信息"
          desc="基础联系方式，方便 HR 与你取得联系"
          :status="form.name?.trim() ? 'complete' : 'partial'"
        >
          <!-- 简历标题（独立字段，spec 中无但数据模型需要） -->
          <div class="re-form-row cols-1" style="margin-bottom: 16px;">
            <div class="re-field">
              <label class="re-field-label"><span class="re-req">*</span> 简历标题</label>
              <input
                v-model="form.title"
                type="text"
                placeholder="如：张三 - Java 开发工程师简历"
                class="re-input"
              />
              <div class="re-field-hint">用于简历列表展示，建议包含姓名与目标岗位</div>
            </div>
          </div>

          <div class="re-form-row cols-2">
            <div class="re-field">
              <label class="re-field-label"><span class="re-req">*</span> 姓名</label>
              <input v-model="form.name" type="text" placeholder="请输入真实姓名" class="re-input" />
            </div>
            <div class="re-field">
              <label class="re-field-label"><span class="re-req">*</span> 手机号码</label>
              <input v-model="form.phone" type="text" placeholder="请输入 11 位手机号" class="re-input" />
            </div>
          </div>
          <div class="re-form-row cols-2">
            <div class="re-field">
              <label class="re-field-label"><span class="re-req">*</span> 电子邮箱</label>
              <input v-model="form.email" type="email" placeholder="your@email.com" class="re-input" />
              <div class="re-field-hint">建议使用常用邮箱，部分 HR 会通过邮件发送面试邀请</div>
            </div>
            <div class="re-field">
              <label class="re-field-label">性别</label>
              <select v-model="form.gender" class="re-select">
                <option value="">请选择</option>
                <option value="男">男</option>
                <option value="女">女</option>
                <option value="保密">保密</option>
              </select>
            </div>
          </div>
          <div class="re-form-row cols-2">
            <div class="re-field">
              <label class="re-field-label">出生日期</label>
              <input v-model="form.birthDate" type="date" class="re-input" />
            </div>
            <div class="re-field">
              <label class="re-field-label">头像 URL</label>
              <input v-model="form.avatar" type="text" placeholder="https://..." class="re-input" />
            </div>
          </div>
        </SectionCard>

        <!-- 求职意向 -->
        <SectionCard
          section-id="sec-objective"
          :icon="Target"
          icon-color="blue"
          title="求职意向"
          desc="明确的求职目标有助于精准匹配岗位"
          :status="ensureJobIntention().position?.trim() ? 'complete' : 'empty'"
        >
          <div class="re-form-row cols-3">
            <div class="re-field">
              <label class="re-field-label"><span class="re-req">*</span> 期望职位</label>
              <input v-model="ensureJobIntention().position" type="text" placeholder="如：Java 开发工程师" class="re-input" />
              <div class="re-field-hint">建议与招聘 JD 岗位名称保持一致</div>
            </div>
            <div class="re-field">
              <label class="re-field-label">期望城市</label>
              <input v-model="ensureJobIntention().city" type="text" placeholder="如：深圳" class="re-input" />
            </div>
            <div class="re-field">
              <label class="re-field-label">工作性质</label>
              <select v-model="ensureJobIntention().jobType" class="re-select">
                <option value="">请选择</option>
                <option value="全职">全职</option>
                <option value="兼职">兼职</option>
                <option value="实习">实习</option>
              </select>
            </div>
          </div>
          <div class="re-form-row cols-3">
            <div class="re-field">
              <label class="re-field-label">最低薪资（K）</label>
              <input v-model.number="ensureJobIntention().salaryMin" type="number" min="0" placeholder="如：15" class="re-input" />
            </div>
            <div class="re-field">
              <label class="re-field-label">最高薪资（K）</label>
              <input v-model.number="ensureJobIntention().salaryMax" type="number" min="0" placeholder="如：25" class="re-input" />
            </div>
            <div class="re-field">
              <label class="re-field-label">到岗时间</label>
              <input v-model="ensureJobIntention().availableTime" type="text" placeholder="如：随时 / 1个月内" class="re-input" />
            </div>
          </div>
        </SectionCard>

        <!-- 教育背景 -->
        <SectionCard
          section-id="sec-education"
          :icon="GraduationCap"
          icon-color="green"
          title="教育背景"
          desc="从最高学历开始填写"
          :status="(form.educations?.length ?? 0) > 0 ? 'complete' : 'empty'"
        >
          <div v-if="form.educations!.length === 0" class="re-empty-tip">暂无教育经历，点击下方按钮添加</div>
          <div v-for="(edu, idx) in form.educations" :key="'edu-'+idx" class="re-entry">
            <div class="re-entry-head">
              <span class="re-entry-badge"><span class="re-entry-dot"></span>教育经历 #{{ idx + 1 }}</span>
              <div class="re-entry-actions">
                <button class="re-entry-btn danger" @click="removeEducation(idx)" title="删除">
                  <Trash2 class="w-3 h-3" />
                </button>
              </div>
            </div>
            <div class="re-form-row cols-3">
              <div class="re-field">
                <label class="re-field-label"><span class="re-req">*</span> 学校</label>
                <input v-model="edu.school" type="text" placeholder="如：北京大学" class="re-input" />
              </div>
              <div class="re-field">
                <label class="re-field-label"><span class="re-req">*</span> 专业</label>
                <input v-model="edu.major" type="text" placeholder="如：计算机科学" class="re-input" />
              </div>
              <div class="re-field">
                <label class="re-field-label">学历</label>
                <select v-model="edu.degree" class="re-select">
                  <option value="">请选择</option>
                  <option value="大专">大专</option>
                  <option value="本科">本科</option>
                  <option value="硕士">硕士</option>
                  <option value="博士">博士</option>
                </select>
              </div>
            </div>
            <div class="re-form-row cols-2">
              <div class="re-field">
                <label class="re-field-label">入学时间</label>
                <input v-model="edu.startDate" type="month" class="re-input" />
              </div>
              <div class="re-field">
                <label class="re-field-label">毕业时间</label>
                <input v-model="edu.endDate" type="month" class="re-input" />
              </div>
            </div>
            <div class="re-form-row cols-1">
              <div class="re-field">
                <label class="re-field-label">经历描述</label>
                <textarea v-model="edu.description" rows="2" placeholder="主修课程、荣誉、绩点等" class="re-textarea"></textarea>
              </div>
            </div>
          </div>
          <button class="re-add-entry" @click="addEducation">
            <Plus class="w-3 h-3" /> 添加教育经历
          </button>
        </SectionCard>

        <!-- 工作经历 -->
        <SectionCard
          section-id="sec-work"
          :icon="Briefcase"
          icon-color="purple"
          title="工作经历"
          desc="用数据量化你的成果，HR 最关注「做了什么」和「效果如何」"
          :status="(form.works?.length ?? 0) > 0 ? 'complete' : 'empty'"
        >
          <div v-if="form.works!.length === 0" class="re-empty-tip">暂无工作经历，点击下方按钮添加</div>
          <div v-for="(w, idx) in form.works" :key="'work-'+idx" class="re-entry">
            <div class="re-entry-head">
              <span class="re-entry-badge"><span class="re-entry-dot"></span>工作经历 #{{ idx + 1 }}</span>
              <div class="re-entry-actions">
                <button class="re-entry-btn danger" @click="removeWork(idx)" title="删除">
                  <Trash2 class="w-3 h-3" />
                </button>
              </div>
            </div>
            <div class="re-form-row cols-2">
              <div class="re-field">
                <label class="re-field-label"><span class="re-req">*</span> 公司名称</label>
                <input v-model="w.company" type="text" placeholder="公司全称" class="re-input" />
              </div>
              <div class="re-field">
                <label class="re-field-label"><span class="re-req">*</span> 担任职位</label>
                <input v-model="w.position" type="text" placeholder="如：高级开发工程师" class="re-input" />
              </div>
            </div>
            <div class="re-form-row cols-2">
              <div class="re-field">
                <label class="re-field-label">入职时间</label>
                <input v-model="w.startDate" type="month" class="re-input" />
              </div>
              <div class="re-field">
                <label class="re-field-label">离职时间</label>
                <input v-model="w.endDate" type="month" class="re-input" />
              </div>
            </div>
            <div class="re-form-row cols-1">
              <div class="re-field">
                <label class="re-field-label"><span class="re-req">*</span> 工作描述</label>
                <textarea v-model="w.description" rows="4" placeholder="用「动词 + 量化结果」格式描述核心职责与业绩" class="re-textarea"></textarea>
                <div class="re-field-hint">推荐使用 STAR 法则：情境 → 任务 → 行动 → 结果</div>
              </div>
            </div>
          </div>
          <button class="re-add-entry" @click="addWork">
            <Plus class="w-3 h-3" /> 添加工作经历
          </button>
        </SectionCard>

        <!-- 项目经历 -->
        <SectionCard
          section-id="sec-project"
          :icon="FolderKanban"
          icon-color="amber"
          title="项目经历"
          desc="突出技术难点和你的核心贡献"
          :status="(form.projects?.length ?? 0) > 0 ? 'complete' : 'empty'"
        >
          <div v-if="form.projects!.length === 0" class="re-empty-tip">暂无项目经历，点击下方按钮添加</div>
          <div v-for="(p, idx) in form.projects" :key="'proj-'+idx" class="re-entry">
            <div class="re-entry-head">
              <span class="re-entry-badge"><span class="re-entry-dot"></span>项目 #{{ idx + 1 }}</span>
              <div class="re-entry-actions">
                <button class="re-entry-btn danger" @click="removeProject(idx)" title="删除">
                  <Trash2 class="w-3 h-3" />
                </button>
              </div>
            </div>
            <div class="re-form-row cols-2">
              <div class="re-field">
                <label class="re-field-label"><span class="re-req">*</span> 项目名称</label>
                <input v-model="p.name" type="text" placeholder="项目名称" class="re-input" />
              </div>
              <div class="re-field">
                <label class="re-field-label">担任角色</label>
                <input v-model="p.role" type="text" placeholder="如：技术负责人" class="re-input" />
              </div>
            </div>
            <div class="re-form-row cols-2">
              <div class="re-field">
                <label class="re-field-label">开始时间</label>
                <input v-model="p.startDate" type="month" class="re-input" />
              </div>
              <div class="re-field">
                <label class="re-field-label">结束时间</label>
                <input v-model="p.endDate" type="month" class="re-input" />
              </div>
            </div>
            <div class="re-form-row cols-1">
              <div class="re-field">
                <label class="re-field-label">项目链接</label>
                <input v-model="p.url" type="text" placeholder="如：https://github.com/..." class="re-input" />
              </div>
            </div>
            <div class="re-form-row cols-1">
              <div class="re-field">
                <label class="re-field-label"><span class="re-req">*</span> 项目描述</label>
                <textarea v-model="p.description" rows="4" placeholder="技术栈、职责与成果" class="re-textarea"></textarea>
              </div>
            </div>
          </div>
          <button class="re-add-entry" @click="addProject">
            <Plus class="w-3 h-3" /> 添加项目经历
          </button>
        </SectionCard>

        <!-- 专业技能 -->
        <SectionCard
          section-id="sec-skills"
          :icon="Terminal"
          icon-color="red"
          title="专业技能"
          desc="建议按熟练程度排列，点击 chip 可切换熟练度"
          :status="(form.skills?.length ?? 0) > 0 ? 'partial' : 'empty'"
        >
          <div class="re-skill-cloud">
            <span
              v-for="(s, idx) in form.skills"
              :key="'skill-'+idx"
              class="re-skill-chip"
              @click="cycleSkillLevel(idx)"
              :title="'点击切换熟练度（当前：'+(s.level||'熟练')+'）'"
            >
              {{ s.name }}
              <span v-if="s.level" class="re-skill-level">{{ s.level }}</span>
              <span class="re-skill-remove" @click.stop="removeSkill(idx)">
                <X class="w-2.5 h-2.5" />
              </span>
            </span>
          </div>
          <div class="re-skill-input-row">
            <input
              v-model="skillInput"
              type="text"
              placeholder="输入技能后按 Enter 添加，如：Spring Boot"
              class="re-input"
              @keydown="onSkillKeydown"
            />
            <button class="re-skill-add-btn" @click="addSkillFromInput">
              <Plus class="w-3.5 h-3.5" /> 添加
            </button>
          </div>
          <div class="re-field-hint">建议 5-10 个，点击 chip 可在「了解/一般/熟练/精通」间切换熟练度</div>
        </SectionCard>

        <!-- 自我评价 -->
        <SectionCard
          section-id="sec-eval"
          :icon="PenLine"
          icon-color="blue"
          title="自我评价"
          desc="用「数字 + 成果」代替空泛描述，突出差异化优势"
          :status="form.selfIntro?.trim() ? 'partial' : 'empty'"
        >
          <div class="re-field">
            <textarea
              v-model="form.selfIntro"
              rows="5"
              placeholder="100-300 字为宜，结构建议：定位 + 核心成果 + 技术深度 + 职业态度"
              class="re-textarea"
              maxlength="500"
            ></textarea>
            <div class="re-field-counter">{{ (form.selfIntro || '').length }} / 500</div>
          </div>
        </SectionCard>

        <!-- 上传简历 -->
        <SectionCard
          section-id="sec-upload"
          :icon="UploadCloud"
          icon-color="purple"
          title="上传简历"
          desc="上传已有简历，可同步至在线简历或直接用于 AI 优化"
          status="empty"
        >
          <div class="re-upload-zone">
            <div class="re-upload-icon">
              <UploadCloud class="w-5 h-5" />
            </div>
            <div class="re-upload-title">点击或拖拽文件到此处上传</div>
            <div class="re-upload-desc">上传后可预览、同步至在线简历，或直接进入 AI 优化</div>
            <div class="re-upload-formats">
              <span class="re-format-tag">PDF</span>
              <span class="re-format-tag">DOCX</span>
              <span class="re-format-tag">≤ 10MB</span>
            </div>
          </div>
          <div class="re-field-hint" style="margin-top: 8px;">
            <AlertCircle class="w-3 h-3 inline" />
            上传功能开发中，当前请使用在线简历编辑
          </div>
        </SectionCard>

        <!-- 底部留白，避免被 fixed action bar 遮挡 -->
        <div style="height: 80px;"></div>
      </main>

      <!-- 右侧评分面板 -->
      <ScorePanel
        :score="form.score"
        :score-detail="form.scoreDetail"
        :scored-time="form.scoredTime"
        :scoring="scoring"
        :optimize-count="optimizeCount"
        @optimize="handleOptimize"
        @rescore="handleScore"
      />
    </div>

    <!-- 底部固定操作栏 -->
    <ResumeActionBar
      :save-status="saveStatus"
      :saving="saving"
      :exporting="exporting"
      :has-id="!!form.id"
      @undo="handleUndo"
      @preview="previewVisible = true"
      @download="handleExportPdf"
      @save="handleSaveDraft"
      @optimize="handleOptimize"
    />

    <!-- 预览弹窗 -->
    <ResumePreviewModal
      :visible="previewVisible"
      :form="form"
      :completeness="resumeCompleteness"
      :exporting="exporting"
      @close="previewVisible = false"
      @export-pdf="handleExportPdf"
    />

    <!-- AI 建议弹窗 -->
    <Teleport to="body">
      <div v-if="adviceVisible" class="re-advice-mask" @click.self="adviceVisible = false">
        <div class="re-advice-box">
          <!-- 头部 -->
          <div class="re-advice-head">
            <h3>
              <Sparkles class="w-4 h-4" style="color: var(--theme-primary);" />
              AI 深度优化建议
              <span v-if="aiAdvice?.grade" class="re-advice-grade" :class="gradeStyle[aiAdvice.grade] || gradeStyle.D">
                等级 {{ aiAdvice.grade }}
              </span>
            </h3>
            <div class="re-advice-head-actions">
              <button
                class="re-advice-refresh-btn"
                :disabled="adviceLoading"
                @click="handleGetAdvice"
              >{{ adviceLoading ? '刷新中...' : '刷新建议' }}</button>
              <button class="re-advice-close" @click="adviceVisible = false">
                <X class="w-4 h-4" />
              </button>
            </div>
          </div>

          <!-- 加载中 -->
          <div v-if="adviceLoading && !aiAdvice" class="re-advice-loading">
            <div class="re-loading-spinner"></div>
            <p>正在生成 AI 优化建议...</p>
          </div>

          <!-- 内容 -->
          <div v-else-if="aiAdvice" class="re-advice-body">
            <!-- 总结 -->
            <div v-if="aiAdvice.summary" class="re-advice-summary">
              {{ aiAdvice.summary }}
            </div>

            <!-- 缺失技能 -->
            <div v-if="aiAdvice.missingSkills && aiAdvice.missingSkills.length > 0" class="re-advice-missing">
              <div class="re-advice-missing-title">
                <AlertCircle class="w-3.5 h-3.5" />
                岗位必备技能缺失（{{ aiAdvice.missingSkills.length }} 项）
              </div>
              <div class="re-advice-missing-chips">
                <span v-for="skill in aiAdvice.missingSkills" :key="skill" class="re-advice-missing-chip">{{ skill }}</span>
              </div>
            </div>

            <!-- 建议列表 -->
            <div v-if="aiAdvice.advices && aiAdvice.advices.length > 0" class="re-advice-list">
              <div v-for="(advice, idx) in aiAdvice.advices" :key="idx" class="re-advice-item">
                <div class="re-advice-item-head">
                  <span class="re-advice-dim">{{ advice.dimension || '综合' }}</span>
                  <span v-if="advice.priority && priorityStyle[advice.priority]" class="re-advice-pri" :class="priorityStyle[advice.priority].class">
                    {{ priorityStyle[advice.priority].label }}
                  </span>
                  <span v-if="advice.type && adviceTypeLabel[advice.type]" class="re-advice-type">
                    {{ adviceTypeLabel[advice.type] }}
                  </span>
                </div>
                <p class="re-advice-content">{{ advice.content }}</p>
                <button
                  v-if="acceptedAdvices.has(idx)"
                  disabled
                  class="re-advice-accepted"
                >
                  <CheckCircle2 class="w-3 h-3" /> 已采纳
                </button>
                <button
                  v-else
                  class="re-advice-accept-btn"
                  @click="acceptAdvice(advice, idx)"
                >
                  <Plus class="w-3 h-3" /> 采纳
                </button>
              </div>
            </div>
            <div v-else class="re-advice-empty">
              <CheckCircle2 class="w-5 h-5" style="color: #16a34a;" />
              各维度得分率良好，暂无改进建议
            </div>

            <!-- 采纳后提示 -->
            <div v-if="acceptedAdvices.size > 0" class="re-advice-accepted-tip">
              <span><CheckCircle2 class="w-3.5 h-3.5 inline" /> 已采纳 {{ acceptedAdvices.size }} 条建议到「自我评价」</span>
              <button @click="adviceVisible = false" class="re-advice-back-edit">去编辑检查</button>
            </div>
          </div>

          <!-- 底部：建立学习计划 -->
          <div v-if="aiAdvice" class="re-advice-footer">
            <div>
              <div class="re-advice-ft-title">针对短板生成学习计划</div>
              <div class="re-advice-ft-desc">基于薄弱点自动生成针对性学习计划</div>
            </div>
            <button @click="gotoStudyPlan" class="re-advice-study-btn">
              去建立学习计划 <ArrowRight class="w-3.5 h-3.5" />
            </button>
          </div>
        </div>
      </div>
    </Teleport>

    <SiteFooter />
  </div>
</template>

<style scoped>
.re-page {
  min-height: 100vh;
}

/* ===== 吸顶栏 ===== */
.re-topbar {
  position: sticky;
  top: 0;
  z-index: 30;
  background: rgba(255,255,255,0.92);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  border-bottom: 1px solid var(--theme-border);
}
.re-topbar-inner {
  max-width: 1280px;
  margin: 0 auto;
  padding: 12px 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
}
.re-topbar-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex: 1;
  justify-content: flex-end;
}
.re-title-input {
  flex: 1;
  max-width: 360px;
  padding: 6px 12px;
  font-size: 13px;
  border: 1px solid var(--theme-border);
  border-radius: 8px;
  outline: none;
  background: #fff;
  color: var(--theme-text);
  transition: border-color 0.15s, box-shadow 0.15s;
}
.re-title-input:focus {
  border-color: var(--theme-primary);
  box-shadow: 0 0 0 3px rgba(220,38,38,0.08);
}
.re-save-badge {
  font-size: 12px;
  padding: 3px 10px;
  border-radius: 10px;
  font-weight: 600;
  white-space: nowrap;
}
.re-save-badge.saving { background: #fffbeb; color: #d97706; }
.re-save-badge.saved { background: #ecfdf5; color: #059669; }

/* ===== 加载 / 错误 ===== */
.re-loading, .re-error {
  max-width: 1280px;
  margin: 0 auto;
  padding: 80px 24px;
  text-align: center;
  background: #fff;
  border: 1px solid var(--theme-border);
  border-radius: 14px;
  margin: 24px auto;
}
.re-loading p, .re-error p { margin-top: 12px; color: #6b7280; font-size: 14px; }
.re-loading-spinner {
  width: 36px;
  height: 36px;
  border: 3px solid var(--theme-border);
  border-top-color: var(--theme-primary);
  border-radius: 50%;
  animation: re-spin 0.8s linear infinite;
  margin: 0 auto;
}
@keyframes re-spin { to { transform: rotate(360deg); } }
.re-retry-btn {
  margin-top: 16px;
  padding: 8px 20px;
  background: var(--theme-primary);
  color: #fff;
  border: none;
  border-radius: 8px;
  font-size: 13px;
  cursor: pointer;
}
.re-retry-btn:hover { background: #b91c1c; }

/* ===== 三栏布局 ===== */
.re-layout {
  display: flex;
  max-width: 1280px;
  margin: 0 auto;
  min-height: calc(100vh - 60px);
}

/* 中间主区 */
.re-main {
  flex: 1;
  padding: 24px 32px 0;
  min-width: 0;
}

/* ===== 表单元素 ===== */
.re-form-row { display: grid; gap: 16px; margin-bottom: 16px; }
.re-form-row.cols-1 { grid-template-columns: 1fr; }
.re-form-row.cols-2 { grid-template-columns: 1fr 1fr; }
.re-form-row.cols-3 { grid-template-columns: 1fr 1fr 1fr; }
.re-form-row:last-child { margin-bottom: 0; }

.re-field { display: flex; flex-direction: column; gap: 5px; }
.re-field-label {
  font-size: 13px;
  font-weight: 600;
  color: var(--theme-text);
  display: flex;
  align-items: center;
  gap: 4px;
}
.re-req { color: var(--theme-primary); font-size: 14px; line-height: 1; }
.re-field-hint {
  font-size: 11px;
  color: #9ca3af;
  line-height: 1.5;
  display: flex;
  align-items: flex-start;
  gap: 4px;
}
.re-field-counter { font-size: 11px; color: #9ca3af; text-align: right; }

.re-input, .re-select, .re-textarea {
  width: 100%;
  padding: 9px 12px;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  font-size: 13px;
  transition: all 0.15s cubic-bezier(0.4, 0, 0.2, 1);
  outline: none;
  background: #fff;
  color: var(--theme-text);
  font-family: inherit;
}
.re-input:hover, .re-select:hover, .re-textarea:hover { border-color: #9ca3af; }
.re-input:focus, .re-select:focus, .re-textarea:focus {
  border-color: var(--theme-primary);
  box-shadow: 0 0 0 3px rgba(220,38,38,0.08);
}
.re-input::placeholder, .re-textarea::placeholder { color: #9ca3af; }
.re-textarea { resize: vertical; min-height: 80px; line-height: 1.65; }
.re-select { appearance: none; cursor: pointer; padding-right: 32px;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='12' viewBox='0 0 12 12'%3E%3Cpath d='M3 4.5L6 7.5L9 4.5' stroke='%239CA3AF' stroke-width='1.5' fill='none' stroke-linecap='round'/%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: right 12px center;
}

/* ===== 经历条目 ===== */
.re-entry {
  border: 1px solid var(--theme-border);
  border-radius: 10px;
  padding: 18px;
  margin-bottom: 12px;
  transition: all 0.2s;
  background: #fff;
}
.re-entry:hover { border-color: #d1d5db; box-shadow: 0 1px 3px rgba(0,0,0,0.06); }
.re-entry-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
}
.re-entry-badge {
  font-size: 11px;
  font-weight: 600;
  color: #6b7280;
  display: flex;
  align-items: center;
  gap: 6px;
}
.re-entry-dot { width: 6px; height: 6px; border-radius: 50%; background: #10b981; }
.re-entry-actions { display: flex; gap: 4px; }
.re-entry-btn {
  width: 28px; height: 28px;
  display: flex; align-items: center; justify-content: center;
  border: none; border-radius: 6px;
  cursor: pointer; font-size: 11px;
  background: transparent;
  color: #9ca3af;
  transition: all 0.15s;
}
.re-entry-btn:hover { background: #f3f4f6; color: var(--theme-text); }
.re-entry-btn.danger:hover { background: #fee2e2; color: var(--theme-primary); }

.re-add-entry {
  width: 100%;
  padding: 10px;
  border: 1px dashed #d1d5db;
  border-radius: 10px;
  background: transparent;
  color: #6b7280;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  font-weight: 500;
}
.re-add-entry:hover {
  border-color: var(--theme-primary);
  color: var(--theme-primary);
  background: #fef2f2;
}

.re-empty-tip {
  font-size: 13px;
  color: #9ca3af;
  text-align: center;
  padding: 12px;
}

/* ===== 技能 chip cloud ===== */
.re-skill-cloud {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 10px;
  min-height: 28px;
}
.re-skill-chip {
  padding: 4px 8px 4px 12px;
  background: #f3f4f6;
  color: var(--theme-text);
  border-radius: 16px;
  font-size: 12px;
  display: flex;
  align-items: center;
  gap: 6px;
  transition: all 0.15s;
  border: 1px solid var(--theme-border);
  font-weight: 500;
  cursor: pointer;
  user-select: none;
}
.re-skill-chip:hover {
  border-color: #fecaca;
  background: #fef2f2;
}
.re-skill-level {
  font-size: 10px;
  color: #9ca3af;
  font-weight: 400;
}
.re-skill-remove {
  width: 14px; height: 14px;
  display: flex; align-items: center; justify-content: center;
  border-radius: 50%;
  cursor: pointer;
  color: #9ca3af;
  transition: all 0.15s;
}
.re-skill-remove:hover { background: var(--theme-primary); color: #fff; }
.re-skill-input-row {
  display: flex;
  gap: 6px;
}
.re-skill-add-btn {
  padding: 8px 14px;
  background: #fff;
  border: 1px solid #d1d5db;
  color: var(--theme-text);
  border-radius: 8px;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  transition: all 0.15s;
  white-space: nowrap;
}
.re-skill-add-btn:hover {
  border-color: var(--theme-primary);
  color: var(--theme-primary);
  background: #fef2f2;
}

/* ===== 上传区 ===== */
.re-upload-zone {
  border: 2px dashed #d1d5db;
  border-radius: 14px;
  padding: 36px 24px;
  text-align: center;
  cursor: pointer;
  transition: all 0.25s;
  background: #fff;
}
.re-upload-zone:hover {
  border-color: var(--theme-primary);
  background: #fef2f2;
}
.re-upload-icon {
  width: 48px; height: 48px;
  background: #f3f4f6;
  border-radius: 14px;
  display: flex; align-items: center; justify-content: center;
  margin: 0 auto 14px;
  color: #9ca3af;
  transition: all 0.2s;
}
.re-upload-zone:hover .re-upload-icon {
  background: #fef2f2;
  color: var(--theme-primary);
}
.re-upload-title { font-size: 14px; font-weight: 600; color: var(--theme-text); margin-bottom: 4px; }
.re-upload-desc { font-size: 12px; color: #9ca3af; }
.re-upload-formats { display: flex; gap: 6px; justify-content: center; margin-top: 12px; }
.re-format-tag {
  font-size: 10px;
  padding: 2px 8px;
  background: #f3f4f6;
  color: #6b7280;
  border-radius: 4px;
  font-weight: 600;
}

/* ===== AI 建议弹窗 ===== */
.re-advice-mask {
  position: fixed;
  inset: 0;
  background: rgba(0,0,0,0.4);
  backdrop-filter: blur(6px);
  -webkit-backdrop-filter: blur(6px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  animation: re-advice-fade 0.2s;
  padding: 20px;
}
@keyframes re-advice-fade { from { opacity: 0; } to { opacity: 1; } }
.re-advice-box {
  background: #fff;
  border-radius: 20px;
  width: 100%;
  max-width: 640px;
  max-height: 85vh;
  overflow-y: auto;
  box-shadow: 0 20px 25px -5px rgba(0,0,0,0.08), 0 8px 10px -6px rgba(0,0,0,0.04);
  animation: re-advice-slide 0.25s cubic-bezier(0.34, 1.56, 0.64, 1);
}
@keyframes re-advice-slide { from { opacity: 0; transform: translateY(12px); } to { opacity: 1; transform: translateY(0); } }
.re-advice-head {
  padding: 18px 24px;
  border-bottom: 1px solid var(--theme-border);
  display: flex;
  align-items: center;
  justify-content: space-between;
  position: sticky;
  top: 0;
  background: #fff;
  z-index: 1;
  border-radius: 20px 20px 0 0;
}
.re-advice-head h3 {
  font-size: 15px;
  font-weight: 700;
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--theme-text);
}
.re-advice-grade {
  margin-left: 8px;
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 10px;
  font-weight: 600;
}
.re-advice-head-actions { display: flex; align-items: center; gap: 8px; }
.re-advice-refresh-btn {
  padding: 5px 12px;
  font-size: 12px;
  border-radius: 6px;
  background: #f9fafb;
  color: #6b7280;
  border: 1px solid var(--theme-border);
  cursor: pointer;
  transition: all 0.15s;
}
.re-advice-refresh-btn:hover:not(:disabled) { border-color: var(--theme-primary); color: var(--theme-primary); }
.re-advice-refresh-btn:disabled { opacity: 0.5; cursor: not-allowed; }
.re-advice-close {
  width: 32px; height: 32px;
  display: flex; align-items: center; justify-content: center;
  border-radius: 8px;
  border: 1px solid var(--theme-border);
  background: #fff;
  color: #6b7280;
  cursor: pointer;
}
.re-advice-close:hover { border-color: #d1d5db; background: #f9fafb; color: var(--theme-text); }

.re-advice-loading {
  padding: 60px 24px;
  text-align: center;
  color: #6b7280;
  font-size: 14px;
}
.re-advice-body { padding: 20px 24px; }

.re-advice-summary {
  background: #f9fafb;
  border-radius: 10px;
  padding: 12px 14px;
  font-size: 13px;
  color: var(--theme-text);
  line-height: 1.6;
  margin-bottom: 14px;
}
.re-advice-missing {
  background: #fef2f2;
  border: 1px solid #fecaca;
  border-radius: 10px;
  padding: 12px 14px;
  margin-bottom: 14px;
}
.re-advice-missing-title {
  font-size: 12px;
  font-weight: 600;
  color: var(--theme-primary);
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 8px;
}
.re-advice-missing-chips { display: flex; flex-wrap: wrap; gap: 6px; }
.re-advice-missing-chip {
  font-size: 11px;
  padding: 2px 10px;
  background: #fee2e2;
  color: var(--theme-primary);
  border: 1px solid #fecaca;
  border-radius: 12px;
}

.re-advice-list { display: flex; flex-direction: column; gap: 10px; }
.re-advice-item {
  background: #f9fafb;
  border-radius: 10px;
  padding: 12px 14px;
  position: relative;
}
.re-advice-item-head {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 6px;
  flex-wrap: wrap;
}
.re-advice-dim { font-size: 12px; font-weight: 600; color: var(--theme-text); }
.re-advice-pri { font-size: 11px; padding: 1px 8px; border-radius: 8px; font-weight: 600; }
.re-advice-type {
  font-size: 11px;
  padding: 1px 8px;
  border-radius: 8px;
  background: #fff;
  color: #6b7280;
  border: 1px solid var(--theme-border);
}
.re-advice-content {
  font-size: 13px;
  color: #4b5563;
  line-height: 1.6;
  padding-right: 80px;
}
.re-advice-accept-btn {
  position: absolute;
  top: 12px;
  right: 12px;
  font-size: 11px;
  padding: 4px 10px;
  border-radius: 6px;
  background: var(--theme-primary);
  color: #fff;
  border: none;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 3px;
  font-weight: 600;
  transition: all 0.15s;
}
.re-advice-accept-btn:hover { background: #b91c1c; }
.re-advice-accepted {
  position: absolute;
  top: 12px;
  right: 12px;
  font-size: 11px;
  padding: 4px 10px;
  border-radius: 6px;
  background: rgba(22,163,74,0.1);
  color: #16a34a;
  border: 1px solid rgba(22,163,74,0.2);
  display: inline-flex;
  align-items: center;
  gap: 3px;
  font-weight: 600;
}
.re-advice-empty {
  text-align: center;
  padding: 24px;
  color: #16a34a;
  font-size: 13px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
}
.re-advice-accepted-tip {
  margin-top: 14px;
  padding: 10px 14px;
  background: rgba(22,163,74,0.06);
  border: 1px solid rgba(22,163,74,0.2);
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 12px;
  color: #16a34a;
}
.re-advice-back-edit {
  font-size: 11px;
  padding: 3px 10px;
  border-radius: 6px;
  background: var(--theme-primary);
  color: #fff;
  border: none;
  cursor: pointer;
  font-weight: 600;
}

.re-advice-footer {
  padding: 16px 24px;
  background: linear-gradient(135deg, var(--theme-primary), #7c3aed);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  border-radius: 0 0 20px 20px;
  flex-wrap: wrap;
}
.re-advice-ft-title { font-size: 14px; font-weight: 700; }
.re-advice-ft-desc { font-size: 12px; opacity: 0.9; margin-top: 2px; }
.re-advice-study-btn {
  padding: 8px 16px;
  background: #fff;
  color: var(--theme-primary);
  border: none;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 4px;
}
.re-advice-study-btn:hover { opacity: 0.9; }

/* ===== 响应式 ===== */
@media (max-width: 768px) {
  .re-main { padding: 16px 14px 0; }
  .re-form-row.cols-2, .re-form-row.cols-3 { grid-template-columns: 1fr; }
  .re-topbar-inner { padding: 10px 14px; }
  .re-title-input { max-width: none; }
}
</style>
