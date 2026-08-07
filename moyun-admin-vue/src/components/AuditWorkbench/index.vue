<template>
  <!--
    审核工作台通用组件
    - 提供左右分栏 + 待审/已办 Tab + 搜索 + 子筛选 + 列表 + 分页 + 详情卡片 + 审核操作区 的统一骨架
    - 业务差异（列表项渲染、详情正文、详情元信息、详情头额外 tag、已办结果额外字段）通过 slot 注入
    - 业务方需提供：listApi / detailApi / auditApi 三个适配函数，及 statusMap / doneFilterOptions / passStatus 等配置
    - embedded=true 时由「内容审核中心」Tab 容器引用，隐藏返回头与外层内边距
  -->
  <div class="app-container audit-container" :class="{ 'is-embedded': embedded }">
    <div class="audit-header" v-if="!embedded">
      <el-button @click="goBack">
        <el-icon><ArrowLeft /></el-icon> 返回
      </el-button>
      <span class="audit-title">{{ auditTitle }}</span>
    </div>

    <div class="audit-body">
      <!-- 左侧：列表区 -->
      <div class="audit-left" :style="{ width: leftWidth }">
        <el-tabs v-model="activeTab" @tab-change="handleTabChange">
          <el-tab-pane label="待审核" name="pending" />
          <el-tab-pane label="已办理" name="done" />
        </el-tabs>

        <div class="search-box">
          <el-input
            v-model="searchKeyword"
            :placeholder="searchPlaceholder"
            clearable
            size="small"
            @keyup.enter="handleSearch"
            @clear="handleSearch"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
        </div>

        <div v-if="activeTab === 'done'" class="sub-filter">
          <el-select v-model="doneFilter" size="small" style="width: 100%" @change="handleDoneFilterChange">
            <el-option
              v-for="opt in doneFilterOptions"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
        </div>

        <div v-loading="listLoading" class="audit-list">
          <div
            v-for="item in list"
            :key="item[idField]"
            class="audit-item"
            :class="{ 'is-active': currentId === item[idField] }"
            @click="handleSelect(item)"
          >
            <slot name="listItem" :item="item" />
          </div>
          <el-empty v-if="!listLoading && list.length === 0" description="暂无数据" />
        </div>

        <pagination
          v-show="total > 0"
          :total="total"
          v-model:page="pageNum"
          v-model:limit="pageSize"
          :page-sizes="[10, 20, 30]"
          @pagination="getList"
        />
      </div>

      <!-- 右侧：详情区 -->
      <div class="audit-right">
        <el-card v-if="currentDetail" v-loading="detailLoading" shadow="never" class="detail-card">
          <template #header>
            <div class="detail-header">
              <span class="detail-title">{{ currentDetail[titleField] }}</span>
              <div class="detail-header-tags">
                <slot name="detailHeaderExtra" :detail="currentDetail" />
                <el-tag :type="getStatusType(currentDetail[statusField])">
                  {{ getStatusLabel(currentDetail[statusField]) }}
                </el-tag>
              </div>
            </div>
            <div v-if="currentDetail[subtitleField]" class="detail-subtitle">
              {{ currentDetail[subtitleField] }}
            </div>
          </template>

          <!-- 详情元信息（业务方注入） -->
          <div v-if="$slots.detailMeta" class="detail-meta">
            <slot name="detailMeta" :detail="currentDetail" />
          </div>

          <!-- 详情封面（业务方注入；不传则默认按 cover 字段渲染） -->
          <div v-if="currentDetail[coverField]" class="detail-cover">
            <span class="meta-label">{{ coverLabel }}</span>
            <el-image
              :src="currentDetail[coverField]"
              fit="cover"
              class="cover-img"
              :preview-src-list="[currentDetail[coverField]]"
            />
          </div>

          <!-- 详情正文（业务方注入：文章预览/专栏简介/话题描述等） -->
          <slot name="detailContent" :detail="currentDetail" />

          <!-- 已办理：审核结果 -->
          <template v-if="activeTab === 'done'">
            <el-divider content-position="left">审核结果</el-divider>
            <div class="audit-result">
              <el-tag :type="getStatusType(currentDetail[statusField])">
                {{ getStatusLabel(currentDetail[statusField]) }}
              </el-tag>
              <div v-if="resolveAuditRemark(currentDetail)" class="audit-remark">
                {{ resolveAuditRemark(currentDetail) }}
              </div>
              <div v-else class="audit-remark audit-remark-empty">（无审核意见）</div>
              <slot name="doneResultExtra" :detail="currentDetail" />
            </div>
          </template>

          <!-- 待审核：审核意见 + 操作按钮 -->
          <template v-if="activeTab === 'pending'">
            <el-divider content-position="left">审核意见</el-divider>
            <el-input
              v-model="auditRemark"
              type="textarea"
              :rows="4"
              placeholder="请输入审核意见（驳回时必填，通过时选填）"
            />
            <div class="audit-actions">
              <el-button type="danger" :loading="rejectLoading" @click="handleReject">驳回</el-button>
              <el-button type="primary" :loading="approveLoading" @click="handleApprove">
                {{ passButtonText }}
              </el-button>
            </div>
          </template>
        </el-card>

        <el-empty v-else :description="emptyDescription" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { ArrowLeft, Search } from '@element-plus/icons-vue';

interface StatusMeta {
  label: string;
  type: any;
}

interface FilterOption {
  label: string;
  value: string;
}

const props = withDefaults(defineProps<{
  /** 嵌入模式：被 Tab 容器引用时为 true */
  embedded?: boolean;
  /** 审核标题（仅非嵌入模式显示） */
  auditTitle: string;
  /** 搜索框占位文本 */
  searchPlaceholder?: string;
  /** 左侧列表区宽度（px） */
  leftWidth?: string;
  /** 详情空状态描述 */
  emptyDescription?: string;
  /** 通过按钮文案 */
  passButtonText?: string;

  /** 状态映射：status -> { label, type } */
  statusMap: Record<string, StatusMeta>;
  /** 已办理 tab 的子筛选项 */
  doneFilterOptions: FilterOption[];
  /** 已办理默认筛选项 */
  doneFilterDefault?: string;
  /** 通过时设置的状态值（如 published / active） */
  passStatus: string;
  /** 驳回时设置的状态值，默认 rejected */
  rejectStatus?: string;
  /** 通过成功提示 */
  passSuccessMessage?: string;
  /** 驳回成功提示 */
  rejectSuccessMessage?: string;

  /** 列表项 id 字段名，默认 id */
  idField?: string;
  /** 详情标题字段名，默认 title */
  titleField?: string;
  /** 详情副标题字段名（可选），默认 subtitle */
  subtitleField?: string;
  /** 详情状态字段名，默认 status */
  statusField?: string;
  /** 详情封面字段名，默认 cover */
  coverField?: string;
  /** 封面前缀文案，默认 "封面："；专栏/话题可改为 "封面：" 等 */
  coverLabel?: string;

  /** 列表查询：返回 { records: any[]; total: number } 或 { rows: any[]; total: number } */
  listApi: (params: any) => Promise<any>;
  /** 详情查询：返回业务详情对象（取 res.data） */
  detailApi: (id: any) => Promise<any>;
  /** 审核操作：传入 { id, status, auditRemark }，业务方负责适配后端字段 */
  auditApi: (payload: { id: any; status: string; auditRemark: string }) => Promise<any>;
  /** 列表/详情查询时附加的搜索字段名（如 title / keyword） */
  searchField?: string;
  /** 驳回原因是否必填，默认 true */
  rejectRemarkRequired?: boolean;
  /** 驳回原因为空时的提示文案 */
  rejectRemarkEmptyMessage?: string;
  /** 返回时回退的目标路由（embedded=false 且无历史时使用） */
  backRoute?: string;
}>(), {
  embedded: false,
  searchPlaceholder: '请输入关键词搜索',
  leftWidth: '420px',
  emptyDescription: '请从左侧选择条目查看详情',
  passButtonText: '通过并发布',
  doneFilterDefault: '',
  rejectStatus: 'rejected',
  passSuccessMessage: '审核通过',
  rejectSuccessMessage: '已驳回',
  idField: 'id',
  titleField: 'title',
  subtitleField: 'subtitle',
  statusField: 'status',
  coverField: 'cover',
  coverLabel: '封面：',
  searchField: 'keyword',
  rejectRemarkRequired: true,
  rejectRemarkEmptyMessage: '请输入驳回原因',
  backRoute: ''
});

const route = useRoute();
const router = useRouter();

const activeTab = ref<'pending' | 'done'>('pending');
const doneFilter = ref<string>(props.doneFilterDefault || props.doneFilterOptions[0]?.value || '');
const searchKeyword = ref('');
const listLoading = ref(false);
const detailLoading = ref(false);
const list = ref<any[]>([]);
const total = ref(0);
const pageNum = ref(1);
const pageSize = ref(10);
const currentId = ref<any>(null);
const currentDetail = ref<any>(null);

const auditRemark = ref('');
const approveLoading = ref(false);
const rejectLoading = ref(false);

function getStatusLabel(status: string) {
  return props.statusMap[status]?.label || status;
}
function getStatusType(status: string) {
  return props.statusMap[status]?.type || 'info';
}

/** 当前 tab 对应的查询状态 */
function getQueryStatus(): string {
  if (activeTab.value === 'pending') return 'pending';
  return doneFilter.value;
}

/** 解析已办详情中的审核意见（按字段优先级回退） */
function resolveAuditRemark(detail: any): string {
  return detail?.auditRemark || detail?.remark || '';
}

async function getList() {
  listLoading.value = true;
  try {
    const params: any = {
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      status: getQueryStatus()
    };
    if (searchKeyword.value) {
      params[props.searchField] = searchKeyword.value;
    }
    const res: any = await props.listApi(params);
    list.value = res?.data?.records || res?.records || res?.rows || res?.data?.rows || [];
    total.value = res?.data?.total ?? res?.total ?? 0;

    if (list.value.length > 0) {
      const exists = list.value.find((item: any) => item[props.idField] === currentId.value);
      if (!exists) {
        await handleSelect(list.value[0]);
      }
    } else if (currentDetail.value && activeTab.value === 'pending') {
      currentDetail.value = null;
      currentId.value = null;
    }
  } catch (error) {
    console.error(`[${props.auditTitle}] 加载列表失败:`, error);
  } finally {
    listLoading.value = false;
  }
}

function handleTabChange() {
  pageNum.value = 1;
  currentId.value = null;
  currentDetail.value = null;
  getList();
}

function handleSearch() {
  pageNum.value = 1;
  currentId.value = null;
  currentDetail.value = null;
  getList();
}

function handleDoneFilterChange() {
  pageNum.value = 1;
  currentId.value = null;
  currentDetail.value = null;
  getList();
}

async function handleSelect(item: any) {
  if (!item) return;
  currentId.value = item[props.idField];
  if (!props.embedded) {
    router.replace({ query: { id: String(item[props.idField]) } });
  }
  await loadDetail(item[props.idField]);
}

async function loadDetail(id: any) {
  detailLoading.value = true;
  try {
    const res: any = await props.detailApi(id);
    currentDetail.value = res?.data ?? res ?? null;
    auditRemark.value = '';
  } catch (error) {
    console.error(`[${props.auditTitle}] 加载详情失败:`, error);
    ElMessage.error(`加载详情失败`);
  } finally {
    detailLoading.value = false;
  }
}

async function handleReject() {
  if (!currentDetail.value) return;
  if (props.rejectRemarkRequired && !auditRemark.value.trim()) {
    ElMessage.warning(props.rejectRemarkEmptyMessage);
    return;
  }
  rejectLoading.value = true;
  try {
    await props.auditApi({
      id: currentDetail.value[props.idField],
      status: props.rejectStatus,
      auditRemark: auditRemark.value
    });
    ElMessage.success(props.rejectSuccessMessage);
    await refreshAfterAudit();
  } catch (error: any) {
    ElMessage.error(error?.message || '审核操作失败');
  } finally {
    rejectLoading.value = false;
  }
}

async function handleApprove() {
  if (!currentDetail.value) return;
  approveLoading.value = true;
  try {
    await props.auditApi({
      id: currentDetail.value[props.idField],
      status: props.passStatus,
      auditRemark: auditRemark.value
    });
    ElMessage.success(props.passSuccessMessage);
    await refreshAfterAudit();
  } catch (error: any) {
    ElMessage.error(error?.message || '审核操作失败');
  } finally {
    approveLoading.value = false;
  }
}

async function refreshAfterAudit() {
  const idx = list.value.findIndex((item: any) => item[props.idField] === currentId.value);
  list.value = list.value.filter((item: any) => item[props.idField] !== currentId.value);
  total.value = Math.max(0, total.value - 1);

  let nextItem: any = null;
  if (list.value.length > 0) {
    nextItem = list.value[Math.min(idx, list.value.length - 1)];
  }

  if (nextItem) {
    currentId.value = nextItem[props.idField];
    if (!props.embedded) {
      router.replace({ query: { id: String(nextItem[props.idField]) } });
    }
    await loadDetail(nextItem[props.idField]);
  } else if (total.value > 0 && pageNum.value > 1) {
    pageNum.value = Math.max(1, pageNum.value - 1);
    await getList();
  } else {
    currentDetail.value = null;
    currentId.value = null;
    if (!props.embedded) {
      router.replace({ query: {} });
    }
  }
}

function goBack() {
  if (window.history.state && window.history.state.back) {
    router.go(-1);
  } else if (props.backRoute) {
    router.push(props.backRoute);
  }
}

onMounted(async () => {
  const idFromQuery = route.query.id;
  await getList();
  if (idFromQuery && !props.embedded) {
    const exists = list.value.find((item: any) => String(item[props.idField]) === String(idFromQuery));
    if (exists) {
      currentId.value = exists[props.idField];
      await loadDetail(exists[props.idField]);
    } else {
      currentId.value = idFromQuery;
      await loadDetail(idFromQuery);
    }
  }
});

defineExpose({ refresh: getList });
</script>

<style scoped>
.audit-container { padding: 20px; }
.audit-container.is-embedded { padding: 0; }

.audit-header {
  display: flex;
  align-items: center;
  margin-bottom: 16px;
}
.audit-title {
  margin-left: 12px;
  font-size: 18px;
  font-weight: 600;
}

.audit-body {
  display: flex;
  gap: 16px;
  align-items: flex-start;
}

.audit-left {
  flex-shrink: 0;
  background: #fff;
  border-radius: 4px;
  padding: 12px;
}
.search-box { margin-bottom: 12px; }
.sub-filter { margin-bottom: 12px; }

.audit-list {
  max-height: calc(100vh - 320px);
  overflow-y: auto;
}
.audit-item {
  padding: 12px;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  margin-bottom: 8px;
  cursor: pointer;
  transition: background-color 0.2s;
}
.audit-item:hover { background-color: var(--el-color-primary-light-9); }
.audit-item.is-active {
  background-color: var(--el-color-primary-light-9);
  border-color: var(--el-color-primary);
}

.audit-right {
  flex: 1;
  min-width: 0;
  max-height: calc(100vh - 140px);
  overflow-y: auto;
  padding-right: 4px;
}
.detail-card { width: 100%; }
.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
}
.detail-header-tags {
  display: flex;
  align-items: center;
  gap: 8px;
}
.detail-title {
  font-size: 16px;
  font-weight: 600;
  word-break: break-all;
}
.detail-subtitle {
  margin-top: 4px;
  font-size: 13px;
  color: #909399;
  word-break: break-all;
}
.detail-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  margin-bottom: 12px;
}
.meta-item { font-size: 13px; }
.meta-label { color: #909399; }
.detail-cover { margin-bottom: 12px; }
.cover-img { width: 220px; height: 140px; border-radius: 4px; }

.audit-result {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.audit-remark {
  padding: 8px 12px;
  background: #f5f7fa;
  border-radius: 4px;
  font-size: 13px;
  color: #606266;
  white-space: pre-wrap;
}
.audit-remark-empty { color: #c0c4cc; }

.audit-actions {
  margin-top: 12px;
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
</style>
