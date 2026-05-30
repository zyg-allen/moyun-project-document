<script setup lang="ts">
import { computed } from 'vue';
import { RouterLink as Link } from 'vue-router';
import { Clock, Tag, User } from 'lucide-vue-next';
import { useArticleStore } from '@/stores/article';
import LazyImage from '@/components/LazyImage.vue';
import type { Article } from '@/types/api';

interface Props {
  article: Article;
}

const props = defineProps<Props>();
const articleStore = useArticleStore();

const isLiked = computed(() => articleStore.likedArticleIds.includes(props.article.id));
const isBookmarked = computed(() => articleStore.bookmarkedArticleIds.includes(props.article.id));

// 截断文本最多50字符
function truncateText(text: string | undefined, maxLength: number = 50): string {
  if (!text) return '';
  if (text.length <= maxLength) return text;
  return text.substring(0, maxLength) + '...';
}

function handleLike(event: Event) {
  event.stopPropagation();
  articleStore.likeArticle(props.article.id);
}

function handleBookmark(event: Event) {
  event.stopPropagation();
  articleStore.bookmarkArticle(props.article.id);
}

function handleShare(event: Event) {
  event.stopPropagation();
  articleStore.shareArticle(props.article.id);
  
  if (navigator.share) {
    navigator.share({
      title: props.article.title,
      text: props.article.excerpt || '',
      url: window.location.origin + '/article/' + props.article.id,
    }).catch(console.error);
  } else {
    navigator.clipboard.writeText(window.location.origin + '/article/' + props.article.id);
    alert('链接已复制到剪贴板');
  }
}

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
</script>

<template>
  <article 
    class="group rounded-lg overflow-hidden border shadow-sm hover:shadow-md hover:-translate-y-0.5 transition-all duration-300" 
    :class="[article.cover ? 'min-h-[100px] sm:min-h-[120px]' : 'min-h-[70px] sm:min-h-[80px]']"
    style="background-color: var(--theme-bg); border-color: var(--theme-border);"
    :aria-label="'文章标题: ' + article.title"
  >
    <Link 
      :to="'/article/' + article.id" 
      target="_blank"
      class="flex flex-col sm:flex-row gap-0 h-full items-stretch"
      :aria-label="'查看文章: ' + article.title"
    >
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
      <div 
        class="flex-1 p-3 sm:p-4 flex flex-col justify-center min-w-0"
      >
        <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-2 w-full">
          <!-- 标题靠左 -->
          <div class="flex-1 min-w-0">
            <Link 
              :to="'/article/' + article.id"
              target="_blank"
              class="text-sm md:text-base font-medium line-clamp-2 transition-colors hover:opacity-80"
              style="color: var(--theme-text);"
            >
              {{ truncateText(article.title) }}
            </Link>
            <!-- 作者信息 -->
            <Link 
              :to="'/author/' + article.authorId"
              @click.stop
              class="flex items-center gap-1.5 mt-1 text-xs transition-colors hover:opacity-80"
              style="color: var(--theme-text-secondary);"
            >
              <div class="w-4 h-4 rounded-full overflow-hidden flex-shrink-0">
                <img 
                  :src="getAuthorAvatar(article)" 
                  :alt="getAuthorUsername(article)"
                  class="w-full h-full object-cover"
                />
              </div>
              <span>{{ getAuthorUsername(article) }}</span>
            </Link>
            <!-- 简介 -->
            <p class="text-xs mt-1 line-clamp-1" style="color: var(--theme-text-secondary);">
              {{ truncateText(article.excerpt) }}
            </p>
          </div>

          <!-- 标签和日期时间靠右 -->
          <div class="flex flex-wrap items-center justify-end gap-2 flex-shrink-0">
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
    </Link>
  </article>
</template>