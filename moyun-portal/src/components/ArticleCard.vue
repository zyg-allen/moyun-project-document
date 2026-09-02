<script setup lang="ts">
import { RouterLink as Link, useRouter } from 'vue-router';
import { formatDate } from '@/utils/date';
import { Clock, Tag } from 'lucide-vue-next';
import LazyImage from '@/components/LazyImage.vue';
import Avatar from '@/components/Avatar.vue';
import type { Article } from '@/types/api';

const router = useRouter();

interface Props {
  article: Article;
}

const props = defineProps<Props>();

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
    class="group rounded-xl overflow-hidden border shadow-sm hover:shadow-theme-md hover:-translate-y-0.5 transition-all duration-300 bg-theme-surface border-theme-border"
    :class="[article.cover ? 'min-h-[100px] sm:min-h-[120px]' : 'min-h-[70px] sm:min-h-[80px]']"
    :aria-label="'文章标题: ' + article.title"
  >
    <div class="flex flex-col sm:flex-row gap-0 h-full items-stretch cursor-pointer" @click="goToArticle(article.id, article.slug)">
      <!-- Cover Image (Optional) -->
      <div v-if="article.cover" class="flex-shrink-0">
        <div class="p-2 sm:p-3">
          <div class="w-24 sm:w-32 md:w-36 h-16 sm:h-20 md:h-22 flex-shrink-0">
            <LazyImage
              :src="article.cover"
              :alt="article.title"
              class="w-full h-full object-cover rounded-lg"
            />
          </div>
        </div>
      </div>

      <!-- Content -->
      <div class="flex-1 p-3 sm:p-4 flex flex-col justify-center min-w-0">
        <!-- 标题 -->
        <Link
          :to="article.slug ? `/article/${article.id}/${encodeURIComponent(article.slug)}` : `/article/${article.id}`"
          class="block card-title line-clamp-2 hover:text-theme-primary transition-colors"
          @click.stop
        >
          {{ article.title }}
        </Link>

        <!-- 作者信息 -->
        <button
          type="button"
          @click.stop="goToAuthor(article.authorId || article.author?.id)"
          class="flex items-center gap-1.5 mt-2 meta-text transition-colors hover:text-theme-primary text-left w-fit"
        >
          <Avatar
            :src="getAuthorAvatar(article)"
            :name="getAuthorUsername(article)"
            size="xs"
          />
          <span>{{ getAuthorUsername(article) }}</span>
        </button>

        <!-- 简介 -->
        <p v-if="article.excerpt" class="card-summary mt-2 line-clamp-2">
          {{ article.excerpt }}
        </p>

        <!-- 标签和日期时间 -->
        <div class="flex flex-wrap items-center justify-between gap-2 mt-2.5">
          <!-- 标签 -->
          <div class="flex flex-wrap gap-1.5">
            <span
              v-for="tag in getTags(article).slice(0, 2)"
              :key="tag"
              class="inline-flex items-center px-2 py-0.5 caption-text rounded-full bg-theme-bg text-theme-text-secondary"
            >
              <Tag class="w-3 h-3 mr-1" aria-hidden="true" />
              {{ tag }}
            </span>
          </div>

          <!-- 日期时间 -->
          <div class="flex items-center gap-1 meta-text flex-shrink-0">
            <Clock class="w-3 h-3" aria-hidden="true" />
            <span>{{ formatDate(article.createdAt, 'YYYY-MM-DD') }}</span>
          </div>
        </div>
      </div>
    </div>
  </article>
</template>

<style scoped>
</style>
