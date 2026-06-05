<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { useHead } from '@vueuse/head';
import { BookOpen, Quote as QuoteIcon, Star, ArrowRight, Users } from 'lucide-vue-next';
import SiteFooter from '@/components/SiteFooter.vue';
import LazyImage from '@/components/LazyImage.vue';

import { getReadingHome } from '@/api/reading';
import { generateSeo } from '@/utils/seo';
import type { Book, BookList, Quote as QuoteType } from '@/types/api';

const router = useRouter();
const loading = ref(false);
const error = ref<string | null>(null);
const bookLists = ref<BookList[]>([]);
const books = ref<Book[]>([]);
const quotes = ref<QuoteType[]>([]);

onMounted(() => {
  loadReadingHome();
});

async function loadReadingHome() {
  try {
    loading.value = true;
    error.value = null;
    
    const response = await getReadingHome();
    
    if (response.code === 200 && response.data) {
      bookLists.value = response.data.bookLists || [];
      books.value = response.data.books || [];
      quotes.value = response.data.quotes || [];
    } else {
      error.value = response.message || '加载数据失败';
    }
  } catch (err) {
    console.error('加载读书空间失败:', err);
    error.value = '加载数据失败，请稍后重试';
  } finally {
    loading.value = false;
  }
}

useHead(
  computed(() => {
    return generateSeo({
      title: '读书空间',
      description: '墨韵·智库读书空间，发现好书，分享阅读'
    });
  })
);
</script>

<template>
  <div class="min-h-screen flex flex-col bg-gray-50">
    <!-- 页面头部 -->
    <div class="py-12 bg-gradient-to-r from-purple-600 to-indigo-700">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 text-center">
        <div class="flex items-center justify-center mb-4">
          <BookOpen class="w-12 h-12 text-white mr-3" />
          <h1 class="text-4xl font-bold text-white">读书空间</h1>
        </div>
        <p class="text-xl text-purple-100 max-w-2xl mx-auto">
          发现好书，分享阅读，让知识流动起来
        </p>
      </div>
    </div>

    <!-- 主要内容 -->
    <div class="flex-1 py-8">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div v-if="loading" class="text-center py-12">
          <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-purple-600 mx-auto"></div>
          <p class="mt-4 text-gray-600">加载中...</p>
        </div>
        
        <div v-else-if="error" class="text-center py-12">
          <div class="bg-red-50 border border-red-200 rounded-lg p-6 max-w-md mx-auto">
            <p class="text-red-600">{{ error }}</p>
            <button 
              @click="loadReadingHome" 
              class="mt-4 px-4 py-2 bg-red-500 text-white rounded-lg hover:bg-red-600 transition"
            >
              重试
            </button>
          </div>
        </div>
        
        <template v-else>
          <!-- 精选书单 -->
          <div v-if="bookLists.length > 0" class="mb-12">
            <div class="flex items-center justify-between mb-6">
              <h2 class="text-2xl font-bold text-gray-900 flex items-center">
                <BookOpen class="w-6 h-6 mr-2 text-purple-600" />
                精选书单
              </h2>
              <button class="text-purple-600 hover:text-purple-800 font-medium flex items-center">
                查看更多
                <ArrowRight class="w-4 h-4 ml-1" />
              </button>
            </div>
            
            <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
              <div 
                v-for="(bookList, index) in bookLists" 
                :key="bookList.id"
                class="bg-white rounded-xl overflow-hidden shadow-sm hover:shadow-md transition cursor-pointer"
              >
                <div class="h-48 bg-gradient-to-r from-purple-100 to-indigo-100 relative">
                  <LazyImage 
                    :src="bookList.cover" 
                    :alt="bookList.title"
                    class="w-full h-full object-cover"
                  />
                </div>
                <div class="p-6">
                  <h3 class="text-lg font-semibold text-gray-900 mb-2">{{ bookList.title }}</h3>
                  <p class="text-gray-600 text-sm mb-4 line-clamp-2">{{ bookList.description }}</p>
                  <div class="flex items-center text-sm text-gray-500">
                    <span class="flex items-center mr-4">
                      <BookOpen class="w-4 h-4 mr-1" />
                      {{ bookList.bookCount }} 本
                    </span>
                    <span class="flex items-center mr-4">
                      <Users class="w-4 h-4 mr-1" />
                      {{ bookList.viewCount }} 浏览
                    </span>
                    <span class="flex items-center">
                      <Star class="w-4 h-4 mr-1" />
                      {{ bookList.likeCount }}
                    </span>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- 热门书籍 -->
          <div v-if="books.length > 0" class="mb-12">
            <div class="flex items-center justify-between mb-6">
              <h2 class="text-2xl font-bold text-gray-900 flex items-center">
                <BookOpen class="w-6 h-6 mr-2 text-purple-600" />
                热门书籍
              </h2>
              <button 
                @click="router.push('/reading/books')"
                class="text-purple-600 hover:text-purple-800 font-medium flex items-center"
              >
                查看更多
                <ArrowRight class="w-4 h-4 ml-1" />
              </button>
            </div>
            
            <div class="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-5 gap-6">
              <div 
                v-for="(book, index) in books" 
                :key="book.id"
                class="group cursor-pointer"
              >
                <div class="aspect-[3/4] rounded-lg overflow-hidden shadow-sm mb-3 group-hover:shadow-md transition">
                  <LazyImage 
                    :src="book.cover" 
                    :alt="book.title"
                    class="w-full h-full object-cover group-hover:scale-105 transition"
                  />
                </div>
                <h3 class="font-medium text-gray-900 text-sm mb-1 line-clamp-2">{{ book.title }}</h3>
                <p class="text-gray-500 text-xs mb-2">{{ book.author }}</p>
                <div class="flex items-center">
                  <Star class="w-3 h-3 text-yellow-400 mr-1" />
                  <span class="text-gray-600 text-xs">{{ book.rating }}</span>
                </div>
              </div>
            </div>
          </div>

          <!-- 金句摘录 -->
          <div v-if="quotes.length > 0">
            <div class="flex items-center justify-between mb-6">
              <h2 class="text-2xl font-bold text-gray-900 flex items-center">
                <Quote class="w-6 h-6 mr-2 text-purple-600" />
                金句摘录
              </h2>
              <button class="text-purple-600 hover:text-purple-800 font-medium flex items-center">
                查看更多
                <ArrowRight class="w-4 h-4 ml-1" />
              </button>
            </div>
            
            <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
              <div 
                v-for="(quote, index) in quotes" 
                :key="quote.id"
                class="bg-white rounded-xl p-6 shadow-sm hover:shadow-md transition"
              >
                <Quote class="w-8 h-8 text-purple-200 mb-4" />
                <p class="text-gray-800 text-lg italic mb-6 leading-relaxed">
                  "{{ quote.content }}"
                </p>
                <div class="flex items-center justify-between">
                  <div v-if="quote.book" class="flex items-center">
                    <div class="w-12 h-16 rounded overflow-hidden mr-3">
                      <LazyImage 
                        :src="quote.book.cover" 
                        :alt="quote.book.title"
                        class="w-full h-full object-cover"
                      />
                    </div>
                    <div>
                      <p class="text-sm font-medium text-gray-900">{{ quote.book.title }}</p>
                      <p class="text-xs text-gray-500">{{ quote.book.author }}</p>
                    </div>
                  </div>
                  <div class="flex items-center text-sm text-gray-500">
                    <Star class="w-4 h-4 mr-1" />
                    {{ quote.likeCount }}
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
