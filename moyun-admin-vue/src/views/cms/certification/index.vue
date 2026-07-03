<template>
  <div class="app-container">
    <!-- 搜索表单 -->
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择状态" clearable style="width: 200px">
          <el-option label="待审核" value="pending" />
          <el-option label="已通过" value="approved" />
          <el-option label="已驳回" value="rejected" />
        </el-select>
      </el-form-item>
      <el-form-item label="认证类型" prop="certType">
        <el-select v-model="queryParams.certType" placeholder="请选择类型" clearable style="width: 200px">
          <el-option label="身份认证" value="identity" />
          <el-option label="创作者认证" value="creator" />
          <el-option label="专家认证" value="expert" />
        </el-select>
      </el-form-item>
      <el-form-item label="真实姓名" prop="realName">
        <el-input
          v-model="queryParams.realName"
          placeholder="请输入真实姓名"
          clearable
          style="width: 200px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="用户ID" prop="userId">
        <el-input
          v-model="queryParams.userId"
          placeholder="请输入用户ID"
          clearable
          style="width: 200px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- 操作按钮 -->
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button icon="Refresh" @click="getList">刷新</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <!-- 数据表格 -->
    <el-table v-loading="loading" :data="dataList">
      <el-table-column label="ID" align="center" prop="id" width="80" />
      <el-table-column label="申请人" align="center" prop="nickname" width="140" :show-overflow-tooltip="true">
        <template #default="scope">
          <span>{{ scope.row.nickname || '-' }}</span>
          <div style="font-size: 12px; color: #909399;">ID: {{ scope.row.userId }}</div>
        </template>
      </el-table-column>
      <el-table-column label="真实姓名" align="center" prop="realName" width="120" :show-overflow-tooltip="true" />
      <el-table-column label="认证类型" align="center" prop="certType" width="120">
        <template #default="scope">
          <el-tag :type="getCertTypeTagType(scope.row.certType)">{{ getCertTypeText(scope.row.certType) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="证件号" align="center" prop="certNo" width="180" :show-overflow-tooltip="true" />
      <el-table-column label="代表作" align="center" prop="works" min-width="160" :show-overflow-tooltip="true">
        <template #default="scope">
          <el-link v-if="scope.row.works" :href="scope.row.works" target="_blank" type="primary" :underline="false">
            {{ scope.row.works }}
          </el-link>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" prop="status" width="100">
        <template #default="scope">
          <el-tag :type="getStatusTagType(scope.row.status)">{{ getStatusText(scope.row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="申请时间" align="center" prop="createdTime" width="160">
        <template #default="scope">
          <span>{{ parseTime(scope.row.createdTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="审核时间" align="center" prop="auditedTime" width="160">
        <template #default="scope">
          <span>{{ scope.row.auditedTime ? parseTime(scope.row.auditedTime) : '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="200" fixed="right">
        <template #default="scope">
          <el-button
            link
            type="primary"
            icon="View"
            @click="handleView(scope.row)"
          >详情</el-button>
          <el-button
            v-if="scope.row.status === 'pending'"
            link
            type="success"
            icon="Check"
            @click="handleAudit(scope.row)"
            v-hasPermi="['cms:certification:audit']"
          >审核</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <pagination
      v-show="total > 0"
      :total="total"
      v-model:page="queryParams.pageNum"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />

    <!-- 详情对话框 -->
    <el-dialog title="认证申请详情" v-model="viewOpen" width="640px" append-to-body>
      <el-descriptions v-if="currentRow" :column="2" border>
        <el-descriptions-item label="申请人">{{ currentRow.nickname || '-' }}</el-descriptions-item>
        <el-descriptions-item label="用户ID">{{ currentRow.userId }}</el-descriptions-item>
        <el-descriptions-item label="真实姓名">{{ currentRow.realName }}</el-descriptions-item>
        <el-descriptions-item label="认证类型">
          <el-tag :type="getCertTypeTagType(currentRow.certType)">{{ getCertTypeText(currentRow.certType) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="证件号">{{ currentRow.certNo || '-' }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="getStatusTagType(currentRow.status)">{{ getStatusText(currentRow.status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="代表作链接" :span="2">
          <el-link v-if="currentRow.works" :href="currentRow.works" target="_blank" type="primary" :underline="false">
            {{ currentRow.works }}
          </el-link>
          <span v-else>-</span>
        </el-descriptions-item>
        <el-descriptions-item label="自我介绍" :span="2">
          <span v-if="currentRow.intro">{{ currentRow.intro }}</span>
          <span v-else style="color: #c0c4cc;">（无）</span>
        </el-descriptions-item>
        <el-descriptions-item v-if="currentRow.certImage" label="证件照" :span="2">
          <el-image
            :src="currentRow.certImage"
            :preview-src-list="[currentRow.certImage]"
            fit="cover"
            style="width: 160px; height: 160px; border-radius: 6px;"
          />
        </el-descriptions-item>
        <el-descriptions-item label="申请时间">{{ parseTime(currentRow.createdTime) }}</el-descriptions-item>
        <el-descriptions-item label="审核时间">{{ currentRow.auditedTime ? parseTime(currentRow.auditedTime) : '-' }}</el-descriptions-item>
        <el-descriptions-item v-if="currentRow.auditRemark" label="审核备注" :span="2">
          {{ currentRow.auditRemark }}
        </el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="viewOpen = false">关 闭</el-button>
          <el-button
            v-if="currentRow && currentRow.status === 'pending'"
            type="success"
            @click="handleAudit(currentRow)"
            v-hasPermi="['cms:certification:audit']"
          >前往审核</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 审核对话框 -->
    <el-dialog title="认证审核" v-model="auditOpen" width="520px" append-to-body>
      <el-form ref="auditRef" :model="auditForm" :rules="auditRules" label-width="100px">
        <el-form-item label="申请人">
          <span>{{ auditForm.nickname || '-' }}（{{ auditForm.realName }}）</span>
        </el-form-item>
        <el-form-item label="认证类型">
          <el-tag :type="getCertTypeTagType(auditForm.certType)">{{ getCertTypeText(auditForm.certType) }}</el-tag>
        </el-form-item>
        <el-form-item label="审核结果" prop="status">
          <el-radio-group v-model="auditForm.status">
            <el-radio label="approved">通过</el-radio>
            <el-radio label="rejected">驳回</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="审核备注" prop="remark">
          <el-input
            v-model="auditForm.remark"
            type="textarea"
            :rows="4"
            placeholder="驳回时必填，通过时选填"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" :loading="auditLoading" @click="submitAudit">确 定</el-button>
          <el-button @click="auditOpen = false">取 消</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="CmsCertification">
import { listCertification, auditCertification } from "@/api/cms/certification";

const { proxy } = getCurrentInstance();

const dataList = ref([]);
const loading = ref(true);
const showSearch = ref(true);
const total = ref(0);

const viewOpen = ref(false);
const auditOpen = ref(false);
const auditLoading = ref(false);
const currentRow = ref(null);

const data = reactive({
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    status: undefined,
    certType: undefined,
    realName: undefined,
    userId: undefined
  },
  auditForm: {
    id: undefined,
    status: 'approved',
    remark: '',
    nickname: '',
    realName: '',
    certType: ''
  },
  auditRules: {
    status: [{ required: true, message: '请选择审核结果', trigger: 'change' }],
    remark: [{ required: true, message: '驳回时审核备注必填', trigger: 'blur' }]
  }
});

const { queryParams, auditForm, auditRules } = toRefs(data);

/** 状态文本 */
function getStatusText(status) {
  const map = { pending: '待审核', approved: '已通过', rejected: '已驳回' };
  return map[status] || status || '-';
}

/** 状态标签类型 */
function getStatusTagType(status) {
  const map = { pending: 'warning', approved: 'success', rejected: 'danger' };
  return map[status] || 'info';
}

/** 认证类型文本 */
function getCertTypeText(certType) {
  const map = { identity: '身份认证', creator: '创作者认证', expert: '专家认证' };
  return map[certType] || certType || '-';
}

/** 认证类型标签类型 */
function getCertTypeTagType(certType) {
  const map = { identity: '', creator: 'success', expert: 'warning' };
  return map[certType] || '';
}

/** 查询列表 */
function getList() {
  loading.value = true;
  listCertification(queryParams.value).then(response => {
    dataList.value = (response.data && response.data.records) || response.rows || [];
    total.value = (response.data && response.data.total) || response.total || 0;
    loading.value = false;
  }).catch(() => {
    loading.value = false;
  });
}

/** 搜索按钮操作 */
function handleQuery() {
  queryParams.value.pageNum = 1;
  getList();
}

/** 重置按钮操作 */
function resetQuery() {
  proxy.resetForm("queryRef");
  handleQuery();
}

/** 详情 */
function handleView(row) {
  currentRow.value = row;
  viewOpen.value = true;
}

/** 打开审核 */
function handleAudit(row) {
  auditForm.value = {
    id: row.id,
    status: 'approved',
    remark: '',
    nickname: row.nickname,
    realName: row.realName,
    certType: row.certType
  };
  auditOpen.value = true;
  viewOpen.value = false;
}

/** 提交审核 */
function submitAudit() {
  proxy.$refs["auditRef"].validate(valid => {
    if (!valid) return;
    if (auditForm.value.status === 'rejected' && !auditForm.value.remark) {
      proxy.$modal.msgError("驳回时审核备注必填");
      return;
    }
    auditLoading.value = true;
    auditCertification({
      id: auditForm.value.id,
      status: auditForm.value.status,
      remark: auditForm.value.remark
    }).then(() => {
      proxy.$modal.msgSuccess("审核成功");
      auditOpen.value = false;
      getList();
    }).finally(() => {
      auditLoading.value = false;
    });
  });
}

onMounted(() => {
  getList();
});
</script>

<style scoped>
.mb8 {
  margin-bottom: 8px;
}
</style>
