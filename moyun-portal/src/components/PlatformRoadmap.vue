<template>
  <!--
    平台能力路线图（首页第一栏右侧 · 替代原「AI 简历诊断示例卡」）

    为什么放这里：首屏第一栏原本左侧是「简历改 3 遍」+「免费简历诊断」，右侧又是整张
    「AI 简历诊断报告」+「生成我的诊断报告」——**简历/模拟面试元素重复堆叠**，且除简历外
    看不出平台还能做什么。这里改为按产品定位展示**全平台 9 环旅程**。

    交互：鼠标划入 / 键盘聚焦 → 节点高亮 + 图标放大 + 弹出提示气泡（含"需登录/游客可看"）；
         按序号逐环点亮形成旅程进度；节点错峰入场；仅用主题变量；尊重 reduced-motion。
    节点路由均取自 router/index.ts 的真实路由（逐条核对）。
  -->
  <nav class="roadmap-wrap" :class="{ 'is-compact': compact }" aria-label="平台能力路线图">
    <ol class="roadmap" :class="{ 'is-band': isBand }" :style="gridStyle">
      <li
        v-for="(node, idx) in nodes"
        :key="node.key"
        class="roadmap-node"
        :class="{
          'is-active': activeIndex === idx,
          'is-before': activeIndex >= 0 && idx < activeIndex,
        }"
        :style="{ animationDelay: `${idx * 0.05}s` }"
      >
        <button
          type="button"
          class="roadmap-dot"
          :aria-label="`${node.label}：${node.hint}${node.auth ? '（需登录）' : ''}`"
          @mouseenter="activeIndex = idx"
          @mouseleave="activeIndex = -1"
          @focus="activeIndex = idx"
          @blur="activeIndex = -1"
          @click="goNode(node)"
        >
          <component :is="node.icon" class="roadmap-icon" />
          <span class="roadmap-step">{{ idx + 1 }}</span>
        </button>

        <span class="roadmap-label">{{ node.label }}</span>

        <span class="roadmap-tip" role="tooltip">
          <span class="roadmap-tip-text">{{ node.hint }}</span>
          <span class="roadmap-tip-meta">
            <span v-if="node.auth" class="roadmap-tag roadmap-tag--auth">需登录</span>
            <span v-else class="roadmap-tag">游客可看</span>
          </span>
        </span>
      </li>
    </ol>
  </nav>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  GraduationCap, FileText, Mic, TrendingUp, ClipboardList,
  MessageCircle, PenLine, Users, BookOpen,
} from 'lucide-vue-next'
import { useAuth } from '@/composables/useAuth'

const props = withDefaults(defineProps<{
  /** 显式指定列数（用于卡片内固定 3 列）；不传则按视口自适应（3/5/9 列） */
  cols?: number
  /** 紧凑模式（卡片内使用，节点更小） */
  compact?: boolean
}>(), {
  cols: 0,
  compact: false,
})

interface RoadmapNode {
  key: string
  label: string
  hint: string
  route: string
  auth: boolean
  icon: unknown
}

/**
 * 9 环旅程节点（对齐产品定位：学习成长 → 简历优化 → 面试 → 复盘 → 复习 →
 * 社区互动 → 文章发布 → 话题 → 专栏）。
 * route 均为真实路由：/learn · /interview/resume/optimize · /interview/voice ·
 * /interview/voice/history · /learn/wrong · /feed · /publish · /topics · /columns
 * auth=true 对应路由 meta.requiresAuth=true（守卫跳登录并带回跳地址）。
 */
const nodes: RoadmapNode[] = [
  { key: 'learn', label: '学习成长', hint: '刷题、知识图谱与学习计划，进度自动记入成长时间线', route: '/learn', auth: false, icon: GraduationCap },
  { key: 'resume', label: '简历优化', hint: '上传简历 → AI 诊断薄弱点 → 按目标岗位生成优化建议', route: '/interview/resume/optimize', auth: true, icon: FileText },
  { key: 'interview', label: 'AI 面试', hint: '语音模拟面试：AI 面试官实时追问，边答边评分', route: '/interview/voice', auth: true, icon: Mic },
  { key: 'review', label: '面试复盘', hint: '回看每场对话与整场报告，定位答得最差的维度', route: '/interview/voice/history', auth: true, icon: TrendingUp },
  { key: 'practice', label: '错题复习', hint: '答错的题自动进错题本，按薄弱点定向重练', route: '/learn/wrong', auth: true, icon: ClipboardList },
  { key: 'community', label: '社区互动', hint: '动态广场看同行动态，点赞评论互相打气', route: '/feed', auth: false, icon: MessageCircle },
  { key: 'publish', label: '文章发布', hint: 'Markdown / 富文本写作，发布前可预览与版本回滚', route: '/publish', auth: true, icon: PenLine },
  { key: 'topic', label: '话题讨论', hint: '发起话题、发表观点，和同行就具体问题聊透', route: '/topics', auth: false, icon: Users },
  { key: 'column', label: '专栏创作', hint: '把系列文章做成专栏，持续连载积累个人品牌', route: '/columns', auth: false, icon: BookOpen },
]

/** 显式列数时用内联样式；未指定则交给 CSS 媒体查询（3/5/9） */
const gridStyle = computed(() => (props.cols > 0
  ? { gridTemplateColumns: `repeat(${props.cols}, minmax(0, 1fr))` }
  : undefined))

/** 单排"旅程连线"仅在宽排（≥7 列或自适应宽屏）时绘制 */
const isBand = computed(() => props.cols === 0 || props.cols >= 7)

const router = useRouter()
const { requireAuth } = useAuth()

const activeIndex = ref(-1)

/** 点击节点：需登录的统一走 requireAuth（带回跳），与全站行为一致 */
function goNode(node: RoadmapNode) {
  if (node.auth && !requireAuth(node.route)) return
  router.push(node.route)
}
</script>

<style scoped>
.roadmap-wrap { width: 100%; }

.roadmap {
  position: relative;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 1.25rem 0.5rem;
  margin: 0;
  padding: 0;
  list-style: none;
}

@media (min-width: 640px) {
  .roadmap { grid-template-columns: repeat(5, minmax(0, 1fr)); }
}

@media (min-width: 1024px) {
  .roadmap { grid-template-columns: repeat(9, minmax(0, 1fr)); gap: 0.75rem 0.25rem; }
  .roadmap.is-band::before {
    content: '';
    position: absolute;
    top: 1.5rem;
    left: 5%;
    right: 5%;
    height: 2px;
    border-radius: 999px;
    background: var(--theme-border);
    z-index: 0;
  }
}

.roadmap-node {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.5rem;
  animation: roadmap-rise 0.5s ease both;
}

.roadmap-dot {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 3rem;
  height: 3rem;
  border-radius: 999px;
  border: 1px solid var(--theme-border);
  background: var(--theme-surface);
  color: var(--theme-primary);
  cursor: pointer;
  transition: transform 0.25s ease, box-shadow 0.25s ease, border-color 0.25s ease,
    background-color 0.25s ease, color 0.25s ease;
}

.roadmap-icon { width: 1.25rem; height: 1.25rem; transition: transform 0.25s ease; }

.roadmap-step {
  position: absolute;
  top: -0.35rem;
  right: -0.35rem;
  min-width: 1.1rem;
  height: 1.1rem;
  padding: 0 0.2rem;
  border-radius: 999px;
  font-size: 0.625rem;
  line-height: 1.1rem;
  text-align: center;
  font-variant-numeric: tabular-nums;
  background: var(--theme-accent);
  color: var(--theme-text-secondary);
  border: 1px solid var(--theme-border);
}

.roadmap-label {
  font-size: 0.75rem;
  font-weight: 500;
  color: var(--theme-text);
  text-align: center;
  line-height: 1.25rem;
  transition: color 0.25s ease;
}

/* 高亮：鼠标划入 / 键盘聚焦 */
.roadmap-node.is-active .roadmap-dot,
.roadmap-dot:focus-visible {
  transform: translateY(-3px);
  border-color: color-mix(in srgb, var(--theme-primary) 55%, var(--theme-border));
  background: var(--theme-primary);
  color: var(--theme-on-primary);
  box-shadow: 0 10px 22px -10px color-mix(in srgb, var(--theme-primary) 55%, transparent);
}
.roadmap-node.is-active .roadmap-icon { transform: scale(1.12); }
.roadmap-node.is-active .roadmap-label { color: var(--theme-primary); }
.roadmap-node.is-before .roadmap-dot {
  border-color: color-mix(in srgb, var(--theme-primary) 40%, var(--theme-border));
}

/* 提示气泡 */
.roadmap-tip {
  position: absolute;
  bottom: calc(100% + 0.4rem);
  left: 50%;
  z-index: 30;
  width: max-content;
  max-width: 14rem;
  padding: 0.5rem 0.625rem;
  border-radius: 0.625rem;
  border: 1px solid var(--theme-border);
  background: var(--theme-surface);
  box-shadow: 0 12px 28px -14px rgba(0, 0, 0, 0.35);
  font-size: 0.6875rem;
  line-height: 1.05rem;
  color: var(--theme-text-secondary);
  text-align: left;
  opacity: 0;
  transform: translateX(-50%) translateY(0.375rem);
  pointer-events: none;
  transition: opacity 0.2s ease, transform 0.2s ease;
}
.roadmap-node.is-active .roadmap-tip { opacity: 1; transform: translateX(-50%) translateY(0); }
.roadmap-tip-meta { display: block; margin-top: 0.25rem; }

.roadmap-tag {
  display: inline-block;
  padding: 0.05rem 0.35rem;
  border-radius: 999px;
  font-size: 0.625rem;
  line-height: 1rem;
  background: var(--theme-accent);
  color: var(--theme-text-secondary);
}
.roadmap-tag--auth { background: var(--theme-primary-soft); color: var(--theme-primary); }

/* 紧凑模式（卡片内） */
.is-compact .roadmap { gap: 0.875rem 0.375rem; }
.is-compact .roadmap-dot { width: 2.5rem; height: 2.5rem; }
.is-compact .roadmap-icon { width: 1.05rem; height: 1.05rem; }
.is-compact .roadmap-label { font-size: 0.6875rem; line-height: 1.125rem; }
.is-compact .roadmap-step { min-width: 1rem; height: 1rem; font-size: 0.5625rem; line-height: 1rem; }

@keyframes roadmap-rise {
  from { opacity: 0; transform: translateY(0.5rem); }
  to { opacity: 1; transform: translateY(0); }
}

@media (prefers-reduced-motion: reduce) {
  .roadmap-node { animation: none; }
  .roadmap-dot, .roadmap-icon, .roadmap-label, .roadmap-tip { transition: none; }
  .roadmap-node.is-active .roadmap-dot { transform: none; }
  .roadmap-node.is-active .roadmap-icon { transform: none; }
}
</style>
