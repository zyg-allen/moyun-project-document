<template>
  <div class="app-container">
    <el-row :gutter="16">
      <!-- 主功能宫格 -->
      <el-col :span="14">
        <el-card shadow="hover">
          <template #header>
            <span>主功能宫格（"我的"页第一组）</span>
          </template>
          <el-table v-loading="loading" :data="mainFeatures" size="small">
            <el-table-column label="图标" width="60" align="center">
              <template #default="scope">
                <span style="font-size: 18px">{{ scope.row.icon }}</span>
              </template>
            </el-table-column>
            <el-table-column label="功能名称" min-width="110">
              <template #default="scope">
                <template v-if="editId === scope.row.id">
                  <el-input v-model="editForm.featureName" size="small" style="width: 120px" />
                  <el-input v-model="editForm.remark" size="small" placeholder="跳转链接（链接型入口）"
                    style="width: 120px; margin-top: 4px" />
                </template>
                <span v-else>{{ scope.row.featureName }}
                  <el-tag v-if="scope.row.badge" type="danger" size="small">{{ scope.row.badge }}</el-tag>
                </span>
              </template>
            </el-table-column>
            <el-table-column label="排序" prop="sortNum" width="80" align="center">
              <template #default="scope">
                <el-input-number v-if="editId === scope.row.id" v-model="editForm.sortNum" :min="0" :max="99" size="small" style="width: 80px" />
                <span v-else>{{ scope.row.sortNum }}</span>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="90" align="center">
              <template #default="scope">
                <el-tag :type="scope.row.status === 'done' ? 'success' : 'info'" size="small">
                  {{ scope.row.status === 'done' ? '已上线' : '开发中' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="展示" width="130" align="center">
              <template #default="scope">
                <el-switch v-model="scope.row.visible" :active-value="1" :inactive-value="0"
                           :disabled="editId === scope.row.id"
                           @change="handleToggleVisible(scope.row)" />
              </template>
            </el-table-column>
            <el-table-column label="操作" width="150" align="center">
              <template #default="scope">
                <template v-if="editId === scope.row.id">
                  <el-button link type="primary" size="small" @click="handleSave">保存</el-button>
                  <el-button link size="small" @click="cancelEdit">取消</el-button>
                </template>
                <el-button v-else link type="primary" size="small" v-hasPermi="['cms:ledgerAppFeature:edit']"
                           @click="startEdit(scope.row)">编辑</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>

      <!-- 推荐小功能 -->
      <el-col :span="10">
        <el-card shadow="hover">
          <template #header>
            <span>推荐小功能（"我的"页第二组）</span>
          </template>
          <el-table v-loading="loading" :data="recommendFeatures" size="small">
            <el-table-column label="图标" width="60" align="center">
              <template #default="scope">
                <span style="font-size: 18px">{{ scope.row.icon }}</span>
              </template>
            </el-table-column>
            <el-table-column label="功能名称" min-width="100">
              <template #default="scope">
                <template v-if="editId === scope.row.id">
                  <el-input v-model="editForm.featureName" size="small" style="width: 110px" />
                  <el-input v-model="editForm.remark" size="small" placeholder="跳转链接（链接型入口）"
                    style="width: 110px; margin-top: 4px" />
                </template>
                <span v-else>{{ scope.row.featureName }}</span>
              </template>
            </el-table-column>
            <el-table-column label="排序" prop="sortNum" width="70" align="center">
              <template #default="scope">
                <el-input-number v-if="editId === scope.row.id" v-model="editForm.sortNum" :min="0" :max="99" size="small" style="width: 70px" />
                <span v-else>{{ scope.row.sortNum }}</span>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="85" align="center">
              <template #default="scope">
                <el-tag :type="scope.row.status === 'done' ? 'success' : 'info'" size="small">
                  {{ scope.row.status === 'done' ? '已上线' : '开发中' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="展示" width="80" align="center">
              <template #default="scope">
                <el-switch v-model="scope.row.visible" :active-value="1" :inactive-value="0"
                           :disabled="editId === scope.row.id"
                           @change="handleToggleVisible(scope.row)" />
              </template>
            </el-table-column>
            <el-table-column label="操作" width="70" align="center">
              <template #default="scope">
                <el-button v-if="editId === scope.row.id" link type="primary" size="small" @click="handleSave">保存</el-button>
                <el-button v-else link type="primary" size="small" v-hasPermi="['cms:ledgerAppFeature:edit']"
                           @click="startEdit(scope.row)">编辑</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <el-alert
      title="运营说明：开发中功能默认隐藏（小程序端不展示），功能上线后在开关列打开即可生效；小程序端拉取失败时回退内置默认清单"
      type="info" :closable="false" style="margin-top: 16px" />
  </div>
</template>

<script setup name="LedgerAppFeature">
import { listAppFeatures, updateAppFeature } from "@/api/cms/ledger";

const { proxy } = getCurrentInstance();

const loading = ref(false);
const mainFeatures = ref([]);
const recommendFeatures = ref([]);
const editId = ref(null);
const editForm = reactive({ featureName: '', sortNum: 0, remark: '' });

function getList() {
  loading.value = true;
  listAppFeatures().then(response => {
    const rows = response.data || [];
    mainFeatures.value = rows.filter(r => r.groupType === 'main');
    recommendFeatures.value = rows.filter(r => r.groupType === 'recommend');
  }).finally(() => loading.value = false);
}

function startEdit(row) {
  cancelEdit();
  editId.value = row.id;
  editForm.featureName = row.featureName;
  editForm.sortNum = row.sortNum;
  editForm.remark = row.remark || '';
}

function cancelEdit() {
  editId.value = null;
}

function handleSave() {
  updateAppFeature(editId.value, {
    featureName: editForm.featureName,
    sortNum: editForm.sortNum,
    remark: editForm.remark
  }).then(() => {
    proxy.$modal.msgSuccess("保存成功");
    cancelEdit();
    getList();
  });
}

function handleToggleVisible(row) {
  updateAppFeature(row.id, { visible: row.visible }).then(() => {
    proxy.$modal.msgSuccess(row.visible === 1 ? "已开启展示" : "已隐藏");
  }).catch(() => getList());
}

getList();
</script>
