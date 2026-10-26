<template>
  <div class="app-container">
    <el-alert type="info" :closable="false" class="page-tip"
      title="记账模块的下拉候选全部由字典驱动：在这里查看有哪些字典、各有多少条，再点「去维护」进入系统字典页编辑。为避免历史数据对不上，记账字典的「取值」不允许修改；交易类型与账户类型属计算口径，也不允许新增取值。" />

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button icon="Refresh" @click="getList">刷新</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-tag type="success" effect="plain">共 {{ rows.length }} 类字典</el-tag>
      </el-col>
      <el-col :span="1.5">
        <el-tag type="warning" effect="plain">值锁定：只改标签/排序/启停</el-tag>
      </el-col>
    </el-row>

    <el-table v-loading="loading" :data="rows" border>
      <el-table-column label="字典类型（dict_type）" min-width="220">
        <template #default="scope">
          <span class="dict-type">{{ scope.row.dictType }}</span>
        </template>
      </el-table-column>
      <el-table-column label="字典名称" prop="dictName" min-width="240" show-overflow-tooltip />
      <el-table-column label="取值数" width="90" align="center">
        <template #default="scope">
          <el-tag :type="scope.row.count > 0 ? 'success' : 'info'" size="small">{{ scope.row.count }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="锁定级别" width="150" align="center">
        <template #default="scope">
          <el-tooltip v-if="scope.row.semanticLocked"
            content="参与资金/统计计算口径：取值不可改，也不允许新增" placement="top">
            <el-tag type="danger" size="small" effect="plain">语义锁定</el-tag>
          </el-tooltip>
          <el-tooltip v-else content="取值不可改（历史数据按值匹配），可新增/停用" placement="top">
            <el-tag type="warning" size="small" effect="plain">值锁定</el-tag>
          </el-tooltip>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="110" align="center">
        <template #default="scope">
          <el-tag v-if="scope.row.missing" type="info" size="small">未初始化</el-tag>
          <el-tag v-else type="success" size="small">已初始化</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="130" align="center" fixed="right">
        <template #default="scope">
          <el-tooltip v-if="!scope.row.dictId" content="字典尚未初始化，请先执行增量脚本" placement="top">
            <span><el-button link type="primary" icon="Edit" disabled>去维护</el-button></span>
          </el-tooltip>
          <el-button v-else link type="primary" icon="Edit" @click="goMaintain(scope.row)">去维护</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-alert class="page-tip mt16" type="warning" :closable="false"
      title="新增取值前请确认 App 端已支持该值（例如新增计量单位需要 App 渲染支持）。交易类型/账户类型的新增已被后端拒绝。" />
  </div>
</template>

<script setup name="LedgerDict">
import { getLedgerDictOverview } from '@/api/cms/ledger'

const router = useRouter()
const loading = ref(false)
const rows = ref([])

function getList() {
  loading.value = true
  getLedgerDictOverview().then(res => {
    rows.value = res.data || []
  }).finally(() => { loading.value = false })
}

/** 跳系统字典数据页（维护单一入口，避免两套编辑逻辑） */
function goMaintain(row) {
  router.push('/system/dict-data/index/' + row.dictId)
}

getList()
</script>

<style scoped>
.page-tip { margin-bottom: 16px; }
.mt16 { margin-top: 16px; }
.dict-type { font-family: Consolas, Monaco, monospace; font-size: 13px; color: #303133; }
</style>
