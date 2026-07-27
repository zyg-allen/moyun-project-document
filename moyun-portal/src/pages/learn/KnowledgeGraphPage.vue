<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { useHead } from '@vueuse/head';
import { Network, Loader2, ChevronLeft, Tag, Target, Sparkles, AlertCircle } from 'lucide-vue-next';
import SiteFooter from '@/components/SiteFooter.vue';
import Breadcrumb from '@/components/Breadcrumb.vue';
import { generateSeo } from '@/utils/seo';
import { getKnowledgeGraph } from '@/api/learnStats';
import { getMyMockProfile } from '@/api/mockInterview';
import { useAuth } from '@/composables/useAuth';
import type { KnowledgeGraph, KnowledgeNode } from '@/api/learnStats';
import type { UserProfileSnapshotVO } from '@/types/api';

const router = useRouter();
const { isAuthenticated } = useAuth();

useHead(computed(() => generateSeo({
  title: '知识图谱',
  description: '墨韵智库知识图谱与标签云 - 可视化面试题知识点分布与你的掌握度。',
  canonicalPath: '/learn/knowledge',
})));

// ==================== 状态 ====================
const loading = ref(true);
const error = ref<string | null>(null);
const graph = ref<KnowledgeGraph | null>(null);
// v5.9 阶段3：画像薄弱点（与图谱数据源一致，用于高亮与跳转）
const profile = ref<UserProfileSnapshotVO | null>(null);
const weakTagNames = computed<Set<string>>(() => {
  const set = new Set<string>();
  if (profile.value?.weakTags) {
    for (const wt of profile.value.weakTags) {
      if (wt.tagName) set.add(wt.tagName);
    }
  }
  return set;
});
// 必备技能集合（用于在图谱中标识岗位相关节点）
const requiredSkillNames = computed<Set<string>>(() => {
  const set = new Set<string>();
  if (profile.value?.requiredSkills) {
    for (const s of profile.value.requiredSkills) set.add(s);
  }
  return set;
});
// 节点是否为薄弱点（按名称匹配，因为图谱节点 tagId 与画像 tagId 可能不一致）
function isWeakNode(node: KnowledgeNode): boolean {
  return weakTagNames.value.has(node.name);
}
// 节点是否为岗位必备技能
function isRequiredNode(node: KnowledgeNode): boolean {
  return requiredSkillNames.value.has(node.name);
}

const breadcrumbs = computed(() => [
  { label: '学习中心', path: '/learn' },
  { label: '知识图谱' },
]);

async function loadGraph() {
  loading.value = true;
  error.value = null;
  try {
    // 不传 userId：后端在已登录时回退到当前用户，未登录时返回全局标签云
    const [graphRes, profileRes] = await Promise.all([
      getKnowledgeGraph(),
      isAuthenticated() ? getMyMockProfile({}).catch(() => null) : Promise.resolve(null),
    ]);
    if (graphRes.code === 200) {
      graph.value = graphRes.data;
    } else {
      error.value = graphRes.message || '加载知识图谱失败';
    }
    if (profileRes && profileRes.code === 200 && profileRes.data) {
      profile.value = profileRes.data;
    }
  } catch (err) {
    const e = err as { message?: string };
    error.value = e?.message || '加载知识图谱失败，请稍后重试';
  } finally {
    loading.value = false;
  }
}

onMounted(loadGraph);

/** 跳转到该标签的题目列表（按关键词搜索） */
function gotoTagQuestions(tagName: string) {
  router.push({ path: '/interview/questions', query: { keyword: tagName } });
}

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
    <!-- 吸顶面包屑栏 -->
    <div
      class="border-b sticky top-0 z-30 backdrop-blur-sm py-3"
      style="background-color: var(--theme-surface); border-color: var(--theme-border);"
    >
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 flex items-center justify-between gap-4">
        <Breadcrumb :items="breadcrumbs" />
      </div>
    </div>

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

          <!-- v5.9 阶段3：我的薄弱点侧栏（画像驱动） -->
          <section
            v-if="profile && profile.personalized"
            class="rounded-lg p-4 sm:p-6"
            style="background: linear-gradient(135deg, var(--theme-surface), color-mix(in srgb, var(--theme-primary) 6%, var(--theme-surface))); border: 1px solid var(--theme-border);"
          >
            <h2 class="font-semibold flex items-center gap-2 mb-3" style="color: var(--theme-text);">
              <Target class="w-5 h-5" style="color: var(--theme-primary);" />
              我的薄弱点
              <span class="text-xs font-normal px-2 py-0.5 rounded-full" style="background-color: var(--theme-bg); color: var(--theme-text-secondary);">
                来自画像快照
              </span>
            </h2>
            <p class="text-xs mb-3" style="color: var(--theme-text-secondary);">
              以下知识点失败率较高，建议优先攻克。点击标签可跳转至相关题目。
            </p>
            <!-- 薄弱点标签云（可点击跳转） -->
            <div v-if="profile.weakTags && profile.weakTags.length > 0" class="flex flex-wrap gap-2">
              <button
                v-for="wt in profile.weakTags"
                :key="wt.tagId"
                @click="gotoTagQuestions(wt.tagName)"
                class="inline-flex items-center gap-1 px-3 py-1 rounded-full text-sm transition hover:scale-105"
                style="background-color: rgba(239,68,68,0.08); color: #ef4444; border: 1px solid rgba(239,68,68,0.2);"
                :title="`答 ${wt.total} 题，通过 ${wt.solved}，失败率 ${Math.round((wt.failRate || 0) * 100)}%`"
              >
                {{ wt.tagName }}
                <span class="text-[10px] opacity-70">
                  {{ Math.round((wt.failRate || 0) * 100) }}%
                </span>
              </button>
            </div>
            <div v-else class="text-sm py-3 text-center" style="color: var(--theme-text-secondary);">
              <AlertCircle class="w-4 h-4 inline mr-1" />
              暂无薄弱点数据，多做题以激活画像分析
            </div>
            <!-- 岗位必备技能未掌握提示 -->
            <div
              v-if="profile.requiredSkills && profile.requiredSkills.length > 0"
              class="mt-4 pt-3 border-t"
              style="border-color: var(--theme-border);"
            >
              <div class="text-xs mb-2 flex items-center" style="color: var(--theme-text-secondary);">
                <Sparkles class="w-3.5 h-3.5 mr-1" />
                岗位必备技能（红色为图谱中未掌握，建议重点突破）
              </div>
              <div class="flex flex-wrap gap-1.5">
                <span
                  v-for="skill in profile.requiredSkills"
                  :key="skill"
                  class="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-xs"
                  :style="weakTagNames.has(skill)
                    ? { backgroundColor: 'rgba(239,68,68,0.08)', color: '#ef4444', border: '1px solid rgba(239,68,68,0.2)' }
                    : { backgroundColor: 'rgba(16,185,129,0.08)', color: '#10b981', border: '1px solid rgba(16,185,129,0.2)' }"
                >{{ skill }}</span>
              </div>
            </div>
          </section>

          <!-- 标签云 -->
          <section class="rounded-lg p-4 sm:p-6" style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);">
            <h2 class="font-semibold flex items-center gap-2 mb-4" style="color: var(--theme-text);">
              <Tag class="w-5 h-5" style="color: var(--theme-primary);" />
              标签云（字号=题目数，颜色=掌握度）
              <span
                v-if="profile && profile.personalized"
                class="text-xs font-normal px-2 py-0.5 rounded-full"
                style="background-color: rgba(239,68,68,0.1); color: #ef4444;"
              >红色描边=薄弱点</span>
            </h2>
            <div v-if="(graph.nodes || []).length" class="flex flex-wrap items-center gap-2">
              <button
                v-for="node in graph.nodes"
                :key="`tag-${node.tagId}`"
                @click="gotoTagQuestions(node.name)"
                class="inline-block rounded-full px-3 py-1 leading-tight transition-transform hover:scale-110 cursor-pointer"
                :style="{
                  fontSize: tagFontSize(node) + 'px',
                  color: masteryColor(node.mastery || 0),
                  backgroundColor: 'var(--theme-bg)',
                  border: isWeakNode(node) ? '2px solid #ef4444' : (isRequiredNode(node) ? '2px solid var(--theme-primary)' : '1px solid var(--theme-border)'),
                }"
                :title="`${node.name}：题目 ${node.questionCount} 道 / 掌握 ${node.mastery || 0}%${isWeakNode(node) ? ' / 薄弱点' : ''}`"
              >
                {{ node.name }}
                <span class="text-[10px] opacity-70">({{ node.questionCount }})</span>
              </button>
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
              <span
                v-if="profile && profile.personalized"
                class="flex items-center gap-1 ml-2"
              >
                <span class="w-3 h-3 rounded-full inline-block" style="border: 2px solid #ef4444;"></span>薄弱点
              </span>
            </div>
          </section>

          <!-- SVG 关系图 -->
          <section class="rounded-lg p-4 sm:p-6" style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);">
            <h2 class="font-semibold flex items-center gap-2 mb-4" style="color: var(--theme-text);">
              <Network class="w-5 h-5" style="color: var(--theme-primary);" />
              知识关系图（标签共现）
              <span
                v-if="profile && profile.personalized"
                class="text-xs font-normal px-2 py-0.5 rounded-full"
                style="background-color: rgba(239,68,68,0.1); color: #ef4444;"
              >薄弱节点已高亮</span>
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
                  <g v-for="node in positionedNodes" :key="`node-${node.tagId}`" @click="gotoTagQuestions(node.name)" style="cursor: pointer;">
                    <!-- 薄弱点脉冲环 -->
                    <circle
                      v-if="isWeakNode(node)"
                      :cx="node.x" :cy="node.y" :r="node.r + 4"
                      fill="none"
                      stroke="#ef4444"
                      stroke-width="1.5"
                      opacity="0.5"
                    >
                      <animate attributeName="r" :values="`${node.r + 4};${node.r + 8};${node.r + 4}`" dur="2s" repeatCount="indefinite" />
                      <animate attributeName="opacity" values="0.5;0.2;0.5" dur="2s" repeatCount="indefinite" />
                    </circle>
                    <circle
                      :cx="node.x" :cy="node.y" :r="node.r"
                      :fill="masteryColor(node.mastery || 0)"
                      :stroke="isWeakNode(node) ? '#ef4444' : (isRequiredNode(node) ? 'var(--theme-primary)' : 'var(--theme-surface)')"
                      :stroke-width="isWeakNode(node) || isRequiredNode(node) ? 3 : 2"
                    >
                      <title>{{ node.name }}：题目 {{ node.questionCount }} 道 / 通过 {{ node.solved || 0 }} / 掌握 {{ node.mastery || 0 }}%{{ isWeakNode(node) ? ' / 薄弱点' : '' }}</title>
                    </circle>
                    <text
                      :x="node.x" :y="node.y + node.r + 12"
                      text-anchor="middle"
                      :font-size="isWeakNode(node) ? 12 : 11"
                      :font-weight="isWeakNode(node) ? 600 : 400"
                      :fill="isWeakNode(node) ? '#ef4444' : 'var(--theme-text-secondary)'"
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
