<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { useHead } from '@vueuse/head';
import { Briefcase, BookOpen, Star, ArrowRight, Trophy, FileText } from 'lucide-vue-next';
import SiteFooter from '@/components/SiteFooter.vue';
import LazyImage from '@/components/LazyImage.vue';

import { getInterviewHome } from '@/api/interview';
import { generateSeo } from '@/utils/seo';
import type { InterviewCategory, InterviewQuestion, InterviewExperience, ResumeTemplate } from '@/types/api';
import { getSafeAvatar } from '@/utils/avatar';

const router = useRouter();
const loading = ref(false);
const error = ref<string | null>(null);
const categories = ref<InterviewCategory[]>([]);
const hotQuestions = ref<InterviewQuestion[]>([]);
const experiences = ref<InterviewExperience[]>([]);
const resumeTemplates = ref<ResumeTemplate[]>([]);

onMounted(() => {
  loadInterviewHome();
});

async function loadInterviewHome() {
  try {
    loading.value = true;
    error.value = null;
    
    const response = await getInterviewHome();
    
    if (response.code === 200 && response.data) {
      categories.value = response.data.categories || [];
      hotQuestions.value = response.data.hotQuestions || [];
      experiences.value = response.data.experiences || [];
      resumeTemplates.value = response.data.resumeTemplates || [];
    } else {
      error.value = response.message || '加载数据失败';
    }
  } catch (err) {
    console.error('加载面试指南失败:', err);
    error.value = '加载数据失败，请稍后重试';
  } finally {
    loading.value = false;
  }
}

function getDifficultyColor(difficulty: string) {
  switch(difficulty) {
    case 'easy': return 'bg-green-100 text-green-700';
    case 'medium': return 'bg-yellow-100 text-yellow-700';
    case 'hard': return 'bg-red-100 text-red-700';
    default: return 'bg-gray-100 text-gray-700';
  }
}

function getDifficultyText(difficulty: string) {
  switch(difficulty) {
    case 'easy': return '简单';
    case 'medium': return '中等';
    case 'hard': return '困难';
    default: return '未知';
  }
}

useHead(
  computed(() => {
    return generateSeo({
      title: '面试指南',
      description: '墨韵·智库面试指南，助力面试成功'
    });
  })
);
</script>

<template>
  <div class="min-h-screen flex flex-col bg-gray-50">
    <!-- 页面头部 -->
    <div class="py-12 bg-gradient-to-r from-blue-600 to-cyan-700">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 text-center">
        <div class="flex items-center justify-center mb-4">
          <Briefcase class="w-12 h-12 text-white mr-3" />
          <h1 class="text-4xl font-bold text-white">面试指南</h1>
        </div>
        <p class="text-xl text-blue-100 max-w-2xl mx-auto">
          备战面试，提升能力，斩获心仪offer
        </p>
      </div>
    </div>

    <!-- 主要内容 -->
    <div class="flex-1 py-8">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div v-if="loading" class="text-center py-12">
          <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mx-auto"></div>
          <p class="mt-4 text-gray-600">加载中...</p>
        </div>
        
        <div v-else-if="error" class="text-center py-12">
          <div class="bg-red-50 border border-red-200 rounded-lg p-6 max-w-md mx-auto">
            <p class="text-red-600">{{ error }}</p>
            <button 
              @click="loadInterviewHome" 
              class="mt-4 px-4 py-2 bg-red-500 text-white rounded-lg hover:bg-red-600 transition"
            >
              重试
            </button>
          </div>
        </div>
        
        <template v-else>
          <!-- 题目分类 -->
          <div v-if="categories.length > 0" class="mb-12">
            <h2 class="text-2xl font-bold text-gray-900 mb-6 flex items-center">
              <BookOpen class="w-6 h-6 mr-2 text-blue-600" />
              题目分类
            </h2>
            
            <div class="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-5 gap-4">
              <div 
                v-for="category in categories" 
                :key="category.id"
                class="bg-white rounded-xl p-6 shadow-sm hover:shadow-md transition cursor-pointer"
              >
                <div class="w-12 h-12 bg-blue-100 rounded-lg flex items-center justify-center mb-4">
                  <BookOpen class="w-6 h-6 text-blue-600" />
                </div>
                <h3 class="text-lg font-semibold text-gray-900 mb-2">{{ category.name }}</h3>
                <p class="text-gray-500 text-sm mb-3 line-clamp-2">{{ category.description }}</p>
                <div class="text-sm text-blue-600 font-medium">
                  {{ category.questionCount }} 道题目
                </div>
              </div>
            </div>
          </div>

          <!-- 热门题目 -->
          <div v-if="hotQuestions.length > 0" class="mb-12">
            <div class="flex items-center justify-between mb-6">
              <h2 class="text-2xl font-bold text-gray-900 flex items-center">
                <Trophy class="w-6 h-6 mr-2 text-blue-600" />
                热门题目
              </h2>
              <button 
                class="text-gray-400 cursor-default font-medium flex items-center"
              >
                面试指南专属页面
              </button>
            </div>
            
            <div class="space-y-4">
              <div 
                v-for="(question, index) in hotQuestions" 
                :key="question.id"
                class="bg-white rounded-xl p-6 shadow-sm hover:shadow-md transition cursor-pointer"
              >
                <div class="flex items-start justify-between">
                  <div class="flex-1">
                    <div class="flex items-center mb-3">
                      <span class="w-8 h-8 bg-blue-100 rounded-full flex items-center justify-center text-blue-600 font-bold text-sm mr-3">
                        {{ index + 1 }}
                      </span>
                      <span 
                        :class="['px-3 py-1 rounded-full text-xs font-medium mr-3', getDifficultyColor(question.difficulty)]"
                      >
                        {{ getDifficultyText(question.difficulty) }}
                      </span>
                    </div>
                    <h3 class="text-lg font-semibold text-gray-900 mb-3">{{ question.title }}</h3>
                    <div class="flex items-center gap-4 text-sm text-gray-500">
                      <div v-if="question.tags && question.tags.length > 0" class="flex flex-wrap gap-2">
                        <span 
                          v-for="tag in question.tags" 
                          :key="tag"
                          class="px-2 py-1 bg-gray-100 rounded text-xs"
                        >
                          {{ tag }}
                        </span>
                      </div>
                      <div v-if="question.companies && question.companies.length > 0" class="flex items-center">
                        <span class="text-gray-400 mr-1">热门公司：</span>
                        {{ question.companies.slice(0, 3).join('、') }}
                        {{ question.companies.length > 3 ? '...' : '' }}
                      </div>
                    </div>
                  </div>
                  <div class="text-right ml-6">
                    <div class="text-sm text-gray-500 mb-2">
                      <span>通过率 {{ question.acceptanceRate }}%</span>
                    </div>
                    <div class="text-sm text-gray-500">
                      <span>{{ question.submissionCount }} 次提交</span>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- 面经分享 -->
          <div v-if="experiences.length > 0" class="mb-12">
            <div class="flex items-center justify-between mb-6">
              <h2 class="text-2xl font-bold text-gray-900 flex items-center">
                <Briefcase class="w-6 h-6 mr-2 text-blue-600" />
                面经分享
              </h2>
              <button class="text-blue-600 hover:text-blue-800 font-medium flex items-center">
                查看更多
                <ArrowRight class="w-4 h-4 ml-1" />
              </button>
            </div>
            
            <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
              <div 
                v-for="experience in experiences" 
                :key="experience.id"
                class="bg-white rounded-xl overflow-hidden shadow-sm hover:shadow-md transition cursor-pointer"
              >
                <div class="p-6">
                  <div class="flex items-center mb-4">
                    <div class="w-12 h-12 rounded-full overflow-hidden mr-4">
                      <LazyImage 
                        :src="getSafeAvatar(experience.user.avatar, experience.user.id)" 
                        :alt="experience.user.nickname"
                        class="w-full h-full object-cover"
                      />
                    </div>
                    <div>
                      <p class="font-medium text-gray-900">{{ experience.user.nickname }}</p>
                      <p class="text-sm text-gray-500">{{ experience.company }} · {{ experience.position }}</p>
                    </div>
                  </div>
                  <h3 class="text-lg font-semibold text-gray-900 mb-3">{{ experience.title }}</h3>
                  <p class="text-gray-600 text-sm mb-4 line-clamp-3">{{ experience.content }}</p>
                  <div class="flex items-center text-sm text-gray-500">
                    <span class="flex items-center mr-4">
                      <Star class="w-4 h-4 mr-1" />
                      {{ experience.likeCount }}
                    </span>
                    <span class="flex items-center mr-4">
                      👁 {{ experience.viewCount }}
                    </span>
                    <span class="flex items-center">
                      💬 {{ experience.commentCount }}
                    </span>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- 简历模板 -->
          <div v-if="resumeTemplates.length > 0">
            <div class="flex items-center justify-between mb-6">
              <h2 class="text-2xl font-bold text-gray-900 flex items-center">
                <FileText class="w-6 h-6 mr-2 text-blue-600" />
                简历模板
              </h2>
              <button class="text-blue-600 hover:text-blue-800 font-medium flex items-center">
                查看更多
                <ArrowRight class="w-4 h-4 ml-1" />
              </button>
            </div>
            
            <div class="grid grid-cols-1 md:grid-cols-3 gap-6">
              <div 
                v-for="template in resumeTemplates" 
                :key="template.id"
                class="bg-white rounded-xl overflow-hidden shadow-sm hover:shadow-md transition cursor-pointer"
              >
                <div class="h-48 bg-gray-100">
                  <LazyImage 
                    :src="template.cover" 
                    :alt="template.title"
                    class="w-full h-full object-cover"
                  />
                </div>
                <div class="p-6">
                  <h3 class="text-lg font-semibold text-gray-900 mb-2">{{ template.title }}</h3>
                  <p class="text-gray-600 text-sm mb-3">{{ template.description }}</p>
                  <span class="inline-block px-3 py-1 bg-blue-100 text-blue-700 rounded-full text-xs mb-3">
                    {{ template.category }}
                  </span>
                  <div class="flex items-center text-sm text-gray-500">
                    <span class="flex items-center mr-4">
                      <Star class="w-4 h-4 mr-1" />
                      {{ template.likeCount }}
                    </span>
                    <span class="flex items-center">
                      📥 {{ template.downloadCount }}
                    </span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </template>
      </div>
    </div>

    <!-- 页脚 -->
    <div class="mt-auto">
      <SiteFooter />
    </div>
  </div>
</template>
