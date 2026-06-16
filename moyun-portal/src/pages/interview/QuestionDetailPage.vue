<script setup lang="ts">
import { ref, onMounted, computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useHead } from '@vueuse/head';
import {
  ChevronLeft, ChevronDown, ChevronUp, ThumbsUp, Bookmark,
  Code2, MessageSquare, CheckCircle, XCircle, Clock, Zap, Lightbulb, BookOpen
} from 'lucide-vue-next';
import SiteFooter from '@/components/SiteFooter.vue';
import { generateSeo } from '@/utils/seo';
import {
  getQuestionDetail, submitAnswer, toggleQuestionLike, toggleQuestionBookmark,
} from '@/api/interview';
import type { InterviewQuestionDetailVO, InterviewSubmissionVO } from '@/types/api';

const route = useRoute();
const router = useRouter();

const questionId = computed(() => route.params.id as string);

const loading = ref(false);
const submitting = ref(false);
const question = ref<InterviewQuestionDetailVO | null>(null);
const submissions = ref<InterviewSubmissionVO[]>([]);
const toast = ref<{ message: string; type: 'success' | 'error' } | null>(null);
const toastTimer = ref<number | null>(null);

const showHint = ref(false);
const showSolution = ref(false);

const answerType = ref<'code' | 'text'>('code');
const language = ref('javascript');
const codeContent = ref('');
const textContent = ref('');

const difficultyMap: Record<string, { label: string; class: string }> = {
  easy: { label: '简单', class: 'bg-green-100 text-green-700' },
  medium: { label: '中等', class: 'bg-yellow-100 text-yellow-700' },
  hard: { label: '困难', class: 'bg-red-100 text-red-700' },
};

function showToast(message: string, type: 'success' | 'error' = 'success') {
  toast.value = { message, type };
  if (toastTimer.value) window.clearTimeout(toastTimer.value);
  toastTimer.value = window.setTimeout(() => {
    toast.value = null;
  }, 3000);
}

onMounted(() => {
  loadQuestionDetail();
});

async function loadQuestionDetail() {
  try {
    loading.value = true;
    const res = await getQuestionDetail(questionId.value);
    if (res.code === 200 && res.data) {
      question.value = res.data;
      submissions.value = res.data.mySubmissions?.slice(0, 10) || [];
    } else {
      showToast(res.message || '加载题目失败', 'error');
    }
  } catch (err: any) {
    console.error('加载题目详情失败:', err);
    showToast(err?.message || '加载题目详情失败，请稍后重试', 'error');
  } finally {
    loading.value = true ? false : false;
    loading.value = false;
  }
}

async function handleSubmit() {
  if (!question.value) return;
  const body: any = { answerType: answerType.value };
  if (answerType.value === 'code') {
    if (!codeContent.value.trim()) {
      showToast('请输入代码内容', 'error');
      return;
    }
    body.code = codeContent.value;
    body.language = language.value;
  } else {
    if (!textContent.value.trim()) {
      showToast('请输入答案内容', 'error');
      return;
    }
    body.content = textContent.value;
    body.language = language.value;
  }
  try {
    submitting.value = true;
    const res = await submitAnswer(question.value.id, body);
    if (res.code === 200 && res.data) {
      submissions.value = [res.data, ...submissions.value].slice(0, 10);
      showToast('提交成功！', 'success');
    } else {
      showToast(res.message || '提交失败', 'error');
    }
  } catch (err: any) {
    console.error('提交答案失败:', err);
    showToast(err?.message || '提交失败，请稍后重试', 'error');
  } finally {
    submitting.value = false;
  }
}

async function handleLike() {
  if (!question.value) return;
  try {
    const res = await toggleQuestionLike(question.value.id);
    if (res.code === 200 && res.data) {
      question.value.liked = res.data.liked;
      question.value.likeCount = res.data.likeCount;
      showToast(res.data.liked ? '点赞成功' : '已取消点赞', 'success');
    } else {
      showToast(res.message || '操作失败', 'error');
    }
  } catch (err: any) {
    showToast(err?.message || '操作失败', 'error');
  }
}

async function handleBookmark() {
  if (!question.value) return;
  try {
    const res = await toggleQuestionBookmark(question.value.id);
    if (res.code === 200 && res.data) {
      question.value.bookmarked = res.data.bookmarked;
      showToast(res.data.bookmarked ? '收藏成功' : '已取消收藏', 'success');
    } else {
      showToast(res.message || '操作失败', 'error');
    }
  } catch (err: any) {
    showToast(err?.message || '操作失败', 'error');
  }
}

useHead(
  computed(() => {
    return generateSeo({
      title: question.value?.title || '题目详情',
      description: question.value?.description?.slice(0, 120) || '面试题目',
    });
  })
);
</script>

<template>
  <div class="min-h-screen flex flex-col bg-gray-50">
    <!-- 顶部导航 -->
    <div class="bg-white border-b border-gray-200 sticky top-0 z-30">
      <div class="max-w-6xl mx-auto px-4 py-3 flex items-center justify-between">
        <button
          @click="router.back()"
          class="flex items-center text-gray-600 hover:text-blue-600 transition text-sm"
        >
          <ChevronLeft class="w-4 h-4 mr-1" />
          返回
        </button>
        <span class="text-sm text-gray-500">面试题目</span>
        <span class="w-12"></span>
      </div>
    </div>

    <!-- Toast -->
    <div
      v-if="toast"
      class="fixed top-20 left-1/2 -translate-x-1/2 z-50 px-4 py-2 rounded-lg shadow-lg text-sm transition"
      :class="toast.type === 'success' ? 'bg-green-500 text-white' : 'bg-red-500 text-white'"
    >
      {{ toast.message }}
    </div>

    <!-- 主体内容 -->
    <div class="flex-1 py-8">
      <div class="max-w-6xl mx-auto px-4">
        <!-- Loading -->
        <div v-if="loading && !question" class="text-center py-12">
          <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mx-auto"></div>
          <p class="mt-4 text-gray-600">加载中...</p>
        </div>

        <template v-else-if="question">
          <!-- 题目标题区 -->
          <div class="bg-white rounded-xl shadow-sm p-6 mb-6">
            <div class="flex items-start justify-between mb-4">
              <div class="flex-1">
                <div class="flex items-center flex-wrap gap-2 mb-3">
                  <span
                    class="px-3 py-1 rounded-full text-xs font-medium"
                    :class="difficultyMap[question.difficulty]?.class || 'bg-gray-100 text-gray-700'"
                  >
                    {{ difficultyMap[question.difficulty]?.label || question.difficulty }}
                  </span>
                  <span v-if="question.categoryName" class="px-3 py-1 bg-blue-50 text-blue-700 rounded-full text-xs font-medium">
                    <BookOpen class="w-3 h-3 inline mr-1" />
                    {{ question.categoryName }}
                  </span>
                  <span
                    v-for="tag in question.tags"
                    :key="tag"
                    class="px-2 py-1 bg-gray-100 text-gray-600 rounded text-xs"
                  >
                    #{{ tag }}
                  </span>
                </div>
                <h1 class="text-2xl font-bold text-gray-900 mb-2">{{ question.title }}</h1>
                <div class="flex flex-wrap items-center gap-4 text-sm text-gray-500">
                  <span class="flex items-center">
                    <Zap class="w-4 h-4 mr-1 text-yellow-500" />
                    通过率 {{ question.acceptanceRate }}%
                  </span>
                  <span class="flex items-center">
                    <MessageSquare class="w-4 h-4 mr-1" />
                    {{ question.submissionCount }} 次提交
                  </span>
                  <span class="flex items-center">
                    <ThumbsUp class="w-4 h-4 mr-1" />
                    {{ question.likeCount }} 点赞
                  </span>
                </div>
              </div>

              <!-- 操作区 -->
              <div class="flex items-center gap-2 ml-4">
                <button
                  @click="handleLike"
                  class="flex items-center px-4 py-2 rounded-lg text-sm transition"
                  :class="question.liked ? 'bg-blue-50 text-blue-600 border border-blue-200' : 'bg-white text-gray-700 border border-gray-200 hover:border-blue-300'"
                >
                  <ThumbsUp class="w-4 h-4 mr-1" />
                  {{ question.liked ? '已点赞' : '点赞' }}
                </button>
                <button
                  @click="handleBookmark"
                  class="flex items-center px-4 py-2 rounded-lg text-sm transition"
                  :class="question.bookmarked ? 'bg-yellow-50 text-yellow-600 border border-yellow-200' : 'bg-white text-gray-700 border border-gray-200 hover:border-yellow-300'"
                >
                  <Bookmark class="w-4 h-4 mr-1" />
                  {{ question.bookmarked ? '已收藏' : '收藏' }}
                </button>
              </div>
            </div>

            <!-- 公司标签 -->
            <div v-if="question.companies && question.companies.length > 0" class="flex items-center flex-wrap gap-2">
              <span class="text-sm text-gray-500 mr-2">出现公司：</span>
              <span
                v-for="c in question.companies"
                :key="c.id"
                class="px-3 py-1 bg-indigo-50 text-indigo-700 rounded-full text-xs font-medium"
              >
                {{ c.name }}
              </span>
            </div>
          </div>

          <!-- 题目描述 -->
          <div class="bg-white rounded-xl shadow-sm p-6 mb-6">
            <h2 class="text-lg font-semibold text-gray-900 mb-3">题目描述</h2>
            <div class="text-gray-700 leading-relaxed whitespace-pre-wrap text-sm">
              {{ question.description || '暂无题目描述' }}
            </div>
          </div>

          <!-- Hint -->
          <div v-if="question.hint" class="bg-white rounded-xl shadow-sm mb-6 overflow-hidden">
            <button
              @click="showHint = !showHint"
              class="w-full px-6 py-4 flex items-center justify-between hover:bg-gray-50 transition"
            >
              <div class="flex items-center text-gray-800">
                <Lightbulb class="w-5 h-5 mr-2 text-yellow-500" />
                <span class="font-medium">提示 (Hint)</span>
              </div>
              <ChevronDown v-if="!showHint" class="w-5 h-5 text-gray-400" />
              <ChevronUp v-else class="w-5 h-5 text-gray-400" />
            </button>
            <div v-if="showHint" class="px-6 pb-6 border-t border-gray-100 pt-4">
              <div class="text-gray-700 text-sm leading-relaxed whitespace-pre-wrap bg-yellow-50 p-4 rounded-lg">
                {{ question.hint }}
              </div>
            </div>
          </div>

          <!-- Solution -->
          <div v-if="question.solution" class="bg-white rounded-xl shadow-sm mb-6 overflow-hidden">
            <button
              @click="showSolution = !showSolution"
              class="w-full px-6 py-4 flex items-center justify-between hover:bg-gray-50 transition"
            >
              <div class="flex items-center text-gray-800">
                <CheckCircle class="w-5 h-5 mr-2 text-green-500" />
                <span class="font-medium">参考答案 (Solution)</span>
              </div>
              <ChevronDown v-if="!showSolution" class="w-5 h-5 text-gray-400" />
              <ChevronUp v-else class="w-5 h-5 text-gray-400" />
            </button>
            <div v-if="showSolution" class="px-6 pb-6 border-t border-gray-100 pt-4">
              <pre class="bg-gray-900 text-gray-100 rounded-lg p-4 text-xs overflow-x-auto"><code>{{ question.solution }}</code></pre>
            </div>
          </div>

          <!-- 练习区 -->
          <div class="bg-white rounded-xl shadow-sm p-6 mb-6">
            <div class="flex items-center justify-between mb-4">
              <h2 class="text-lg font-semibold text-gray-900 flex items-center">
                <Code2 class="w-5 h-5 mr-2 text-blue-500" />
                编写你的答案
              </h2>
              <div class="flex items-center gap-3 text-sm">
                <div class="flex items-center bg-gray-100 rounded-lg overflow-hidden">
                  <button
                    @click="answerType = 'code'"
                    class="px-3 py-1.5 transition"
                    :class="answerType === 'code' ? 'bg-blue-500 text-white' : 'text-gray-600'"
                  >
                    代码
                  </button>
                  <button
                    @click="answerType = 'text'"
                    class="px-3 py-1.5 transition"
                    :class="answerType === 'text' ? 'bg-blue-500 text-white' : 'text-gray-600'"
                  >
                    文本
                  </button>
                </div>
                <select
                  v-if="answerType === 'code'"
                  v-model="language"
                  class="px-3 py-1.5 bg-gray-100 rounded-lg text-gray-700 text-sm focus:outline-none"
                >
                  <option value="javascript">JavaScript</option>
                  <option value="typescript">TypeScript</option>
                  <option value="python">Python</option>
                  <option value="java">Java</option>
                  <option value="go">Go</option>
                  <option value="cpp">C++</option>
                  <option value="rust">Rust</option>
                </select>
              </div>
            </div>

            <textarea
              v-if="answerType === 'code'"
              v-model="codeContent"
              class="w-full h-64 p-4 border border-gray-200 rounded-lg font-mono text-sm bg-gray-900 text-gray-100 focus:outline-none focus:ring-2 focus:ring-blue-500"
              placeholder="在此输入你的代码解... 例如:&#10;function twoSum(nums, target) {&#10;  // TODO: 你的答案&#10;}"
            ></textarea>

            <textarea
              v-else
              v-model="textContent"
              class="w-full h-64 p-4 border border-gray-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
              placeholder="在此输入你的分析或文字答案..."
            ></textarea>

            <div class="flex items-center justify-between mt-4">
              <span class="text-xs text-gray-400">提示：你可以多次提交，最近 10 次会在下方展示</span>
              <button
                @click="handleSubmit"
                :disabled="submitting"
                class="px-6 py-2 bg-blue-600 hover:bg-blue-700 disabled:bg-gray-400 text-white rounded-lg text-sm font-medium transition flex items-center"
              >
                <span v-if="submitting" class="w-4 h-4 rounded-full border-2 border-white border-t-transparent animate-spin mr-2"></span>
                {{ submitting ? '提交中...' : '提交答案' }}
              </button>
            </div>
          </div>

          <!-- 提交历史 -->
          <div v-if="submissions.length > 0" class="bg-white rounded-xl shadow-sm p-6">
            <h2 class="text-lg font-semibold text-gray-900 mb-4 flex items-center">
              <Clock class="w-5 h-5 mr-2 text-gray-500" />
              最近提交 ({{ submissions.length }})
            </h2>
            <div class="overflow-hidden rounded-lg border border-gray-100">
              <table class="w-full text-sm">
                <thead class="bg-gray-50 text-gray-600">
                  <tr>
                    <th class="px-4 py-2 text-left font-medium">时间</th>
                    <th class="px-4 py-2 text-left font-medium">语言/类型</th>
                    <th class="px-4 py-2 text-left font-medium">状态</th>
                    <th class="px-4 py-2 text-left font-medium">运行</th>
                    <th class="px-4 py-2 text-left font-medium">内存</th>
                  </tr>
                </thead>
                <tbody>
                  <tr
                    v-for="s in submissions"
                    :key="s.id"
                    class="border-t border-gray-100 hover:bg-gray-50 transition"
                  >
                    <td class="px-4 py-3 text-gray-700">{{ s.createTime || '-' }}</td>
                    <td class="px-4 py-3 text-gray-700">{{ s.language || s.answerType || '-' }}</td>
                    <td class="px-4 py-3">
                      <span
                        v-if="s.isSuccess"
                        class="inline-flex items-center text-green-600"
                      >
                        <CheckCircle class="w-4 h-4 mr-1" />
                        通过
                      </span>
                      <span v-else class="inline-flex items-center text-red-600">
                        <XCircle class="w-4 h-4 mr-1" />
                        {{ s.status || '未通过' }}
                      </span>
                    </td>
                    <td class="px-4 py-3 text-gray-700">{{ s.runtime ? s.runtime + ' ms' : '-' }}</td>
                    <td class="px-4 py-3 text-gray-700">{{ s.memoryUsage ? s.memoryUsage + ' KB' : '-' }}</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </template>

        <div v-else class="text-center py-12">
          <p class="text-gray-600">未找到题目信息</p>
          <button
            @click="router.push('/interview')"
            class="mt-4 px-4 py-2 bg-blue-500 text-white rounded-lg text-sm hover:bg-blue-600 transition"
          >
            返回面试指南
          </button>
        </div>
      </div>
    </div>

    <SiteFooter />
  </div>
</template>
