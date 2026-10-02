<template>
  <div class="app-container">
    <!-- 搜索表单 -->
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="90px">
      <el-form-item label="用户ID" prop="userId">
        <el-input v-model="queryParams.userId" placeholder="用户ID" clearable style="width: 160px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="持卡人" prop="holderName">
        <el-input v-model="queryParams.holderName" placeholder="持卡人姓名" clearable style="width: 160px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="核验状态" prop="verifyStatus">
        <el-select v-model="queryParams.verifyStatus" placeholder="核验状态" clearable style="width: 140px">
          <el-option label="待核验" value="PENDING" />
          <el-option label="已通过" value="VERIFIED" />
          <el-option label="已驳回" value="REJECTED" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button icon="Refresh" @click="getList">刷新</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <!-- 数据表格（卡号仅脱敏展示，密文永不下发后台） -->
    <el-table v-loading="loading" :data="dataList">
      <el-table-column label="ID" align="center" prop="id" width="70" />
      <el-table-column label="用户ID" align="center" prop="userId" width="90" />
      <el-table-column label="持卡人" align="center" prop="holderName" width="110" />
      <el-table-column label="卡号（脱敏）" align="center" prop="cardNoMasked" width="190" />
      <el-table-column label="银行" align="center" prop="bankName" width="130" :show-overflow-tooltip="true">
        <template #default="scope">{{ scope.row.bankName || scope.row.bankCode || '-' }}</template>
      </el-table-column>
      <el-table-column label="核验状态" align="center" prop="verifyStatus" width="100">
        <template #default="scope">
          <el-tag :type="verifyTagType(scope.row.verifyStatus)" size="small">{{ verifyLabel(scope.row.verifyStatus) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="默认卡" align="center" prop="isDefault" width="80">
        <template #default="scope">
          <el-tag v-if="scope.row.isDefault === 1" type="success" size="small">默认</el-tag>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="绑定时间" align="center" prop="createTime" width="160">
        <template #default="scope">{{ parseTime(scope.row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="150" fixed="right">
        <template #default="scope">
          <!-- 人工核实：仅 PENDING 可操作（终态卡不再重复核实，避免反复改写资金相关状态） -->
          <el-button
            v-if="scope.row.verifyStatus === 'PENDING'"
            link
            type="primary"
            icon="EditPen"
            v-hasPermi="['cms:payBankCard:verify']"
            @click="openVerify(scope.row)"
          >人工核实</el-button>
          <span v-else>-</span>
        </template>
      </el-table-column>
    </el-table>

    <!-- 人工核实对话框 -->
    <el-dialog v-model="verifyOpen" title="银行卡人工核实" width="460px" append-to-body>
      <el-form label-width="90px">
        <el-form-item label="持卡人">
          <span>{{ verifyForm.holderName }}</span>
        </el-form-item>
        <el-form-item label="卡号">
          <span>{{ verifyForm.cardNoMasked }}</span>
        </el-form-item>
        <el-form-item label="核实结果">
          <el-radio-group v-model="verifyForm.verifyStatus">
            <el-radio value="VERIFIED">人工确认通过</el-radio>
            <el-radio value="REJECTED">人工判定不通过</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="提示">
          <span style="color:#909399;font-size:12px;line-height:1.5">
            请先与用户核对姓名、卡号与预留手机号；核实通过后该卡方可用于提现。
          </span>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="verifyOpen = false">取 消</el-button>
          <el-button type="primary" :loading="verifySubmitting" @click="submitVerify">确 定</el-button>
        </div>
      </template>
    </el-dialog>

    <pagination
      v-show="total > 0"
      :total="total"
      v-model:page="queryParams.pageNum"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />
  </div>
</template>

<script setup name="CmsPayBankCard">
import { getCurrentInstance, ref, reactive, onMounted } from "vue";
import { listBankCard, verifyBankCard } from "@/api/cms/pay";

const { proxy } = getCurrentInstance();

const dataList = ref([]);
const loading = ref(true);
const showSearch = ref(true);
const total = ref(0);

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  userId: undefined,
  holderName: undefined,
  verifyStatus: undefined
});

function verifyLabel(status) {
  const map = { PENDING: '待核验', VERIFIED: '已通过', REJECTED: '已驳回' };
  return map[status] || status;
}

function verifyTagType(status) {
  const map = { PENDING: 'info', VERIFIED: 'success', REJECTED: 'danger' };
  return map[status] || 'info';
}

// ── 人工核实（清单 #52：此前后台只有只读接口，PENDING 卡永远无法变为终态）──
const verifyOpen = ref(false);
const verifySubmitting = ref(false);
const verifyForm = reactive({
  id: undefined,
  holderName: '',
  cardNoMasked: '',
  verifyStatus: 'VERIFIED'
});

function openVerify(row) {
  verifyForm.id = row.id;
  verifyForm.holderName = row.holderName;
  verifyForm.cardNoMasked = row.cardNoMasked;
  verifyForm.verifyStatus = 'VERIFIED';
  verifyOpen.value = true;
}

async function submitVerify() {
  if (!verifyForm.id || !verifyForm.verifyStatus) return;
  verifySubmitting.value = true;
  try {
    await verifyBankCard(verifyForm.id, verifyForm.verifyStatus);
    proxy.$modal.msgSuccess('核实结果已保存');
    verifyOpen.value = false;
    getList();
  } finally {
    verifySubmitting.value = false;
  }
}

function getList() {
  loading.value = true;
  listBankCard(queryParams).then((response) => {
    dataList.value = response.data.records || [];
    total.value = response.data.total || 0;
    loading.value = false;
  }).catch(() => {
    loading.value = false;
  });
}

function handleQuery() {
  queryParams.pageNum = 1;
  getList();
}

function resetQuery() {
  proxy.resetForm("queryRef");
  handleQuery();
}

onMounted(() => {
  getList();
});
</script>
