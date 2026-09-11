<script setup lang="ts">
import { RouterLink as Link } from 'vue-router';
import { Eye, Heart, Tag } from 'lucide-vue-next';
import LazyImage from './LazyImage.vue';
import Avatar from '@/components/Avatar.vue';
import type { Article } from '@/types/api';

interface Props {
  article: Article;
  /** 是否显示封面图（默认 true），文章详情页的"相关推荐"会传入 false 隐藏图片 */
  showCover?: boolean;
}

const props = withDefaults(defineProps<Props>(), {
  showCover: true,
});

// 获取作者用户名
function getAuthorUsername(article: Article): string {
  if (article.author?.username) return article.author.username;
  if ('authorUsername' in article) return article.authorUsername as string;
  if (article.author?.nickname) return article.author.nickname;
  if ('authorNickname' in article) return article.authorNickname as string;
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
    class="group rounded-xl overflow-hidden border shadow-sm hover:shadow-theme-md hover:-translate-y-0.5 transition-all duration-300 bg-theme-surface border-theme-border"
    :aria-label="'相关文章: ' + article.title"
  >
    <Link
      :to="`/article/${article.id}`"
      class="block"
      :aria-label="'查看文章: ' + article.title"
    >
      <!-- Cover Image（详情页相关推荐传入 showCover=false 可隐藏封面） -->
      <div v-if="showCover && article.cover" class="relative aspect-[16/9] overflow-hidden">
        <LazyImage
          :src="article.cover"
          :alt="article.title"
          :aspect-ratio="16/9"
        />
        <div class="absolute inset-0 bg-gradient-to-t from-black/50 via-transparent to-transparent opacity-0 group-hover:opacity-100 transition-opacity" />
        <div class="absolute top-3 left-3" v-if="getCategoryName(article)">
          <span class="px-2 py-0.5 text-white caption-text rounded-full bg-theme-primary">
            {{ getCategoryName(article) }}
          </span>
        </div>
      </div>

      <!-- Content -->
      <div class="p-4">
        <!-- 不显示封面时，把分类徽标移到标题上方（避免丢失分类信息） -->
        <div v-if="!showCover && getCategoryName(article)" class="mb-2">
          <span class="inline-block px-2 py-0.5 text-white caption-text rounded-full bg-theme-primary">
            {{ getCategoryName(article) }}
          </span>
        </div>
        <h3 class="card-title mb-2 line-clamp-2 group-hover:text-theme-primary transition-colors">
          {{ article.title }}
        </h3>

        <p class="card-summary mb-3 line-clamp-2">
          {{ article.excerpt || '' }}
        </p>

        <div class="flex flex-wrap gap-1.5 mb-3">
          <span
            v-for="tag in getTags(article).slice(0, 2)"
            :key="tag"
            class="inline-flex items-center px-2 py-0.5 caption-text rounded-full bg-theme-bg text-theme-text-secondary"
          >
            <Tag class="w-3 h-3 mr-1" aria-hidden="true" />
            {{ tag }}
          </span>
        </div>

        <div class="flex items-center justify-between meta-text">
          <div class="flex items-center space-x-1">
            <Avatar
              :src="getAuthorAvatar(article)"
              :name="getAuthorUsername(article)"
              size="sm"
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
