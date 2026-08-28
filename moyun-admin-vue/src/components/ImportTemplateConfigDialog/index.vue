<template>
  <el-dialog
    v-model="visible"
    :title="title"
    width="1100px"
    :close-on-click-modal="false"
    :destroy-on-close="true"
    append-to-body
    @closed="handleClosed"
  >
    <!-- 顶部业务选择 -->
    <div class="biz-bar">
      <el-form :inline="true" @submit.prevent>
        <el-form-item label="业务">
          <el-select
            v-model="businessKey"
            placeholder="请选择业务"
            style="width: 220px"
            @change="loadConfig"
          >
            <el-option
              v-for="b in businessOptions"
              :key="b.value"
              :label="b.label"
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
            :icon="Download"
            :disabled="!businessKey"
            :loading="saving"
            @click="handleSave"
          >保存配置</el-button>
          <el-tooltip content="保存后将先删除原有配置，再批量插入新配置" placement="top">
            <el-icon class="ml8 hint-icon"><InfoFilled /></el-icon>
          </el-tooltip>
        </el-form-item>
      </el-form>
      <div class="biz-desc" v-if="currentBusinessRemark">{{ currentBusinessRemark }}</div>
    </div>

    <el-alert
      title="说明：列顺序按「排序号」升序生成；必填字段表头会自动追加 * 标记；下拉值优先于字典类型"
      type="info"
      :closable="false"
      show-icon
      class="mb12"
    />

    <!-- 字段配置表 -->
    <el-table
      v-loading="loading"
      :data="configList"
      border
      size="small"
      max-height="520"
      row-key="rowKey"
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
      <el-table-column label="操作" width="80" align="right" fixed="right">
        <template #default="{ $index }">
          <el-button type="danger" link size="small" @click="handleRemove($index)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="footer-tip">
      <span>共 {{ configList.length }} 个字段，启用 {{ enabledCount }} 个</span>
    </div>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="visible = false">关 闭</el-button>
        <el-button type="primary" :loading="saving" :disabled="!businessKey" @click="handleSave">
          保 存
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup name="ImportTemplateConfigDialog">
import { ref, reactive, computed, watch, getCurrentInstance } from "vue";
import { Plus, Download, InfoFilled } from "@element-plus/icons-vue";
import {
  listImportTemplateConfig,
  saveImportTemplateConfig,
  BUSINESS_KEYS
} from "@/api/cms/importTemplate";

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  // 默认业务标识
  defaultBusinessKey: { type: String, default: "" },
  // 标题
  title: { type: String, default: "导入模板字段配置" },
  // 可选业务（不传则使用内置 BUSINESS_KEYS）
  businessOptions: { type: Array, default: () => BUSINESS_KEYS }
});

const emit = defineEmits(["update:modelValue", "saved"]);

const { proxy } = getCurrentInstance();

const visible = computed({
  get: () => props.modelValue,
  set: (v) => emit("update:modelValue", v)
});

// ----- 状态 -----
const businessKey = ref(props.defaultBusinessKey || "");
const configList = ref([]);
const loading = ref(false);
const saving = ref(false);
const rowKeySeq = ref(0);

const currentBusinessRemark = computed(() => {
  const b = props.businessOptions.find((x) => x.value === businessKey.value);
  return b?.remark || "";
});

const enabledCount = computed(
  () => configList.value.filter((c) => c.status === "0").length
);

// ----- 弹窗打开时初始化 -----
watch(
  () => props.modelValue,
  (v) => {
    if (v) {
      businessKey.value = props.defaultBusinessKey || (props.businessOptions[0]?.value ?? "");
      if (businessKey.value) {
        loadConfig();
      }
    }
  },
  { immediate: true }
);

// ----- 加载配置 -----
async function loadConfig() {
  if (!businessKey.value) {
    configList.value = [];
    return;
  }
  loading.value = true;
  try {
    const resp = await listImportTemplateConfig(businessKey.value);
    const list = resp?.data || [];
    // 补 rowKey
    configList.value = list.map((item, idx) => ({
      ...normalize(item),
      rowKey: `r_${idx}_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`
    }));
    if (configList.value.length === 0) {
      proxy.$modal.msgInfo("当前业务尚未配置字段，可点「新增字段」开始维护");
    }
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

// ----- 新增一行 -----
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

// ----- 删除一行 -----
function handleRemove(index) {
  configList.value.splice(index, 1);
}

// ----- 保存 -----
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
  // 去重校验
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
      emit("saved", { businessKey: businessKey.value, configs: resp.data || payload.configs });
      // 保存后刷新本地配置（拿到新 id）
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

function handleClosed() {
  configList.value = [];
  businessKey.value = "";
}
</script>

<style scoped>
.mb12 { margin-bottom: 12px; }
.ml8 { margin-left: 8px; }
.hint-icon {
  color: #909399;
  cursor: help;
  vertical-align: middle;
}
.biz-bar {
  margin-bottom: 12px;
}
.biz-desc {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}
.footer-tip {
  margin-top: 8px;
  font-size: 12px;
  color: #909399;
}
</style>
