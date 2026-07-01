<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch, computed, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useHead } from '@vueuse/head'
import {
  ArrowLeft,
  ChevronLeft,
  ChevronRight,
  List as ListIcon,
  X,
  BookOpen,
  Clock,
  Eye,
  Settings as SettingsIcon,
} from 'lucide-vue-next'
import MarkdownRenderer from '@/components/MarkdownRenderer.vue'
import SiteFooter from '@/components/SiteFooter.vue'
import ChapterToc from '@/components/ChapterToc.vue'
import ReadingSettingsPanel from '@/components/reading/ReadingSettingsPanel.vue'
import { generateSeo } from '@/utils/seo'
import { formatShortDate } from '@/utils/date'
import {
  getBookChapterDetail,
  getBookChapterNav,
  getBookChapterList,
  getBookDetail,
  updateBookshelfLastChapter,
} from '@/api/reading'
import { useReadingProgress } from '@/composables/useReadingProgress'
import { useReadingPreference } from '@/composables/useReadingPreference'
import type { Book, BookChapter, BookChapterNav, ReadingPreference } from '@/types/api'

const route = useRoute()
const router = useRouter()

// ----- 状态 -----
const loading = ref(false)
const error = ref<string | null>(null)
const chapter = ref<BookChapter | null>(null)
const nav = ref<BookChapterNav | null>(null)
const chapters = ref<BookChapter[]>([])
const book = ref<Book | null>(null)
const tocLoading = ref(false)
const tocVisible = ref(false) // 移动端抽屉控制
// 桌面端左侧目录显隐（持久化到 localStorage，保持用户偏好）
const desktopTocVisible = ref(
    (() => {
      try {
        const saved = localStorage.getItem('reading:desktopTocVisible');
        // 默认显示（未设置时返回 true）
        return saved === null ? true : saved === 'true';
      } catch {
        return true;
      }
    })()
);
function toggleDesktopToc() {
  desktopTocVisible.value = !desktopTocVisible.value;
  try {
    localStorage.setItem('reading:desktopTocVisible', String(desktopTocVisible.value));
  } catch {
    // 静默忽略 localStorage 写入失败
  }
}
// 顶栏"目录"按钮统一入口：
// - 桌面端（>= lg 断点）：切换左侧常驻目录显隐
// - 移动端（< lg 断点）：打开抽屉式目录
function toggleToc() {
  // lg 断点对应 1024px
  const isDesktop = typeof window !== 'undefined' && window.matchMedia('(min-width: 1024px)').matches;
  if (isDesktop) {
    toggleDesktopToc();
  } else {
    tocVisible.value = !tocVisible.value;
  }
}
const settingsVisible = ref(false) // 阅读设置面板
// 请求序号：防止快速切换章节时旧请求覆盖新请求的数据
let loadSeq = 0

// ----- 阅读进度 & 偏好 -----
const { startReporting, stopReporting, updateOffset, restoreProgress } = useReadingProgress(
    computed(() => bookId.value || chapter.value?.bookId)
)
const { preference, updatePreference, resetPreference } = useReadingPreference()

// ----- 计算属性 -----
const bookId = computed(() => route.params.bookId as string)
const chapterId = computed(() => route.params.chapterId as string)

const editorMode = computed<'richtext' | 'markdown'>(() => {
  return (chapter.value?.editorMode as 'richtext' | 'markdown') || 'richtext'
})

const chapterContent = computed(() => chapter.value?.content || '')
const chapterMarkdown = computed(() => chapter.value?.contentMarkdown || '')

const wordCountText = computed(() => {
  const wc = chapter.value?.wordCount
  if (!wc) return ''
  return `${wc} 字`
})

const viewCountText = computed(() => {
  const vc = chapter.value?.viewCount
  if (!vc) return ''
  return `${vc} 阅读`
})

// 顶部进度文本：第 X / Y 章
const progressText = computed(() => {
  const current = chapter.value?.chapterNo
  const total = chapters.value.length || book.value?.chapterCount
  if (!current) return ''
  if (total) return `${current} / ${total}`
  return `${current}`
})

// 阅读器主题变量映射（覆盖站点主题）
const READER_THEME_VARS: Record<string, Record<string, string>> = {
  light: {
    '--theme-bg': '#ffffff',
    '--theme-surface': '#f9fafb',
    '--theme-text': '#1f2937',
    '--theme-text-secondary': '#6b7280',
    '--theme-border': '#e5e7eb',
  },
  dark: {
    '--theme-bg': '#1a1a1a',
    '--theme-surface': '#262626',
    '--theme-text': '#e5e5e5',
    '--theme-text-secondary': '#9ca3af',
    '--theme-border': '#404040',
  },
  sepia: {
    '--theme-bg': '#f4ecd8',
    '--theme-surface': '#faf4e6',
    '--theme-text': '#5b4636',
    '--theme-text-secondary': '#8b7355',
    '--theme-border': '#e0d5b8',
  },
}

// 字体族 CSS 映射
const FONT_FAMILY_CSS: Record<string, string> = {
  system: 'system-ui, -apple-system, sans-serif',
  serif: 'Georgia, "Times New Roman", serif',
  song: '"SimSun", "STSong", serif',
  hei: '"SimHei", "Microsoft YaHei", sans-serif',
}

// 阅读器根容器样式：主题覆盖 + 排版 CSS 变量
const readerStyle = computed(() => {
  const style: Record<string, string> = {
    backgroundColor: 'var(--theme-bg)',
  }
  // 主题覆盖（default 时跟随站点主题，不覆盖）
  const themeVars = READER_THEME_VARS[preference.value.theme]
  if (themeVars) {
    Object.assign(style, themeVars)
  }
  // 排版变量
  style['--reader-font-size'] = `${preference.value.fontSize}px`
  style['--reader-line-height'] = String(preference.value.lineHeight)
  style['--reader-font-family'] = FONT_FAMILY_CSS[preference.value.fontFamily] || FONT_FAMILY_CSS.system
  style['--reader-letter-spacing'] = `${preference.value.letterSpacing || 0}px`
  style['--reader-paragraph-spacing'] = `${preference.value.paragraphSpacing || 1.2}em`
  return style
})

// ----- 加载逻辑 -----
async function loadAll() {
  if (!chapterId.value) {
    error.value = '缺少章节 ID'
    return
  }
  // 章节切换前强制上报上一章进度（startReporting 内部会先 stopReporting）
  const seq = ++loadSeq // 自增请求序号，旧请求结果会被丢弃
  loading.value = true
  error.value = null
  try {
    // 并行加载：章节详情 + 导航
    const [chapterResp, navResp] = await Promise.all([
      getBookChapterDetail(chapterId.value),
      getBookChapterNav(chapterId.value).catch(() => null),
    ])
    // 竞态守卫：若期间又触发了新的 loadAll，丢弃本次结果
    if (seq !== loadSeq) return

    if (chapterResp.code === 200 && chapterResp.data) {
      chapter.value = chapterResp.data
      // 拿到 bookId 后并行加载书籍信息和章节目录
      const bid = chapter.value.bookId || bookId.value
      if (bid) {
        const [bookResp, listResp] = await Promise.all([
          getBookDetail(bid).catch(() => null),
          getBookChapterList(bid).catch(() => null),
        ])
        if (seq !== loadSeq) return // 竞态守卫
        if (bookResp?.code === 200 && bookResp.data?.book) {
          book.value = bookResp.data.book
        }
        if (listResp?.code === 200 && listResp.data) {
          chapters.value = Array.isArray(listResp.data) ? listResp.data : []
        }
      }

      // ===== 第二阶段：阅读进度恢复 + 上报 + 书架同步 =====
      const cId = chapter.value.id
      const cNo = chapter.value.chapterNo || 0
      const bidForProgress = chapter.value.bookId || bookId.value
      // 启动节流上报（内部会先 stopReporting 强制上报上一章）
      startReporting(cId, cNo)
      // 恢复上次阅读位置
      let restoredOffset = 0
      try {
        restoredOffset = await restoreProgress()
      } catch (e) {
        restoredOffset = 0
      }
      if (seq !== loadSeq) return // 竞态守卫：恢复期间又切了章节，不滚动
      // 同步书架最后阅读章节（fire and forget，失败静默）
      if (bidForProgress) {
        updateBookshelfLastChapter(bidForProgress, Number(cId), cNo).catch(() => {})
      }
      // 等待渲染后滚动到恢复位置
      await nextTick()
      if (seq !== loadSeq) return // 竞态守卫
      if (typeof window !== 'undefined') {
        if (restoredOffset > 0) {
          window.scrollTo({ top: restoredOffset, behavior: 'auto' })
        } else {
          window.scrollTo({ top: 0, behavior: 'smooth' })
        }
      }
    } else {
      error.value = chapterResp.message || '加载章节失败'
      chapter.value = null
    }
    if (navResp?.code === 200 && navResp.data) {
      nav.value = navResp.data
    } else {
      nav.value = null
    }
  } catch (err) {
    if (seq !== loadSeq) return // 竞态守卫
    console.error('加载章节详情失败:', err)
    error.value = '加载章节失败，请稍后重试'
    chapter.value = null
  } finally {
    // 仅当本次是最新请求时才关闭 loading，避免先完成的旧请求误关 loading
    if (seq === loadSeq) {
      loading.value = false
    }
  }
}

// ----- 滚动监听（节流上报 offset）-----
let scrollRafId: number | null = null
function onScroll() {
  if (scrollRafId !== null) return
  scrollRafId = window.requestAnimationFrame(() => {
    scrollRafId = null
    updateOffset(window.scrollY || window.pageYOffset || 0)
  })
}

// ----- 跳转 -----
function goBackToBook() {
  const bid = bookId.value || chapter.value?.bookId
  if (bid) {
    router.push(`/reading/book/${bid}`)
  } else {
    router.push('/reading')
  }
}

function goChapter(targetId: string | number | null) {
  if (!targetId) return
  const bid = bookId.value || chapter.value?.bookId
  if (bid) {
    router.push(`/reading/book/${bid}/chapter/${targetId}`)
    // 关闭移动端抽屉
    tocVisible.value = false
  }
}

function goPrev() {
  if (nav.value?.prev) goChapter(nav.value.prev.id)
}
function goNext() {
  if (nav.value?.next) goChapter(nav.value.next.id)
}

function handleTocSelect(chapterId: string | number) {
  goChapter(chapterId)
}

// 键盘左右翻页
function onKeydown(e: KeyboardEvent) {
  if (e.target instanceof HTMLInputElement || e.target instanceof HTMLTextAreaElement) return
  if (e.key === 'ArrowLeft' && nav.value?.prev) {
    e.preventDefault()
    goPrev()
  } else if (e.key === 'ArrowRight' && nav.value?.next) {
    e.preventDefault()
    goNext()
  }
}

// ----- SEO -----
useHead(
    computed(() => {
      if (!chapter.value) {
        return generateSeo({
          title: '章节阅读',
          description: '墨韵·智库读书空间 - 章节阅读',
          type: 'article',
          canonicalPath: '/reading',
        })
      }
      const title = `${chapter.value.title} - ${book.value?.title || '读书空间'}`
      const canonicalPath = `/reading/book/${bookId.value}/chapter/${chapter.value.id}`
      return generateSeo({
        title,
        description: book.value?.summary || `《${book.value?.title || ''}》${chapter.value.title}`,
        type: 'article',
        canonicalPath,
        jsonLd: {
          '@context': 'https://schema.org',
          '@type': 'Chapter',
          name: chapter.value.title,
          position: chapter.value.chapterNo,
          isPartOf: {
            '@type': 'Book',
            name: book.value?.title || '',
            author: {
              '@type': 'Person',
              name: book.value?.author || '',
            },
          },
          url: canonicalPath,
        },
      })
    })
)

onMounted(() => {
  loadAll()
  window.addEventListener('keydown', onKeydown)
  window.addEventListener('scroll', onScroll, { passive: true })
})

onUnmounted(() => {
  window.removeEventListener('keydown', onKeydown)
  window.removeEventListener('scroll', onScroll)
  // 强制上报最后阅读进度（composable 内部 onUnmounted 也会调用，此处显式调用确保顺序）
  stopReporting()
  if (scrollRafId !== null) {
    window.cancelAnimationFrame(scrollRafId)
    scrollRafId = null
  }
})

// 路由参数变化时重新加载
watch(
    () => [route.params.bookId, route.params.chapterId],
    () => {
      loadAll()
    }
)
</script>

<template>
  <div class="min-h-screen flex flex-col" :style="readerStyle">
    <!-- 顶部导航条 -->
    <header
        class="sticky top-0 z-30 border-b backdrop-blur"
        style="background-color: var(--theme-surface); border-color: var(--theme-border);"
    >
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-14 flex items-center justify-between gap-3">
        <!-- 左侧：返回 + 书名 -->
        <div class="flex items-center gap-2 min-w-0">
          <button
              @click="goBackToBook"
              class="inline-flex items-center gap-1 text-sm font-medium transition-colors hover:opacity-80 focus:outline-none flex-shrink-0"
              style="color: var(--theme-text-secondary);"
              aria-label="返回书籍详情"
          >
            <ArrowLeft class="w-4 h-4" aria-hidden="true" />
            <span class="hidden sm:inline">返回</span>
          </button>
          <span style="color: var(--theme-text-secondary);">/</span>
          <span
              v-if="book"
              class="text-sm truncate"
              style="color: var(--theme-text);"
              :title="book.title"
          >
            {{ book.title }}
          </span>
        </div>
        <!-- 右侧：进度 + 目录 + 设置 -->
        <div class="flex items-center gap-2 flex-shrink-0">
          <!-- 阅读进度 -->
          <span
              v-if="progressText"
              class="text-xs hidden sm:inline px-2 py-1 rounded-md"
              style="color: var(--theme-text-secondary); background-color: var(--theme-bg);"
          >
            {{ progressText }}
          </span>
          <button
              @click="settingsVisible = true"
              class="inline-flex items-center gap-1 px-3 py-1.5 rounded-lg text-sm font-medium transition-colors focus:outline-none"
              style="background-color: var(--theme-bg); color: var(--theme-text); border: 1px solid var(--theme-border);"
              aria-label="阅读设置"
              title="阅读设置"
          >
            <SettingsIcon class="w-4 h-4" aria-hidden="true" />
            <span class="hidden sm:inline">设置</span>
          </button>
          <button
              @click="toggleToc"
              class="inline-flex items-center gap-1 px-3 py-1.5 rounded-lg text-sm font-medium transition-colors focus:outline-none"
              :style="{
              backgroundColor: desktopTocVisible ? 'var(--theme-primary)' : 'var(--theme-bg)',
              color: desktopTocVisible ? '#ffffff' : 'var(--theme-text)',
              border: '1px solid var(--theme-border)'
            }"
              :aria-label="desktopTocVisible ? '隐藏目录' : '显示目录'"
              :title="desktopTocVisible ? '隐藏目录' : '显示目录'"
          >
            <ListIcon class="w-4 h-4" aria-hidden="true" />
            <span class="hidden sm:inline">目录</span>
          </button>
        </div>
      </div>
    </header>

    <!-- 主体：左侧目录（桌面端可收起/展开） + 右侧正文 -->
    <div class="flex-1">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6">
        <div class="flex gap-6">
          <!-- 左侧目录（桌面端，可由顶栏"目录"按钮收起；移动端用抽屉） -->
          <aside
              v-show="desktopTocVisible"
              class="reader-aside-desktop w-72 flex-shrink-0"
          >
            <div class="sticky top-20">
              <ChapterToc
                  :chapters="chapters"
                  :current-chapter-id="chapterId"
                  :loading="tocLoading"
                  @select="handleTocSelect"
              />
            </div>
          </aside>

          <!-- 中间正文 -->
          <main class="flex-1 min-w-0">
            <!-- 加载状态 -->
            <div v-if="loading" class="py-16 text-center">
              <div
                  class="inline-block w-12 h-12 border-4 border-t-4 rounded-full animate-spin"
                  style="border-color: var(--theme-border); border-top-color: var(--theme-primary);"
              ></div>
              <p class="mt-4" style="color: var(--theme-text-secondary);">加载中...</p>
            </div>

            <!-- 错误状态 -->
            <div v-else-if="error" class="py-16 text-center">
              <BookOpen class="w-12 h-12 mx-auto mb-4" style="color: var(--theme-text-secondary);" aria-hidden="true" />
              <p class="text-lg mb-4" style="color: var(--theme-text);">{{ error }}</p>
              <button
                  @click="loadAll"
                  class="px-6 py-2 rounded-lg font-medium transition-colors focus:outline-none"
                  style="background-color: var(--theme-primary); color: white;"
              >
                重试
              </button>
            </div>

            <!-- 空数据 -->
            <div v-else-if="!chapter" class="py-16 text-center">
              <BookOpen class="w-12 h-12 mx-auto mb-4" style="color: var(--theme-text-secondary);" aria-hidden="true" />
              <p class="text-lg" style="color: var(--theme-text-secondary);">章节不存在或未发布</p>
            </div>

            <!-- 章节内容 -->
            <article v-else>
              <!-- 章节标题 -->
              <div
                  class="mb-6 pb-4 border-b"
                  style="border-color: var(--theme-border);"
              >
                <div class="flex items-center gap-2 text-xs mb-2" style="color: var(--theme-text-secondary);">
                  <span>第 {{ chapter.chapterNo }} 章</span>
                  <span v-if="wordCountText" class="flex items-center gap-1">
                    <span>·</span>
                    <BookOpen class="w-3 h-3" aria-hidden="true" />
                    <span>{{ wordCountText }}</span>
                  </span>
                  <span v-if="viewCountText" class="flex items-center gap-1">
                    <span>·</span>
                    <Eye class="w-3 h-3" aria-hidden="true" />
                    <span>{{ viewCountText }}</span>
                  </span>
                  <span v-if="chapter.publishTime" class="flex items-center gap-1">
                    <span>·</span>
                    <Clock class="w-3 h-3" aria-hidden="true" />
                    <span>{{ formatShortDate(chapter.publishTime) }}</span>
                  </span>
                </div>
                <h1 class="text-2xl sm:text-3xl font-bold leading-tight" style="color: var(--theme-text);">
                  {{ chapter.title }}
                </h1>
              </div>

              <!-- VIP 标记 -->
              <div
                  v-if="chapter.isFree === false"
                  class="mb-4 p-3 rounded-lg flex items-center gap-2 text-sm"
                  style="background-color: var(--theme-accent); color: var(--theme-primary);"
              >
                <span class="px-2 py-0.5 rounded text-xs font-bold" style="background-color: var(--theme-primary); color: white;">
                  VIP
                </span>
                <span>本章为 VIP 章节当前为预览模式，完整内容需开通 VIP</span>
              </div>

              <!-- 正文 -->
              <div
                  class="chapter-content-wrapper rounded-xl p-6 sm:p-8 shadow-sm"
                  style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);"
              >
                <MarkdownRenderer
                    :content="chapterContent"
                    :content-markdown="chapterMarkdown"
                    :editor-mode="editorMode"
                />
              </div>

              <!-- 章节底部：上一章/下一章 -->
              <nav
                  class="mt-8 grid grid-cols-1 sm:grid-cols-2 gap-3"
                  aria-label="章节导航"
              >
                <button
                    v-if="nav?.prev"
                    type="button"
                    class="flex items-center gap-3 p-4 rounded-xl text-left transition-colors hover:shadow-md"
                    style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);"
                    @click="goPrev"
                >
                  <ChevronLeft class="w-5 h-5 flex-shrink-0" style="color: var(--theme-primary);" aria-hidden="true" />
                  <div class="min-w-0">
                    <div class="text-xs" style="color: var(--theme-text-secondary);">上一章</div>
                    <div class="text-sm font-medium truncate" style="color: var(--theme-text);">
                      {{ nav.prev.title }}
                    </div>
                  </div>
                </button>
                <div v-else></div>

                <button
                    v-if="nav?.next"
                    type="button"
                    class="flex items-center gap-3 p-4 rounded-xl text-right transition-colors hover:shadow-md sm:col-start-2"
                    style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);"
                    @click="goNext"
                >
                  <div class="min-w-0 flex-1">
                    <div class="text-xs" style="color: var(--theme-text-secondary);">下一章</div>
                    <div class="text-sm font-medium truncate" style="color: var(--theme-text);">
                      {{ nav.next.title }}
                    </div>
                  </div>
                  <ChevronRight class="w-5 h-5 flex-shrink-0" style="color: var(--theme-primary);" aria-hidden="true" />
                </button>
              </nav>

              <!-- 键盘提示 -->
              <p class="mt-6 text-center text-xs" style="color: var(--theme-text-secondary);">
                提示：可使用键盘 ← → 翻页
              </p>
            </article>
          </main>
        </div>
      </div>
    </div>

    <!-- 移动端目录抽屉 -->
    <Teleport to="body">
      <transition name="fade">
        <div
            v-if="tocVisible"
            class="fixed inset-0 z-50 lg:hidden"
            @click.self="tocVisible = false"
        >
          <div class="absolute inset-0 bg-black/40"></div>
          <div
              class="absolute left-0 top-0 bottom-0 w-80 max-w-[85vw] overflow-y-auto p-4 shadow-xl"
              style="background-color: var(--theme-surface);"
          >
            <div class="flex items-center justify-between mb-3">
              <span class="font-medium" style="color: var(--theme-text);">章节目录</span>
              <button
                  @click="tocVisible = false"
                  style="color: var(--theme-text-secondary);"
                  aria-label="关闭"
              >
                <X class="w-5 h-5" aria-hidden="true" />
              </button>
            </div>
            <ChapterToc
                :chapters="chapters"
                :current-chapter-id="chapterId"
                :loading="tocLoading"
                :default-expanded="true"
                @select="handleTocSelect"
            />
          </div>
        </div>
      </transition>
    </Teleport>

    <!-- 阅读设置面板 -->
    <ReadingSettingsPanel
        v-model="settingsVisible"
        :preference="preference"
        @update:preference="updatePreference"
        @reset="resetPreference"
    />

    <!-- 页脚 -->
    <SiteFooter />
  </div>
</template>

<style scoped>
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
.chapter-content-wrapper :deep(.prose) {
  max-width: none;
  font-family: var(--reader-font-family, system-ui, sans-serif);
}
.chapter-content-wrapper :deep(.prose p) {
  font-size: var(--reader-font-size, 18px);
  line-height: var(--reader-line-height, 1.85);
  letter-spacing: var(--reader-letter-spacing, 0px);
  margin-bottom: var(--reader-paragraph-spacing, 1.2em);
}
.chapter-content-wrapper :deep(.prose h1),
.chapter-content-wrapper :deep(.prose h2),
.chapter-content-wrapper :deep(.prose h3) {
  font-family: var(--reader-font-family, system-ui, sans-serif);
}
/* 桌面端左侧目录：移动端（<1024px）隐藏，由抽屉接管；桌面端由 v-show 控制 */
.reader-aside-desktop {
  display: none;
}
@media (min-width: 1024px) {
  .reader-aside-desktop {
    display: block;
  }
}
</style>
