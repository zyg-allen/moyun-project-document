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
  ChevronUp,
  ChevronDown,
  CheckCircle2,
  BookCheck,
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
import { useUserStore } from '@/stores/user'
import type { Book, BookChapter, BookChapterNav, ReadingPreference } from '@/types/api'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

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

// ----- 阅读模式（scroll 滚动 / paginate 分页）-----
// 单独 localStorage 持久化，不进 ReadingPreference 服务端同步（避免后端 schema 改动）
const READING_MODE_KEY = 'reading:mode';
type ReadingMode = 'scroll' | 'paginate';
const readingMode = ref<ReadingMode>(
    (() => {
      try {
        const saved = localStorage.getItem(READING_MODE_KEY);
        return saved === 'paginate' ? 'paginate' : 'scroll';
      } catch {
        return 'scroll';
      }
    })()
);
function toggleReadingMode() {
  readingMode.value = readingMode.value === 'scroll' ? 'paginate' : 'scroll';
  try {
    localStorage.setItem(READING_MODE_KEY, readingMode.value);
  } catch {
    // 静默忽略 localStorage 写入失败
  }
  // 切换模式后重置分页状态，等下一帧重新计算
  currentPage.value = 0;
  totalPages.value = 1;
  chapterFinishedMarked.value = false;
  nextTick(() => {
    recalcPages();
    if (readingMode.value === 'scroll') {
      // 滚动模式：恢复到顶部
      window.scrollTo({ top: 0, behavior: 'auto' });
    } else if (paginateContainer.value) {
      // 分页模式：跳到第一页
      paginateContainer.value.scrollTo({ top: 0, behavior: 'auto' });
    }
  });
}

// ----- 分页模式状态 -----
const paginateContainer = ref<HTMLElement | null>(null);
const currentPage = ref(0);
const totalPages = ref(1);
// 章节完成标记：本章"读到末尾"已触发过 markChapterFinished，避免重复上报（章节切换时重置）
const chapterFinishedMarked = ref(false);

// ----- 阅读进度 & 偏好 -----
const { startReporting, stopReporting, updateOffset, markChapterFinished, restoreProgress } = useReadingProgress(
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

// 是否为最后一章（用于判断"本书已读完"）
const isLastChapter = computed(() => {
  if (!nav.value?.next) return true // 没有 next 即视为最后一章
  return false;
})

// 分页模式：页码指示文本
const pageIndicatorText = computed(() => {
  if (totalPages.value <= 1) return ''
  return `${currentPage.value + 1} / ${totalPages.value}`
})

// 是否显示"本章已读完"卡片
const showChapterFinishedCard = computed(() => {
  return readingMode.value === 'paginate'
      && chapterFinishedMarked.value
      && currentPage.value >= totalPages.value - 1;
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
      // 章节切换：重置分页状态 + 章节完成标记
      currentPage.value = 0
      totalPages.value = 1
      chapterFinishedMarked.value = false
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
      // 同步书架最后阅读章节（仅登录用户，fire and forget，失败静默）
      if (bidForProgress && userStore.isAuthenticated) {
        updateBookshelfLastChapter(bidForProgress, Number(cId), cNo).catch(() => {})
      }
      // 等待渲染后处理位置恢复 + 分页计算
      await nextTick()
      if (seq !== loadSeq) return // 竞态守卫
      if (typeof window !== 'undefined') {
        if (readingMode.value === 'paginate') {
          // 分页模式：计算总页数，恢复到上次阅读位置对应的页码
          recalcPages()
          if (restoredOffset > 0 && paginateContainer.value) {
            const clientH = paginateContainer.value.clientHeight
            currentPage.value = Math.floor(restoredOffset / clientH)
            paginateContainer.value.scrollTo({
              top: currentPage.value * clientH,
              behavior: 'auto',
            })
          } else if (paginateContainer.value) {
            paginateContainer.value.scrollTo({ top: 0, behavior: 'auto' })
          }
        } else {
          // 滚动模式：恢复到上次的 window 偏移
          if (restoredOffset > 0) {
            window.scrollTo({ top: restoredOffset, behavior: 'auto' })
          } else {
            window.scrollTo({ top: 0, behavior: 'smooth' })
          }
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

// ----- 滚动监听（节流上报 offset + 检测章节完成）-----
let scrollRafId: number | null = null
function onScroll() {
  if (scrollRafId !== null) return
  scrollRafId = window.requestAnimationFrame(() => {
    scrollRafId = null
    updateOffset(window.scrollY || window.pageYOffset || 0)
    // 滚动模式：检测是否到页面底部，触发章节完成
    if (readingMode.value === 'scroll' && !chapterFinishedMarked.value) {
      const scrollable = document.documentElement
      const threshold = 50 // 距底部 50px 视为已读完
      if (scrollable.scrollTop + window.innerHeight >= scrollable.scrollHeight - threshold) {
        chapterFinishedMarked.value = true
        markChapterFinished()
      }
    }
  })
}

// ----- 分页模式：内部滚动监听 -----
let paginateRafId: number | null = null
function onPaginateScroll() {
  if (paginateRafId !== null) return
  paginateRafId = window.requestAnimationFrame(() => {
    paginateRafId = null
    if (!paginateContainer.value) return
    const el = paginateContainer.value
    // 用 round 计算 currentPage（用于 UI 显示）
    // 注意：不能用 round 判断末页，因为 Math.round 与 Math.ceil 在非整数倍时失配
    // （如 scrollHeight/clientHeight = 3.1 时 totalPages-1 = 3 但 max round = 2，永不触发）
    currentPage.value = Math.round(el.scrollTop / el.clientHeight)
    // 修复 v1.1.1：改用"距底部距离"判断末页，与 scroll 模式 onScroll 一致
    // 阈值 5px 容忍浮点误差 + smooth scroll 惯性
    const distanceToBottom = el.scrollHeight - el.scrollTop - el.clientHeight
    if (distanceToBottom <= 5 && !chapterFinishedMarked.value) {
      chapterFinishedMarked.value = true
      // 强制把 currentPage 钳制到末页，确保 showChapterFinishedCard 计算正确
      currentPage.value = totalPages.value - 1
      markChapterFinished()
    }
  })
}

/**
 * 重新计算分页数：章节加载完成 / 字号变化 / 视口变化时调用
 * 由 onMounted、loadAll 末尾、字号偏好变化、window resize 触发
 */
function recalcPages() {
  if (readingMode.value !== 'paginate') return
  if (!paginateContainer.value) return
  const el = paginateContainer.value
  totalPages.value = Math.max(1, Math.ceil(el.scrollHeight / el.clientHeight))
  if (currentPage.value > totalPages.value - 1) {
    currentPage.value = Math.max(0, totalPages.value - 1)
  }
}

function goPrevPage() {
  if (readingMode.value !== 'paginate' || !paginateContainer.value) return
  if (currentPage.value > 0) {
    currentPage.value--
    paginateContainer.value.scrollTo({
      top: currentPage.value * paginateContainer.value.clientHeight,
      behavior: 'smooth',
    })
  } else if (nav.value?.prev) {
    // 在第一页继续往前 → 翻上一章
    goPrev()
  }
}

function goNextPage() {
  if (readingMode.value !== 'paginate' || !paginateContainer.value) return
  if (currentPage.value < totalPages.value - 1) {
    currentPage.value++
    paginateContainer.value.scrollTo({
      top: currentPage.value * paginateContainer.value.clientHeight,
      behavior: 'smooth',
    })
    // 到最后一页触发章节完成
    if (currentPage.value >= totalPages.value - 1 && !chapterFinishedMarked.value) {
      chapterFinishedMarked.value = true
      markChapterFinished()
    }
  } else if (nav.value?.next) {
    // 在最后一页继续往后 → 翻下一章
    goNext()
  }
}

// ----- 触摸滑动翻页（移动端分页模式）-----
let touchStartX = 0
let touchStartY = 0
function onTouchStart(e: TouchEvent) {
  if (readingMode.value !== 'paginate') return
  touchStartX = e.touches[0].clientX
  touchStartY = e.touches[0].clientY
}
function onTouchEnd(e: TouchEvent) {
  if (readingMode.value !== 'paginate') return
  const dx = e.changedTouches[0].clientX - touchStartX
  const dy = e.changedTouches[0].clientY - touchStartY
  // 仅当横向滑动距离 > 纵向 且超过阈值时才视为翻页手势
  if (Math.abs(dx) > Math.abs(dy) && Math.abs(dx) > 50) {
    if (dx > 0) goPrevPage()
    else goNextPage()
  }
}

// ----- 窗口尺寸变化时重新计算分页 -----
function onResize() {
  recalcPages()
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

// 键盘左右翻页（滚动模式翻章节，分页模式翻页 + 到边界翻章节）
function onKeydown(e: KeyboardEvent) {
  if (e.target instanceof HTMLInputElement || e.target instanceof HTMLTextAreaElement) return
  if (readingMode.value === 'scroll') {
    // 滚动模式：← → 直接翻章节
    if (e.key === 'ArrowLeft' && nav.value?.prev) {
      e.preventDefault()
      goPrev()
    } else if (e.key === 'ArrowRight' && nav.value?.next) {
      e.preventDefault()
      goNext()
    }
  } else {
    // 分页模式：← → 翻页，到首/末页时翻章节
    if (e.key === 'ArrowLeft') {
      e.preventDefault()
      goPrevPage()
    } else if (e.key === 'ArrowRight') {
      e.preventDefault()
      goNextPage()
    }
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
  window.addEventListener('resize', onResize)
})

onUnmounted(() => {
  window.removeEventListener('keydown', onKeydown)
  window.removeEventListener('scroll', onScroll)
  window.removeEventListener('resize', onResize)
  // 强制上报最后阅读进度（composable 内部 onUnmounted 也会调用，此处显式调用确保顺序）
  stopReporting()
  if (scrollRafId !== null) {
    window.cancelAnimationFrame(scrollRafId)
    scrollRafId = null
  }
  if (paginateRafId !== null) {
    window.cancelAnimationFrame(paginateRafId)
    paginateRafId = null
  }
})

// 路由参数变化时重新加载
watch(
    () => [route.params.bookId, route.params.chapterId],
    () => {
      loadAll()
    }
)

// 阅读偏好变化（字号/行距/字体等）后重新计算分页
watch(
    () => preference.value.fontSize,
    () => nextTick(() => recalcPages())
)
watch(
    () => preference.value.lineHeight,
    () => nextTick(() => recalcPages())
)
watch(
    () => preference.value.fontFamily,
    () => nextTick(() => recalcPages())
)
</script>

<template>
  <div class="min-h-screen flex flex-col" :style="readerStyle">
    <!-- 顶部导航条 -->
    <header
        class="sticky top-0 z-30 border-b backdrop-blur-sm"
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
          <!-- 阅读模式切换：滚动 / 分页 -->
          <button
              @click="toggleReadingMode"
              class="inline-flex items-center gap-1 px-3 py-1.5 rounded-lg text-sm font-medium transition-colors focus:outline-none"
              :style="{
                backgroundColor: readingMode === 'paginate' ? 'var(--theme-primary)' : 'var(--theme-bg)',
                color: readingMode === 'paginate' ? '#ffffff' : 'var(--theme-text)',
                border: '1px solid var(--theme-border)'
              }"
              :aria-label="readingMode === 'paginate' ? '切换为滚动模式' : '切换为分页模式'"
              :title="readingMode === 'paginate' ? '当前：分页模式，点击切回滚动' : '当前：滚动模式，点击切到分页'"
          >
            <BookOpen v-if="readingMode === 'scroll'" class="w-4 h-4" aria-hidden="true" />
            <BookCheck v-else class="w-4 h-4" aria-hidden="true" />
            <span class="hidden sm:inline">{{ readingMode === 'paginate' ? '分页' : '滚动' }}</span>
          </button>
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
              <!-- 滚动模式：原整章 v-html 渲染，window 滚动 -->
              <div
                  v-if="readingMode === 'scroll'"
                  class="chapter-content-wrapper rounded-xl p-6 sm:p-8 shadow-sm"
                  style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);"
              >
                <MarkdownRenderer
                    :content="chapterContent"
                    :content-markdown="chapterMarkdown"
                    :editor-mode="editorMode"
                />
                <!-- 滚动模式：到底部时显示已读完提示 -->
                <div
                    v-if="chapterFinishedMarked"
                    class="mt-8 p-4 rounded-lg flex items-center gap-3"
                    style="background-color: var(--theme-accent); border: 1px solid var(--theme-border);"
                >
                  <CheckCircle2 class="w-5 h-5 flex-shrink-0" style="color: var(--theme-primary);" aria-hidden="true" />
                  <div class="flex-1 min-w-0">
                    <div v-if="isLastChapter" class="text-sm font-medium" style="color: var(--theme-text);">
                      本书已读完
                    </div>
                    <div v-else class="text-sm font-medium" style="color: var(--theme-text);">
                      本章已读完
                    </div>
                    <div class="text-xs" style="color: var(--theme-text-secondary);">
                      <span v-if="isLastChapter">恭喜完成整本书的阅读</span>
                      <span v-else>可继续阅读下一章</span>
                    </div>
                  </div>
                </div>
              </div>

              <!-- 分页模式：固定高度容器 + 内部滚动 + 触摸滑动 -->
              <div
                  v-else
                  ref="paginateContainer"
                  class="chapter-content-paginate rounded-xl shadow-sm"
                  style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);"
                  @scroll.passive="onPaginateScroll"
                  @touchstart.passive="onTouchStart"
                  @touchend.passive="onTouchEnd"
              >
                <div class="chapter-content-inner p-6 sm:p-8">
                  <MarkdownRenderer
                      :content="chapterContent"
                      :content-markdown="chapterMarkdown"
                      :editor-mode="editorMode"
                  />
                  <!-- 分页模式：到最后一页时显示已读完卡片 -->
                  <div
                      v-if="showChapterFinishedCard"
                      class="mt-8 p-4 rounded-lg flex items-center gap-3"
                      style="background-color: var(--theme-accent); border: 1px solid var(--theme-border);"
                  >
                    <CheckCircle2 class="w-5 h-5 flex-shrink-0" style="color: var(--theme-primary);" aria-hidden="true" />
                    <div class="flex-1 min-w-0">
                      <div v-if="isLastChapter" class="text-sm font-medium" style="color: var(--theme-text);">
                        本书已读完
                      </div>
                      <div v-else class="text-sm font-medium" style="color: var(--theme-text);">
                        本章已读完
                      </div>
                      <div class="text-xs" style="color: var(--theme-text-secondary);">
                        <span v-if="isLastChapter">恭喜完成整本书的阅读</span>
                        <span v-else>可继续阅读下一章</span>
                      </div>
                    </div>
                  </div>
                </div>
              </div>

              <!-- 分页模式：底部翻页控制 + 页码指示器 -->
              <div
                  v-if="readingMode === 'paginate'"
                  class="mt-4 flex items-center justify-between gap-3 p-3 rounded-xl"
                  style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);"
              >
                <button
                    type="button"
                    class="inline-flex items-center gap-1 px-3 py-1.5 rounded-lg text-sm font-medium transition-colors focus:outline-none disabled:opacity-40"
                    style="background-color: var(--theme-bg); color: var(--theme-text); border: 1px solid var(--theme-border);"
                    :disabled="currentPage <= 0 && !nav?.prev"
                    @click="goPrevPage"
                >
                  <ChevronUp class="w-4 h-4" aria-hidden="true" />
                  <span>上一页</span>
                </button>
                <span
                    v-if="pageIndicatorText"
                    class="text-xs px-2 py-1 rounded-md"
                    style="color: var(--theme-text-secondary); background-color: var(--theme-bg);"
                >
                  {{ pageIndicatorText }}
                </span>
                <span v-else class="text-xs" style="color: var(--theme-text-secondary);">单页</span>
                <button
                    type="button"
                    class="inline-flex items-center gap-1 px-3 py-1.5 rounded-lg text-sm font-medium transition-colors focus:outline-none disabled:opacity-40"
                    style="background-color: var(--theme-primary); color: white; border: 1px solid var(--theme-primary);"
                    :disabled="currentPage >= totalPages - 1 && !nav?.next"
                    @click="goNextPage"
                >
                  <span>下一页</span>
                  <ChevronDown class="w-4 h-4" aria-hidden="true" />
                </button>
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
                提示：可使用键盘 ← → 翻{{ readingMode === 'paginate' ? '页' : '章' }}，移动端可左右滑动翻页
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

/* ===== v1.1 分页阅读模式 =====
 * 设计要点：
 *   - 容器固定高度（viewport - 头部 - 内边距 - 其他 UI 元素）
 *   - overflow-y: auto 内部滚动，window 滚动不参与
 *   - smooth scroll 实现翻页动画
 *   - iOS momentum scrolling 触摸顺滑
 *   - 内容样式继承自滚动模式（字号/行距/字体），保证两种模式视觉一致
 */
.chapter-content-paginate {
  /* 视口高度 - 顶栏(56px) - 主体 py-6(48px) - 章节标题区(~90px) - 分页控件(~66px) - 章节导航(~102px) */
  height: calc(100vh - 360px);
  min-height: 320px;
  overflow-y: auto;
  scroll-behavior: smooth;
  -webkit-overflow-scrolling: touch;
  overscroll-behavior: contain;
}
@media (max-width: 640px) {
  /* 移动端：移除桌面端章节导航占用空间，给内容更多高度 */
  .chapter-content-paginate {
    height: calc(100vh - 280px);
  }
}
.chapter-content-paginate :deep(.prose) {
  max-width: none;
  font-family: var(--reader-font-family, system-ui, sans-serif);
}
.chapter-content-paginate :deep(.prose p) {
  font-size: var(--reader-font-size, 18px);
  line-height: var(--reader-line-height, 1.85);
  letter-spacing: var(--reader-letter-spacing, 0px);
  margin-bottom: var(--reader-paragraph-spacing, 1.2em);
}
.chapter-content-paginate :deep(.prose h1),
.chapter-content-paginate :deep(.prose h2),
.chapter-content-paginate :deep(.prose h3) {
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
