<script setup lang="ts">
import { RouterLink as Link } from 'vue-router';
import { Eye, Heart, Clock, Tag } from 'lucide-vue-next';
import LazyImage from './LazyImage.vue';
import type { Article } from '@/types';

interface Props {
  article: Article;
}

defineProps<Props>();
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
        <div class="absolute top-3 left-3">
          <span class="px-2.5 py-1 text-white text-xs font-medium rounded-full" style="background-color: var(--theme-primary);">
            {{ article.category }}
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
          {{ article.excerpt }}
        </p>

        <div class="flex flex-wrap gap-1.5 mb-3">
          <span 
            v-for="tag in article.tags.slice(0, 2)" 
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
              :src="article.author.avatar" 
              :alt="article.author.username"
              class="w-6 h-6 rounded-full"
              loading="lazy"
            />
            <span>{{ article.author.username }}</span>
          </div>
          <div class="flex items-center space-x-3">
            <div class="flex items-center space-x-1">
              <Eye class="w-3.5 h-3.5" aria-hidden="true" />
              <span>{{ article.views }}</span>
            </div>
            <div class="flex items-center space-x-1">
              <Heart class="w-3.5 h-3.5" aria-hidden="true" />
              <span>{{ article.likes }}</span>
            </div>
          </div>
        </div>
      </div>
    </Link>
  </article>
</template>