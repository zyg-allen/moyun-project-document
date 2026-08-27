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
  ArrowRight, PenLine, UploadCloud, Terminal, FolderKanban, X, ShieldCheck,
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
  getMyResumeList, parseResumeAttachment,
} from '@/api/interview';
import { useDictData } from '@/composables/useDictData';
import type { ResumeParseVO } from '@/types/api';
import { getCurrentUser } from '@/api/user';
import { getMyCertification, type CreatorCertification } from '@/api/certification';
import { aiFieldAssist, type FieldAssistSuggestion } from '@/api/resumeOptimize';
import { uploadFile } from '@/api/upload';
import { getToken } from '@/api/client';
import type {
  UserResumeVO, UserResumeJobIntention, UserResumeEducationItem, UserResumeWorkItem,
  UserResumeProjectItem, UserResumeSkillItem, UserResumeScoreItem,
  ResumeAiAdviceVO, ResumeAiAdviceItem,
} from '@/types/api';
import { useToast } from '@/composables/useToast';

const route = useRoute();
const router = useRouter();
const toast = useToast();

const editId = computed(() => route.params.id as string | undefined);
const isEdit = computed(() => !!editId.value);

// 到岗时间字典下拉（portal_available_time；字典未配置时用本地默认兜底，值为文本可直接入库）
const dictMap = useDictData(['portal_available_time']);
const AVAILABLE_TIME_FALLBACK = ['随时到岗', '一周内到岗', '两周内到岗', '一个月内到岗', '三个月内到岗', '面议'];
const availableTimeOptions = computed(() => {
  const dictItems = dictMap['portal_available_time'] || [];
  const opts = dictItems.length > 0
    ? dictItems.map((d) => d.dictLabel)
    : AVAILABLE_TIME_FALLBACK;
  // 历史存量值不在字典中时动态补入，保证反显不丢失
  const current = ensureJobIntention().availableTime?.trim();
  if (current && !opts.includes(current)) opts.unshift(current);
  return opts;
});

// 无 :id 进入时若已写过简历，反显最新一版续编（form.id 带上后续保存即为更新）
const hasReflectedResume = ref(false);

const pageTitle = computed(() => (isEdit.value || hasReflectedResume.value) ? '编辑简历' : '创建简历');

// 加载 / 状态
const loadingDetail = ref(false);
const pageError = ref<string | null>(null);
const exporting = ref(false);
const scoring = ref(false);
const adviceLoading = ref(false);
const aiAdvice = ref<ResumeAiAdviceVO | null>(null);
const saveStatus = ref<'idle' | 'saving' | 'saved' | 'dirty'>('idle');
const saving = ref(false);
const loaded = ref(false);

// 弹窗控制：预览弹窗 / AI 建议弹窗
const previewVisible = ref(false);
const adviceVisible = ref(false);

// 已采纳建议索引集合
const acceptedAdvices = ref<Set<string>>(new Set());

// 左侧导航当前高亮项（scroll spy）
const activeSection = ref('sec-personal');
const mainScrollRef = ref<HTMLElement | null>(null);

// 技能输入框（chip cloud 添加）
const skillInput = ref('');

// 头像上传
const avatarUploading = ref(false);
const avatarInputRef = ref<HTMLInputElement | null>(null);

function triggerAvatarUpload() {
  avatarInputRef.value?.click();
}

function onAvatarChange(e: Event) {
  const input = e.target as HTMLInputElement;
  const file = input.files?.[0];
  if (!file) return;
  // 仅允许图片类型
  if (!file.type.startsWith('image/')) {
    toast.error('请选择图片文件');
    input.value = '';
    return;
  }
  avatarUploading.value = true;
  uploadFile(file, { module: 'resume' })
    .then((res) => {
      if (res.code === 200 && res.data?.fileUrl) {
        form.avatar = res.data.fileUrl;
        toast.success('头像上传成功');
      } else {
        toast.error(res.message || '头像上传失败');
      }
    })
    .catch((err) => {
      toast.error((err as Error)?.message || '头像上传失败');
    })
    .finally(() => {
      avatarUploading.value = false;
      input.value = ''; // 允许重复选择同一文件
    });
}

function removeAvatar() {
  form.avatar = '';
}

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
      activeAdviceTab.value = 'all';
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
  high: { label: '高优先级', class: 'bg-theme-danger-bg text-theme-danger' },
  medium: { label: '中优先级', class: 'bg-theme-warning-bg text-theme-warning' },
  low: { label: '低优先级', class: 'bg-theme-surface text-theme-text-secondary' },
};
const adviceTypeLabel: Record<string, string> = { fill: '补充缺失', refine: '优化已有', match: '岗位匹配' };
const gradeStyle: Record<string, string> = {
  A: 'bg-theme-success-bg text-theme-success',
  B: 'bg-theme-info-bg text-theme-info',
  C: 'bg-theme-warning-bg text-theme-warning',
  D: 'bg-theme-danger-bg text-theme-danger',
};

// ============ AI 建议：模块 Tab + 采纳自动填充（v10.11 对齐原型） ============

// 维度元信息：Tab 显示名 / 页面锚点（无 optimized 的结构化字段引导手动完善）
const DIMENSION_META: Record<string, { label: string; anchor: string }> = {
  '基本信息': { label: '基本信息', anchor: 'sec-personal' },
  '求职意向': { label: '求职意向', anchor: 'sec-objective' },
  '教育经历': { label: '教育背景', anchor: 'sec-education' },
  '工作经历': { label: '工作经历', anchor: 'sec-work' },
  '项目经历': { label: '项目经历', anchor: 'sec-project' },
  '技能列表': { label: '专业技能', anchor: 'sec-skills' },
  '自我介绍': { label: '自我评价', anchor: 'sec-eval' },
  '岗位匹配度': { label: '岗位匹配', anchor: 'sec-skills' },
};
// Tab 固定顺序（含全部）
const ADVICE_TAB_ORDER = ['基本信息', '求职意向', '教育经历', '工作经历', '项目经历', '技能列表', '自我介绍', '岗位匹配度'];

// 当前激活 Tab（'all' = 全部）
const activeAdviceTab = ref('all');

// 出现过建议的维度 Tab（固定顺序过滤）
const adviceTabs = computed(() => {
  const dims = new Set((aiAdvice.value?.advices || []).map(a => a.dimension || ''));
  return [
    { value: 'all', label: `全部 (${(aiAdvice.value?.advices || []).length})` },
    ...ADVICE_TAB_ORDER.filter(d => dims.has(d)).map(d => ({
      value: d,
      label: DIMENSION_META[d]?.label || d,
    })),
  ];
});

// 当前 Tab 过滤后的建议列表
const filteredAdvices = computed(() => {
  const list = aiAdvice.value?.advices || [];
  return activeAdviceTab.value === 'all'
    ? list
    : list.filter(a => (a.dimension || '') === activeAdviceTab.value);
});

// 建议唯一 key（Tab 过滤后索引不稳定，用内容做 key）
function adviceKey(a: ResumeAiAdviceItem): string {
  return `${a.dimension || ''}|${(a.content || '').slice(0, 24)}`;
}

// 从 scoreDetail 取维度得分（用于卡片徽章）
function dimensionScore(dim: string): { score: number; maxScore: number; rate: number } | null {
  const item = (form.scoreDetail || []).find(s => s.item === dim);
  if (!item || !item.maxScore) return null;
  return { score: item.score, maxScore: item.maxScore, rate: item.score / item.maxScore };
}

// 维度得分徽章颜色
function scoreBadgeClass(rate: number): string {
  if (rate >= 0.8) return 'bg-theme-success-bg text-theme-success';
  if (rate >= 0.6) return 'bg-theme-info-bg text-theme-info';
  if (rate >= 0.4) return 'bg-theme-warning-bg text-theme-warning';
  return 'bg-theme-danger-bg text-theme-danger';
}

// 维度评分条颜色（按得分率分级）
function metricColor(rate: number): string {
  if (rate >= 0.8) return 'var(--theme-success)';
  if (rate >= 0.6) return 'var(--theme-info)';
  if (rate >= 0.4) return 'var(--theme-warning)';
  return 'var(--theme-danger)';
}

/**
 * 解析 optimized 技能文本并追加到技能列表（去重）
 * 格式："精通：A、B\n熟练：C\n了解：D、E" 或纯顿号列表（默认"了解"）
 */
function appendSkillsFromText(text: string): number {
  const existing = new Set((form.skills || []).map(s => (s.name || '').trim()).filter(Boolean));
  let added = 0;
  for (const rawLine of text.split('\n')) {
    const line = rawLine.trim();
    if (!line) continue;
    let level = '了解';
    let namesPart = line;
    // "精通：/熟练：/了解："前缀 → level
    const m = line.match(/^(精通|熟练|了解|一般)[:：]\s*(.+)$/);
    if (m) {
      level = m[1];
      namesPart = m[2];
    }
    for (const raw of namesPart.split(/[、,，;；\/]/)) {
      const name = raw.trim().replace(/[。.\s]+$/, '');
      if (!name || name.length > 30 || existing.has(name) || /^\[.*\]$/.test(name)) continue;
      (form.skills = form.skills || []).push({ name, level, category: '' });
      existing.add(name);
      added++;
    }
  }
  return added;
}

/**
 * 采纳建议 —— optimized 按维度映射自动填充到对应表单字段
 * <p>字段映射（注意与简历表单结构对齐）：
 * <ul>
 *   <li>自我介绍 → form.selfIntro（整体替换）</li>
 *   <li>教育经历 → educations[0].description（无记录则引导先添加）</li>
 *   <li>工作经历 → works[0].description（同上）</li>
 *   <li>项目经历 → projects[0].description（同上）</li>
 *   <li>技能列表 / 岗位匹配度 → 解析文本追加 skills 条目（去重，含熟练度分级）</li>
 *   <li>基本信息 / 求职意向 → 结构化字段无法文本填充，滚动到对应区块手动完善</li>
 * </ul></p>
 */
function acceptAdvice(advice: ResumeAiAdviceItem) {
  const key = adviceKey(advice);
  if (acceptedAdvices.value.has(key)) {
    toast.info('该建议已采纳');
    return;
  }
  const dim = advice.dimension || '';
  const meta = DIMENSION_META[dim];
  const text = advice.optimized?.trim();

  // 无优化文本（结构化字段维度）：引导到对应区块手动完善
  if (!text) {
    if (meta) scrollToSection(meta.anchor);
    adviceVisible.value = false;
    toast.info('该维度需在表单中手动完善，已为您定位到对应区块');
    return;
  }

  switch (dim) {
    case '自我介绍':
      form.selfIntro = text;
      break;
    case '教育经历':
      if (!form.educations?.length) {
        toast.error('请先添加一条教育背景记录，再采纳该建议');
        scrollToSection('sec-education');
        adviceVisible.value = false;
        return;
      }
      form.educations[0].description = text;
      break;
    case '工作经历':
      if (!form.works?.length) {
        toast.error('请先添加一条工作经历记录，再采纳该建议');
        scrollToSection('sec-work');
        adviceVisible.value = false;
        return;
      }
      form.works[0].description = text;
      break;
    case '项目经历':
      if (!form.projects?.length) {
        toast.error('请先添加一条项目经历记录，再采纳该建议');
        scrollToSection('sec-project');
        adviceVisible.value = false;
        return;
      }
      form.projects[0].description = text;
      break;
    case '技能列表':
    case '岗位匹配度': {
      const added = appendSkillsFromText(text);
      if (added === 0) {
        toast.info('优化文本暂无可识别的技能条目，请手动复制到技能列表');
        return;
      }
      toast.success(`已追加 ${added} 项技能到「专业技能」`);
      acceptedAdvices.value.add(key);
      return;
    }
    default:
      // 未知维度兜底：追加到自我评价
      form.selfIntro = (form.selfIntro || '') + (form.selfIntro?.trim() ? '\n\n' : '') + text;
  }
  acceptedAdvices.value.add(key);
  toast.success(`已填充到「${meta?.label || dim}」`);
  // v10.14 修复断裂点「AI建议采纳后未联动保存」：已保存过的简历自动静默保存
  autoSaveAfterAdopt();
}

/** 采纳 AI 建议后联动保存：已有 id 的简历静默保存；新建简历靠 dirty 提示兜底 */
function autoSaveAfterAdopt() {
  if (form.id) {
    doSave(true);
  }
}

// ============ AI 实时辅助编辑（v10.14 设计文档 P0 需求#2） ============
// 字段级 AI 优化：工作/项目描述、自我评价旁「✨AI优化」→ 3 个差异化版本 → 采纳替换

const assistVisible = ref(false);
const assistLoading = ref(false);
const assistSuggestions = ref<FieldAssistSuggestion[]>([]);
/** 采纳写入目标：type + 列表索引（selfIntro 为标量） */
const assistTarget = ref<{ type: 'work' | 'project' | 'selfIntro'; index: number } | null>(null);
/** 辅助目标的当前原文（弹窗中展示） */
const assistOriginal = ref('');

const ASSIST_VERSION_LABELS = ['版本 1 · 成果量化（推荐）', '版本 2 · 技术深度', '版本 3 · 业务价值'];

async function openFieldAssist(type: 'work' | 'project' | 'selfIntro', index: number) {
  let text = '';
  let field: 'work_description' | 'project_description' | 'self_intro';
  if (type === 'work') {
    text = form.works?.[index]?.description || '';
    field = 'work_description';
  } else if (type === 'project') {
    text = form.projects?.[index]?.description || '';
    field = 'project_description';
  } else {
    text = form.selfIntro || '';
    field = 'self_intro';
  }
  if (!text.trim()) {
    toast.error('请先输入内容，AI 才能帮你优化');
    return;
  }
  assistTarget.value = { type, index };
  assistOriginal.value = text;
  assistVisible.value = true;
  assistLoading.value = true;
  assistSuggestions.value = [];
  try {
    const res = await aiFieldAssist({
      field,
      originalText: text,
      position: form.jobIntention?.position || undefined,
      skillNames: form.skills?.map(s => s.name).filter(Boolean),
    });
    if (res.code === 200 && res.data) {
      assistSuggestions.value = res.data;
    } else {
      toast.error(res.message || 'AI 辅助生成失败');
    }
  } catch (err: any) {
    toast.error(err?.message || 'AI 辅助生成失败，请稍后重试');
  } finally {
    assistLoading.value = false;
  }
}

/** 采纳版本：替换目标字段内容并联动保存 */
function adoptAssist(text: string) {
  const t = assistTarget.value;
  if (!t) return;
  if (t.type === 'work') {
    form.works![t.index].description = text;
  } else if (t.type === 'project') {
    form.projects![t.index].description = text;
  } else {
    form.selfIntro = text;
  }
  assistVisible.value = false;
  toast.success('已替换为 AI 优化版本');
  autoSaveAfterAdopt();
}

// 一键把缺失技能加入技能清单（level=了解，避免虚标）
function addMissingSkills() {
  const skills = aiAdvice.value?.missingSkills || [];
  if (!skills.length) return;
  const text = '了解：' + skills.join('、');
  const added = appendSkillsFromText(text);
  toast.success(added > 0 ? `已将 ${added} 项缺失技能加入「专业技能」（熟练度：了解）` : '缺失技能均已存在');
}

// ============ 附件简历：上传 + 解析 + 覆盖填充（v10.12） ============

const ACCEPT_EXTS = ['.pdf', '.doc', '.docx', '.txt', '.md'];
const UPLOAD_MAX_SIZE = 10 * 1024 * 1024; // 10MB（与后端一致）

const attachmentInput = ref<HTMLInputElement | null>(null);
const uploading = ref(false);
const parsing = ref(false);
const dragOver = ref(false);
/** 已上传附件信息（本地状态；fileUrl 持久化在 form.fileUrl） */
const attachment = ref<{ name: string; size: number; file: File | null; fileUrl: string } | null>(null);
/** 解析结果预览（确认后覆盖填充） */
const parseResult = ref<ResumeParseVO | null>(null);
const parsePreviewVisible = ref(false);

// 文件校验：类型 + 大小
function validateFile(file: File): string | null {
  const ext = '.' + (file.name.split('.').pop() || '').toLowerCase();
  if (!ACCEPT_EXTS.includes(ext)) {
    return `不支持的文件类型 ${ext}，仅支持 PDF / Word / TXT / Markdown`;
  }
  if (file.size > UPLOAD_MAX_SIZE) {
    return `文件超过 10MB（当前 ${(file.size / 1024 / 1024).toFixed(1)}MB）`;
  }
  return null;
}

// 上传附件（点击 / 拖拽统一入口）
async function handleAttachmentFile(file: File) {
  const err = validateFile(file);
  if (err) {
    toast.error(err);
    return;
  }
  try {
    uploading.value = true;
    const res = await uploadFile(file, { module: 'resume' });
    if (res.code === 200 && res.data?.fileUrl) {
      form.fileUrl = res.data.fileUrl;
      attachment.value = { name: file.name, size: file.size, file, fileUrl: res.data.fileUrl };
      toast.success('附件上传成功，可解析后同步至在线简历');
    } else {
      toast.error(res.message || '附件上传失败');
    }
  } catch (e) {
    toast.error((e as Error)?.message || '附件上传失败');
  } finally {
    uploading.value = false;
  }
}

function onAttachmentChange(e: Event) {
  const input = e.target as HTMLInputElement;
  const file = input.files?.[0];
  if (file) handleAttachmentFile(file);
  input.value = ''; // 允许重复选择同一文件
}

function onDrop(e: DragEvent) {
  dragOver.value = false;
  const file = e.dataTransfer?.files?.[0];
  if (file) handleAttachmentFile(file);
}

function removeAttachment() {
  attachment.value = null;
  form.fileUrl = '';
}

// 文件大小格式化
function fmtSize(bytes: number): string {
  if (bytes < 1024) return bytes + 'B';
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(0) + 'KB';
  return (bytes / 1024 / 1024).toFixed(1) + 'MB';
}

// 解析附件 → 预览确认
async function parseAttachment() {
  const file = attachment.value?.file;
  if (!file) {
    // 刷新后本地 File 丢失：提示重新上传再解析
    toast.error('附件为历史记录，请重新上传后再解析');
    return;
  }
  try {
    parsing.value = true;
    const res = await parseResumeAttachment(file);
    if (res.code === 200 && res.data) {
      parseResult.value = res.data;
      parsePreviewVisible.value = true;
    } else {
      toast.error(res.message || '附件解析失败');
    }
  } catch (e) {
    toast.error((e as Error)?.message || '附件解析失败');
  } finally {
    parsing.value = false;
  }
}

// 解析结果摘要（预览弹窗用）
const parseSummary = computed(() => {
  const p = parseResult.value;
  if (!p) return [];
  const items: { label: string; value: string }[] = [];
  if (p.name) items.push({ label: '姓名', value: p.name });
  if (p.gender) items.push({ label: '性别', value: p.gender });
  if (p.birthDate) items.push({ label: '出生日期', value: p.birthDate });
  if (p.phone) items.push({ label: '电话', value: p.phone });
  if (p.email) items.push({ label: '邮箱', value: p.email });
  if (p.jobIntention?.position) items.push({ label: '期望职位', value: p.jobIntention.position });
  if (p.jobIntention?.city) items.push({ label: '期望城市', value: p.jobIntention.city });
  if (p.educations?.length) items.push({ label: '教育经历', value: `${p.educations.length} 条` });
  if (p.works?.length) items.push({ label: '工作经历', value: `${p.works.length} 条` });
  if (p.projects?.length) items.push({ label: '项目经历', value: `${p.projects.length} 条` });
  if (p.skills?.length) items.push({ label: '专业技能', value: `${p.skills.length} 项` });
  if (p.selfIntro) items.push({ label: '自我评价', value: '已识别' });
  return items;
});

/**
 * 确认覆盖填充：解析结果按语义映射覆盖在线简历表单
 * 规则：null/空字段不覆盖（保留原值）；数组字段非空时整体替换
 */
function applyParseResult() {
  const p = parseResult.value;
  if (!p) return;
  if (p.name) form.name = p.name;
  if (p.gender) form.gender = p.gender;
  if (p.birthDate) form.birthDate = p.birthDate;
  if (p.phone) form.phone = p.phone;
  if (p.email) form.email = p.email;
  if (p.title && !form.title) form.title = p.title;
  if (p.selfIntro) form.selfIntro = p.selfIntro;
  if (p.jobIntention) {
    const ji = ensureJobIntention();
    if (p.jobIntention.position) ji.position = p.jobIntention.position;
    if (p.jobIntention.city) ji.city = p.jobIntention.city;
    if (p.jobIntention.salaryMin != null) ji.salaryMin = p.jobIntention.salaryMin;
    if (p.jobIntention.salaryMax != null) ji.salaryMax = p.jobIntention.salaryMax;
    if (p.jobIntention.jobType) ji.jobType = p.jobIntention.jobType;
    if (p.jobIntention.availableTime) ji.availableTime = p.jobIntention.availableTime;
  }
  if (p.educations?.length) form.educations = p.educations;
  if (p.works?.length) form.works = p.works;
  if (p.projects?.length) form.projects = p.projects;
  if (p.skills?.length) form.skills = p.skills;

  parsePreviewVisible.value = false;
  saveStatus.value = 'dirty';
  toast.success('已将附件内容覆盖填充到在线简历，请检查后保存');
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

/** 将简历详情填充进表单（编辑加载与"反显最新一版"共用） */
function fillFromDetail(d: any) {
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
  // 历史附件回显（本地 File 不可恢复，仅展示；解析需重新上传）
  if (d.fileUrl) {
    attachment.value = { name: '历史附件简历', size: 0, file: null, fileUrl: d.fileUrl };
  }
}

async function loadDetail(): Promise<boolean> {
  if (!editId.value) return false;
  try {
    loadingDetail.value = true;
    pageError.value = null;
    const res = await getResumeDetail(editId.value);
    if (res.code === 200 && res.data) {
      fillFromDetail(res.data);
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

/**
 * 反显最新一版简历（无 :id 进入创建页时调用）
 * 列表按 updateTime 倒序，取第一份即最新；拉详情填充表单并带上 form.id，
 * 后续保存即为"续编该简历"而不是新建，避免产生大量重复简历。
 * 无简历 / 未登录 / 加载失败时返回 false，走个人中心信息预填。
 */
async function reflectLatestResume(): Promise<boolean> {
  if (!getToken()) return false;
  try {
    const listRes = await getMyResumeList({ pageNum: 1, pageSize: 1 });
    if (listRes.code === 200 && listRes.data?.list?.length) {
      const latest = listRes.data.list[0];
      const detailRes = await getResumeDetail(latest.id!);
      if (detailRes.code === 200 && detailRes.data) {
        fillFromDetail(detailRes.data);
        hasReflectedResume.value = true;
        await nextTick();
        saveStatus.value = 'saved';
        return true;
      }
    }
  } catch (err) {
    // 反显失败静默：回退到个人中心预填
    console.warn('反显最新简历失败:', err);
  }
  return false;
}

onMounted(() => {
  if (isEdit.value && editId.value) {
    loadDetail().then((ok) => {
      if (ok) nextTick(() => { loaded.value = true; });
    });
  } else if (route.query.fromTemplate) {
    // 模板入口（v10.13）：跳过最新简历反显，个人中心预填 + 模板标题/岗位预填
    prefillFromProfile().then(() => {
      applyTemplateQuery();
      nextTick(() => { loaded.value = true; });
    });
  } else {
    // 无 :id 进入：若已写过简历则反显最新一版续编（带 form.id，保存即更新）；
    // 没有简历才走个人中心基础信息预填（仅填空字段不覆盖）
    reflectLatestResume().then((reflected) => {
      if (!reflected) prefillFromProfile();
      nextTick(() => { loaded.value = true; });
    });
  }
  // 实名认证状态（用于姓名字段的可选实名填充，失败静默）
  loadCertifiedInfo();
  window.addEventListener('scroll', handleScrollSpy, { passive: true });
});

/** 新建简历时用个人中心信息预填基础字段（只填空值，不覆盖用户已输入内容） */
async function prefillFromProfile() {
  if (!getToken()) return; // 未登录不预填（编辑页本身需要登录，此处兜底）
  try {
    const res = await getCurrentUser();
    if (res.code !== 200 || !res.data) return;
    const u = res.data;
    if (!form.name?.trim()) form.name = u.nickname || u.username || '';
    if (!form.gender?.trim()) form.gender = u.gender || '';
    if (!form.birthDate?.trim()) form.birthDate = u.birthday || '';
    if (!form.phone?.trim()) form.phone = u.phone || '';
    if (!form.email?.trim()) form.email = u.email || '';
    if (!form.avatar?.trim()) form.avatar = u.avatar || '';
    // 个人中心的职位可作为求职意向的默认岗位
    if (!form.jobIntention.position?.trim()) form.jobIntention.position = u.position || '';
    if (!form.title?.trim()) form.title = form.name ? `${form.name}的简历` : '';
  } catch (err) {
    // 预填失败静默处理，不影响创建流程
    console.warn('个人信息预填失败:', err);
  }
}

/** 模板入口预填：基于模板创建新简历（标题+期望岗位），模板文件可通过模板库下载参考 */
function applyTemplateQuery() {
  const templateTitle = String(route.query.templateTitle || '');
  const templateCategory = String(route.query.templateCategory || '');
  if (templateTitle && !form.title?.trim()) {
    form.title = `${templateTitle}风格 · 我的简历`;
  }
  if (templateCategory && !form.jobIntention.position?.trim()) {
    form.jobIntention.position = templateCategory;
  }
}

// ============ 实名姓名可选填充（v10.8 实名合规） ============
// 已通过身份认证的用户可在姓名字段一键使用实名姓名（主动选择，不自动回填，保护隐私边界）
const certifiedInfo = ref<CreatorCertification | null>(null);
const hasApprovedIdentity = computed(() =>
  certifiedInfo.value?.status === 'approved'
  && certifiedInfo.value?.certType === 'identity'
  && !!certifiedInfo.value?.realName?.trim()
);
// 加载实名认证记录（失败静默，仅影响可选填充按钮的显隐）
async function loadCertifiedInfo() {
  if (!getToken()) return;
  try {
    const res = await getMyCertification();
    if (res.code === 200) {
      certifiedInfo.value = res.data || null;
    }
  } catch (err) {
    console.warn('加载实名认证状态失败:', err);
  }
}
// 一键填充实名姓名；性别/出生日期仅在为空时顺带补全（来自证件号推导，均为本人可见数据）
function useCertifiedName() {
  const cert = certifiedInfo.value;
  if (!cert?.realName?.trim()) return;
  form.name = cert.realName.trim();
  if (!form.gender?.trim() && cert.derivedGender) form.gender = cert.derivedGender;
  if (!form.birthDate?.trim() && cert.derivedBirth) form.birthDate = cert.derivedBirth;
  toast.success('已填充实名姓名');
}

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
    prefillFromProfile(); // 切回新建态同样反显个人中心信息
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
        <!-- 岗位优化入口（v10.13） -->
        <div
          v-if="form.id"
          class="flex items-center justify-between gap-3 rounded-xl border p-3.5 mb-4"
          style="background: linear-gradient(90deg, color-mix(in srgb, var(--theme-primary) 6%, transparent), color-mix(in srgb, var(--theme-primary) 1%, transparent)); border-color: color-mix(in srgb, var(--theme-primary) 25%, var(--theme-border));"
        >
          <div class="flex items-center gap-2.5">
            <span class="text-lg">🎯</span>
            <div>
              <div class="text-sm font-semibold" style="color: var(--theme-primary);">有了目标岗位？试试岗位精准优化</div>
              <div class="text-xs text-theme-text-secondary mt-0.5">粘贴 JD 匹配评分 → AI 逐项优化前后对比 → 一键采纳</div>
            </div>
          </div>
          <button
            class="shrink-0 text-xs font-medium px-4 py-2 rounded-lg text-white"
            style="background: var(--theme-primary);"
            @click="router.push(`/interview/resume/optimize?resumeId=${form.id}`)"
          >
            进入优化工作台 →
          </button>
        </div>

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
              <!-- 已实名用户可选一键填充实名姓名（主动选择，不自动回填） -->
              <button
                v-if="hasApprovedIdentity"
                type="button"
                class="re-certified-fill"
                title="使用实名认证预留的姓名，性别与出生日期仅在为空时补全"
                @click="useCertifiedName"
              >
                <ShieldCheck class="w-3.5 h-3.5" />
                使用实名姓名
              </button>
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
              <label class="re-field-label">头像</label>
              <div class="re-avatar-field">
                <div class="re-avatar-preview" @click="triggerAvatarUpload" title="点击上传头像">
                  <img v-if="form.avatar" :src="form.avatar" alt="头像" class="re-avatar-img" />
                  <User v-else class="re-avatar-placeholder" />
                  <div v-if="avatarUploading" class="re-avatar-loading">上传中…</div>
                </div>
                <div class="re-avatar-actions">
                  <button type="button" class="re-avatar-btn" :disabled="avatarUploading" @click="triggerAvatarUpload">
                    {{ avatarUploading ? '上传中…' : (form.avatar ? '更换头像' : '上传头像') }}
                  </button>
                  <button v-if="form.avatar" type="button" class="re-avatar-btn re-avatar-btn-danger" @click="removeAvatar">
                    移除
                  </button>
                </div>
              </div>
              <input ref="avatarInputRef" type="file" accept="image/*" class="hidden" @change="onAvatarChange" />
              <div class="re-field-hint">支持 jpg/png/webp，建议正方形照片；也会在简历预览中展示</div>
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
              <select v-model="ensureJobIntention().availableTime" class="re-input">
                <option value="" disabled>请选择到岗时间</option>
                <option v-for="opt in availableTimeOptions" :key="opt" :value="opt">{{ opt }}</option>
              </select>
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
                <label class="re-field-label">
                  <span class="re-req">*</span> 工作描述
                  <button type="button" class="re-ai-assist-btn" title="AI 生成3个优化版本" @click="openFieldAssist('work', idx)">
                    <Sparkles class="w-3 h-3" /> AI优化
                  </button>
                </label>
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
                <label class="re-field-label">
                  <span class="re-req">*</span> 项目描述
                  <button type="button" class="re-ai-assist-btn" title="AI 生成3个优化版本" @click="openFieldAssist('project', idx)">
                    <Sparkles class="w-3 h-3" /> AI优化
                  </button>
                </label>
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
            <div class="re-field-label" style="display:flex;align-items:center;gap:6px;margin-bottom:6px;">
              <button type="button" class="re-ai-assist-btn" title="AI 生成3个优化版本" @click="openFieldAssist('selfIntro', 0)">
                <Sparkles class="w-3 h-3" /> AI优化
              </button>
            </div>
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
          <!-- 附件上传区（点击 / 拖拽，v10.12 实装） -->
          <div
            class="re-upload-zone"
            :class="{ 're-upload-dragover': dragOver, 're-upload-disabled': uploading }"
            @click="!uploading && attachmentInput?.click()"
            @dragover.prevent="dragOver = true"
            @dragleave.prevent="dragOver = false"
            @drop.prevent="onDrop"
          >
            <input
              ref="attachmentInput"
              type="file"
              :accept="ACCEPT_EXTS.join(',')"
              class="hidden"
              @change="onAttachmentChange"
            />
            <div class="re-upload-icon">
              <UploadCloud class="w-5 h-5" />
            </div>
            <div class="re-upload-title">{{ uploading ? '正在上传…' : '点击或拖拽文件到此处上传' }}</div>
            <div class="re-upload-desc">上传后可作为附件简历，并可解析内容覆盖填充到在线简历</div>
            <div class="re-upload-formats">
              <span class="re-format-tag">PDF</span>
              <span class="re-format-tag">DOC/DOCX</span>
              <span class="re-format-tag">TXT/MD</span>
              <span class="re-format-tag">≤ 10MB</span>
            </div>
          </div>

          <!-- 已上传附件卡片 -->
          <div v-if="attachment" class="re-attach-card">
            <div class="re-attach-icon"><FileText class="w-4 h-4" /></div>
            <div class="re-attach-info">
              <div class="re-attach-name">{{ attachment.name }}</div>
              <div class="re-attach-meta">
                附件简历
                <template v-if="attachment.size"> · {{ fmtSize(attachment.size) }}</template>
                <template v-if="!attachment.file"> · 历史记录（重新上传后可解析）</template>
              </div>
            </div>
            <div class="re-attach-actions">
              <button class="re-attach-btn primary" :disabled="parsing || !attachment.file" @click="parseAttachment">
                <Sparkles class="w-3 h-3" /> {{ parsing ? '解析中…' : '解析并填充' }}
              </button>
              <a v-if="attachment.fileUrl" :href="attachment.fileUrl" target="_blank" class="re-attach-btn">预览</a>
              <button class="re-attach-btn danger" @click="removeAttachment">移除</button>
            </div>
          </div>

          <div class="re-field-hint" style="margin-top: 8px;">
            <AlertCircle class="w-3 h-3 inline" />
            解析结果会覆盖填充到在线简历对应字段（空字段保留原值），填充后请检查并保存
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
            <!-- 评分总览（对齐原型 analysis-overview） -->
            <div class="re-advice-overview">
              <div class="re-ao-top">
                <div class="re-ao-score-block">
                  <div class="re-ao-score-num">{{ aiAdvice.score ?? form.score ?? '-' }}</div>
                  <div class="re-ao-score-label">综合评分 / {{ aiAdvice.grade ? '按等级 ' + aiAdvice.grade : '100' }}</div>
                  <div class="re-ao-score-desc">{{ aiAdvice.grade === 'A' ? '优秀 · 竞争力强' : aiAdvice.grade === 'B' ? '良好 · 仍有提升空间' : aiAdvice.grade === 'C' ? '合格 · 需重点优化' : '待完善 · 建议全面补充' }}</div>
                </div>
                <div class="re-ao-detail">
                  <p v-if="aiAdvice.summary" class="re-ao-summary">{{ aiAdvice.summary }}</p>
                  <div v-if="aiAdvice.aiPowered" class="re-ao-source">
                    <Sparkles class="w-3 h-3 inline" /> AI 模型生成
                  </div>
                </div>
              </div>
              <!-- 维度评分条（来自评分明细） -->
              <div v-if="form.scoreDetail && form.scoreDetail.length" class="re-ao-metrics">
                <div v-for="sd in form.scoreDetail" :key="sd.item" class="re-metric-row">
                  <span class="re-metric-label">{{ sd.item }}</span>
                  <div class="re-metric-bar">
                    <div
                      class="re-metric-fill"
                      :style="{ width: Math.round((sd.score / sd.maxScore) * 100) + '%', background: metricColor(sd.score / sd.maxScore) }"
                    ></div>
                  </div>
                  <span class="re-metric-val" :style="{ color: metricColor(sd.score / sd.maxScore) }">{{ sd.score }}/{{ sd.maxScore }}</span>
                </div>
              </div>
            </div>

            <!-- 缺失技能 -->
            <div v-if="aiAdvice.missingSkills && aiAdvice.missingSkills.length > 0" class="re-advice-missing">
              <div class="re-advice-missing-title">
                <AlertCircle class="w-3.5 h-3.5" />
                岗位必备技能缺失（{{ aiAdvice.missingSkills.length }} 项）
                <button class="re-missing-add-btn" @click="addMissingSkills">
                  <Plus class="w-3 h-3" /> 一键加入技能清单
                </button>
              </div>
              <div class="re-advice-missing-chips">
                <span v-for="skill in aiAdvice.missingSkills" :key="skill" class="re-advice-missing-chip">{{ skill }}</span>
              </div>
            </div>

            <!-- 模块 Tab 栏 -->
            <div v-if="adviceTabs.length > 2" class="re-advice-tabs">
              <button
                v-for="tab in adviceTabs"
                :key="tab.value"
                class="re-advice-tab"
                :class="{ active: activeAdviceTab === tab.value }"
                @click="activeAdviceTab = tab.value"
              >
                {{ tab.label }}
                <span v-if="tab.value === activeAdviceTab" class="re-tab-dot"></span>
              </button>
            </div>

            <!-- 建议卡片列表（对齐原型 a-card） -->
            <div v-if="filteredAdvices.length > 0" class="re-advice-list">
              <div v-for="advice in filteredAdvices" :key="adviceKey(advice)" class="re-advice-card">
                <div class="re-advice-card-head">
                  <span class="re-advice-dim">{{ DIMENSION_META[advice.dimension || '']?.label || advice.dimension || '综合' }}</span>
                  <span
                    v-if="dimensionScore(advice.dimension || '')"
                    class="re-advice-score-badge"
                    :class="scoreBadgeClass(dimensionScore(advice.dimension || '')!.rate)"
                  >
                    {{ dimensionScore(advice.dimension || '')!.score }} 分
                  </span>
                  <span v-if="advice.priority && priorityStyle[advice.priority]" class="re-advice-pri" :class="priorityStyle[advice.priority].class">
                    {{ priorityStyle[advice.priority].label }}
                  </span>
                  <span v-if="advice.type && adviceTypeLabel[advice.type]" class="re-advice-type">
                    {{ adviceTypeLabel[advice.type] }}
                  </span>
                </div>
                <div class="re-advice-card-body">
                  <!-- 优化建议（content：为什么改） -->
                  <div v-if="advice.content" class="re-feedback-tip">
                    <div class="re-fb-label"><AlertCircle class="w-3 h-3" /> 优化建议</div>
                    <p>{{ advice.content }}</p>
                  </div>
                  <!-- AI 优化结果（optimized：改完后长什么样，可直接采纳） -->
                  <div v-if="advice.optimized" class="re-diff-block">
                    <div class="re-diff-header">
                      <span><Sparkles class="w-3 h-3 inline" style="color: var(--theme-primary);" /> AI 优化结果</span>
                      <span class="re-diff-sub">可直接采纳</span>
                    </div>
                    <pre class="re-diff-body">{{ advice.optimized }}</pre>
                    <p v-if="advice.optimized.includes('[')" class="re-diff-ph">[ ] 内为占位符，采纳后请在表单中替换为真实数据</p>
                  </div>
                  <!-- 操作按钮 -->
                  <div class="re-card-actions">
                    <button
                      v-if="acceptedAdvices.has(adviceKey(advice))"
                      disabled
                      class="re-advice-accepted"
                    >
                      <CheckCircle2 class="w-3 h-3" /> 已采纳
                    </button>
                    <button
                      v-else
                      class="re-advice-accept-btn"
                      @click="acceptAdvice(advice)"
                    >
                      <CheckCircle2 class="w-3 h-3" /> {{ advice.optimized ? '采纳填充' : '去完善' }}
                    </button>
                  </div>
                </div>
              </div>
            </div>
            <div v-else-if="aiAdvice.advices && aiAdvice.advices.length > 0" class="re-advice-empty">
              <CheckCircle2 class="w-5 h-5" style="color: var(--theme-success);" />
              该模块暂无建议，看看其他 Tab
            </div>
            <div v-else class="re-advice-empty">
              <CheckCircle2 class="w-5 h-5" style="color: var(--theme-success);" />
              各维度得分率良好，暂无改进建议
            </div>

            <!-- 采纳后提示 -->
            <div v-if="acceptedAdvices.size > 0" class="re-advice-accepted-tip">
              <span><CheckCircle2 class="w-3.5 h-3.5 inline" /> 已采纳 {{ acceptedAdvices.size }} 条建议并填充到对应字段</span>
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

    <!-- AI 实时辅助弹窗（v10.14 设计文档 P0 需求#2：字段级 3 版本建议） -->
    <Teleport to="body">
      <div v-if="assistVisible" class="re-advice-mask" @click.self="assistVisible = false">
        <div class="re-advice-box" style="max-width: 640px;">
          <div class="re-advice-head">
            <h3>
              <Sparkles class="w-4 h-4" style="color: var(--theme-primary);" />
              AI 实时优化
              <span class="re-advice-grade" style="background: color-mix(in srgb, var(--theme-primary) 10%, transparent); color: var(--theme-primary);">
                {{ assistTarget?.type === 'work' ? '工作描述' : assistTarget?.type === 'project' ? '项目描述' : '自我评价' }}
              </span>
            </h3>
            <div class="re-advice-head-actions">
              <button class="re-advice-close" @click="assistVisible = false">
                <X class="w-4 h-4" />
              </button>
            </div>
          </div>

          <!-- 原文（对比基准） -->
          <div v-if="assistOriginal" class="re-assist-original">
            <div class="re-assist-label">📌 原始内容</div>
            <p>{{ assistOriginal }}</p>
          </div>

          <!-- 加载中 -->
          <div v-if="assistLoading" class="re-advice-loading">
            <div class="re-loading-spinner"></div>
            <p>AI 正在生成 3 个优化版本...</p>
          </div>

          <!-- 版本列表 -->
          <div v-else class="re-advice-body">
            <div
              v-for="(s, i) in assistSuggestions"
              :key="i"
              class="re-assist-version"
              :class="{ 're-assist-recommend': i === 0 }"
            >
              <div class="re-assist-version-head">
                <span class="re-assist-version-tag">{{ ASSIST_VERSION_LABELS[i] || `版本 ${i + 1}` }}</span>
                <span v-if="s.reason" class="re-assist-reason">💡 {{ s.reason }}</span>
              </div>
              <p class="re-assist-text">{{ s.text }}</p>
              <button class="re-assist-adopt-btn" @click="adoptAssist(s.text)">采纳此版本</button>
            </div>
            <div v-if="!assistSuggestions.length" class="re-empty-tip" style="padding: 24px 0;">
              AI 未生成有效建议，请补充内容后重试
            </div>
            <div class="re-field-hint" style="margin-top: 8px;">
              提示：AI 可能使用 [X%]、[X万] 等占位符标记需你确认的数据，采纳后请替换为真实数值
            </div>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- 附件解析结果预览弹窗（v10.12） -->
    <Teleport to="body">
      <div v-if="parsePreviewVisible" class="re-advice-mask" @click.self="parsePreviewVisible = false">
        <div class="re-advice-box" style="max-width: 560px;">
          <div class="re-advice-head">
            <h3>
              <FileText class="w-4 h-4" style="color: var(--theme-primary);" />
              解析结果确认
              <span v-if="parseResult?.aiPowered" class="re-advice-grade" :class="gradeStyle.A">
                <Sparkles class="w-3 h-3 inline" /> AI 结构化解析
              </span>
              <span v-else class="re-advice-grade" :class="gradeStyle.C">规则粗解析</span>
            </h3>
            <div class="re-advice-head-actions">
              <button class="re-advice-close" @click="parsePreviewVisible = false">
                <X class="w-4 h-4" />
              </button>
            </div>
          </div>

          <div class="re-advice-body">
            <!-- 识别摘要 -->
            <div v-if="parseSummary.length" class="re-parse-summary">
              <div v-for="item in parseSummary" :key="item.label" class="re-parse-row">
                <span class="re-parse-label">{{ item.label }}</span>
                <span class="re-parse-value">{{ item.value }}</span>
              </div>
            </div>
            <div v-else class="re-advice-empty">
              <AlertCircle class="w-5 h-5" style="color: var(--theme-warning);" />
              未能识别出有效字段，请检查附件内容或换用文本版简历
            </div>

            <div v-if="!parseResult?.aiPowered" class="re-field-hint" style="margin-top: 10px;">
              <AlertCircle class="w-3 h-3 inline" />
              当前为规则粗解析（AI 未启用或调用失败），仅能识别基础字段；启用 AI 模型后可完整解析教育/工作/项目经历
            </div>

            <div class="re-field-hint" style="margin-top: 10px;">
              <AlertCircle class="w-3 h-3 inline" />
              确认后将覆盖填充到在线简历对应字段（解析为空的字段保留原值），此操作不会自动保存，请检查后手动保存
            </div>
          </div>

          <div class="re-advice-footer" style="border-top: 1px solid var(--theme-border); padding: 14px 20px;">
            <div>
              <div class="re-advice-ft-title">覆盖填充到在线简历</div>
              <div class="re-advice-ft-desc">{{ parseSummary.length }} 项字段待填充</div>
            </div>
            <div style="display: flex; gap: 8px;">
              <button class="re-attach-btn" @click="parsePreviewVisible = false">取消</button>
              <button
                class="re-advice-study-btn"
                :disabled="!parseSummary.length"
                @click="applyParseResult"
              >
                <CheckCircle2 class="w-3.5 h-3.5" /> 确认填充
              </button>
            </div>
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
  background: var(--theme-surface);
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
.re-save-badge.saving { background: var(--theme-warning-bg); color: var(--theme-warning); }
.re-save-badge.saved { background: var(--theme-success-bg); color: var(--theme-success); }

/* ===== 加载 / 错误 ===== */
.re-loading, .re-error {
  max-width: 1280px;
  margin: 0 auto;
  padding: 80px 24px;
  text-align: center;
  background: var(--theme-surface);
  border: 1px solid var(--theme-border);
  border-radius: 14px;
  margin: 24px auto;
}
.re-loading p, .re-error p { margin-top: 12px; color: var(--theme-text-secondary); font-size: 14px; }
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
.re-retry-btn:hover { background: var(--theme-danger); }

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
  color: var(--theme-text-secondary);
  line-height: 1.5;
  display: flex;
  align-items: flex-start;
  gap: 4px;
}
/* 字段级 AI 优化按钮（v10.14 P0 需求#2：AI 实时辅助编辑） */
.re-ai-assist-btn {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  margin-left: auto;
  padding: 2px 8px;
  font-size: 11px;
  font-weight: 500;
  color: var(--theme-primary);
  background: rgba(124, 58, 237, 0.08);
  border: 1px solid rgba(124, 58, 237, 0.25);
  border-radius: 999px;
  cursor: pointer;
  transition: all 0.15s;
}
.re-ai-assist-btn:hover {
  background: rgba(124, 58, 237, 0.15);
  border-color: rgba(124, 58, 237, 0.45);
}
/* AI 实时辅助弹窗 */
.re-assist-original {
  margin: 12px 16px 0;
  padding: 10px 12px;
  background: var(--theme-accent);
  border: 1px solid var(--theme-border);
  border-radius: 8px;
}
.re-assist-original p {
  font-size: 12px;
  color: var(--theme-text-secondary);
  line-height: 1.6;
  white-space: pre-line;
  max-height: 90px;
  overflow-y: auto;
  margin: 4px 0 0;
}
.re-assist-label {
  font-size: 11px;
  color: var(--theme-text-secondary);
  font-weight: 500;
}
.re-assist-version {
  border: 1px solid var(--theme-border);
  border-radius: 10px;
  padding: 12px 14px;
  margin-bottom: 10px;
  transition: border-color 0.15s;
}
.re-assist-version:hover {
  border-color: rgba(124, 58, 237, 0.4);
}
.re-assist-recommend {
  border-color: rgba(124, 58, 237, 0.4);
  background: rgba(124, 58, 237, 0.03);
}
.re-assist-version-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 8px;
}
.re-assist-version-tag {
  font-size: 11px;
  font-weight: 600;
  color: var(--theme-primary);
  background: rgba(124, 58, 237, 0.08);
  padding: 2px 8px;
  border-radius: 999px;
  flex-shrink: 0;
}
.re-assist-reason {
  font-size: 11px;
  color: var(--theme-text-secondary);
}
.re-assist-text {
  font-size: 13px;
  color: var(--theme-text);
  line-height: 1.7;
  white-space: pre-line;
  margin: 0 0 10px;
}
.re-assist-adopt-btn {
  font-size: 12px;
  font-weight: 500;
  color: #fff;
  background: var(--theme-primary);
  border: none;
  border-radius: 6px;
  padding: 5px 14px;
  cursor: pointer;
  transition: opacity 0.15s;
}
.re-assist-adopt-btn:hover {
  opacity: 0.85;
}
/* 实名姓名一键填充按钮（v10.8：已实名用户专属，主动选择填充） */
.re-certified-fill {
  margin-top: 6px;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 10px;
  border-radius: 8px;
  border: 1px solid color-mix(in srgb, var(--theme-info) 40%, transparent);
  background-color: var(--theme-info-bg);
  color: var(--theme-info);
  font-size: 12px;
  font-weight: 500;
  cursor: pointer;
  transition: opacity 0.15s ease;
}
.re-certified-fill:hover { opacity: 0.85; }
.re-field-counter { font-size: 11px; color: var(--theme-text-secondary); text-align: right; }

/* 头像上传组件 */
.re-avatar-field { display: flex; align-items: center; gap: 12px; }
.re-avatar-preview {
  position: relative;
  width: 56px;
  height: 56px;
  border-radius: 50%;
  border: 1px dashed var(--theme-border);
  overflow: hidden;
  flex-shrink: 0;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--theme-accent);
  transition: border-color 0.15s;
}
.re-avatar-preview:hover { border-color: var(--theme-primary); }
.re-avatar-img { width: 100%; height: 100%; object-fit: cover; }
.re-avatar-placeholder { width: 22px; height: 22px; color: var(--theme-text-secondary); }
.re-avatar-loading {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 10px;
  color: #fff;
  background: rgba(0, 0, 0, 0.45);
}
.re-avatar-actions { display: flex; flex-direction: column; gap: 6px; }
.re-avatar-btn {
  padding: 4px 12px;
  border-radius: 6px;
  border: 1px solid var(--theme-border);
  background: var(--theme-surface);
  font-size: 12px;
  color: var(--theme-text);
  cursor: pointer;
  transition: all 0.15s;
  white-space: nowrap;
}
.re-avatar-btn:hover:not(:disabled) { border-color: var(--theme-primary); color: var(--theme-primary); }
.re-avatar-btn:disabled { opacity: 0.6; cursor: not-allowed; }
.re-avatar-btn-danger:hover:not(:disabled) { border-color: var(--theme-danger); color: var(--theme-danger); }
.hidden { display: none; }

.re-input, .re-select, .re-textarea {
  width: 100%;
  padding: 9px 12px;
  border: 1px solid var(--theme-border);
  border-radius: 8px;
  font-size: 13px;
  transition: all 0.15s cubic-bezier(0.4, 0, 0.2, 1);
  outline: none;
  background: var(--theme-surface);
  color: var(--theme-text);
  font-family: inherit;
}
.re-input:hover, .re-select:hover, .re-textarea:hover { border-color: var(--theme-text-secondary); }
.re-input:focus, .re-select:focus, .re-textarea:focus {
  border-color: var(--theme-primary);
  box-shadow: 0 0 0 3px rgba(220,38,38,0.08);
}
.re-input::placeholder, .re-textarea::placeholder { color: var(--theme-text-secondary); }
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
  background: var(--theme-surface);
}
.re-entry:hover { border-color: var(--theme-border); box-shadow: 0 1px 3px rgba(0,0,0,0.06); }
.re-entry-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
}
.re-entry-badge {
  font-size: 11px;
  font-weight: 600;
  color: var(--theme-text-secondary);
  display: flex;
  align-items: center;
  gap: 6px;
}
.re-entry-dot { width: 6px; height: 6px; border-radius: 50%; background: var(--theme-success); }
.re-entry-actions { display: flex; gap: 4px; }
.re-entry-btn {
  width: 28px; height: 28px;
  display: flex; align-items: center; justify-content: center;
  border: none; border-radius: 6px;
  cursor: pointer; font-size: 11px;
  background: transparent;
  color: var(--theme-text-secondary);
  transition: all 0.15s;
}
.re-entry-btn:hover { background: var(--theme-accent); color: var(--theme-text); }
.re-entry-btn.danger:hover { background: var(--theme-danger-bg); color: var(--theme-primary); }

.re-add-entry {
  width: 100%;
  padding: 10px;
  border: 1px dashed var(--theme-border);
  border-radius: 10px;
  background: transparent;
  color: var(--theme-text-secondary);
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
  background: var(--theme-danger-bg);
}

.re-empty-tip {
  font-size: 13px;
  color: var(--theme-text-secondary);
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
  background: var(--theme-accent);
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
  border-color: color-mix(in srgb, var(--theme-danger) 40%, transparent);
  background: var(--theme-danger-bg);
}
.re-skill-level {
  font-size: 10px;
  color: var(--theme-text-secondary);
  font-weight: 400;
}
.re-skill-remove {
  width: 14px; height: 14px;
  display: flex; align-items: center; justify-content: center;
  border-radius: 50%;
  cursor: pointer;
  color: var(--theme-text-secondary);
  transition: all 0.15s;
}
.re-skill-remove:hover { background: var(--theme-primary); color: #fff; }
.re-skill-input-row {
  display: flex;
  gap: 6px;
}
.re-skill-add-btn {
  padding: 8px 14px;
  background: var(--theme-surface);
  border: 1px solid var(--theme-border);
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
  background: var(--theme-danger-bg);
}

/* ===== 上传区 ===== */
.re-upload-zone {
  border: 2px dashed var(--theme-border);
  border-radius: 14px;
  padding: 36px 24px;
  text-align: center;
  cursor: pointer;
  transition: all 0.25s;
  background: var(--theme-surface);
}
.re-upload-zone:hover {
  border-color: var(--theme-primary);
  background: var(--theme-danger-bg);
}
.re-upload-icon {
  width: 48px; height: 48px;
  background: var(--theme-accent);
  border-radius: 14px;
  display: flex; align-items: center; justify-content: center;
  margin: 0 auto 14px;
  color: var(--theme-text-secondary);
  transition: all 0.2s;
}
.re-upload-zone:hover .re-upload-icon {
  background: var(--theme-danger-bg);
  color: var(--theme-primary);
}
.re-upload-title { font-size: 14px; font-weight: 600; color: var(--theme-text); margin-bottom: 4px; }
.re-upload-desc { font-size: 12px; color: var(--theme-text-secondary); }
.re-upload-formats { display: flex; gap: 6px; justify-content: center; margin-top: 12px; }
.re-format-tag {
  font-size: 10px;
  padding: 2px 8px;
  background: var(--theme-accent);
  color: var(--theme-text-secondary);
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
  background: var(--theme-surface);
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
  background: var(--theme-surface);
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
  background: var(--theme-accent);
  color: var(--theme-text-secondary);
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
  background: var(--theme-surface);
  color: var(--theme-text-secondary);
  cursor: pointer;
}
.re-advice-close:hover { border-color: var(--theme-border); background: var(--theme-accent); color: var(--theme-text); }

.re-advice-loading {
  padding: 60px 24px;
  text-align: center;
  color: var(--theme-text-secondary);
  font-size: 14px;
}
.re-advice-body { padding: 20px 24px; }

.re-advice-summary {
  background: var(--theme-accent);
  border-radius: 10px;
  padding: 12px 14px;
  font-size: 13px;
  color: var(--theme-text);
  line-height: 1.6;
  margin-bottom: 14px;
}
.re-advice-missing {
  background: var(--theme-danger-bg);
  border: 1px solid color-mix(in srgb, var(--theme-danger) 40%, transparent);
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
  background: var(--theme-danger-bg);
  color: var(--theme-primary);
  border: 1px solid color-mix(in srgb, var(--theme-danger) 40%, transparent);
  border-radius: 12px;
}

.re-advice-list { display: flex; flex-direction: column; gap: 12px; }

/* ===== 附件上传区交互态（v10.12） ===== */
.re-upload-zone { cursor: pointer; transition: all 0.15s; }
.re-upload-dragover {
  border-color: var(--theme-primary) !important;
  background: color-mix(in srgb, var(--theme-primary) 6%, var(--theme-surface)) !important;
  transform: scale(1.01);
}
.re-upload-disabled { pointer-events: none; opacity: 0.6; }

/* ===== 已上传附件卡片 ===== */
.re-attach-card {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 12px;
  padding: 12px 14px;
  background: var(--theme-surface);
  border: 1px solid var(--theme-border);
  border-radius: 10px;
}
.re-attach-icon {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  background: var(--theme-accent);
  color: var(--theme-text-secondary);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.re-attach-info { flex: 1; min-width: 0; }
.re-attach-name {
  font-size: 13px;
  font-weight: 600;
  color: var(--theme-text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.re-attach-meta { font-size: 11px; color: var(--theme-text-secondary); margin-top: 2px; }
.re-attach-actions { display: flex; align-items: center; gap: 6px; flex-shrink: 0; }
.re-attach-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  padding: 5px 12px;
  border-radius: 6px;
  border: 1px solid var(--theme-border);
  background: var(--theme-surface);
  color: var(--theme-text);
  cursor: pointer;
  text-decoration: none;
  transition: all 0.15s;
}
.re-attach-btn:hover { border-color: var(--theme-primary); color: var(--theme-primary); }
.re-attach-btn.primary {
  background: var(--theme-primary);
  border-color: var(--theme-primary);
  color: #fff;
  font-weight: 600;
}
.re-attach-btn.primary:hover { opacity: 0.9; }
.re-attach-btn.primary:disabled { opacity: 0.5; cursor: not-allowed; }
.re-attach-btn.danger { color: var(--theme-danger); }
.re-attach-btn.danger:hover { border-color: var(--theme-danger); color: var(--theme-danger); background: var(--theme-danger-bg); }

/* ===== 解析结果摘要 ===== */
.re-parse-summary {
  background: var(--theme-accent);
  border: 1px solid var(--theme-border);
  border-radius: 10px;
  padding: 6px 14px;
}
.re-parse-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 0;
  border-bottom: 1px dashed var(--theme-border);
}
.re-parse-row:last-child { border-bottom: none; }
.re-parse-label { font-size: 12px; color: var(--theme-text-secondary); }
.re-parse-value { font-size: 13px; font-weight: 600; color: var(--theme-text); }

/* ===== 评分总览（对齐原型 analysis-overview） ===== */
.re-advice-overview {
  background: var(--theme-accent);
  border: 1px solid var(--theme-border);
  border-radius: 12px;
  padding: 16px;
  margin-bottom: 14px;
}
.re-ao-top { display: flex; gap: 20px; align-items: flex-start; }
.re-ao-score-block {
  flex-shrink: 0;
  text-align: center;
  min-width: 96px;
  padding: 8px 12px;
  background: var(--theme-surface);
  border-radius: 10px;
  border: 1px solid var(--theme-border);
}
.re-ao-score-num {
  font-size: 34px;
  font-weight: 800;
  line-height: 1.1;
  color: var(--theme-primary);
}
.re-ao-score-label { font-size: 11px; color: var(--theme-text-secondary); margin-top: 2px; }
.re-ao-score-desc { font-size: 11px; color: var(--theme-text-secondary); margin-top: 4px; font-weight: 600; }
.re-ao-detail { flex: 1; min-width: 0; }
.re-ao-summary { font-size: 13px; color: var(--theme-text); line-height: 1.6; }
.re-ao-source {
  margin-top: 8px;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 11px;
  font-weight: 600;
  color: var(--theme-primary);
  background: color-mix(in srgb, var(--theme-primary) 8%, var(--theme-surface));
  padding: 2px 10px;
  border-radius: 10px;
}
.re-ao-metrics { margin-top: 14px; display: flex; flex-direction: column; gap: 8px; }
.re-metric-row { display: flex; align-items: center; gap: 10px; }
.re-metric-label { width: 72px; flex-shrink: 0; font-size: 12px; color: var(--theme-text-secondary); text-align: right; }
.re-metric-bar {
  flex: 1;
  height: 8px;
  background: var(--theme-border);
  border-radius: 4px;
  overflow: hidden;
}
.re-metric-fill { height: 100%; border-radius: 4px; transition: width 0.4s ease; }
.re-metric-val { width: 52px; flex-shrink: 0; font-size: 11px; font-weight: 600; }

/* ===== 模块 Tab 栏 ===== */
.re-advice-tabs {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
  margin-bottom: 12px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--theme-border);
}
.re-advice-tab {
  position: relative;
  font-size: 12px;
  padding: 5px 14px;
  border-radius: 16px;
  background: var(--theme-accent);
  color: var(--theme-text-secondary);
  border: 1px solid var(--theme-border);
  cursor: pointer;
  transition: all 0.15s;
}
.re-advice-tab:hover { color: var(--theme-primary); border-color: var(--theme-primary); }
.re-advice-tab.active {
  background: var(--theme-primary);
  color: #fff;
  border-color: var(--theme-primary);
  font-weight: 600;
}
.re-tab-dot { display: none; }

/* ===== 建议卡片（对齐原型 a-card） ===== */
.re-advice-card {
  background: var(--theme-surface);
  border: 1px solid var(--theme-border);
  border-radius: 12px;
  overflow: hidden;
}
.re-advice-card-head {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  background: var(--theme-accent);
  border-bottom: 1px solid var(--theme-border);
  flex-wrap: wrap;
}
.re-advice-dim { font-size: 13px; font-weight: 700; color: var(--theme-text); }
.re-advice-score-badge {
  font-size: 11px;
  padding: 1px 8px;
  border-radius: 8px;
  font-weight: 600;
}
.re-advice-pri { font-size: 11px; padding: 1px 8px; border-radius: 8px; font-weight: 600; }
.re-advice-type {
  font-size: 11px;
  padding: 1px 8px;
  border-radius: 8px;
  background: var(--theme-surface);
  color: var(--theme-text-secondary);
  border: 1px solid var(--theme-border);
}
.re-advice-card-body { padding: 12px 14px; }

/* 优化建议反馈块（原型 feedback-block tip） */
.re-feedback-tip {
  background: var(--theme-warning-bg);
  border: 1px solid color-mix(in srgb, var(--theme-warning) 40%, transparent);
  border-radius: 8px;
  padding: 10px 12px;
  margin-bottom: 10px;
}
.re-fb-label {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 11px;
  font-weight: 700;
  color: var(--theme-warning);
  margin-bottom: 4px;
}
.re-feedback-tip p { font-size: 12.5px; color: var(--theme-warning); line-height: 1.6; }

/* AI 优化结果块（原型 diff-block） */
.re-diff-block {
  background: var(--theme-success-bg);
  border: 1px solid color-mix(in srgb, var(--theme-success) 40%, transparent);
  border-radius: 8px;
  padding: 10px 12px;
  margin-bottom: 10px;
}
.re-diff-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 11px;
  font-weight: 700;
  color: var(--theme-success);
  margin-bottom: 6px;
}
.re-diff-sub { font-weight: 400; opacity: 0.7; }
.re-diff-body {
  font-family: inherit;
  font-size: 12.5px;
  color: var(--theme-success);
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
  margin: 0;
}
.re-diff-ph { font-size: 11px; color: var(--theme-success); margin-top: 6px; }
.re-card-actions { display: flex; align-items: center; gap: 8px; }
.re-card-actions .re-advice-accepted,
.re-card-actions .re-advice-accept-btn { position: static; }
/* 缺失技能一键加入按钮 */
.re-missing-add-btn {
  margin-left: auto;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 11px;
  font-weight: 600;
  padding: 3px 10px;
  border-radius: 6px;
  background: var(--theme-surface);
  color: var(--theme-primary);
  border: 1px solid var(--theme-primary);
  cursor: pointer;
  transition: all 0.15s;
}
.re-missing-add-btn:hover { background: color-mix(in srgb, var(--theme-primary) 8%, var(--theme-surface)); }
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
.re-advice-accept-btn:hover { background: var(--theme-danger); }
.re-advice-accepted {
  position: absolute;
  top: 12px;
  right: 12px;
  font-size: 11px;
  padding: 4px 10px;
  border-radius: 6px;
  background: rgba(22,163,74,0.1);
  color: var(--theme-success);
  border: 1px solid rgba(22,163,74,0.2);
  display: inline-flex;
  align-items: center;
  gap: 3px;
  font-weight: 600;
}
.re-advice-empty {
  text-align: center;
  padding: 24px;
  color: var(--theme-success);
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
  color: var(--theme-success);
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
  background: linear-gradient(135deg, var(--theme-primary), var(--theme-primary));
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
  background: var(--theme-surface);
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
