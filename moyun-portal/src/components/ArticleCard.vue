<script setup lang="ts">
import { RouterLink as Link, useRouter } from 'vue-router';
import { Clock, Tag } from 'lucide-vue-next';
import LazyImage from '@/components/LazyImage.vue';
import Avatar from '@/components/Avatar.vue';
import type { Article } from '@/types/api';

const router = useRouter();

interface Props {
  article: Article;
}

const props = defineProps<Props>();

function formatDateTime(dateStr: string | undefined) {
  if (!dateStr) return '';
  const date = new Date(dateStr);
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  const hours = String(date.getHours()).padStart(2, '0');
  const minutes = String(date.getMinutes()).padStart(2, '0');
  return `${year}-${month}-${day} ${hours}:${minutes}`;
}

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

// 获取标签列表
function getTags(article: Article): string[] {
  if (Array.isArray(article.tags)) return article.tags;
  if ('tagNames' in article && Array.isArray(article.tagNames)) return article.tagNames as string[];
  return [];
}

// 统一的文章跳转逻辑（支持slug）
function goToArticle(articleId: string | number | undefined, slug?: string) {
  if (!articleId) {
    console.warn('文章ID为空，无法跳转');
    return;
  }
  const path = slug
    ? `/article/${String(articleId)}/${encodeURIComponent(slug)}`
    : `/article/${String(articleId)}`;
  router.push(path);
}

// 统一的作者跳转逻辑
function goToAuthor(authorId: string | number | undefined) {
  if (!authorId) return;
  router.push('/author/' + String(authorId));
}
</script>

<template>
  <article
      class="group rounded-lg overflow-hidden border shadow-sm hover:shadow-md hover:-translate-y-0.5 transition-all duration-300"
      :class="[article.cover ? 'min-h-[100px] sm:min-h-[120px]' : 'min-h-[70px] sm:min-h-[80px]']"
      style="background-color: var(--theme-bg); border-color: var(--theme-border);"
      :aria-label="'文章标题: ' + article.title"
  >
    <div class="flex flex-col sm:flex-row gap-0 h-full items-stretch cursor-pointer">
      <!-- Cover Image (Optional) -->
      <div v-if="article.cover" class="flex-shrink-0">
        <div class="py-2 sm:py-3 px-2 sm:px-3">
          <div class="w-24 sm:w-32 md:w-40 h-16 sm:h-20 md:h-24 flex-shrink-0">
            <LazyImage
                :src="article.cover"
                :alt="article.title"
                class="w-full h-full object-cover rounded-md"
            />
          </div>
        </div>
      </div>

      <!-- Content -->
      <div class="flex-1 p-3 sm:p-4 flex flex-col justify-center min-w-0">
        <!-- 标题（line-clamp 自适应截断，避免与 truncateText 重复截断） -->
        <Link
            :to="article.slug ? `/article/${article.id}/${encodeURIComponent(article.slug)}` : `/article/${article.id}`"
            class="block text-sm md:text-base font-medium line-clamp-2 leading-snug transition-colors hover:opacity-80"
            style="color: var(--theme-text);"
        >
          {{ article.title }}
        </Link>

        <!-- 作者信息 -->
        <button
            type="button"
            @click.stop="goToAuthor(article.authorId || article.author?.id)"
            class="flex items-center gap-1.5 mt-1.5 text-xs transition-colors hover:opacity-80 cursor-pointer text-left"
            style="color: var(--theme-text-secondary);"
        >
          <Avatar
              :src="getAuthorAvatar(article)"
              :name="getAuthorUsername(article)"
              size="xs"
          />
          <span>{{ getAuthorUsername(article) }}</span>
        </button>

        <!-- 简介（line-clamp-2 提升信息密度） -->
        <p v-if="article.excerpt" class="text-xs mt-1.5 line-clamp-2" style="color: var(--theme-text-secondary);">
          {{ article.excerpt }}
        </p>

        <!-- 标签和日期时间 -->
        <div class="flex flex-wrap items-center justify-between gap-2 mt-2">
          <!-- 标签 -->
          <div class="flex flex-wrap gap-1.5">
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

          <!-- 日期时间 -->
          <div class="flex items-center gap-1 text-xs flex-shrink-0" style="color: var(--theme-text-secondary);">
            <Clock class="w-3 h-3" aria-hidden="true" />
            <span>{{ formatDateTime(article.createdAt) }}</span>
          </div>
        </div>
      </div>
    </div>
  </article>
</template>
