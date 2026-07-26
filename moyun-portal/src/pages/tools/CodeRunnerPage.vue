<script setup lang="ts">
import { ref, computed } from 'vue';
import { useRouter } from 'vue-router';
import { useHead } from '@vueuse/head';
import {
  Play, Loader2, Terminal, History, X, Clock,
  CheckCircle2, AlertCircle, Timer, Cpu, Code2, Trash2,
} from 'lucide-vue-next';
import SiteFooter from '@/components/SiteFooter.vue';
import Breadcrumb from '@/components/Breadcrumb.vue';
import { generateSeo } from '@/utils/seo';
import { runCode, getMyCodeRuns } from '@/api/codeRun';
import { useToast } from '@/composables/useToast';
import type { CodeRunVO } from '@/types/api';

const router = useRouter();
const toast = useToast();

type Lang = 'java' | 'python' | 'javascript';

// 各语言默认模板
const STARTER_CODE: Record<Lang, string> = {
  python: `# Python 在线运行示例\nname = input("请输入你的名字：")\nprint(f"你好，{name}！欢迎使用墨韵代码沙箱")\nprint("1+1 =", 1+1)`,
  javascript: `// JavaScript 在线运行示例\nconst greeting = "Hello from Moyun sandbox";\nconsole.log(greeting);\nconsole.log("1+1 =", 1+1);\n[1,2,3].map(x => x*2).forEach(x => console.log(x));`,
  java: `// Java 在线运行示例（类名须为 Main）\nimport java.util.Scanner;\n\npublic class Main {\n    public static void main(String[] args) {\n        Scanner sc = new Scanner(System.in);\n        String name = sc.hasNext() ? sc.next() : "World";\n        System.out.println("Hello, " + name + "!");\n        System.out.println("1+1 = " + (1+1));\n    }\n}`,
};

const language = ref<Lang>('python');
const code = ref<string>(STARTER_CODE.python);
const stdin = ref<string>('');
const running = ref(false);
const result = ref<CodeRunVO | null>(null);

// 历史抽屉
const historyOpen = ref(false);
const historyLoading = ref(false);
const history = ref<CodeRunVO[]>([]);
const historyTotal = ref(0);
const historyPage = ref(1);
const historyPageSize = 10;

const historyTotalPages = computed(() => Math.max(1, Math.ceil(historyTotal.value / historyPageSize)));

const breadcrumbs = computed(() => [
  { label: '面试指南', path: '/interview' },
  { label: '在线编程' },
]);

const statusText = computed(() => statusLabel(result.value?.status));

const statusIcon = computed(() => {
  switch (result.value?.status) {
    case 'success': return CheckCircle2;
    case 'failed': return AlertCircle;
    case 'timeout': return Clock;
    case 'running': return Loader2;
    default: return CheckCircle2;
  }
});

const statusColor = computed(() => statusColorValue(result.value?.status));

useHead(computed(() => generateSeo({
  title: '在线代码运行',
  description: '在线运行 Java / Python / JavaScript 代码，沙箱执行，超时 5 秒，输出截断 1MB',
  keywords: ['在线代码运行', '代码沙箱', 'Python', 'Java', 'JavaScript', '墨韵'],
  canonicalPath: '/tools/code',
  robots: 'noindex,nofollow',
})));

function selectLang(lang: Lang) {
  if (language.value === lang) return;
  // 切换语言时若当前编辑器内容为空或仍是默认模板，则替换为对应模板
  const isDefault = code.value === STARTER_CODE[language.value] || code.value.trim() === '';
  language.value = lang;
  if (isDefault) {
    code.value = STARTER_CODE[lang];
  }
}

async function handleRun() {
  if (running.value) return;
  if (!code.value.trim()) {
    toast.error('代码不能为空');
    return;
  }
  running.value = true;
  result.value = null;
  try {
    const res = await runCode({
      language: language.value,
      code: code.value,
      stdin: stdin.value || undefined,
    });
    if (res.code === 200 && res.data) {
      result.value = res.data;
    } else {
      toast.error(res.message || '运行失败');
    }
  } catch (err) {
    const e = err as { message?: string };
    toast.error(e?.message || '运行失败，请稍后重试');
  } finally {
    running.value = false;
  }
}

async function openHistory() {
  historyOpen.value = true;
  if (history.value.length === 0) {
    historyPage.value = 1;
    await loadHistory();
  }
}

async function loadHistory() {
  historyLoading.value = true;
  try {
    const res = await getMyCodeRuns({ pageNum: historyPage.value, pageSize: historyPageSize });
    if (res.code === 200 && res.data) {
      history.value = res.data.list || [];
      historyTotal.value = res.data.total || 0;
    }
  } catch (err) {
    const e = err as { message?: string };
    toast.error(e?.message || '加载历史失败');
  } finally {
    historyLoading.value = false;
  }
}

function gotoHistoryPage(p: number) {
  if (p < 1 || p > historyTotalPages.value) return;
  historyPage.value = p;
  loadHistory();
}

function loadFromHistory(item: CodeRunVO) {
  language.value = (item.language as Lang) || 'python';
  code.value = item.code || '';
  stdin.value = item.stdin || '';
  result.value = item;
  historyOpen.value = false;
}

function clearEditor() {
  if (!window.confirm('确定清空当前代码与输入吗？')) return;
  code.value = '';
  stdin.value = '';
  result.value = null;
}

function formatTime(ms?: number) {
  if (ms == null) return '-';
  if (ms < 1000) return ms + ' ms';
  return (ms / 1000).toFixed(2) + ' s';
}

function formatMem(kb?: number) {
  if (kb == null) return '-';
  if (kb < 1024) return kb + ' KB';
  return (kb / 1024).toFixed(2) + ' MB';
}

function formatCreateTime(t?: string) {
  if (!t) return '';
  return t.replace('T', ' ').slice(0, 19);
}

// 状态相关纯函数
function statusLabel(s?: string): string {
  switch (s) {
    case 'success': return '运行成功';
    case 'failed': return '运行失败';
    case 'timeout': return '运行超时';
    case 'running': return '运行中';
    default: return s || '-';
  }
}
function statusColorValue(s?: string): string {
  switch (s) {
    case 'success': return '#16a34a';
    case 'failed': return '#ef4444';
    case 'timeout': return '#f59e0b';
    default: return 'var(--theme-text-secondary)';
  }
}
function statusBadgeBg(s?: string): string {
  switch (s) {
    case 'success': return 'rgba(22,163,74,0.12)';
    case 'failed': return 'rgba(239,68,68,0.12)';
    case 'timeout': return 'rgba(245,158,11,0.12)';
    default: return 'var(--theme-bg)';
  }
}
function statusBadgeFg(s?: string): string {
  switch (s) {
    case 'success': return '#16a34a';
    case 'failed': return '#ef4444';
    case 'timeout': return '#f59e0b';
    default: return 'var(--theme-text-secondary)';
  }
}
</script>

<template>
  <div class="min-h-screen flex flex-col" style="background-color: var(--theme-bg);">
    <!-- 顶部面包屑栏 -->
    <div class="border-b sticky top-0 z-30 backdrop-blur-sm py-3" style="background-color: var(--theme-surface); border-color: var(--theme-border);">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 flex items-center justify-between gap-4">
        <Breadcrumb :items="breadcrumbs" />
        <div class="flex items-center gap-2">
          <button
            @click="openHistory"
            class="flex items-center text-sm px-3 py-1.5 rounded-lg transition hover:opacity-80"
            style="background-color: var(--theme-bg); color: var(--theme-text); border: 1px solid var(--theme-border);"
          >
            <History class="w-4 h-4 mr-1" />
            运行历史
          </button>
        </div>
      </div>
    </div>

    <!-- 主内容：左编辑器 / 右输出 -->
    <div class="flex-1 py-6">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div class="grid grid-cols-1 lg:grid-cols-2 gap-5">
          <!-- 左侧：编辑器 -->
          <div
            class="rounded-xl border overflow-hidden flex flex-col"
            style="background-color: var(--theme-surface); border-color: var(--theme-border);"
          >
            <!-- 语言切换 + 操作 -->
            <div
              class="flex items-center justify-between px-3 py-2 border-b"
              style="border-color: var(--theme-border); background-color: var(--theme-bg);"
            >
              <div class="flex items-center gap-1">
                <button
                  v-for="lang in (['python','javascript','java'] as Lang[])"
                  :key="lang"
                  @click="selectLang(lang)"
                  class="px-3 py-1.5 rounded-md text-xs font-medium transition"
                  :style="language === lang
                    ? { backgroundColor: 'var(--theme-primary)', color: '#fff' }
                    : { backgroundColor: 'var(--theme-surface)', color: 'var(--theme-text-secondary)', border: '1px solid var(--theme-border)' }"
                >
                  {{ lang === 'javascript' ? 'JavaScript' : lang === 'python' ? 'Python' : 'Java' }}
                </button>
              </div>
              <button
                @click="clearEditor"
                class="flex items-center text-xs px-2 py-1 rounded-md transition hover:opacity-80"
                style="color: var(--theme-text-secondary);"
                title="清空代码"
              >
                <Trash2 class="w-3.5 h-3.5 mr-1" />清空
              </button>
            </div>
            <!-- 代码编辑器 -->
            <textarea
              v-model="code"
              spellcheck="false"
              class="w-full p-4 font-mono text-sm resize-none focus:outline-none"
              style="background-color: var(--theme-surface); color: var(--theme-text); height: 320px;"
              :placeholder="`在此输入 ${language} 代码…`"
            ></textarea>
            <!-- 标准输入 -->
            <div
              class="border-t"
              style="border-color: var(--theme-border); background-color: var(--theme-bg);"
            >
              <div class="px-3 py-1.5 flex items-center text-xs" style="color: var(--theme-text-secondary);">
                <Terminal class="w-3.5 h-3.5 mr-1" />标准输入（stdin，可选）
              </div>
              <textarea
                v-model="stdin"
                spellcheck="false"
                class="w-full px-4 pb-3 pt-1 font-mono text-xs resize-none focus:outline-none"
                style="background-color: var(--theme-bg); color: var(--theme-text); height: 80px;"
                placeholder="如需程序读取输入，请在此填写…"
              ></textarea>
            </div>
            <!-- 运行按钮 -->
            <div class="px-3 py-2 border-t flex items-center justify-between" style="border-color: var(--theme-border);">
              <span class="text-xs" style="color: var(--theme-text-secondary);">
                超时 5s · 输出限 1MB · 无网络访问
              </span>
              <button
                @click="handleRun"
                :disabled="running"
                class="inline-flex items-center px-4 py-2 text-white rounded-lg text-sm transition hover:opacity-90 disabled:opacity-50"
                style="background-color: var(--theme-primary);"
              >
                <Loader2 v-if="running" class="w-4 h-4 mr-1 animate-spin" />
                <Play v-else class="w-4 h-4 mr-1" />
                {{ running ? '运行中' : '运行代码' }}
              </button>
            </div>
          </div>

          <!-- 右侧：输出 -->
          <div
            class="rounded-xl border overflow-hidden flex flex-col"
            style="background-color: var(--theme-surface); border-color: var(--theme-border);"
          >
            <div
              class="flex items-center justify-between px-3 py-2 border-b"
              style="border-color: var(--theme-border); background-color: var(--theme-bg);"
            >
              <span class="text-xs font-medium flex items-center" style="color: var(--theme-text);">
                <Terminal class="w-4 h-4 mr-1.5" />运行结果
              </span>
              <div v-if="result" class="flex items-center gap-3 text-xs" style="color: var(--theme-text-secondary);">
                <span class="flex items-center" :style="{ color: statusColor }">
                  <component :is="statusIcon" class="w-3.5 h-3.5 mr-0.5" />
                  {{ statusText }}
                </span>
                <span class="flex items-center" title="耗时">
                  <Timer class="w-3.5 h-3.5 mr-0.5" />{{ formatTime(result.runtimeMs) }}
                </span>
                <span class="flex items-center" title="内存">
                  <Cpu class="w-3.5 h-3.5 mr-0.5" />{{ formatMem(result.memKb) }}
                </span>
              </div>
            </div>

            <!-- 空状态 -->
            <div
              v-if="!result && !running"
              class="flex-1 flex flex-col items-center justify-center p-8 text-center"
              style="min-height: 320px;"
            >
              <Code2 class="w-12 h-12 mb-3" style="color: var(--theme-text-secondary); opacity: 0.4;" />
              <p class="text-sm" style="color: var(--theme-text-secondary);">
                点击「运行代码」查看输出结果
              </p>
            </div>

            <!-- 运行中骨架 -->
            <div
              v-else-if="running"
              class="flex-1 flex flex-col items-center justify-center p-8"
              style="min-height: 320px;"
            >
              <Loader2 class="w-8 h-8 animate-spin mb-3" style="color: var(--theme-primary);" />
              <p class="text-sm" style="color: var(--theme-text-secondary);">沙箱执行中，请稍候…</p>
            </div>

            <!-- 结果输出 -->
            <div v-else class="flex-1 flex flex-col overflow-hidden" style="min-height: 320px;">
              <!-- 标准输出 -->
              <div class="flex-1 overflow-auto">
                <div class="px-4 py-2 text-xs font-medium border-b sticky top-0" style="color: var(--theme-text-secondary); background-color: var(--theme-bg); border-color: var(--theme-border);">
                  stdout
                </div>
                <pre
                  class="px-4 py-3 font-mono text-sm whitespace-pre-wrap break-all"
                  style="color: var(--theme-text);"
                >{{ result?.output || '(无输出)' }}</pre>
              </div>
              <!-- 错误输出 -->
              <div v-if="result?.errorMsg" class="border-t overflow-auto" style="border-color: var(--theme-border); max-height: 200px;">
                <div class="px-4 py-2 text-xs font-medium border-b sticky top-0 flex items-center" style="color: #ef4444; background-color: var(--theme-bg); border-color: var(--theme-border);">
                  <AlertCircle class="w-3.5 h-3.5 mr-1" />stderr
                </div>
                <pre
                  class="px-4 py-3 font-mono text-xs whitespace-pre-wrap break-all"
                  style="color: #ef4444;"
                >{{ result.errorMsg }}</pre>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 历史抽屉 -->
    <transition name="drawer-fade">
      <div
        v-if="historyOpen"
        class="fixed inset-0 z-40"
        style="background-color: rgba(0,0,0,0.4);"
        @click="historyOpen = false"
      ></div>
    </transition>
    <transition name="drawer-slide">
      <div
        v-if="historyOpen"
        class="fixed top-0 right-0 bottom-0 z-50 w-full max-w-md flex flex-col shadow-2xl"
        style="background-color: var(--theme-surface); border-left: 1px solid var(--theme-border);"
      >
        <div class="flex items-center justify-between px-4 py-3 border-b" style="border-color: var(--theme-border);">
          <span class="text-sm font-medium flex items-center" style="color: var(--theme-text);">
            <History class="w-4 h-4 mr-1.5" />运行历史
          </span>
          <button @click="historyOpen = false" class="p-1 rounded hover:opacity-70" style="color: var(--theme-text-secondary);">
            <X class="w-5 h-5" />
          </button>
        </div>

        <div class="flex-1 overflow-auto">
          <div v-if="historyLoading" class="flex flex-col items-center justify-center py-16">
            <Loader2 class="w-8 h-8 animate-spin mb-3" style="color: var(--theme-primary);" />
            <p class="text-sm" style="color: var(--theme-text-secondary);">加载中…</p>
          </div>
          <div
            v-else-if="history.length === 0"
            class="flex flex-col items-center justify-center py-16"
          >
            <Clock class="w-10 h-10 mb-3" style="color: var(--theme-text-secondary); opacity: 0.4;" />
            <p class="text-sm" style="color: var(--theme-text-secondary);">暂无运行记录</p>
          </div>
          <div v-else class="divide-y" style="border-color: var(--theme-border);">
            <button
              v-for="item in history"
              :key="item.id"
              @click="loadFromHistory(item)"
              class="w-full text-left px-4 py-3 transition hover:opacity-80"
              style="border-color: var(--theme-border);"
            >
              <div class="flex items-center justify-between mb-1">
                <span
                  class="text-xs font-medium px-2 py-0.5 rounded"
                  :style="{ backgroundColor: statusBadgeBg(item.status), color: statusBadgeFg(item.status) }"
                >
                  {{ item.language }}
                </span>
                <span class="text-xs" style="color: var(--theme-text-secondary);">
                  {{ formatCreateTime(item.createTime) }}
                </span>
              </div>
              <div class="flex items-center gap-3 text-xs" style="color: var(--theme-text-secondary);">
                <span class="flex items-center" :style="{ color: statusColorValue(item.status) }">
                  {{ statusLabel(item.status) }}
                </span>
                <span class="flex items-center">
                  <Timer class="w-3 h-3 mr-0.5" />{{ formatTime(item.runtimeMs) }}
                </span>
              </div>
              <p class="mt-1 text-xs font-mono truncate" style="color: var(--theme-text);">
                {{ (item.code || '').slice(0, 60) }}{{ (item.code || '').length > 60 ? '…' : '' }}
              </p>
            </button>
          </div>
        </div>

        <div v-if="historyTotalPages > 1" class="px-4 py-3 border-t flex items-center justify-center gap-2" style="border-color: var(--theme-border);">
          <button
            @click="gotoHistoryPage(historyPage - 1)"
            :disabled="historyPage === 1"
            class="px-3 py-1.5 rounded-lg text-xs transition disabled:opacity-40"
            style="background-color: var(--theme-bg); border: 1px solid var(--theme-border); color: var(--theme-text);"
          >上一页</button>
          <span class="text-xs" style="color: var(--theme-text-secondary);">
            {{ historyPage }} / {{ historyTotalPages }}
          </span>
          <button
            @click="gotoHistoryPage(historyPage + 1)"
            :disabled="historyPage === historyTotalPages"
            class="px-3 py-1.5 rounded-lg text-xs transition disabled:opacity-40"
            style="background-color: var(--theme-bg); border: 1px solid var(--theme-border); color: var(--theme-text);"
          >下一页</button>
        </div>
      </div>
    </transition>

    <SiteFooter />
  </div>
</template>
