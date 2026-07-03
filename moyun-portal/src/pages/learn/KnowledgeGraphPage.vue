<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { useHead } from '@vueuse/head';
import { Network, Loader2, ChevronLeft, Tag } from 'lucide-vue-next';
import SiteFooter from '@/components/SiteFooter.vue';
import { generateSeo } from '@/utils/seo';
import { getKnowledgeGraph } from '@/api/learnStats';
import type { KnowledgeGraph, KnowledgeNode } from '@/api/learnStats';

useHead(computed(() => generateSeo({
  title: '知识图谱',
  description: '墨韵智库知识图谱与标签云 - 可视化面试题知识点分布与你的掌握度。',
  canonicalPath: '/learn/knowledge',
})));

// ==================== 状态 ====================
const loading = ref(true);
const error = ref<string | null>(null);
const graph = ref<KnowledgeGraph | null>(null);

async function loadGraph() {
  loading.value = true;
  error.value = null;
  try {
    // 不传 userId：后端在已登录时回退到当前用户，未登录时返回全局标签云
    const res = await getKnowledgeGraph();
    if (res.code === 200) {
      graph.value = res.data;
    } else {
      error.value = res.message || '加载知识图谱失败';
    }
  } catch (err) {
    const e = err as { message?: string };
    error.value = e?.message || '加载知识图谱失败，请稍后重试';
  } finally {
    loading.value = false;
  }
}

onMounted(loadGraph);

// ==================== 标签云：尺寸 / 颜色 ====================
const maxQuestionCount = computed(() => {
  const nodes = graph.value?.nodes || [];
  return Math.max(1, ...nodes.map((n) => n.questionCount || 0));
});

/** 标签字号：根据 questionCount 在 12-26px 之间映射 */
function tagFontSize(node: KnowledgeNode): number {
  const ratio = (node.questionCount || 0) / maxQuestionCount.value;
  return Math.round(12 + ratio * 14);
}

/** 掌握度着色：0 红 → 50 黄 → 100 绿 */
function masteryColor(mastery: number): string {
  if (mastery <= 0) return 'var(--theme-text-secondary)';
  if (mastery < 30) return '#ef4444';   // 红
  if (mastery < 60) return '#f59e0b';   // 琥珀
  if (mastery < 85) return '#3b82f6';   // 蓝
  return '#10b981';                      // 绿
}

// ==================== SVG 关系图（环形布局） ====================
const GRAPH_SIZE = 600;
const GRAPH_CENTER = GRAPH_SIZE / 2;
const GRAPH_RADIUS = 220;

interface PositionedNode extends KnowledgeNode {
  x: number;
  y: number;
  r: number;
}

const positionedNodes = computed<PositionedNode[]>(() => {
  const nodes = graph.value?.nodes || [];
  const n = nodes.length;
  if (n === 0) return [];
  const maxQ = maxQuestionCount.value;
  return nodes.map((node, i) => {
    const angle = (i / n) * Math.PI * 2 - Math.PI / 2; // 从顶部开始
    const ratio = (node.questionCount || 0) / maxQ;
    return {
      ...node,
      x: GRAPH_CENTER + GRAPH_RADIUS * Math.cos(angle),
      y: GRAPH_CENTER + GRAPH_RADIUS * Math.sin(angle),
      r: 10 + ratio * 18, // 10-28
    };
  });
});

const nodeIdToPos = computed<Map<number, PositionedNode>>(() => {
  const m = new Map<number, PositionedNode>();
  positionedNodes.value.forEach((p) => m.set(p.tagId, p));
  return m;
});

const positionedEdges = computed(() => {
  const edges = graph.value?.edges || [];
  const map = nodeIdToPos.value;
  return edges
    .map((e) => {
      const s = map.get(e.source);
      const t = map.get(e.target);
      if (!s || !t) return null;
      return { ...e, x1: s.x, y1: s.y, x2: t.x, y2: t.y };
    })
    .filter((e): e is NonNullable<typeof e> => e !== null);
});

const maxEdgeWeight = computed(() => {
  const edges = graph.value?.edges || [];
  return Math.max(1, ...edges.map((e) => e.weight || 0));
});

// ==================== 统计 ====================
const stats = computed(() => {
  const nodes = graph.value?.nodes || [];
  let totalQuestions = 0;
  let mastered = 0;
  nodes.forEach((n) => {
    totalQuestions += n.questionCount || 0;
    if ((n.mastery || 0) >= 85) mastered++;
  });
  const avgMastery = nodes.length > 0
    ? Math.round(nodes.reduce((s, n) => s + (n.mastery || 0), 0) / nodes.length)
    : 0;
  return { tagCount: nodes.length, totalQuestions, mastered, avgMastery };
});
</script>

<template>
  <div class="min-h-screen flex flex-col" style="background-color: var(--theme-bg);">
    <!-- 顶部条 -->
    <div class="border-b" style="background-color: var(--theme-surface); border-color: var(--theme-border);">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div class="flex items-center gap-3 h-16">
          <router-link to="/learn" class="p-2 rounded-lg transition-colors hover:bg-gray-100" style="color: var(--theme-text-secondary);">
            <ChevronLeft class="w-5 h-5" />
          </router-link>
          <h1 class="text-lg sm:text-xl font-semibold flex items-center gap-2" style="color: var(--theme-text);">
            <Network class="w-5 h-5" style="color: var(--theme-primary);" />
            知识图谱
          </h1>
          <span v-if="graph?.userId" class="ml-auto text-xs px-2 py-1 rounded-full" style="background-color: var(--theme-bg); color: var(--theme-text-secondary);">
            当前查看：我的掌握度
          </span>
          <span v-else class="ml-auto text-xs px-2 py-1 rounded-full" style="background-color: var(--theme-bg); color: var(--theme-text-secondary);">
            全站标签云（登录后查看个人掌握度）
          </span>
        </div>
      </div>
    </div>

    <!-- 主内容 -->
    <div class="flex-1 py-8">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 space-y-6">
        <!-- 加载中 -->
        <div v-if="loading" class="flex flex-col items-center justify-center py-20" style="color: var(--theme-text-secondary);">
          <Loader2 class="w-8 h-8 animate-spin mb-3" />
          <span>正在加载知识图谱...</span>
        </div>

        <!-- 错误 -->
        <div v-else-if="error" class="rounded-lg p-6 text-center" style="background-color: var(--theme-surface); border: 1px solid var(--theme-border); color: var(--theme-text-secondary);">
          {{ error }}
          <button @click="loadGraph" class="ml-3 underline">重试</button>
        </div>

        <template v-else-if="graph">
          <!-- 汇总 -->
          <section class="grid grid-cols-2 md:grid-cols-4 gap-3">
            <div class="rounded-lg p-4" style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);">
              <div class="text-xs" style="color: var(--theme-text-secondary);">知识点标签</div>
              <div class="text-2xl font-bold mt-1" style="color: var(--theme-text);">{{ stats.tagCount }}</div>
            </div>
            <div class="rounded-lg p-4" style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);">
              <div class="text-xs" style="color: var(--theme-text-secondary);">关联题目</div>
              <div class="text-2xl font-bold mt-1" style="color: var(--theme-text);">{{ stats.totalQuestions }}</div>
            </div>
            <div class="rounded-lg p-4" style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);">
              <div class="text-xs" style="color: var(--theme-text-secondary);">已掌握(≥85%)</div>
              <div class="text-2xl font-bold mt-1" style="color: #10b981;">{{ stats.mastered }}</div>
            </div>
            <div class="rounded-lg p-4" style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);">
              <div class="text-xs" style="color: var(--theme-text-secondary);">平均掌握度</div>
              <div class="text-2xl font-bold mt-1" style="color: #3b82f6;">{{ stats.avgMastery }}%</div>
            </div>
          </section>

          <!-- 标签云 -->
          <section class="rounded-lg p-4 sm:p-6" style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);">
            <h2 class="font-semibold flex items-center gap-2 mb-4" style="color: var(--theme-text);">
              <Tag class="w-5 h-5" style="color: var(--theme-primary);" />
              标签云（字号=题目数，颜色=掌握度）
            </h2>
            <div v-if="(graph.nodes || []).length" class="flex flex-wrap items-center gap-2">
              <span
                v-for="node in graph.nodes"
                :key="`tag-${node.tagId}`"
                class="inline-block rounded-full px-3 py-1 leading-tight cursor-default transition-transform hover:scale-110"
                :style="{
                  fontSize: tagFontSize(node) + 'px',
                  color: masteryColor(node.mastery || 0),
                  backgroundColor: 'var(--theme-bg)',
                  border: '1px solid var(--theme-border)',
                }"
                :title="`${node.name}：题目 ${node.questionCount} 道 / 掌握 ${node.mastery || 0}%`"
              >
                {{ node.name }}
                <span class="text-[10px] opacity-70">({{ node.questionCount }})</span>
              </span>
            </div>
            <div v-else class="text-sm py-8 text-center" style="color: var(--theme-text-secondary);">
              暂无标签数据
            </div>
            <!-- 颜色图例 -->
            <div class="flex flex-wrap items-center gap-3 mt-4 text-[11px]" style="color: var(--theme-text-secondary);">
              <span>掌握度：</span>
              <span class="flex items-center gap-1"><span class="w-3 h-3 rounded-full inline-block" style="background-color: var(--theme-text-secondary);"></span>未练习</span>
              <span class="flex items-center gap-1"><span class="w-3 h-3 rounded-full inline-block" style="background-color: #ef4444;"></span>&lt;30%</span>
              <span class="flex items-center gap-1"><span class="w-3 h-3 rounded-full inline-block" style="background-color: #f59e0b;"></span>30-60%</span>
              <span class="flex items-center gap-1"><span class="w-3 h-3 rounded-full inline-block" style="background-color: #3b82f6;"></span>60-85%</span>
              <span class="flex items-center gap-1"><span class="w-3 h-3 rounded-full inline-block" style="background-color: #10b981;"></span>≥85%</span>
            </div>
          </section>

          <!-- SVG 关系图 -->
          <section class="rounded-lg p-4 sm:p-6" style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);">
            <h2 class="font-semibold flex items-center gap-2 mb-4" style="color: var(--theme-text);">
              <Network class="w-5 h-5" style="color: var(--theme-primary);" />
              知识关系图（标签共现）
            </h2>
            <div v-if="positionedNodes.length" class="w-full overflow-x-auto">
              <svg :viewBox="`0 0 ${GRAPH_SIZE} ${GRAPH_SIZE}`" class="w-full h-auto" style="min-width: 480px; max-width: 640px; margin: 0 auto; display: block;" role="img" aria-label="知识关系图">
                <!-- 边 -->
                <g>
                  <line
                    v-for="(e, i) in positionedEdges"
                    :key="`edge-${i}`"
                    :x1="e.x1" :y1="e.y1" :x2="e.x2" :y2="e.y2"
                    :stroke="'var(--theme-border)'"
                    :stroke-width="Math.max(1, Math.min(4, (e.weight / maxEdgeWeight) * 4))"
                    :opacity="0.6"
                  />
                </g>
                <!-- 节点 -->
                <g>
                  <g v-for="node in positionedNodes" :key="`node-${node.tagId}`">
                    <circle
                      :cx="node.x" :cy="node.y" :r="node.r"
                      :fill="masteryColor(node.mastery || 0)"
                      :stroke="'var(--theme-surface)'"
                      stroke-width="2"
                    >
                      <title>{{ node.name }}：题目 {{ node.questionCount }} 道 / 通过 {{ node.solved || 0 }} / 掌握 {{ node.mastery || 0 }}%</title>
                    </circle>
                    <text
                      :x="node.x" :y="node.y + node.r + 12"
                      text-anchor="middle"
                      font-size="11"
                      fill="var(--theme-text-secondary)"
                    >{{ node.name }}</text>
                  </g>
                </g>
              </svg>
            </div>
            <div v-else class="text-sm py-8 text-center" style="color: var(--theme-text-secondary);">
              暂无标签数据
            </div>
          </section>
        </template>
      </div>
    </div>

    <SiteFooter />
  </div>
</template>
