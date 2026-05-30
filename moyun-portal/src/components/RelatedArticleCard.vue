<script setup lang="ts">
import { RouterLink as Link } from 'vue-router';
import { Eye, Heart, Clock, Tag } from 'lucide-vue-next';
import LazyImage from './LazyImage.vue';
import type { Article } from '@/types/api';

interface Props {
  article: Article;
}

const props = defineProps<Props>();

// 获取作者用户名
function getAuthorUsername(article: Article): string {
  if (article.author?.username) return article.author.username;
  if ('authorUsername' in article) return article.authorUsername as string;
  return '';
}

// 获取作者头像
function getAuthorAvatar(article: Article): string {
  if (article.author?.avatar) return article.author.avatar;
  if ('authorAvatar' in article) return article.authorAvatar as string;
  return '';
}

// 获取分类名称
function getCategoryName(article: Article): string {
  if (article.category) return article.category;
  if ('categoryName' in article) return article.categoryName as string;
  return '';
}

// 获取标签列表
function getTags(article: Article): string[] {
  if (Array.isArray(article.tags)) return article.tags;
  if ('tagNames' in article && Array.isArray(article.tagNames)) return article.tagNames as string[];
  return [];
}
</script>

<template>
  <article 
    class="group rounded-2xl overflow-hidden border shadow-sm hover:shadow-lg hover:-translate-y-1 transition-all duration-300" 
    style="background-color: var(--theme-bg); border-color: var(--theme-border);"
    :aria-label="'相关文章: ' + article.title"
  >
    <Link 
      :to="`/article/${article.id}`" 
      class="block"
      :aria-label="'查看文章: ' + article.title"
    >
      <!-- Cover Image -->
      <div v-if="article.cover" class="relative aspect-[16/9] overflow-hidden">
        <LazyImage 
          :src="article.cover" 
          :alt="article.title"
          :aspect-ratio="16/9"
        />
        <div class="absolute inset-0 bg-gradient-to-t from-black/50 via-transparent to-transparent opacity-0 group-hover:opacity-100 transition-opacity" />
        <div class="absolute top-3 left-3" v-if="getCategoryName(article)">
          <span class="px-2.5 py-1 text-white text-xs font-medium rounded-full" style="background-color: var(--theme-primary);">
            {{ getCategoryName(article) }}
          </span>
        </div>
      </div>

      <!-- Content -->
      <div class="p-4">
        <Link 
          :to="`/article/${article.id}`"
          class="block text-base font-bold mb-2 line-clamp-2 transition-colors hover:opacity-80"
          style="color: var(--theme-text);"
        >
          {{ article.title }}
        </Link>

        <p class="text-sm mb-3 line-clamp-2" style="color: var(--theme-text-secondary);">
          {{ article.excerpt || '' }}
        </p>

        <div class="flex flex-wrap gap-1.5 mb-3">
          <span 
            v-for="tag in getTags(article).slice(0, 2)" 
            :key="tag"
            class="inline-flex items-center px-2 py-0.5 text-xs rounded-full"
            style="background-color: var(--theme-surface); color: var(--theme-text-secondary);"
          >
            <Tag class="w-3 h-3 mr-1" aria-hidden="true" />
            {{ tag }}
          </span>
        </div>

        <div class="flex items-center justify-between text-xs" style="color: var(--theme-text-secondary);">
          <div class="flex items-center space-x-1">
            <img 
              :src="getAuthorAvatar(article)" 
              :alt="getAuthorUsername(article)"
              class="w-6 h-6 rounded-full"
              loading="lazy"
            />
            <span>{{ getAuthorUsername(article) }}</span>
          </div>
          <div class="flex items-center space-x-3">
            <div class="flex items-center space-x-1">
              <Eye class="w-3.5 h-3.5" aria-hidden="true" />
              <span>{{ article.views || 0 }}</span>
            </div>
            <div class="flex items-center space-x-1">
              <Heart class="w-3.5 h-3.5" aria-hidden="true" />
              <span>{{ article.likes || 0 }}</span>
            </div>
          </div>
        </div>
      </div>
    </Link>
  </article>
</template>