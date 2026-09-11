<script setup lang="ts">
/**
 * 编程题做题页（v10.6 阶段3 → v10.7 接入真实 OJ 判题）
 *
 * 布局：左右分栏（左题目描述 | 右代码编辑器 + 判题结果）
 * 判题：服务端权威评测（POST /portal/judge/submit，ProcessJudgeEngine 真实执行）
 *       - 代码模板为 ACM 模式（stdin 读输入 / stdout 写输出），与判题引擎一致
 *       - 7 语言全支持：javascript/typescript/python/java/go/cpp/rust
 *       - 隐藏用例内容不下发（防作弊，与选择题答案剥离同标准）
 * 复用：CodeEditor.vue（Monaco Editor）
 */
import { ref, computed, onMounted, onBeforeUnmount, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useHead } from '@vueuse/head';
import {
  Play, Send, Loader2, AlertCircle,
  CheckCircle2, XCircle, Clock, List, RefreshCw, Lightbulb,
  FileText, BookOpen, History, Terminal, ChevronLeft, ChevronRight,
} from 'lucide-vue-next';
import Breadcrumb from '@/components/Breadcrumb.vue';
import CodeEditor from '@/components/CodeEditor.vue';
import { generateSeo } from '@/utils/seo';
import { getQuestionDetail, getQuestionNeighbor } from '@/api/interview';
import { submitJudge, getSampleTestCases } from '@/api/judge';
import type { InterviewQuestionDetailVO, InterviewQuestionNeighborVO, JudgeResultVO, TestCaseVO } from '@/types/api';
import { useToast } from '@/composables/useToast';

const route = useRoute();
const router = useRouter();
const toast = useToast();

// ========== 题目数据 ==========
const loading = ref(true);
const error = ref<string | null>(null);
const question = ref<InterviewQuestionDetailVO | null>(null);

// 相邻题目导航（与来源列表页筛选同源：sort 升序 + createTime 降序）
const neighbor = ref<InterviewQuestionNeighborVO | null>(null);

// 样例用例（后端真实数据，判题结果回填展示用）
const sampleCases = ref<TestCaseVO[]>([]);

// ========== 代码编辑器 ==========
const LANGUAGE_OPTIONS = [
  { value: 'javascript', label: 'JavaScript' },
  { value: 'typescript', label: 'TypeScript' },
  { value: 'python', label: 'Python 3' },
  { value: 'java', label: 'Java' },
  { value: 'go', label: 'Go' },
  { value: 'cpp', label: 'C++ 17' },
  { value: 'rust', label: 'Rust' },
];
const selectedLanguage = ref('javascript');

/**
 * ACM 模式代码模板（stdin/stdout，与 ProcessJudgeEngine 一致）
 * 以「两数之和」完整可跑解法演示 IO 格式，用户可基于此改写
 */
const CODE_TEMPLATES: Record<string, string> = {
  javascript: `// ACM 模式：从标准输入读取，向标准输出写结果
// 输入格式：第1行数组，第2行目标值，如 "[2,7,11,15]" / "9"
// 输出格式：如下标对，如 "[0,1]"
const lines = require('fs').readFileSync(0, 'utf8').split('\\n');
const nums = JSON.parse(lines[0]);
const target = Number(lines[1]);

const seen = new Map();
for (let i = 0; i < nums.length; i++) {
  const j = seen.get(target - nums[i]);
  if (j !== undefined) {
    console.log(JSON.stringify([j, i]));
    process.exit(0);
  }
  seen.set(nums[i], i);
}
console.log(JSON.stringify([]));`,
  typescript: `// ACM 模式：从标准输入读取，向标准输出写结果
const lines = require('fs').readFileSync(0, 'utf8').split('\\n');
const nums: number[] = JSON.parse(lines[0]);
const target: number = Number(lines[1]);

const seen = new Map<number, number>();
for (let i = 0; i < nums.length; i++) {
  const j = seen.get(target - nums[i]);
  if (j !== undefined) {
    console.log(JSON.stringify([j, i]));
    process.exit(0);
  }
  seen.set(nums[i], i);
}
console.log(JSON.stringify([]));`,
  python: `# ACM 模式：从标准输入读取，向标准输出写结果
import sys
import json

lines = sys.stdin.read().split('\\n')
nums = json.loads(lines[0])
target = int(lines[1])

seen = {}
for i, v in enumerate(nums):
    if target - v in seen:
        # 注意：separators 去掉逗号后空格，保证输出为 [0,1] 而非 [0, 1]
        print(json.dumps([seen[target - v], i], separators=(',', ':')))
        sys.exit(0)
    seen[v] = i
print(json.dumps([], separators=(',', ':')))`,
  java: `// ACM 模式：框架已自动注入 BufferedReader br 读取标准输入
// 直接在 main 体内写逻辑即可（无需定义类与方法）
String l1 = br.readLine();
String l2 = br.readLine();

String inner = l1.substring(1, l1.length() - 1);
String[] parts = inner.isEmpty() ? new String[0] : inner.split(",");
int[] nums = new int[parts.length];
for (int i = 0; i < parts.length; i++) {
    nums[i] = Integer.parseInt(parts[i].trim());
}
int target = Integer.parseInt(l2.trim());

java.util.Map<Integer, Integer> seen = new java.util.HashMap<>();
for (int i = 0; i < nums.length; i++) {
    Integer j = seen.get(target - nums[i]);
    if (j != null) {
        System.out.println("[" + j + "," + i + "]");
        return;
    }
    seen.put(nums[i], i);
}
System.out.println("[]");`,
  go: `// ACM 模式：从标准输入读取，向标准输出写结果
package main

import (
	"bufio"
	"fmt"
	"os"
	"strconv"
	"strings"
)

func main() {
	sc := bufio.NewScanner(os.Stdin)
	sc.Buffer(make([]byte, 1<<20), 1<<20)
	sc.Scan()
	l1 := strings.TrimSpace(sc.Text())
	sc.Scan()
	target, _ := strconv.Atoi(strings.TrimSpace(sc.Text()))

	inner := strings.Trim(l1, "[]")
	var nums []int
	if inner != "" {
		for _, p := range strings.Split(inner, ",") {
			v, _ := strconv.Atoi(strings.TrimSpace(p))
			nums = append(nums, v)
		}
	}

	seen := map[int]int{}
	for i, v := range nums {
		if j, ok := seen[target-v]; ok {
			fmt.Printf("[%d,%d]\\n", j, i)
			return
		}
		seen[v] = i
	}
	fmt.Println("[]")
}`,
  cpp: `// ACM 模式：从标准输入读取，向标准输出写结果
#include <bits/stdc++.h>
using namespace std;

int main() {
    string line;
    getline(cin, line);
    // 解析 "[2,7,11,15]"
    line = line.substr(1, line.size() - 2);
    vector<int> nums;
    stringstream ss(line);
    string tok;
    while (getline(ss, tok, ',')) {
        nums.push_back(stoi(tok));
    }
    int target;
    cin >> target;

    unordered_map<int, int> seen;
    for (int i = 0; i < (int)nums.size(); i++) {
        auto it = seen.find(target - nums[i]);
        if (it != seen.end()) {
            cout << "[" << it->second << "," << i << "]\\n";
            return 0;
        }
        seen[nums[i]] = i;
    }
    cout << "[]\\n";
    return 0;
}`,
  rust: `// ACM 模式：从标准输入读取，向标准输出写结果
use std::collections::HashMap;
use std::io::{self, Read};

fn main() {
    let mut input = String::new();
    io::stdin().read_to_string(&mut input).unwrap();
    let mut lines = input.lines();
    let arr_line = lines.next().unwrap_or("[]");
    let target: i64 = lines.next().unwrap_or("0").trim().parse().unwrap_or(0);

    let inner = arr_line.trim().trim_start_matches('[').trim_end_matches(']');
    let nums: Vec<i64> = if inner.is_empty() {
        vec![]
    } else {
        inner.split(',').map(|s| s.trim().parse().unwrap_or(0)).collect()
    };

    let mut seen: HashMap<i64, usize> = HashMap::new();
    for (i, v) in nums.iter().enumerate() {
        if let Some(&j) = seen.get(&(target - v)) {
            println!("[{},{}]", j, i);
            return;
        }
        seen.insert(*v, i);
    }
    println!("[]");
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

// ========== 判题状态（服务端权威） ==========
const isRunning = ref(false);
const isSubmitting = ref(false);
/** 当前展示的判题结果（null=未运行） */
const judgeResult = ref<JudgeResultVO | null>(null);
/** 当前结果面板模式：run=运行（样例视角）/ submit=提交（全量判定） */
const resultMode = ref<'run' | 'submit'>('run');

// 提交历史（本会话内）
interface SubmissionRecord {
  submissionId: string | number;
  time: string;
  language: string;
  status: string;
  statusName: string;
  passedCount: number;
  totalCount: number;
  maxRuntime?: number;
  maxMemory?: number;
}
const submissionHistory = ref<SubmissionRecord[]>([]);

/** 判题状态元数据（AC/WA/TLE/MLE/RE/CE/SE/PENDING） */
const STATUS_META: Record<string, { label: string; color: string }> = {
  AC: { label: '通过', color: '#a6e3a1' },
  WA: { label: '答案错误', color: '#f38ba8' },
  TLE: { label: '超时限制', color: '#fab387' },
  MLE: { label: '内存超限', color: '#fab387' },
  RE: { label: '运行时错误', color: '#fab387' },
  CE: { label: '编译错误', color: '#fab387' },
  SE: { label: '系统错误', color: '#a6adc8' },
  PENDING: { label: '判题中', color: '#89b4fa' },
};
function statusMeta(code?: string) {
  return STATUS_META[(code || '').toUpperCase()] || { label: code || '未知', color: '#a6adc8' };
}

/** 判题结果状态汇总（供面板头部徽章） */
const resultBadge = computed(() => {
  if (!judgeResult.value) return null;
  const r = judgeResult.value;
  const meta = statusMeta(r.status);
  return {
    label: r.statusName || meta.label,
    color: meta.color,
    passed: r.passedCount,
    total: r.totalCount,
  };
});

/** 结果面板用例明细（样例回填输入/期望，隐藏用例仅状态） */
const caseDisplays = computed(() => {
  const r = judgeResult.value;
  if (!r?.caseResults?.length) return [];
  return r.caseResults.map((cr) => {
    // 样例用例：按 orderNum 匹配回填 input/expectedOutput
    const matched = cr.isSample
      ? sampleCases.value.find((tc) => tc.orderNum === cr.caseIndex)
      : undefined;
    return {
      caseNum: cr.caseIndex,
      isSample: !!cr.isSample,
      passed: !!cr.passed,
      input: matched?.input,
      expected: matched?.expectedOutput,
      actual: cr.actualOutput,
      error: cr.errorMessage,
      timeMs: cr.runtime,
    };
  });
});

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
  description: '在线编程题练习，多语言 IDE，真实 OJ 判题',
  keywords: ['编程题', '在线练习', 'OJ', '在线判题', 'IDE'],
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

// ========== 加载题目 + 真实样例用例 ==========
async function loadQuestion() {
  loading.value = true;
  error.value = null;
  try {
    const id = route.params.id;
    const res = await getQuestionDetail(id as string | number);
    if (res.code === 200 && res.data) {
      question.value = res.data;
      // 真实样例用例（判题数据，权威来源）
      await loadSampleCases(id as string | number);
    } else {
      error.value = res.message || '加载题目失败';
    }
  } catch (err: any) {
    error.value = err?.message || '加载题目失败，请稍后重试';
  } finally {
    loading.value = false;
  }
  loadNeighbor();
}

/** 相邻题目导航：与来源列表页同源筛选（difficulty/keyword 由列表页跳转时透传） */
async function loadNeighbor() {
  neighbor.value = null;
  try {
    const params: { practiceMode: string; difficulty?: string; keyword?: string } = {
      practiceMode: 'coding',
    };
    const q = route.query;
    if (typeof q.difficulty === 'string' && q.difficulty) params.difficulty = q.difficulty;
    if (typeof q.keyword === 'string' && q.keyword) params.keyword = q.keyword;
    const res = await getQuestionNeighbor(route.params.id as string | number, params);
    if (res.code === 200 && res.data) {
      neighbor.value = res.data;
    }
  } catch {
    // 导航数据加载失败不影响做题主流程
  }
}

/** 切题跳转（保留筛选上下文）；未提交的代码不跨题保留，确认防误触 */
function gotoNeighbor(id: string | number | null | undefined, dir: 'prev' | 'next') {
  if (id == null) return;
  const hasUnsavedCode = !submissionHistory.value.some(
    (r) => r.language === selectedLanguage.value
  ) && userCode.value.trim() !== (CODE_TEMPLATES[selectedLanguage.value] || '').trim();
  if (hasUnsavedCode) {
    if (!window.confirm('当前代码尚未提交，切换题目后不会保留，确定切换吗？')) return;
  }
  if (isRunning.value || isSubmitting.value) {
    toast.info('判题进行中，请等待完成后再切换');
    return;
  }
  const query: Record<string, string> = {};
  if (typeof route.query.difficulty === 'string' && route.query.difficulty) query.difficulty = route.query.difficulty;
  if (typeof route.query.keyword === 'string' && route.query.keyword) query.keyword = route.query.keyword;
  toast.info(dir === 'prev' ? '已切换到上一题' : '已切换到下一题');
  router.push({ path: `/learn/practice/coding/${id}`, query });
  window.scrollTo({ top: 0 });
}

async function loadSampleCases(questionId: string | number) {
  try {
    const res = await getSampleTestCases(questionId);
    if (res.code === 200 && Array.isArray(res.data)) {
      sampleCases.value = res.data;
    }
  } catch {
    // 样例加载失败不阻塞做题，左侧示例区留空
  }
}

// ========== 判题（服务端权威评测） ==========

/**
 * 调用后端 OJ 真实评测
 * @param mode run=运行（样例视角） / submit=提交（全量判定+历史记录）
 */
async function executeJudge(mode: 'run' | 'submit'): Promise<JudgeResultVO | null> {
  const q = question.value;
  if (!q) return null;
  const code = userCode.value;
  if (!code.trim()) {
    error.value = '代码不能为空';
    return null;
  }
  error.value = null;
  try {
    const res = await submitJudge({
      questionId: q.id,
      code,
      language: selectedLanguage.value,
      // run=仅样例自测（不落提交记录）；submit=全量判定（计统计+成长闭环）
      mode,
    });
    if (res.code === 200 && res.data) {
      return res.data;
    }
    error.value = res.message || '判题失败，请稍后重试';
    return null;
  } catch (err: any) {
    error.value = err?.message || '判题请求失败，请检查网络后重试';
    return null;
  }
}

async function handleRun() {
  if (isRunning.value || isSubmitting.value) return;
  isRunning.value = true;
  try {
    const result = await executeJudge('run');
    if (result) {
      judgeResult.value = result;
      resultMode.value = 'run';
    }
  } finally {
    isRunning.value = false;
  }
}

async function handleSubmit() {
  if (isRunning.value || isSubmitting.value) return;
  isSubmitting.value = true;
  try {
    const result = await executeJudge('submit');
    if (result) {
      judgeResult.value = result;
      resultMode.value = 'submit';
      // 记录提交历史（会话内）
      submissionHistory.value.unshift({
        submissionId: result.submissionId ?? '-',
        time: new Date().toLocaleTimeString('zh-CN', { hour12: false }),
        language: selectedLanguage.value,
        status: result.status,
        statusName: result.statusName || statusMeta(result.status).label,
        passedCount: result.passedCount,
        totalCount: result.totalCount,
        maxRuntime: result.maxRuntime,
        maxMemory: result.maxMemory,
      });
      activeTab.value = 'submissions';
    }
  } finally {
    isSubmitting.value = false;
  }
}

function resetCode() {
  if (confirm('确定要重置代码吗？当前代码将丢失。')) {
    codeByLang.value[selectedLanguage.value] = CODE_TEMPLATES[selectedLanguage.value] || '';
    judgeResult.value = null;
  }
}

function gotoList() {
  router.push('/learn/practice/coding');
}

/** 提交后跳转详情页：查看答题大纲、参考代码与评分标准（与选择题做题页入口一致） */
function gotoDetail() {
  if (question.value) {
    router.push(`/interview/question/${question.value.id}`);
  }
}

/** 样例用例展示列表（左侧示例区）：优先后端数据，降级为空 */
const displaySampleCases = computed(() => sampleCases.value);

// ========== 生命周期 ==========
onMounted(() => {
  loadQuestion();
  startTimer();
});

onBeforeUnmount(() => {
  stopTimer();
});

// 同页面切题（上一题/下一题）：路由参数变化时重置做题状态并加载新题
watch(() => route.params.id, (newId, oldId) => {
  if (newId && newId !== oldId) {
    // 判题进行中不允许切题（gotoNeighbor 已拦截，此处兜底）
    if (isRunning.value || isSubmitting.value) return;
    question.value = null;
    sampleCases.value = [];
    judgeResult.value = null;
    resultMode.value = 'run';
    submissionHistory.value = [];
    // 代码按语言分槽跨题不保留：重置为模板
    codeByLang.value = { javascript: CODE_TEMPLATES.javascript };
    selectedLanguage.value = 'javascript';
    activeTab.value = 'desc';
    error.value = null;
    loadQuestion();
  }
});
</script>

<template>
  <div class="coding-practice-page flex flex-col w-full max-w-[1280px] mx-auto" style="background-color: var(--theme-bg); height: calc(100vh - 56px);">
    <!-- 面包屑 + 工具栏 -->
    <div class="flex items-center justify-between px-4 py-2 border-b" style="border-color: var(--theme-border); background-color: var(--theme-card-bg);">
      <Breadcrumb :items="breadcrumbs" />
      <div class="flex items-center gap-2">
        <!-- 上一题/下一题（与来源列表页同源筛选，连续练习） -->
        <button
          @click="gotoNeighbor(neighbor?.prevId, 'prev')"
          :disabled="!neighbor?.prevId"
          :title="neighbor?.prevTitle ? `上一题：${neighbor.prevTitle}` : '已是第一题'"
          class="inline-flex items-center gap-1 text-xs px-2 py-1 rounded border disabled:opacity-40 disabled:cursor-not-allowed max-w-[160px]"
          style="border-color: var(--theme-border); color: var(--theme-text-secondary);"
        >
          <ChevronLeft class="w-3 h-3 flex-shrink-0" />
          <span class="truncate">上一题</span>
        </button>
        <span v-if="neighbor?.currentIndex" class="text-xs font-mono px-2 py-0.5 rounded"
              style="background-color: var(--theme-bg); color: var(--theme-text-secondary);">
          {{ neighbor.currentIndex }}/{{ neighbor.total }}
        </span>
        <button
          @click="gotoNeighbor(neighbor?.nextId, 'next')"
          :disabled="!neighbor?.nextId"
          :title="neighbor?.nextTitle ? `下一题：${neighbor.nextTitle}` : '已是最后一题'"
          class="inline-flex items-center gap-1 text-xs px-2 py-1 rounded border disabled:opacity-40 disabled:cursor-not-allowed max-w-[160px]"
          style="border-color: var(--theme-border); color: var(--theme-text-secondary);"
        >
          <span class="truncate">下一题</span>
          <ChevronRight class="w-3 h-3 flex-shrink-0" />
        </button>
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
              { key: 'submissions', label: `提交记录${submissionHistory.length ? ` (${submissionHistory.length})` : ''}`, icon: History },
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

            <!-- 判题模式说明 -->
            <div class="p-3 rounded-lg border mb-4 flex items-start gap-2" style="background-color: var(--theme-bg); border-color: var(--theme-border);">
              <Terminal class="w-4 h-4 mt-0.5 flex-shrink-0" style="color: var(--theme-primary);" />
              <div class="text-xs leading-relaxed" style="color: var(--theme-text-secondary);">
                <strong style="color: var(--theme-text);">判题模式：标准输入/输出（ACM 模式）</strong><br>
                代码从标准输入 stdin 读取用例输入，将结果输出到标准输出 stdout。
                支持样例与隐藏用例真实评测，隐藏用例通过后不展示内容。
              </div>
            </div>

            <!-- 示例测试用例（后端真实数据） -->
            <div v-if="displaySampleCases.length" class="space-y-3">
              <h3 class="text-sm font-bold mb-2" style="color: var(--theme-text);">示例</h3>
              <div v-for="tc in displaySampleCases" :key="tc.id" class="p-3 rounded-lg border" style="background-color: var(--theme-bg); border-color: var(--theme-border);">
                <div class="text-xs font-bold mb-1" style="color: var(--theme-text-secondary);">示例 {{ tc.orderNum }}</div>
                <div class="font-mono text-xs mb-1 whitespace-pre-wrap"><strong>输入：</strong>{{ tc.input }}</div>
                <div class="font-mono text-xs whitespace-pre-wrap"><strong>输出：</strong>{{ tc.expectedOutput }}</div>
                <div v-if="tc.explanation" class="text-[11px] mt-1.5 pt-1.5 border-t" style="color: var(--theme-text-secondary); border-color: var(--theme-border);">{{ tc.explanation }}</div>
              </div>
            </div>

            <!-- 标签 -->
            <div v-if="question.tags && question.tags.length" class="flex gap-1.5 mt-5 flex-wrap">
              <span v-for="tag in question.tags" :key="tag" class="text-[10px] px-2 py-0.5 rounded-full"
                    style="background-color: var(--theme-primary-bg); color: var(--theme-primary);">
                {{ tag }}
              </span>
            </div>

            <!-- 底部上一题/下一题导航（连续练习） -->
            <div class="mt-6 pt-4 border-t flex items-center justify-between gap-3"
                 style="border-color: var(--theme-border);">
              <button
                @click="gotoNeighbor(neighbor?.prevId, 'prev')"
                :disabled="!neighbor?.prevId"
                :title="neighbor?.prevTitle ? `上一题：${neighbor.prevTitle}` : '已是第一题'"
                class="flex items-center gap-1.5 px-3 py-1.5 text-xs font-medium rounded-lg border transition-colors disabled:opacity-40 disabled:cursor-not-allowed max-w-[45%]"
                style="border-color: var(--theme-border); color: var(--theme-text);"
              >
                <ChevronLeft class="w-4 h-4 flex-shrink-0" />
                <span class="truncate">{{ neighbor?.prevTitle || '上一题' }}</span>
              </button>
              <button
                @click="gotoNeighbor(neighbor?.nextId, 'next')"
                :disabled="!neighbor?.nextId"
                :title="neighbor?.nextTitle ? `下一题：${neighbor.nextTitle}` : '已是最后一题'"
                class="flex items-center gap-1.5 px-3 py-1.5 text-xs font-medium rounded-lg border transition-colors disabled:opacity-40 disabled:cursor-not-allowed max-w-[45%]"
                :style="neighbor?.nextId
                  ? { borderColor: 'var(--theme-primary)', color: 'var(--theme-primary)' }
                  : { borderColor: 'var(--theme-border)', color: 'var(--theme-text)' }"
              >
                <span class="truncate">{{ neighbor?.nextTitle || '下一题' }}</span>
                <ChevronRight class="w-4 h-4 flex-shrink-0" />
              </button>
            </div>
            <p v-if="neighbor && !neighbor.nextId" class="mt-2 text-[11px]" style="color: var(--theme-text-secondary);">
              已是当前筛选范围内的最后一题，可返回列表换个难度继续练习
            </p>
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
            <div v-if="submissionHistory.length === 0" class="text-center py-10">
              <History class="w-10 h-10 mx-auto mb-2" style="color: var(--theme-text-secondary); opacity: 0.4;" />
              <p class="text-xs" style="color: var(--theme-text-secondary);">暂无提交记录，点击右侧"提交"按钮开始判题</p>
            </div>
            <div v-else class="space-y-2">
              <div v-for="(s, idx) in submissionHistory" :key="idx"
                   class="p-3 rounded-lg border flex items-center gap-3" style="border-color: var(--theme-border); background-color: var(--theme-bg);">
                <span class="text-[10px] font-mono px-1.5 py-0.5 rounded font-bold"
                      :style="{ backgroundColor: statusMeta(s.status).color + '22', color: statusMeta(s.status).color }">
                  {{ s.status }}
                </span>
                <div class="flex-1 min-w-0">
                  <div class="text-xs font-medium" :style="{ color: statusMeta(s.status).color }">
                    {{ s.statusName }}
                    <span class="font-normal" style="color: var(--theme-text-secondary);">· {{ s.passedCount }}/{{ s.totalCount }} 用例通过</span>
                  </div>
                  <div class="text-[10px] mt-0.5" style="color: var(--theme-text-secondary);">
                    {{ s.time }} · {{ s.language.toUpperCase() }}
                    <template v-if="s.maxRuntime != null"> · {{ s.maxRuntime }}ms</template>
                    <template v-if="s.maxMemory != null"> · {{ (s.maxMemory / 1024).toFixed(1) }}MB</template>
                    <template v-if="s.submissionId !== '-'"> · #{{ s.submissionId }}</template>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      <!-- 右侧：代码编辑器 + 判题结果 -->
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
            :disabled="isRunning || isSubmitting"
            class="inline-flex items-center gap-1 text-xs px-3 py-1 rounded font-medium transition-opacity disabled:opacity-40"
            style="background-color: #313244; color: #cdd6f4; border: 1px solid #45475a;"
          >
            <Loader2 v-if="isRunning" class="w-3 h-3 animate-spin" />
            <Play v-else class="w-3 h-3" />
            {{ isRunning ? '判题中' : '运行' }}
          </button>
          <button
            @click="handleSubmit"
            :disabled="isSubmitting || isRunning"
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
          <AlertCircle class="w-3 h-3 flex-shrink-0" />
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

        <!-- 判题结果面板（服务端真实评测） -->
          <div v-if="judgeResult" class="border-t" style="background-color: #181825; border-color: #313244; max-height: 40%;">
          <!-- 结果头部：状态徽章 + 统计 -->
          <div class="px-3 py-2 flex items-center gap-2 border-b flex-wrap" style="border-color: #313244;">
            <span class="text-xs font-bold" style="color: #cdd6f4;">{{ resultMode === 'run' ? '运行结果' : '提交结果' }}</span>
            <span v-if="resultBadge" class="text-[10px] px-1.5 py-0.5 rounded font-bold text-white"
                  :style="{ backgroundColor: statusMeta(judgeResult.status).color }">
              {{ resultBadge.label }}
            </span>
            <span class="text-[10px]" style="color: #a6adc8;">
              用例 {{ judgeResult.passedCount }}/{{ judgeResult.totalCount }}
            </span>
            <span v-if="judgeResult.maxRuntime != null" class="text-[10px] font-mono" style="color: #6c7086;">
              {{ judgeResult.maxRuntime }}ms
            </span>
            <span v-if="judgeResult.maxMemory != null" class="text-[10px] font-mono" style="color: #6c7086;">
              {{ (judgeResult.maxMemory / 1024).toFixed(1) }}MB
            </span>
            <!-- 提交后引导：查看答题大纲、参考代码等完整内容 -->
            <button
              v-if="resultMode === 'submit' && question"
              @click="gotoDetail"
              title="查看题目解析、参考代码与评分标准"
              class="ml-auto inline-flex items-center gap-1 text-[10px] px-2 py-1 rounded transition-colors"
              style="color: #cdd6f4; border: 1px solid #45475a;"
            >
              <BookOpen class="w-3 h-3" />
              查看完整解析
            </button>
          </div>

          <div class="overflow-y-auto p-2 space-y-1.5" style="max-height: calc(40vh - 44px);">
            <!-- 编译/系统错误：整块展示错误信息 -->
            <div v-if="judgeResult.errorMessage && ['CE', 'SE'].includes((judgeResult.status || '').toUpperCase())"
                 class="p-2.5 rounded text-[11px] font-mono whitespace-pre-wrap"
                 style="background-color: #1E1E2E; color: #f38ba8; border: 1px solid #45475a;">
              {{ judgeResult.errorMessage }}
            </div>

            <!-- 首个失败用例详情（仅样例失败时后端回填，隐藏用例不泄露） -->
            <div v-if="judgeResult.failedCaseInput != null && !(judgeResult.caseResults || []).length"
                 class="p-2.5 rounded text-[11px]" style="background-color: #1E1E2E; border: 1px solid #45475a;">
              <div class="flex items-center gap-1.5 mb-1.5">
                <XCircle class="w-3.5 h-3.5" style="color: #f38ba8;" />
                <span class="font-bold" style="color: #f38ba8;">未通过的用例</span>
              </div>
              <div class="font-mono space-y-1" style="color: #a6adc8;">
                <div>输入：{{ judgeResult.failedCaseInput }}</div>
                <div>期望：{{ judgeResult.failedCaseExpected }}</div>
                <div :style="{ color: '#f38ba8' }">实际：{{ judgeResult.failedCaseActual || judgeResult.errorMessage || '(无输出)' }}</div>
              </div>
            </div>

            <!-- 用例明细列表 -->
            <div v-for="c in caseDisplays" :key="c.caseNum" class="p-2 rounded text-[11px]" style="background-color: #1E1E2E;">
              <div class="flex items-center gap-1.5 mb-1">
                <CheckCircle2 v-if="c.passed" class="w-3 h-3" style="color: #a6e3a1;" />
                <XCircle v-else class="w-3 h-3" style="color: #f38ba8;" />
                <span style="color: #cdd6f4;">用例 {{ c.caseNum }}</span>
                <span class="text-[9px] px-1 py-0.5 rounded" :style="c.isSample ? { backgroundColor: '#45475a', color: '#a6adc8' } : { backgroundColor: '#313244', color: '#6c7086' }">
                  {{ c.isSample ? '样例' : '隐藏' }}
                </span>
                <span class="ml-auto font-mono" style="color: #6c7086;">{{ c.timeMs ?? '-' }}ms</span>
              </div>
              <!-- 样例失败：展示输入/期望/实际 -->
              <div v-if="!c.passed && c.isSample && (c.input != null || c.error)" class="font-mono space-y-0.5" style="color: #a6adc8;">
                <div v-if="c.input != null">输入：{{ c.input }}</div>
                <div v-if="c.expected != null">期望：{{ c.expected }}</div>
                <div v-if="c.error" style="color: #f38ba8;">错误：{{ c.error }}</div>
                <div v-else-if="c.actual != null" :style="{ color: '#f38ba8' }">实际：{{ c.actual }}</div>
              </div>
              <!-- 隐藏用例失败：只显示错误类型，不泄露内容 -->
              <div v-else-if="!c.passed && !c.isSample" class="font-mono" style="color: #6c7086;">
                <span v-if="c.error">{{ c.error }}</span>
                <span v-else>未通过（隐藏用例内容不展示）</span>
              </div>
            </div>

            <!-- 兜底：无用例明细但整体失败 -->
            <div v-if="!caseDisplays.length && !judgeResult.failedCaseInput && !['CE', 'SE'].includes((judgeResult.status || '').toUpperCase())"
                 class="p-2.5 rounded text-[11px] text-center" style="background-color: #1E1E2E; color: #a6adc8;">
              {{ judgeResult.statusName || statusMeta(judgeResult.status).label }}
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
