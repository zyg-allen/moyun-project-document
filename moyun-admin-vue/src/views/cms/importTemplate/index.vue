<template>
  <div class="app-container">
    <!-- 业务选择 + 操作 -->
    <el-form :inline="true" class="search-form">
      <el-form-item label="业务">
        <el-select
          v-model="businessKey"
          placeholder="请选择业务"
          style="width: 260px"
          @change="loadConfig"
        >
          <el-option
            v-for="b in BUSINESS_KEYS"
            :key="b.value"
            :label="`${b.label}（${b.value}）`"
            :value="b.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button
          type="primary"
          :icon="Plus"
          :disabled="!businessKey"
          @click="handleAdd"
        >新增字段</el-button>
        <el-button
          type="success"
          :icon="Check"
          :disabled="!businessKey"
          :loading="saving"
          @click="handleSave"
        >保存配置</el-button>
        <el-button
          :icon="Refresh"
          :disabled="!businessKey"
          @click="loadConfig"
        >刷新</el-button>
      </el-form-item>
    </el-form>

    <el-alert
      title="说明：列顺序按「排序号」升序生成；必填字段表头自动追加 * 标记；下拉值优先于字典类型。保存后将先删除原配置，再批量插入。"
      type="info"
      :closable="false"
      show-icon
      class="mb12"
    />

    <el-table
      v-loading="loading"
      :data="configList"
      border
      size="small"
      max-height="600"
      row-key="rowKey"
      :empty-text="businessKey ? '当前业务尚未配置字段' : '请先选择业务'"
    >
      <el-table-column label="排序" width="80" align="center">
        <template #default="{ row }">
          <el-input-number v-model="row.sort" :min="0" :max="9999" controls-position="right" size="small" style="width: 70px" />
        </template>
      </el-table-column>
      <el-table-column label="字段名 (Java)" width="170">
        <template #default="{ row }">
          <el-input v-model="row.fieldName" placeholder="如 title" size="small" />
        </template>
      </el-table-column>
      <el-table-column label="Excel 列名" width="160">
        <template #default="{ row }">
          <el-input v-model="row.columnName" placeholder="如 题目标题" size="small" />
        </template>
      </el-table-column>
      <el-table-column label="必填" width="70" align="center">
        <template #default="{ row }">
          <el-switch v-model="row.required" :active-value="1" :inactive-value="0" size="small" />
        </template>
      </el-table-column>
      <el-table-column label="类型" width="120">
        <template #default="{ row }">
          <el-select v-model="row.fieldType" size="small" style="width: 100%">
            <el-option label="字符串" value="string" />
            <el-option label="数字" value="number" />
            <el-option label="日期" value="date" />
            <el-option label="字典/枚举" value="dict" />
          </el-select>
        </template>
      </el-table-column>
      <el-table-column label="下拉值 (逗号分隔)" width="200">
        <template #default="{ row }">
          <el-input
            v-model="row.comboValues"
            placeholder="easy,medium,hard"
            size="small"
            :disabled="row.fieldType !== 'dict'"
          />
        </template>
      </el-table-column>
      <el-table-column label="字段说明" min-width="200">
        <template #default="{ row }">
          <el-input v-model="row.description" placeholder="模板第2行说明" size="small" />
        </template>
      </el-table-column>
      <el-table-column label="示例值" min-width="160">
        <template #default="{ row }">
          <el-input v-model="row.exampleValue" placeholder="模板第3行示例" size="small" />
        </template>
      </el-table-column>
      <el-table-column label="列宽" width="80" align="center">
        <template #default="{ row }">
          <el-input-number v-model="row.columnWidth" :min="8" :max="60" size="small" controls-position="right" style="width: 70px" />
        </template>
      </el-table-column>
      <el-table-column label="启用" width="70" align="center">
        <template #default="{ row }">
          <el-switch v-model="row.status" active-value="0" inactive-value="1" size="small" />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="80" align="center" fixed="right">
        <template #default="{ $index }">
          <el-button type="danger" link size="small" @click="handleRemove($index)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="footer-tip">
      共 {{ configList.length }} 个字段，启用 {{ enabledCount }} 个
    </div>
  </div>
</template>

<script setup name="ImportTemplateConfig">
import { ref, computed, onMounted, getCurrentInstance } from "vue";
import { Plus, Check, Refresh } from "@element-plus/icons-vue";
import {
  listImportTemplateConfig,
  saveImportTemplateConfig,
  BUSINESS_KEYS
} from "@/api/cms/importTemplate";

const { proxy } = getCurrentInstance();

const businessKey = ref("");
const configList = ref([]);
const loading = ref(false);
const saving = ref(false);

const enabledCount = computed(
  () => configList.value.filter((c) => c.status === "0").length
);

async function loadConfig() {
  if (!businessKey.value) {
    configList.value = [];
    return;
  }
  loading.value = true;
  try {
    const resp = await listImportTemplateConfig(businessKey.value);
    const list = resp?.data || [];
    configList.value = list.map((item, idx) => ({
      ...normalize(item),
      rowKey: `r_${idx}_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`
    }));
  } catch (e) {
    proxy.$modal.msgError("加载配置失败：" + (e.message || e));
  } finally {
    loading.value = false;
  }
}

function normalize(item) {
  return {
    id: item.id || null,
    businessKey: item.businessKey || businessKey.value,
    fieldName: item.fieldName || "",
    columnName: item.columnName || "",
    description: item.description || "",
    exampleValue: item.exampleValue || "",
    required: item.required ?? 0,
    fieldType: item.fieldType || "string",
    dictType: item.dictType || "",
    comboValues: item.comboValues || "",
    columnWidth: item.columnWidth ?? 20,
    sort: item.sort ?? configList.value.length + 1,
    status: item.status || "0",
    remark: item.remark || ""
  };
}

function handleAdd() {
  if (!businessKey.value) {
    proxy.$modal.msgWarning("请先选择业务");
    return;
  }
  configList.value.push({
    id: null,
    businessKey: businessKey.value,
    fieldName: "",
    columnName: "",
    description: "",
    exampleValue: "",
    required: 0,
    fieldType: "string",
    dictType: "",
    comboValues: "",
    columnWidth: 20,
    sort: configList.value.length + 1,
    status: "0",
    remark: "",
    rowKey: `r_new_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`
  });
}

function handleRemove(index) {
  configList.value.splice(index, 1);
}

async function handleSave() {
  if (!businessKey.value) {
    proxy.$modal.msgWarning("请先选择业务");
    return;
  }
  // 校验
  for (let i = 0; i < configList.value.length; i++) {
    const c = configList.value[i];
    if (!c.fieldName || !c.fieldName.trim()) {
      proxy.$modal.msgError(`第 ${i + 1} 行：字段名不能为空`);
      return;
    }
    if (!c.columnName || !c.columnName.trim()) {
      proxy.$modal.msgError(`第 ${i + 1} 行：Excel 列名不能为空`);
      return;
    }
  }
  const fieldNameSet = new Set();
  for (let i = 0; i < configList.value.length; i++) {
    const fn = configList.value[i].fieldName.trim();
    if (fieldNameSet.has(fn)) {
      proxy.$modal.msgError(`字段名「${fn}」重复，请检查`);
      return;
    }
    fieldNameSet.add(fn);
  }

  saving.value = true;
  try {
    const payload = {
      businessKey: businessKey.value,
      configs: configList.value.map((c, idx) => ({
        id: c.id,
        businessKey: businessKey.value,
        fieldName: c.fieldName.trim(),
        columnName: c.columnName.trim(),
        description: c.description || null,
        exampleValue: c.exampleValue || null,
        required: c.required ?? 0,
        fieldType: c.fieldType || "string",
        dictType: c.dictType || null,
        comboValues: c.comboValues || null,
        columnWidth: c.columnWidth ?? 20,
        sort: c.sort ?? idx + 1,
        status: c.status || "0",
        remark: c.remark || null
      }))
    };
    const resp = await saveImportTemplateConfig(payload);
    if (resp?.code === 200) {
      proxy.$modal.msgSuccess("保存成功");
      await loadConfig();
    } else {
      proxy.$modal.msgError(resp?.msg || "保存失败");
    }
  } catch (e) {
    proxy.$modal.msgError("保存失败：" + (e.message || e));
  } finally {
    saving.value = false;
  }
}

onMounted(() => {
  // 默认加载题库业务配置
  businessKey.value = "interview_question";
  loadConfig();
});
</script>

<style scoped>
.app-container { padding: 20px; }
.search-form { margin-bottom: 12px; }
.mb12 { margin-bottom: 12px; }
.footer-tip {
  margin-top: 12px;
  font-size: 12px;
  color: #909399;
}
</style>
