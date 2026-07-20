<script setup lang="ts">
/**
 * 我的文章选择器（v1.1.3 新增）
 *
 * 业务背景：
 *   - 专栏编辑页（/column/edit/:id）：创建/编辑专栏时勾选文章
 *   - 专栏详情页（/column/:id）：管理文章时增量添加
 *
 *   两处共用此组件，从 GET /portal/article/my 拉候选列表，支持按标题搜索 + 多选 + 回填。
 *
 * 使用方式：
 *   <MyArticlePicker v-model="selectedIds" :exclude-ids="excludedIds" />
 *   - v-model 绑定已选 articleId 列表（string[]）
 *   - exclude-ids：候选列表中要排除的 articleId（例如已在专栏中的文章）
 *
 * 数据源：
 *   - 后端 GET /portal/article/my 已强制 authorId = 当前登录用户，无需前端传 authorId
 *   - 默认仅展示 status=published 的文章（避免把草稿/审核中的勾选进来）
 */
import { ref, computed, watch, onMounted } from 'vue';
import { Search, Loader2, Check, X, FileText } from 'lucide-vue-next';
import LazyImage from '@/components/LazyImage.vue';
import Empty from '@/components/Empty.vue';
import { getMyArticles } from '@/api/article';
import type { Article } from '@/types/api';

const props = withDefaults(defineProps<{
  // v1.1.3 修复：允许 string 和 number 混合（Article.id 是 string，ArticleSimpleVO.id 是 string|number）
  modelValue: Array<string | number>;
  // 候选列表排除的 articleId（已在专栏中的文章）
  excludeIds?: Array<string | number>;
  // 是否多选（默认 true；详情页增量添加场景可设 false 单选后即加入）
  multiple?: boolean;
  // 候选列表筛选状态（默认只展示已发布）
  status?: string;
  // 占位提示
  placeholder?: string;
  // 最多候选显示条数（防止一次拉太多）
  maxCandidates?: number;
}>(), {
  multiple: true,
  status: 'published',
  placeholder: '搜索文章标题...',
  maxCandidates: 50,
});

const emit = defineEmits<{
  'update:modelValue': [val: Array<string | number>];
  // 单选模式下选中后立即触发
  select: [article: Article];
}>();

const keyword = ref('');
const loading = ref(false);
const candidates = ref<Article[]>([]);
const selectedIds = ref<Array<string | number>>([...props.modelValue]);
const dropdownOpen = ref(false);

// 已选文章的完整对象（用于回填展示标题）
const selectedArticles = ref<Map<string | number, Article>>(new Map());

// 排除集合（string 化统一比较）
const excludeIdSet = computed(() => {
  const s = new Set<string>();
  (props.excludeIds || []).forEach(id => s.add(String(id)));
  return s;
});

// 过滤后的候选列表
const filteredCandidates = computed(() => {
  const kw = keyword.value.trim().toLowerCase();
  let list = candidates.value;
  if (kw) {
    list = list.filter(a => (a.title || '').toLowerCase().includes(kw));
  }
  // 排除 excludeIds 中的文章
  list = list.filter(a => !excludeIdSet.value.has(String(a.id)));
  // 限制显示数量
  return list.slice(0, props.maxCandidates);
});

// 已选 ID 集合（string 化）
const selectedIdSet = computed(() => {
  const s = new Set<string>();
  selectedIds.value.forEach(id => s.add(String(id)));
  return s;
});

// 加载我的文章列表
async function loadCandidates() {
  loading.value = true;
  try {
    const res = await getMyArticles({
      pageNum: 1,
      pageSize: 100, // 一次拉 100 条候选，覆盖大多数用户的文章量
      status: props.status,
    });
    if (res.code === 200 && res.data) {
      // 兼容 Page<PortalArticle> 与 list 两种返回结构
      const records = (res.data as any).records || (res.data as any).list || [];
      candidates.value = records as Article[];
      // 把已选的 article 完整对象回填到 selectedArticles
      candidates.value.forEach(a => {
        if (selectedIdSet.value.has(String(a.id))) {
          selectedArticles.value.set(String(a.id), a);
        }
      });
    }
  } catch (e) {
    console.warn('MyArticlePicker: 加载我的文章列表失败', e);
  } finally {
    loading.value = false;
  }
}

function toggleSelect(id: string | number, article: Article) {
  const sid = String(id);
  if (props.multiple) {
    if (selectedIdSet.value.has(sid)) {
      selectedIds.value = selectedIds.value.filter(x => String(x) !== sid);
      selectedArticles.value.delete(sid);
    } else {
      selectedIds.value = [...selectedIds.value, id];
      selectedArticles.value.set(sid, article);
    }
    emit('update:modelValue', selectedIds.value);
  } else {
    // 单选模式：选中后立即触发 select 事件，不更新 v-model
    emit('select', article);
    dropdownOpen.value = false;
  }
}

function removeSelected(id: string | number) {
  const sid = String(id);
  selectedIds.value = selectedIds.value.filter(x => String(x) !== sid);
  selectedArticles.value.delete(sid);
  emit('update:modelValue', selectedIds.value);
}

function toggleDropdown() {
  dropdownOpen.value = !dropdownOpen.value;
  if (dropdownOpen.value && candidates.value.length === 0) {
    loadCandidates();
  }
}

function closeDropdown() {
  dropdownOpen.value = false;
}

// 外部 modelValue 变化时同步到内部
watch(() => props.modelValue, (newVal) => {
  // 简化：只在长度不一致或元素不同步时重置
  const newStr = (newVal || []).map(x => String(x)).sort().join(',');
  const oldStr = selectedIds.value.map(x => String(x)).sort().join(',');
  if (newStr !== oldStr) {
    selectedIds.value = [...(newVal || [])];
  }
}, { deep: true });

onMounted(() => {
  // 预加载候选列表（用于回填展示标题）
  if (selectedIds.value.length > 0) {
    loadCandidates();
  }
});
</script>

<template>
  <div class="relative">
    <!-- 已选列表 -->
    <div v-if="multiple && selectedIds.length > 0" class="flex flex-wrap gap-2 mb-2">
      <div
        v-for="id in selectedIds"
        :key="id"
        class="inline-flex items-center gap-1 px-2 py-1 rounded-md text-xs"
        style="background-color: var(--theme-accent); color: var(--theme-primary);"
      >
        <FileText class="w-3 h-3" />
        <span class="max-w-[200px] truncate">
          {{ selectedArticles.get(String(id))?.title || `#${id}` }}
        </span>
        <button
          type="button"
          @click="removeSelected(id)"
          class="ml-1 opacity-70 hover:opacity-100"
          title="移除"
        >
          <X class="w-3 h-3" />
        </button>
      </div>
    </div>

    <!-- 搜索 + 下拉容器 -->
    <div class="relative">
      <div class="flex items-center gap-2 px-3 py-2 rounded-lg" style="background-color: var(--theme-bg); border: 1px solid var(--theme-border);">
        <Search class="w-4 h-4 flex-shrink-0" style="color: var(--theme-text-secondary);" />
        <input
          v-model="keyword"
          @focus="toggleDropdown"
          @click="toggleDropdown"
          type="text"
          :placeholder="placeholder"
          class="flex-1 bg-transparent text-sm focus:outline-none"
          style="color: var(--theme-text);"
        />
        <Loader2 v-if="loading" class="w-4 h-4 animate-spin" style="color: var(--theme-text-secondary);" />
        <button
          v-if="!multiple && keyword"
          type="button"
          @click="keyword = ''"
          class="opacity-70 hover:opacity-100"
        >
          <X class="w-4 h-4" style="color: var(--theme-text-secondary);" />
        </button>
      </div>

      <!-- 下拉候选列表 -->
      <div
        v-if="dropdownOpen"
        class="absolute z-50 mt-1 w-full max-h-72 overflow-y-auto rounded-lg shadow-lg"
        style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);"
      >
        <div v-if="loading" class="py-6 text-center text-sm" style="color: var(--theme-text-secondary);">
          加载中...
        </div>
        <div v-else-if="filteredCandidates.length === 0" class="py-6">
          <Empty :description="keyword ? '没有匹配的文章' : '暂无可选文章'" />
        </div>
        <div v-else>
          <button
            v-for="a in filteredCandidates"
            :key="a.id"
            type="button"
            @click="toggleSelect(a.id, a)"
            class="w-full flex items-center gap-2 px-3 py-2 text-left transition hover:bg-black/5"
            :style="selectedIdSet.has(String(a.id)) ? 'background-color: var(--theme-accent);' : ''"
          >
            <!-- 复选框 -->
            <div
              v-if="multiple"
              class="w-4 h-4 rounded border flex items-center justify-center flex-shrink-0"
              :style="selectedIdSet.has(String(a.id))
                ? 'background-color: var(--theme-primary); border-color: var(--theme-primary);'
                : 'border-color: var(--theme-border);'"
            >
              <Check v-if="selectedIdSet.has(String(a.id))" class="w-3 h-3 text-white" />
            </div>
            <!-- 封面 -->
            <div v-if="a.cover" class="w-8 h-10 rounded overflow-hidden flex-shrink-0">
              <LazyImage :src="a.cover" :alt="a.title" class="w-full h-full object-cover" />
            </div>
            <!-- 标题 + 摘要 -->
            <div class="flex-1 min-w-0">
              <p class="text-sm truncate" style="color: var(--theme-text);">{{ a.title }}</p>
              <p v-if="a.excerpt" class="text-xs truncate" style="color: var(--theme-text-secondary);">{{ a.excerpt }}</p>
            </div>
            <!-- ID 提示 -->
            <span class="text-xs flex-shrink-0" style="color: var(--theme-text-secondary);">#{{ a.id }}</span>
          </button>
        </div>
      </div>
    </div>

    <!-- 点击外部关闭下拉 -->
    <div
      v-if="dropdownOpen"
      class="fixed inset-0 z-40"
      @click="closeDropdown"
    />
  </div>
</template>
