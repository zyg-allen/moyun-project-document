<script setup lang="ts">
/**
 * 编程题做题页（v10.6 阶段3）
 *
 * 参考 docs/_Coze_Drive_Coze项目助手_ai_interview_system/code_question_page.html 原型
 * 布局：左右分栏（左题目描述 | 右代码编辑器 + 测试结果）
 * 判定：前端 JS 沙箱（new Function），其他语言提示切换 JS
 * 复用：CodeEditor.vue（Monaco Editor）
 */
import { ref, computed, onMounted, onBeforeUnmount } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useHead } from '@vueuse/head';
import {
  ChevronLeft, ChevronRight, Play, Send, Loader2, AlertCircle,
  CheckCircle2, XCircle, Clock, List, RefreshCw, Lightbulb,
  FileText, BookOpen, History,
} from 'lucide-vue-next';
import Breadcrumb from '@/components/Breadcrumb.vue';
import CodeEditor from '@/components/CodeEditor.vue';
import { generateSeo } from '@/utils/seo';
import { getQuestionDetail } from '@/api/interview';
import type { InterviewQuestionDetailVO } from '@/types/api';

const route = useRoute();
const router = useRouter();

// ========== 题目数据 ==========
const loading = ref(true);
const error = ref<string | null>(null);
const question = ref<InterviewQuestionDetailVO | null>(null);

// 测试用例（从后端加载）
interface TestCase {
  input: string;
  expectedOutput: string;
  isSample: number;
  orderNum: number;
  explanation?: string;
}
const testCases = ref<TestCase[]>([]);

// ========== 代码编辑器 ==========
const LANGUAGE_OPTIONS = [
  { value: 'javascript', label: 'JavaScript' },
  { value: 'typescript', label: 'TypeScript' },
  { value: 'python', label: 'Python' },
  { value: 'java', label: 'Java' },
];
const selectedLanguage = ref('javascript');

// 各语言默认代码模板
const CODE_TEMPLATES: Record<string, string> = {
  javascript: `/**
 * @param {number[]} nums
 * @param {number} target
 * @return {number[]}
 */
function solution(nums, target) {
    // 在此编写你的代码
    const map = new Map();
    for (let i = 0; i < nums.length; i++) {
        const complement = target - nums[i];
        if (map.has(complement)) {
            return [map.get(complement), i];
        }
        map.set(nums[i], i);
    }
    return [];
}`,
  typescript: `function solution(nums: number[], target: number): number[] {
    // 在此编写你的代码
    const map = new Map<number, number>();
    for (let i = 0; i < nums.length; i++) {
        const complement = target - nums[i];
        if (map.has(complement)) {
            return [map.get(complement)!, i];
        }
        map.set(nums[i], i);
    }
    return [];
}`,
  python: `class Solution:
    def twoSum(self, nums: List[int], target: int) -> List[int]:
        # 在此编写你的代码
        pass`,
  java: `class Solution {
    public int[] twoSum(int[] nums, int target) {
        // 在此编写你的代码
        return new int[]{};
    }
}`,
};

// 用户代码（按语言分槽存储，切换语言保留）
const codeByLang = ref<Record<string, string>>({
  javascript: CODE_TEMPLATES.javascript,
});
const userCode = computed({
  get: () => codeByLang.value[selectedLanguage.value] || CODE_TEMPLATES[selectedLanguage.value] || '',
  set: (val: string) => { codeByLang.value[selectedLanguage.value] = val; },
});

function changeLanguage(lang: string) {
  if (!codeByLang.value[lang]) {
    codeByLang.value[lang] = CODE_TEMPLATES[lang] || '';
  }
  selectedLanguage.value = lang;
}

// ========== 左侧 Tab ==========
type LeftTab = 'desc' | 'editorial' | 'submissions';
const activeTab = ref<LeftTab>('desc');

// ========== 运行/提交状态 ==========
const isRunning = ref(false);
const isSubmitting = ref(false);
const runResults = ref<RunResult[]>([]);
const lastRunStatus = ref<'idle' | 'running' | 'pass' | 'fail'>('idle');

interface RunResult {
  caseNum: number;
  passed: boolean;
  input: string;
  expected: string;
  actual: string;
  error?: string;
  timeMs: number;
}

// ========== 计时器 ==========
const elapsed = ref(0);
let timer: number | null = null;
function startTimer() {
  if (timer) return;
  timer = window.setInterval(() => { elapsed.value++; }, 1000);
}
function stopTimer() {
  if (timer) { clearInterval(timer); timer = null; }
}
function formatTime(s: number): string {
  const h = Math.floor(s / 3600).toString().padStart(2, '0');
  const m = Math.floor((s % 3600) / 60).toString().padStart(2, '0');
  const sec = (s % 60).toString().padStart(2, '0');
  return `${h}:${m}:${sec}`;
}

// ========== SEO ==========
useHead(computed(() => generateSeo({
  title: question.value ? `编程练习 - ${question.value.title}` : '编程题练习',
  description: '在线编程题练习，多语言 IDE，测试用例判定',
  keywords: ['编程题', '在线练习', 'IDE', '旭林'],
  canonicalPath: `/learn/practice/coding/${route.params.id}`,
})));

const breadcrumbs = computed(() => [
  { label: '学习中心', path: '/learn' },
  { label: '刷题中心', path: '/learn/practice' },
  { label: '编程题', path: '/learn/practice/coding' },
  { label: '做题' },
]);

const DIFFICULTY_MAP: Record<string, { label: string; class: string }> = {
  easy: { label: '简单', class: 'bg-green-100 text-green-700' },
  medium: { label: '中等', class: 'bg-yellow-100 text-yellow-700' },
  hard: { label: '困难', class: 'bg-red-100 text-red-700' },
};

// ========== 加载题目 ==========
async function loadQuestion() {
  loading.value = true;
  error.value = null;
  try {
    const id = route.params.id;
    const res = await getQuestionDetail(id as string | number);
    if (res.code === 200 && res.data) {
      question.value = res.data;
      // 测试用例从 mySubmissions 或单独字段获取，这里先 mock 基础用例
      // 实际需要后端提供 /portal/interview/question/{id}/testcases 接口
      // 阶段3先用题目描述中的示例作为测试用例
      testCases.value = parseTestCasesFromDesc(res.data);
    } else {
      error.value = res.message || '加载题目失败';
    }
  } catch (err: any) {
    error.value = err?.message || '加载题目失败，请稍后重试';
  } finally {
    loading.value = false;
  }
}

// 从题目描述中解析示例测试用例（临时方案，阶段3b接后端接口）
function parseTestCasesFromDesc(q: InterviewQuestionDetailVO): TestCase[] {
  // 简单解析：从 description 中找 "输入：" 和 "输出：" 模式
  const cases: TestCase[] = [];
  if (!q.description) return cases;
  const lines = q.description.split('\n');
  let currentInput = '';
  let currentOutput = '';
  let hasInput = false;
  let hasOutput = false;
  let orderNum = 1;
  for (const line of lines) {
    const inputMatch = line.match(/输入[:：]\s*(.+)/);
    const outputMatch = line.match(/输出[:：]\s*(.+)/);
    if (inputMatch) {
      if (hasInput && hasOutput) {
        cases.push({ input: currentInput, expectedOutput: currentOutput, isSample: 1, orderNum: orderNum++ });
      }
      currentInput = inputMatch[1].trim();
      hasInput = true;
      hasOutput = false;
    } else if (outputMatch) {
      currentOutput = outputMatch[1].trim();
      hasOutput = true;
    }
  }
  if (hasInput && hasOutput) {
    cases.push({ input: currentInput, expectedOutput: currentOutput, isSample: 1, orderNum: orderNum });
  }
  return cases;
}

// ========== 运行测试（前端 JS 沙箱） ==========
function runCode(): RunResult[] {
  const results: RunResult[] = [];
  const code = userCode.value;
  if (!code) return results;

  for (const tc of testCases.value) {
    const start = performance.now();
    try {
      // 解析输入参数：把 "[2,7,11,15]\n9" 转成 [2,7,11,15], 9
      const inputParts = tc.input.split('\n').map(s => s.trim()).filter(Boolean);
      const args = inputParts.map(parseInputValue);

      // 前端沙箱执行：new Function 包装用户代码，返回 solution 函数
      // 注意：这里用 new Function 而非 eval，隔离作用域
      const wrappedCode = `
        ${code}
        return solution;
      `;
      const solutionFn = new Function(wrappedCode)();
      if (typeof solutionFn !== 'function') {
        throw new Error('未找到 solution 函数，请确保代码中定义了 function solution(...)');
      }
      const actual = solutionFn(...args);
      const actualStr = formatOutput(actual);
      const expected = tc.expectedOutput.trim();

      results.push({
        caseNum: tc.orderNum,
        passed: actualStr === expected,
        input: tc.input,
        expected: tc.expectedOutput,
        actual: actualStr,
        timeMs: Math.round(performance.now() - start),
      });
    } catch (e: any) {
      results.push({
        caseNum: tc.orderNum,
        passed: false,
        input: tc.input,
        expected: tc.expectedOutput,
        actual: '',
        error: e?.message || String(e),
        timeMs: Math.round(performance.now() - start),
      });
    }
  }
  return results;
}

// 解析输入值：尝试 JSON.parse，失败则当字符串
function parseInputValue(s: string): any {
  try {
    return JSON.parse(s);
  } catch {
    return s;
  }
}

// 格式化输出：数组转 [a,b]，对象转 JSON
function formatOutput(val: any): string {
  if (Array.isArray(val)) {
    return '[' + val.join(',') + ']';
  }
  if (typeof val === 'boolean') return String(val);
  if (val === null || val === undefined) return '';
  return String(val);
}

async function handleRun() {
  if (selectedLanguage.value !== 'javascript' && selectedLanguage.value !== 'typescript') {
    error.value = '当前阶段仅支持 JavaScript 运行测试，其他语言请切换到 JavaScript';
    return;
  }
  isRunning.value = true;
  lastRunStatus.value = 'running';
  error.value = null;
  // 模拟异步（给 UI loading 时间）
  await new Promise(r => setTimeout(r, 200));
  try {
    runResults.value = runCode();
    lastRunStatus.value = runResults.value.every(r => r.passed) ? 'pass' : 'fail';
  } catch (e: any) {
    error.value = e?.message || '运行失败';
    lastRunStatus.value = 'fail';
  } finally {
    isRunning.value = false;
  }
}

async function handleSubmit() {
  if (selectedLanguage.value !== 'javascript' && selectedLanguage.value !== 'typescript') {
    error.value = '当前阶段仅支持 JavaScript 提交，其他语言请切换到 JavaScript';
    return;
  }
  isSubmitting.value = true;
  error.value = null;
  await new Promise(r => setTimeout(r, 300));
  try {
    runResults.value = runCode();
    lastRunStatus.value = runResults.value.every(r => r.passed) ? 'pass' : 'fail';
    activeTab.value = 'submissions';
    // TODO 阶段3b：调用后端 /portal/learn/practice/submit 持久化提交记录
  } catch (e: any) {
    error.value = e?.message || '提交失败';
    lastRunStatus.value = 'fail';
  } finally {
    isSubmitting.value = false;
  }
}

function resetCode() {
  if (confirm('确定要重置代码吗？当前代码将丢失。')) {
    codeByLang.value[selectedLanguage.value] = CODE_TEMPLATES[selectedLanguage.value] || '';
    runResults.value = [];
    lastRunStatus.value = 'idle';
  }
}

function gotoList() {
  router.push('/learn/practice/coding');
}

// ========== 生命周期 ==========
onMounted(() => {
  loadQuestion();
  startTimer();
});

onBeforeUnmount(() => {
  stopTimer();
});
</script>

<template>
  <div class="coding-practice-page flex flex-col w-full max-w-[1280px] mx-auto" style="background-color: var(--theme-bg); height: calc(100vh - 56px);">
    <!-- 面包屑 + 工具栏 -->
    <div class="flex items-center justify-between px-4 py-2 border-b" style="border-color: var(--theme-border); background-color: var(--theme-card-bg);">
      <Breadcrumb :items="breadcrumbs" />
      <div class="flex items-center gap-2">
        <span class="text-xs font-mono px-2 py-0.5 rounded" style="background-color: var(--theme-bg); color: var(--theme-text-secondary);">
          <Clock class="w-3 h-3 inline mr-0.5" />{{ formatTime(elapsed) }}
        </span>
        <button @click="gotoList" class="inline-flex items-center gap-1 text-xs px-2 py-1 rounded border" style="border-color: var(--theme-border); color: var(--theme-text-secondary);">
          <List class="w-3 h-3" />题目列表
        </button>
      </div>
    </div>

    <!-- 加载中 -->
    <div v-if="loading" class="flex-1 flex flex-col items-center justify-center">
      <Loader2 class="w-8 h-8 animate-spin mb-3" style="color: var(--theme-primary);" />
      <p class="text-sm" style="color: var(--theme-text-secondary);">加载题目中...</p>
    </div>

    <!-- 错误 -->
    <div v-else-if="error && !question" class="flex-1 flex flex-col items-center justify-center">
      <AlertCircle class="w-8 h-8 mb-3" style="color: #DC2626;" />
      <p class="text-sm mb-4" style="color: var(--theme-text);">{{ error }}</p>
      <button @click="loadQuestion" class="px-4 py-2 text-sm text-white rounded-lg" style="background-color: var(--theme-primary);">重试</button>
    </div>

    <!-- 主体：左右分栏 -->
    <div v-else-if="question" class="flex-1 flex overflow-hidden">
      <!-- 左侧：题目描述 -->
      <section class="w-1/2 flex flex-col border-r" style="border-color: var(--theme-border); background-color: var(--theme-card-bg); min-width: 320px;">
        <!-- 题头 -->
        <div class="px-5 py-3 border-b" style="border-color: var(--theme-border);">
          <div class="flex items-center gap-2 mb-1">
            <h1 class="text-base font-bold" style="color: var(--theme-text);">{{ question.title }}</h1>
            <span class="text-[10px] px-1.5 py-0.5 rounded font-medium"
                  :class="DIFFICULTY_MAP[question.difficulty]?.class || 'bg-gray-100 text-gray-600'">
              {{ DIFFICULTY_MAP[question.difficulty]?.label || question.difficulty || '未分级' }}
            </span>
          </div>
          <div class="flex items-center gap-3 text-[10px]" style="color: var(--theme-text-secondary);">
            <span v-if="question.acceptanceRate">通过率 {{ question.acceptanceRate }}%</span>
            <span v-if="question.submissionCount">提交 {{ question.submissionCount }}</span>
          </div>
        </div>

        <!-- Tab 栏 -->
        <div class="flex border-b px-5" style="border-color: var(--theme-border);">
          <button
            v-for="tab in [
              { key: 'desc', label: '题目描述', icon: FileText },
              { key: 'editorial', label: '题解', icon: BookOpen },
              { key: 'submissions', label: '提交记录', icon: History },
            ]"
            :key="tab.key"
            @click="activeTab = tab.key as LeftTab"
            :class="[
              'flex items-center gap-1.5 px-3 py-2 text-xs font-medium border-b-2 transition-colors',
              activeTab === tab.key ? '' : 'border-transparent'
            ]"
            :style="activeTab === tab.key
              ? { color: 'var(--theme-primary)', borderColor: 'var(--theme-primary)' }
              : { color: 'var(--theme-text-secondary)' }"
          >
            <component :is="tab.icon" class="w-3 h-3" />
            {{ tab.label }}
          </button>
        </div>

        <!-- Tab 内容 -->
        <div class="flex-1 overflow-y-auto p-5 text-sm leading-relaxed" style="color: var(--theme-text);">
          <!-- 题目描述 -->
          <div v-if="activeTab === 'desc'">
            <p v-if="question.description" class="whitespace-pre-wrap mb-4">{{ question.description }}</p>
            <!-- 示例测试用例 -->
            <div v-if="testCases.length" class="space-y-3">
              <h3 class="text-sm font-bold mb-2" style="color: var(--theme-text);">示例</h3>
              <div v-for="tc in testCases" :key="tc.orderNum" class="p-3 rounded-lg border" style="background-color: var(--theme-bg); border-color: var(--theme-border);">
                <div class="text-xs font-bold mb-1" style="color: var(--theme-text-secondary);">示例 {{ tc.orderNum }}</div>
                <div class="font-mono text-xs mb-1"><strong>输入：</strong>{{ tc.input }}</div>
                <div class="font-mono text-xs"><strong>输出：</strong>{{ tc.expectedOutput }}</div>
              </div>
            </div>
            <!-- 标签 -->
            <div v-if="question.tags && question.tags.length" class="flex gap-1.5 mt-5 flex-wrap">
              <span v-for="tag in question.tags" :key="tag" class="text-[10px] px-2 py-0.5 rounded-full"
                    style="background-color: var(--theme-primary-bg); color: var(--theme-primary);">
                {{ tag }}
              </span>
            </div>
          </div>

          <!-- 题解 -->
          <div v-else-if="activeTab === 'editorial'">
            <div v-if="question.analysis || question.referenceAnswer" class="whitespace-pre-wrap">
              {{ question.analysis || question.referenceAnswer }}
            </div>
            <div v-else-if="question.solution" class="whitespace-pre-wrap font-mono text-xs p-3 rounded-lg" style="background-color: #1E1E2E; color: #cdd6f4;">
              {{ question.solution }}
            </div>
            <div v-else class="text-center py-10">
              <Lightbulb class="w-10 h-10 mx-auto mb-2" style="color: var(--theme-text-secondary); opacity: 0.4;" />
              <p class="text-xs" style="color: var(--theme-text-secondary);">暂无题解</p>
            </div>
          </div>

          <!-- 提交记录 -->
          <div v-else-if="activeTab === 'submissions'">
            <div v-if="runResults.length === 0" class="text-center py-10">
              <History class="w-10 h-10 mx-auto mb-2" style="color: var(--theme-text-secondary); opacity: 0.4;" />
              <p class="text-xs" style="color: var(--theme-text-secondary);">暂无运行记录，点击右侧"运行"按钮</p>
            </div>
            <div v-else class="space-y-2">
              <div v-for="r in runResults" :key="r.caseNum" class="p-3 rounded-lg border" style="border-color: var(--theme-border);">
                <div class="flex items-center gap-2 mb-1">
                  <CheckCircle2 v-if="r.passed" class="w-4 h-4 text-green-500" />
                  <XCircle v-else class="w-4 h-4 text-red-500" />
                  <span class="text-xs font-bold" :style="{ color: r.passed ? '#059669' : '#DC2626' }">
                    用例 {{ r.caseNum }}：{{ r.passed ? '通过' : '未通过' }}
                  </span>
                  <span class="text-[10px] ml-auto" style="color: var(--theme-text-secondary);">{{ r.timeMs }}ms</span>
                </div>
                <div class="font-mono text-[10px] space-y-0.5" style="color: var(--theme-text-secondary);">
                  <div>输入：{{ r.input }}</div>
                  <div>期望：{{ r.expected }}</div>
                  <div v-if="r.error" style="color: #DC2626;">错误：{{ r.error }}</div>
                  <div v-else>实际：{{ r.actual }}</div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      <!-- 右侧：代码编辑器 + 运行结果 -->
      <section class="flex-1 flex flex-col" style="background-color: #1E1E2E; min-width: 400px;">
        <!-- 工具栏 -->
        <div class="flex items-center gap-2 px-3 py-2 border-b" style="background-color: #252538; border-color: #313244;">
          <select v-model="selectedLanguage" @change="changeLanguage(($event.target as HTMLSelectElement).value)"
                  class="text-xs rounded px-2 py-1 outline-none" style="background-color: #313244; color: #cdd6f4; border: 1px solid #45475a;">
            <option v-for="lang in LANGUAGE_OPTIONS" :key="lang.value" :value="lang.value">{{ lang.label }}</option>
          </select>
          <button @click="resetCode" class="text-[11px] px-2 py-1 rounded transition-colors" style="color: #a6adc8; border: 1px solid #45475a;">
            <RefreshCw class="w-3 h-3 inline" /> 重置
          </button>
          <div class="flex-1"></div>
          <button
            @click="handleRun"
            :disabled="isRunning"
            class="inline-flex items-center gap-1 text-xs px-3 py-1 rounded font-medium transition-opacity disabled:opacity-40"
            style="background-color: #313244; color: #cdd6f4; border: 1px solid #45475a;"
          >
            <Loader2 v-if="isRunning" class="w-3 h-3 animate-spin" />
            <Play v-else class="w-3 h-3" />
            {{ isRunning ? '运行中' : '运行' }}
          </button>
          <button
            @click="handleSubmit"
            :disabled="isSubmitting"
            class="inline-flex items-center gap-1 text-xs px-3 py-1 rounded font-medium text-white transition-opacity disabled:opacity-40"
            style="background: linear-gradient(135deg, #DC2626, #B91C1C);"
          >
            <Loader2 v-if="isSubmitting" class="w-3 h-3 animate-spin" />
            <Send v-else class="w-3 h-3" />
            {{ isSubmitting ? '提交中' : '提交' }}
          </button>
        </div>

        <!-- 错误提示 -->
        <div v-if="error" class="px-3 py-1.5 text-[11px] flex items-center gap-1.5" style="background-color: #1E1E2E; color: #f38ba8;">
          <AlertCircle class="w-3 h-3" />
          {{ error }}
        </div>

        <!-- 代码编辑器 -->
        <div class="flex-1 overflow-hidden" style="min-height: 200px;">
          <CodeEditor
            v-model="userCode"
            :language="selectedLanguage"
            height="100%"
            :submit-shortcut="true"
            @submit="handleSubmit"
          />
        </div>

        <!-- 运行结果面板 -->
        <div v-if="runResults.length > 0" class="border-t" style="background-color: #181825; border-color: #313244; max-height: 35%;">
          <div class="px-3 py-2 flex items-center gap-2 border-b" style="border-color: #313244;">
            <span class="text-xs font-bold" style="color: #cdd6f4;">运行结果</span>
            <span v-if="lastRunStatus === 'pass'" class="text-[10px] px-1.5 py-0.5 rounded text-white" style="background-color: #059669;">
              全部通过 ({{ runResults.filter(r => r.passed).length }}/{{ runResults.length }})
            </span>
            <span v-else-if="lastRunStatus === 'fail'" class="text-[10px] px-1.5 py-0.5 rounded text-white" style="background-color: #DC2626;">
              未通过 ({{ runResults.filter(r => r.passed).length }}/{{ runResults.length }})
            </span>
          </div>
          <div class="overflow-y-auto p-2 space-y-1.5" style="max-height: calc(35vh - 40px);">
            <div v-for="r in runResults" :key="r.caseNum" class="p-2 rounded text-[11px]" style="background-color: #1E1E2E;">
              <div class="flex items-center gap-1.5 mb-1">
                <CheckCircle2 v-if="r.passed" class="w-3 h-3" style="color: #a6e3a1;" />
                <XCircle v-else class="w-3 h-3" style="color: #f38ba8;" />
                <span style="color: #cdd6f4;">用例 {{ r.caseNum }}</span>
                <span class="ml-auto font-mono" style="color: #6c7086;">{{ r.timeMs }}ms</span>
              </div>
              <div class="font-mono space-y-0.5" style="color: #a6adc8;">
                <div>输入：{{ r.input }}</div>
                <div>期望：{{ r.expected }}</div>
                <div v-if="r.error" style="color: #f38ba8;">错误：{{ r.error }}</div>
                <div v-else :style="{ color: r.passed ? '#a6e3a1' : '#f38ba8' }">实际：{{ r.actual }}</div>
              </div>
            </div>
          </div>
        </div>
      </section>
    </div>
  </div>
</template>

<style scoped>
.coding-practice-page {
  overflow: hidden;
}

/* 滚动条美化 */
.coding-practice-page ::-webkit-scrollbar {
  width: 6px;
  height: 6px;
}
.coding-practice-page ::-webkit-scrollbar-thumb {
  background: var(--theme-border);
  border-radius: 3px;
}
.coding-practice-page ::-webkit-scrollbar-track {
  background: transparent;
}
</style>
