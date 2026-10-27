<template>
  <div class="app-container">
    <el-alert
      title="统计基于内存计数，服务重启后清零；零调用列表与启动时注册的接口做差集。"
      type="info"
      :closable="false"
      show-icon
      style="margin-bottom: 16px"
    />

    <el-tabs v-model="tab" @tab-change="onTab">
      <el-tab-pane label="调用排行" name="called">
        <div class="toolbar">
          <el-input v-model="keyword" placeholder="搜索接口路径" clearable style="width: 320px" @input="filterCalled" />
          <el-button :loading="loading" @click="loadCalled">刷新</el-button>
        </div>
        <el-table :data="calledRows" border stripe height="calc(100vh - 300px)">
          <el-table-column type="index" label="#" width="60" />
          <el-table-column prop="api" label="接口" min-width="420" show-overflow-tooltip />
          <el-table-column prop="count" label="调用次数" width="120" sortable />
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="零调用接口" name="zero">
        <div class="toolbar">
          <el-input v-model="keyword" placeholder="搜索接口路径" clearable style="width: 320px" @input="filterZero" />
          <el-button :loading="zeroLoading" @click="loadZero">刷新</el-button>
          <span class="hint">共 {{ zeroTotal }} 个</span>
        </div>
        <el-table :data="zeroRows" border stripe height="calc(100vh - 300px)">
          <el-table-column type="index" label="#" width="60" />
          <el-table-column prop="api" label="接口" min-width="520" show-overflow-tooltip />
        </el-table>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import request from '@/utils/request';

defineOptions({ name: 'ApiStats' });

const tab = ref('called');
const keyword = ref('');
const loading = ref(false);
const zeroLoading = ref(false);
const calledAll = ref([]);
const calledRows = ref([]);
const zeroAll = ref([]);
const zeroRows = ref([]);
const zeroTotal = ref(0);

function filterCalled() {
  const k = keyword.value.trim();
  calledRows.value = k ? calledAll.value.filter(i => i.api.includes(k)) : calledAll.value;
}

function filterZero() {
  const k = keyword.value.trim();
  zeroRows.value = k ? zeroAll.value.filter(i => i.api.includes(k)) : zeroAll.value;
}

async function loadCalled() {
  loading.value = true;
  try {
    const res = await request({ url: '/cms/monitor/api-stats', method: 'get', params: { limit: 300 } });
    calledAll.value = res.data?.items || [];
    filterCalled();
  } finally {
    loading.value = false;
  }
}

async function loadZero() {
  zeroLoading.value = true;
  try {
    const res = await request({ url: '/cms/monitor/api-stats/zero-call', method: 'get' });
    zeroAll.value = (res.data?.items || []).map(api => ({ api }));
    zeroTotal.value = res.data?.total || zeroAll.value.length;
    filterZero();
  } finally {
    zeroLoading.value = false;
  }
}

function onTab(name) {
  if (name === 'zero' && !zeroAll.value.length) loadZero();
}

onMounted(loadCalled);
</script>

<style scoped>
.toolbar { display: flex; gap: 12px; align-items: center; margin-bottom: 12px; }
.hint { color: var(--el-text-color-secondary); font-size: 13px; }
</style>
