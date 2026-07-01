<script setup lang="ts">
import { ref, computed, watch, nextTick } from 'vue'
import { List, Search, X, ChevronRight } from 'lucide-vue-next'
import type { BookChapter } from '@/types/api'

interface Props {
  /** 章节列表（已发布，按 chapterNo 排序） */
  chapters: BookChapter[]
  /** 当前正在阅读的章节 ID（用于高亮） */
  currentChapterId?: string | number | null
  /** 加载状态 */
  loading?: boolean
  /** 是否默认展开 */
  defaultExpanded?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  currentChapterId: null,
  loading: false,
  defaultExpanded: true,
})

const emit = defineEmits<{
  (e: 'select', chapterId: string | number): void
}>()

const expanded = ref(props.defaultExpanded)
const keyword = ref('')
const listRef = ref<HTMLElement | null>(null)

// 过滤后的章节
const filteredChapters = computed<BookChapter[]>(() => {
  if (!keyword.value.trim()) return props.chapters
  const kw = keyword.value.trim().toLowerCase()
  return props.chapters.filter(
    (c) =>
      c.title.toLowerCase().includes(kw) ||
      String(c.chapterNo).includes(kw)
  )
})

// 当前章节在完整列表中的位置（用于显示进度）
const currentIndex = computed(() => {
  if (!props.currentChapterId) return -1
  return props.chapters.findIndex(
    (c) => String(c.id) === String(props.currentChapterId)
  )
})

const progressText = computed(() => {
  if (currentIndex.value < 0 || props.chapters.length === 0) return ''
  return `${currentIndex.value + 1} / ${props.chapters.length}`
})

function togglePanel() {
  expanded.value = !expanded.value
}

function handleSelect(chapter: BookChapter) {
  emit('select', chapter.id)
}

function clearKeyword() {
  keyword.value = ''
}

// 当前章节变化时，滚动到对应位置
watch(
  () => props.currentChapterId,
  async () => {
    if (!expanded.value || !props.currentChapterId) return
    await nextTick()
    const active = listRef.value?.querySelector(
      `[data-chapter-id="${props.currentChapterId}"]`
    ) as HTMLElement | null
    if (active) {
      active.scrollIntoView({ behavior: 'smooth', block: 'nearest' })
    }
  }
)
</script>

<template>
  <aside
    class="chapter-toc rounded-xl overflow-hidden shadow-sm"
    style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);"
  >
    <!-- 头部 -->
    <div
      class="flex items-center justify-between px-4 py-3 cursor-pointer select-none"
      style="background-color: var(--theme-bg); border-bottom: 1px solid var(--theme-border);"
      @click="togglePanel"
    >
      <div class="flex items-center gap-2" style="color: var(--theme-text);">
        <List class="w-4 h-4" style="color: var(--theme-primary);" aria-hidden="true" />
        <span class="font-medium text-sm">章节目录</span>
        <span v-if="chapters.length > 0" class="text-xs" style="color: var(--theme-text-secondary);">
          （共 {{ chapters.length }} 章）
        </span>
      </div>
      <div class="flex items-center gap-2">
        <span v-if="progressText" class="text-xs" style="color: var(--theme-text-secondary);">
          {{ progressText }}
        </span>
        <ChevronRight
          class="w-4 h-4 transition-transform"
          :class="expanded ? 'rotate-90' : ''"
          style="color: var(--theme-text-secondary);"
          aria-hidden="true"
        />
      </div>
    </div>

    <!-- 展开内容 -->
    <div v-if="expanded">
      <!-- 搜索框 -->
      <div v-if="chapters.length > 8" class="px-3 pt-3">
        <div
          class="relative flex items-center"
          style="background-color: var(--theme-bg); border: 1px solid var(--theme-border);"
        >
          <Search
            class="absolute left-2 w-3.5 h-3.5"
            style="color: var(--theme-text-secondary);"
            aria-hidden="true"
          />
          <input
            v-model="keyword"
            type="text"
            placeholder="搜索章节"
            class="w-full pl-7 pr-7 py-1.5 text-sm bg-transparent focus:outline-none"
            style="color: var(--theme-text);"
          />
          <button
            v-if="keyword"
            class="absolute right-2"
            style="color: var(--theme-text-secondary);"
            aria-label="清空"
            @click="clearKeyword"
          >
            <X class="w-3.5 h-3.5" aria-hidden="true" />
          </button>
        </div>
      </div>

      <!-- 章节列表 -->
      <div ref="listRef" class="chapter-toc-list max-h-[60vh] overflow-y-auto py-2">
        <div v-if="loading" class="px-4 py-6 text-center text-sm" style="color: var(--theme-text-secondary);">
          加载中...
        </div>
        <div v-else-if="filteredChapters.length === 0" class="px-4 py-6 text-center text-sm" style="color: var(--theme-text-secondary);">
          {{ chapters.length === 0 ? '暂无章节' : '无匹配章节' }}
        </div>
        <ul v-else>
          <li
            v-for="chapter in filteredChapters"
            :key="chapter.id"
            :data-chapter-id="chapter.id"
          >
            <button
              type="button"
              class="w-full flex items-start gap-2 px-4 py-2 text-left text-sm transition-colors hover:bg-opacity-50"
              :class="String(chapter.id) === String(currentChapterId) ? 'is-active' : ''"
              :style="{
                color: String(chapter.id) === String(currentChapterId)
                  ? 'var(--theme-primary)'
                  : 'var(--theme-text)',
                backgroundColor: String(chapter.id) === String(currentChapterId)
                  ? 'var(--theme-bg)'
                  : 'transparent'
              }"
              @click="handleSelect(chapter)"
            >
              <span
                class="flex-shrink-0 inline-flex items-center justify-center w-7 text-xs font-mono"
                style="color: var(--theme-text-secondary);"
              >
                {{ chapter.chapterNo }}
              </span>
              <span class="flex-1 min-w-0 line-clamp-2 leading-snug">
                {{ chapter.title }}
              </span>
              <span
                v-if="!chapter.isFree"
                class="flex-shrink-0 px-1.5 py-0.5 text-[10px] rounded font-medium"
                style="background-color: var(--theme-accent); color: var(--theme-primary);"
              >
                VIP
              </span>
            </button>
          </li>
        </ul>
      </div>
    </div>
  </aside>
</template>

<style scoped>
.chapter-toc-list {
  scrollbar-width: thin;
}
.chapter-toc-list::-webkit-scrollbar {
  width: 6px;
}
.chapter-toc-list::-webkit-scrollbar-thumb {
  background-color: var(--theme-border);
  border-radius: 3px;
}
.chapter-toc-list::-webkit-scrollbar-track {
  background-color: transparent;
}
.is-active {
  border-left: 2px solid var(--theme-primary);
  padding-left: calc(1rem - 2px);
}
.line-clamp-2 {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
</style>
