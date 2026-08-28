<template>
  <el-dialog
    v-model="visible"
    :title="title"
    width="980px"
    :close-on-click-modal="false"
    :destroy-on-close="true"
    append-to-body
    @closed="handleClosed"
  >
    <!-- 顶部提示区 -->
    <el-alert
      v-if="tip"
      :title="tip"
      type="info"
      :closable="false"
      show-icon
      class="mb12"
    />

    <!-- 模板下载条 -->
    <div class="tpl-bar">
      <el-icon><Document /></el-icon>
      <span class="tpl-text">
        请按模板格式整理数据，避免列名错乱、必填项缺失。
      </span>
      <el-button
        type="primary"
        link
        :icon="Download"
        :loading="downloadingTpl"
        @click="handleDownloadTemplate"
      >
        下载导入模板
      </el-button>
    </div>

    <!-- 上传区 -->
    <el-upload
      v-show="phase === 'upload'"
      ref="uploadRef"
      :limit="1"
      :accept="accept"
      :auto-upload="false"
      :on-change="handleFileChange"
      :on-exceed="handleExceed"
      :on-remove="handleRemoveFile"
      :file-list="fileList"
      :disabled="uploading"
      drag
    >
      <el-icon class="el-icon--upload"><upload-filled /></el-icon>
      <div class="el-upload__text">
        将 Excel 文件拖到此处，或<em>点击上传</em>
      </div>
      <template #tip>
        <div class="el-upload__tip">
          支持 {{ accept }} 格式，单文件不超过 {{ maxSizeMb }}MB
          <span v-if="selectedFile" class="ml10 selected-file">
            已选：{{ selectedFile.name }}
          </span>
        </div>
      </template>
    </el-upload>

    <!-- 上传进度 -->
    <el-progress
      v-if="uploading && uploadProgress > 0"
      :percentage="uploadProgress"
      :stroke-width="8"
      status="success"
      class="mt12"
    />

    <!-- 导入结果区 -->
    <div v-if="phase === 'result' && result" class="result-panel">
      <!-- 顶部统计卡片 -->
      <div class="stat-row">
        <div class="stat-card stat-total">
          <div class="stat-num">{{ result.totalRows }}</div>
          <div class="stat-label">总行数</div>
        </div>
        <div class="stat-card stat-success">
          <div class="stat-num">{{ result.successCount }}</div>
          <div class="stat-label">成功</div>
        </div>
        <div class="stat-card stat-fail" :class="{ 'has-fail': result.failCount > 0 }">
          <div class="stat-num">{{ result.failCount }}</div>
          <div class="stat-label">失败</div>
        </div>
      </div>

      <el-alert
        :title="result.msg || `共 ${result.totalRows} 条，成功 ${result.successCount} 条，失败 ${result.failCount} 条`"
        :type="result.failCount === 0 ? 'success' : 'warning'"
        :closable="false"
        show-icon
        class="mt12"
      />

      <!-- 失败明细表 -->
      <div v-if="result.failRows && result.failRows.length" class="fail-section">
        <div class="fail-toolbar">
          <span class="fail-title">
            <el-icon><WarningFilled /></el-icon>
            失败明细（共 {{ result.failRows.length }} 条）
          </span>
          <el-button
            type="danger"
            plain
            size="small"
            :icon="Download"
            :loading="downloadingFail"
            @click="handleDownloadFailRows"
          >
            下载失败行 Excel
          </el-button>
        </div>
        <el-table
          :data="pagedFailRows"
          max-height="320"
          border
          size="small"
          class="mt8"
        >
          <el-table-column label="行号" prop="rowNo" width="70" align="center" />
          <el-table-column label="失败原因" prop="reason" min-width="220" :show-overflow-tooltip="true" />
          <el-table-column label="原始数据" min-width="280">
            <template #default="scope">
              <el-popover
                trigger="hover"
                placement="left"
                :width="420"
              >
                <template #reference>
                  <span class="row-data-summary">{{ formatRowData(scope.row.rowData) }}</span>
                </template>
                <div class="row-data-detail">
                  <div
                    v-for="(val, key) in scope.row.rowData"
                    :key="key"
                    class="row-data-item"
                  >
                    <span class="row-data-key">{{ key }}:</span>
                    <span class="row-data-val">{{ val }}</span>
                  </div>
                </div>
              </el-popover>
            </template>
          </el-table-column>
        </el-table>
        <el-pagination
          v-if="result.failRows.length > pageSize"
          v-model:current-page="failPage"
          :page-size="pageSize"
          :total="result.failRows.length"
          layout="prev, pager, next, total"
          small
          class="mt8 right"
        />
        <div class="fail-tip">
          提示：请下载失败行 Excel，修正数据后重新导入（表头请勿修改）。
        </div>
      </div>

      <!-- 完全成功的提示 -->
      <div v-else class="success-tip">
        <el-icon class="success-icon"><CircleCheckFilled /></el-icon>
        <span>全部导入成功，可关闭对话框</span>
      </div>
    </div>

    <template #footer>
      <div class="dialog-footer">
        <template v-if="phase === 'upload'">
          <el-button @click="visible = false">取 消</el-button>
          <el-button
            type="primary"
            :loading="uploading"
            :disabled="!selectedFile"
            @click="handleUpload"
          >
            开始导入
          </el-button>
        </template>
        <template v-else>
          <el-button
            v-if="result && result.failCount > 0"
            type="primary"
            @click="handleReimport"
          >
            重新导入
          </el-button>
          <el-button @click="visible = false">关 闭</el-button>
        </template>
      </div>
    </template>
  </el-dialog>
</template>

<script setup name="ImportDialog">
import { ref, computed, watch, getCurrentInstance } from "vue";
import {
  UploadFilled,
  Document,
  Download,
  WarningFilled,
  CircleCheckFilled
} from "@element-plus/icons-vue";
import { saveAs } from "file-saver";

const props = defineProps({
  // v-model 控制可见性
  modelValue: { type: Boolean, default: false },
  // 弹窗标题
  title: { type: String, default: "Excel 导入" },
  // 业务标识（仅用于日志，可选）
  businessKey: { type: String, default: "" },
  // 上传 Excel 的 API：函数签名 (formData, onUploadProgress) => Promise<AjaxResult<ImportResult>>
  // AjaxResult 标准结构：{ code, msg, data: ImportResult }
  // ImportResult: { totalRows, successCount, failCount, failRows:[{rowNo,reason,rowData}], msg }
  importApi: { type: Function, required: true },
  // 下载模板的 API：函数签名 () => Promise<Blob>
  templateApi: { type: Function, required: true },
  // 下载失败行的 API：函数签名 (failRows) => Promise<Blob>
  failRowsApi: { type: Function, default: null },
  // 顶部提示语
  tip: { type: String, default: "" },
  // 模板文件名（不含扩展名）
  templateFileName: { type: String, default: "导入模板" },
  // 失败行文件名（不含扩展名）
  failRowsFileName: { type: String, default: "导入失败行" },
  // 接受的文件类型
  accept: { type: String, default: ".xlsx,.xls" },
  // 最大文件大小（MB）
  maxSizeMb: { type: Number, default: 50 }
});

const emit = defineEmits(["update:modelValue", "success"]);

const { proxy } = getCurrentInstance();

const visible = computed({
  get: () => props.modelValue,
  set: (v) => emit("update:modelValue", v)
});

// ----- 状态 -----
const phase = ref("upload"); // 'upload' | 'result'
const uploadRef = ref();
const fileList = ref([]);
const selectedFile = ref(null);
const uploading = ref(false);
const uploadProgress = ref(0);
const downloadingTpl = ref(false);
const downloadingFail = ref(false);
const result = ref(null);

// 失败行分页
const failPage = ref(1);
const pageSize = 20;
const pagedFailRows = computed(() => {
  if (!result.value?.failRows?.length) return [];
  const start = (failPage.value - 1) * pageSize;
  return result.value.failRows.slice(start, start + pageSize);
});

// ----- 弹窗打开时重置 -----
watch(
  () => props.modelValue,
  (v) => {
    if (v) resetAll();
  },
  { immediate: true }
);

function resetAll() {
  phase.value = "upload";
  fileList.value = [];
  selectedFile.value = null;
  uploading.value = false;
  uploadProgress.value = 0;
  downloadingTpl.value = false;
  downloadingFail.value = false;
  result.value = null;
  failPage.value = 1;
}

// ----- 文件选择 -----
function handleFileChange(file) {
  if (!file.raw) return;
  // 校验类型
  const ext = file.name.substring(file.name.lastIndexOf(".")).toLowerCase();
  const accepts = props.accept.split(",").map((s) => s.trim().toLowerCase());
  if (accepts.length && !accepts.includes(ext)) {
    proxy.$modal.msgError(`仅支持 ${props.accept} 格式`);
    uploadRef.value?.clearFiles();
    return;
  }
  // 校验大小
  const sizeMb = file.size / 1024 / 1024;
  if (sizeMb > props.maxSizeMb) {
    proxy.$modal.msgError(`文件大小不能超过 ${props.maxSizeMb}MB`);
    uploadRef.value?.clearFiles();
    return;
  }
  selectedFile.value = file.raw;
  fileList.value = [file];
}

function handleExceed(files) {
  uploadRef.value?.clearFiles();
  const f = files[0];
  uploadRef.value?.handleStart(f);
  selectedFile.value = f;
  fileList.value = [{ name: f.name, raw: f }];
}

function handleRemoveFile() {
  selectedFile.value = null;
  fileList.value = [];
}

// ----- 模板下载 -----
async function handleDownloadTemplate() {
  if (downloadingTpl.value) return;
  downloadingTpl.value = true;
  try {
    const blob = await props.templateApi();
    if (blob instanceof Blob) {
      saveAs(blob, `${props.templateFileName}_${formatNow()}.xlsx`);
    } else {
      // 拦截器对错误响应回退为 JSON，不是 Blob
      proxy.$modal.msgError("模板下载失败，请稍后重试");
    }
  } catch (e) {
    proxy.$modal.msgError("模板下载失败：" + (e.message || e));
  } finally {
    downloadingTpl.value = false;
  }
}

// ----- 上传 -----
async function handleUpload() {
  if (!selectedFile.value) {
    proxy.$modal.msgError("请先选择文件");
    return;
  }
  uploading.value = true;
  uploadProgress.value = 0;
  try {
    const fd = new FormData();
    fd.append("file", selectedFile.value);
    const resp = await props.importApi(fd, (e) => {
      if (e.total) {
        uploadProgress.value = Math.min(99, Math.round((e.loaded / e.total) * 100));
      }
    });
    uploadProgress.value = 100;
    // AjaxResult 结构：{ code, msg, data: ImportResult }
    const payload = resp?.data ?? resp;
    if (payload && (payload.totalRows !== undefined || payload.successCount !== undefined)) {
      result.value = payload;
      phase.value = "result";
      failPage.value = 1;
      // 成功才触发外部刷新
      if (payload.successCount > 0) {
        emit("success", payload);
      }
    } else {
      // 后端返回的 AjaxResult.code 非 200，会被拦截器提示，这里兜底
      proxy.$modal.msgError(resp?.msg || "导入失败");
    }
  } catch (e) {
    proxy.$modal.msgError("导入失败：" + (e.message || e));
  } finally {
    uploading.value = false;
  }
}

// ----- 失败行下载 -----
async function handleDownloadFailRows() {
  if (!props.failRowsApi) {
    proxy.$modal.msgWarning("当前业务未提供失败行导出能力");
    return;
  }
  if (!result.value?.failRows?.length) {
    proxy.$modal.msgWarning("没有失败行可导出");
    return;
  }
  if (downloadingFail.value) return;
  downloadingFail.value = true;
  try {
    const blob = await props.failRowsApi(result.value.failRows);
    if (blob instanceof Blob) {
      saveAs(blob, `${props.failRowsFileName}_${formatNow()}.xlsx`);
    } else {
      proxy.$modal.msgError("失败行下载失败，请稍后重试");
    }
  } catch (e) {
    proxy.$modal.msgError("失败行下载失败：" + (e.message || e));
  } finally {
    downloadingFail.value = false;
  }
}

// ----- 重新导入 -----
function handleReimport() {
  phase.value = "upload";
  selectedFile.value = null;
  fileList.value = [];
  uploadProgress.value = 0;
  result.value = null;
  failPage.value = 1;
}

// ----- 工具方法 -----
function formatRowData(rowData) {
  if (!rowData) return "";
  const pairs = Object.entries(rowData).map(([k, v]) => `${k}=${v ?? ""}`);
  const text = pairs.join(" | ");
  return text.length > 60 ? text.slice(0, 60) + "..." : text;
}

function formatNow() {
  const d = new Date();
  const pad = (n) => String(n).padStart(2, "0");
  return `${d.getFullYear()}${pad(d.getMonth() + 1)}${pad(d.getDate())}${pad(d.getHours())}${pad(d.getMinutes())}${pad(d.getSeconds())}`;
}

function handleClosed() {
  resetAll();
}
</script>

<style scoped>
.mb12 { margin-bottom: 12px; }
.mt8 { margin-top: 8px; }
.mt12 { margin-top: 12px; }
.ml10 { margin-left: 10px; }
.right { text-align: right; }

.tpl-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  background: #f5f7fa;
  border: 1px dashed #dcdfe6;
  border-radius: 4px;
  margin-bottom: 16px;
}
.tpl-text {
  flex: 1;
  font-size: 13px;
  color: #606266;
}
.selected-file {
  color: #67c23a;
  font-weight: 500;
}

/* 结果统计卡片 */
.stat-row {
  display: flex;
  gap: 12px;
}
.stat-card {
  flex: 1;
  text-align: center;
  padding: 14px 8px;
  border-radius: 6px;
  border: 1px solid #ebeef5;
  background: #fafafa;
}
.stat-num {
  font-size: 26px;
  font-weight: 600;
  line-height: 1.2;
}
.stat-label {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}
.stat-total .stat-num { color: #303133; }
.stat-success .stat-num { color: #67c23a; }
.stat-fail .stat-num { color: #909399; }
.stat-fail.has-fail .stat-num { color: #f56c6c; }
.stat-fail.has-fail {
  background: #fef0f0;
  border-color: #fbc4c4;
}

/* 失败明细 */
.fail-section { margin-top: 16px; }
.fail-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.fail-title {
  font-size: 14px;
  color: #f56c6c;
  font-weight: 500;
  display: inline-flex;
  align-items: center;
  gap: 6px;
}
.fail-tip {
  margin-top: 8px;
  font-size: 12px;
  color: #909399;
}

.row-data-summary {
  font-size: 12px;
  color: #606266;
  cursor: pointer;
  display: inline-block;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.row-data-detail {
  max-height: 320px;
  overflow-y: auto;
}
.row-data-item {
  padding: 4px 0;
  border-bottom: 1px dashed #ebeef5;
  font-size: 12px;
}
.row-data-item:last-child { border-bottom: none; }
.row-data-key {
  color: #909399;
  margin-right: 6px;
}
.row-data-val {
  color: #303133;
  word-break: break-all;
}

/* 完全成功 */
.success-tip {
  margin-top: 16px;
  text-align: center;
  padding: 20px;
  color: #67c23a;
}
.success-icon {
  font-size: 36px;
  vertical-align: middle;
  margin-right: 8px;
}
</style>
