<template>
  <el-dialog
    v-model="visible"
    title="文档导入向导"
    width="900px"
    :close-on-click-modal="false"
    :destroy-on-close="true"
    append-to-body
    @closed="handleClosed"
  >
    <el-steps :active="active" finish-status="success" align-center class="wizard-steps">
      <el-step title="上传文档" />
      <el-step title="分章规则" />
      <el-step title="解析预览" />
      <el-step title="确认入库" />
    </el-steps>

    <div class="wizard-body">
      <!-- Step 1: 上传文档 -->
      <div v-show="active === 0">
        <el-form ref="step1Ref" :model="step1Form" :rules="step1Rules" label-width="100px">
          <el-form-item label="所属书籍" prop="bookId">
            <el-select
              v-model="step1Form.bookId"
              placeholder="请选择书籍"
              filterable
              style="width: 100%"
              :disabled="bookIdLocked"
            >
              <el-option
                v-for="b in bookOptions"
                :key="b.id"
                :label="`${b.title}（${b.author || '未知作者'}）`"
                :value="b.id"
              />
            </el-select>
            <div v-if="bookOptions.length === 0" class="empty-tip">
              暂无可选书籍，请先到「书籍管理」创建书籍
            </div>
          </el-form-item>

          <el-form-item label="文档来源">
            <el-radio-group v-model="step1Form.source">
              <el-radio-button label="file">上传文件</el-radio-button>
              <el-radio-button label="text">粘贴文本</el-radio-button>
            </el-radio-group>
          </el-form-item>

          <el-form-item v-if="step1Form.source === 'file'" label="选择文件" prop="file">
            <el-upload
              ref="uploadRef"
              :auto-upload="false"
              :limit="1"
              :on-change="handleFileChange"
              :on-exceed="handleExceed"
              :on-remove="handleRemoveFile"
              :file-list="fileList"
              drag
              accept=".txt,.md,.markdown,.docx,.pdf"
            >
              <el-icon class="el-icon--upload"><upload-filled /></el-icon>
              <div class="el-upload__text">
                将文件拖到此处，或<em>点击上传</em>
              </div>
              <template #tip>
                <div class="el-upload__tip">
                  支持 txt / md / docx / pdf，单个文件不超过 50MB
                </div>
              </template>
            </el-upload>
          </el-form-item>

          <el-form-item v-else label="粘贴文本" prop="text">
            <el-input
              v-model="step1Form.text"
              type="textarea"
              :rows="10"
              placeholder="将小说/文章正文粘贴到这里"
            />
            <div class="el-upload__tip">支持任意带章节标记的文本</div>
          </el-form-item>
        </el-form>
      </div>

      <!-- Step 2: 分章规则 -->
      <div v-show="active === 1">
        <el-form :model="ruleForm" label-width="120px">
          <el-form-item label="分章模式">
            <el-radio-group v-model="ruleForm.mode">
              <el-radio label="auto">自动识别（推荐）</el-radio>
              <el-radio label="regex">自定义正则</el-radio>
              <el-radio label="heading">按标题级别</el-radio>
              <el-radio label="fixed">固定字数</el-radio>
            </el-radio-group>
          </el-form-item>

          <el-form-item v-if="ruleForm.mode === 'regex'" label="章节正则">
            <el-input
              v-model="ruleForm.regex"
              placeholder="例如：^第[零一二三四五六七八九十百千万0-9]+[章节回]"
              style="width: 100%"
            />
            <div class="el-upload__tip">匹配到的行将作为新章节标题</div>
          </el-form-item>

          <el-form-item v-if="ruleForm.mode === 'heading'" label="标题级别">
            <el-select v-model="ruleForm.headingLevel" style="width: 200px">
              <el-option label="H1（#）" value="1" />
              <el-option label="H1-H2" value="2" />
              <el-option label="H1-H3" value="3" />
              <el-option label="H1-H6" value="6" />
            </el-select>
            <span class="ml10">按 Markdown 标题切分</span>
          </el-form-item>

          <el-form-item v-if="ruleForm.mode === 'fixed'" label="每章字数">
            <el-input-number v-model="ruleForm.fixedWordCount" :min="500" :max="20000" :step="500" />
            <span class="ml10">按固定字数切段</span>
          </el-form-item>

          <el-form-item label="识别分卷">
            <el-switch v-model="ruleForm.detectVolume" />
            <span class="ml10">识别"第X卷"等分卷标记并合并到下一章</span>
          </el-form-item>

          <el-form-item label="短章合并阈值">
            <el-input-number v-model="ruleForm.minChapterWords" :min="0" :max="5000" :step="50" />
            <span class="ml10">低于此字数的章节合并到上一章（0=不合并）</span>
          </el-form-item>

          <el-alert
            title="提示：可在下一步预览章节切分结果后再入库"
            type="info"
            :closable="false"
            show-icon
          />
        </el-form>
      </div>

      <!-- Step 3: 解析预览 -->
      <div v-show="active === 2">
        <div v-if="parseLoading" class="parse-loading">
          <el-icon class="is-loading"><loading /></el-icon>
          <span>正在解析文档...</span>
        </div>

        <div v-else-if="parseResult">
          <el-descriptions :column="3" border size="small" class="mb12">
            <el-descriptions-item label="文档标题">
              {{ parseResult.title || '（未识别）' }}
            </el-descriptions-item>
            <el-descriptions-item label="作者">
              {{ parseResult.author || '（未识别）' }}
            </el-descriptions-item>
            <el-descriptions-item label="总字数">
              {{ parseResult.totalWordCount || 0 }}
            </el-descriptions-item>
            <el-descriptions-item label="识别章节数">
              <el-tag type="success">{{ parseResult.chapters?.length || 0 }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="来源格式">
              {{ parseResult.sourceFormat || '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="分章模式">
              {{ ruleForm.mode }}
            </el-descriptions-item>
          </el-descriptions>

          <div class="preview-toolbar">
            <el-button type="text" @click="handleReparse">重新解析</el-button>
            <span class="ml10 muted-tip">
              提示：可直接编辑下方章节标题，或勾选删除不需要的章节
            </span>
          </div>

          <el-table
            :data="parseResult.chapters"
            max-height="380"
            border
            size="small"
            @selection-change="handlePreviewSelectionChange"
          >
            <el-table-column type="selection" width="40" />
            <el-table-column type="index" label="#" width="55" />
            <el-table-column label="章节标题" min-width="220">
              <template #default="scope">
                <el-input v-model="scope.row.title" size="small" />
              </template>
            </el-table-column>
            <el-table-column label="字数" prop="wordCount" width="80" align="center" />
            <el-table-column label="正文预览" min-width="240" :show-overflow-tooltip="true">
              <template #default="scope">
                <span class="content-preview">{{ previewContent(scope.row.content) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="80" align="center">
              <template #default="scope">
                <el-button type="text" size="small" @click="removePreviewChapter(scope.$index)">
                  移除
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <el-empty v-else description="尚未解析" />
      </div>

      <!-- Step 4: 确认入库 -->
      <div v-show="active === 3">
        <el-alert
          title="入库将以下配置写入数据库"
          type="info"
          :closable="false"
          show-icon
          class="mb12"
        />
        <el-form :model="confirmForm" label-width="120px">
          <el-form-item label="目标书籍">
            <el-tag>{{ targetBookLabel }}</el-tag>
          </el-form-item>
          <el-form-item label="待入库章节">
            <el-tag type="success">
              {{ importChapters.length }} 章 / 共 {{ importWordCount }} 字
            </el-tag>
          </el-form-item>
          <el-form-item label="追加方式">
            <el-radio-group v-model="confirmForm.appendMode">
              <el-radio label="append">追加到末尾（自动续号）</el-radio>
            </el-radio-group>
            <div class="el-upload__tip">章节序号将从现有最大值 +1 开始自动分配</div>
          </el-form-item>
          <el-form-item label="发布状态">
            <el-radio-group v-model="confirmForm.autoPublish">
              <el-radio :label="false">存为草稿（推荐，可在列表中再编辑）</el-radio>
              <el-radio :label="true">立即发布</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="编辑器模式">
            <el-radio-group v-model="confirmForm.editorMode">
              <el-radio label="richtext">富文本</el-radio>
              <el-radio label="markdown">Markdown</el-radio>
            </el-radio-group>
            <div class="el-upload__tip">
              原文是否含 Markdown 标记自动判断；富文本会保留段落结构
            </div>
          </el-form-item>
        </el-form>
      </div>
    </div>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="handleBack" :disabled="active === 0 || parseLoading || importing">
          上一步
        </el-button>
        <el-button v-if="active < 3" type="primary" @click="handleNext" :loading="parseLoading">
          下一步
        </el-button>
        <el-button v-if="active === 3" type="success" @click="handleImport" :loading="importing">
          确认导入
        </el-button>
        <el-button @click="visible = false">取消</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup name="ImportWizard">
import { ref, reactive, computed, watch, getCurrentInstance } from "vue";
import { UploadFilled, Loading } from "@element-plus/icons-vue";
import {
  parseDocument,
  parseDocumentText,
  batchImportBookChapter
} from "@/api/portal/bookChapter";

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  // 锁定书籍（从外部传入时不允许更改）
  bookId: { type: [Number, String], default: null },
  bookOptions: { type: Array, default: () => [] }
});

const emit = defineEmits(["update:modelValue", "success"]);

const { proxy } = getCurrentInstance();

const visible = computed({
  get: () => props.modelValue,
  set: (v) => emit("update:modelValue", v)
});

const bookIdLocked = computed(() => !!props.bookId);

// ----- 步骤 -----
const active = ref(0);

// ----- Step 1 -----
const step1Ref = ref();
const uploadRef = ref();
const fileList = ref([]);
const step1Form = reactive({
  bookId: null,
  source: "file",
  file: null,
  text: ""
});
const step1Rules = {
  bookId: [{ required: true, message: "请选择书籍", trigger: "change" }]
};

// ----- Step 2 -----
const ruleForm = reactive({
  mode: "auto",
  regex: "^第[零一二三四五六七八九十百千万0-9]+[章节回部篇]",
  headingLevel: "2",
  fixedWordCount: 3000,
  detectVolume: true,
  minChapterWords: 100
});

// ----- Step 3 -----
const parseLoading = ref(false);
const parseResult = ref(null);
const previewSelection = ref([]);

// ----- Step 4 -----
const confirmForm = reactive({
  appendMode: "append",
  autoPublish: false,
  editorMode: "richtext"
});
const importing = ref(false);

// ----- 初始化 bookId -----
watch(
  () => [props.modelValue, props.bookId],
  ([v, bid]) => {
    if (v) {
      resetWizard();
      if (bid) step1Form.bookId = Number(bid);
    }
  },
  { immediate: true }
);

function resetWizard() {
  active.value = 0;
  step1Form.bookId = props.bookId ? Number(props.bookId) : null;
  step1Form.source = "file";
  step1Form.file = null;
  step1Form.text = "";
  fileList.value = [];
  ruleForm.mode = "auto";
  parseResult.value = null;
  previewSelection.value = [];
  confirmForm.appendMode = "append";
  confirmForm.autoPublish = false;
  confirmForm.editorMode = "richtext";
  importing.value = false;
  parseLoading.value = false;
}

// ----- 文件操作 -----
function handleFileChange(file) {
  if (file.raw) {
    step1Form.file = file.raw;
    fileList.value = [file];
  }
}
function handleExceed(files) {
  uploadRef.value?.clearFiles();
  const f = files[0];
  uploadRef.value?.handleStart(f);
  step1Form.file = f;
  fileList.value = [{ name: f.name, raw: f }];
}
function handleRemoveFile() {
  step1Form.file = null;
  fileList.value = [];
}

// ----- 步骤切换 -----
async function handleNext() {
  if (active.value === 0) {
    if (!step1Form.bookId) {
      proxy.$modal.msgError("请选择书籍");
      return;
    }
    if (step1Form.source === "file" && !step1Form.file) {
      proxy.$modal.msgError("请选择文件");
      return;
    }
    if (step1Form.source === "text" && !step1Form.text?.trim()) {
      proxy.$modal.msgError("请粘贴文本");
      return;
    }
    active.value = 1;
    return;
  }
  if (active.value === 1) {
    await doParse();
    return;
  }
  if (active.value === 2) {
    if (!parseResult.value?.chapters?.length) {
      proxy.$modal.msgError("没有可入库的章节");
      return;
    }
    active.value = 3;
    return;
  }
}

function handleBack() {
  if (active.value > 0) active.value -= 1;
}

// ----- 解析 -----
async function doParse() {
  parseLoading.value = true;
  parseResult.value = null;
  try {
    let resp;
    if (step1Form.source === "file") {
      const fd = new FormData();
      fd.append("file", step1Form.file);
      Object.keys(ruleForm).forEach((k) => {
        const v = ruleForm[k];
        if (v !== null && v !== undefined && v !== "") {
          fd.append(k, typeof v === "boolean" ? String(v) : v);
        }
      });
      resp = await parseDocument(fd);
    } else {
      resp = await parseDocumentText({
        text: step1Form.text,
        ...ruleForm
      });
    }
    const data = resp.data || resp;
    if (data && data.success !== false && data.chapters) {
      parseResult.value = data;
      active.value = 2;
    } else {
      proxy.$modal.msgError(data?.errorMsg || "解析失败");
    }
  } catch (e) {
    proxy.$modal.msgError("解析失败: " + (e.message || e));
  } finally {
    parseLoading.value = false;
  }
}

function handleReparse() {
  active.value = 1;
}

// ----- 预览编辑 -----
function handlePreviewSelectionChange(sel) {
  previewSelection.value = sel;
}
function removePreviewChapter(index) {
  parseResult.value.chapters.splice(index, 1);
}
function previewContent(content) {
  if (!content) return "";
  const text = String(content).replace(/<[^>]+>/g, "").replace(/\s+/g, " ").trim();
  return text.length > 80 ? text.slice(0, 80) + "..." : text;
}

// ----- 入库 -----
const importChapters = computed(() => parseResult.value?.chapters || []);
const importWordCount = computed(() =>
  importChapters.value.reduce((s, c) => s + (c.wordCount || 0), 0)
);
const targetBookLabel = computed(() => {
  const b = props.bookOptions.find((x) => x.id === step1Form.bookId);
  return b ? `${b.title}（${b.author || "未知作者"}）` : `#${step1Form.bookId}`;
});

async function handleImport() {
  if (!step1Form.bookId) {
    proxy.$modal.msgError("请选择书籍");
    return;
  }
  if (!importChapters.value.length) {
    proxy.$modal.msgError("没有可入库的章节");
    return;
  }
  importing.value = true;
  try {
    const chapters = importChapters.value.map((c, idx) => ({
      title: c.title || `第${idx + 1}章`,
      content: c.content || "",
      contentMarkdown: c.contentMarkdown || null,
      editorMode: confirmForm.editorMode,
      wordCount: c.wordCount || 0,
      isFree: true,
      isPublished: false
    }));
    const resp = await batchImportBookChapter({
      bookId: step1Form.bookId,
      chapters,
      autoPublish: confirmForm.autoPublish
    });
    if (resp.code === 200) {
      proxy.$modal.msgSuccess(resp.msg || "导入成功");
      emit("success", { bookId: step1Form.bookId, count: chapters.length });
      visible.value = false;
    } else {
      proxy.$modal.msgError(resp.msg || "导入失败");
    }
  } catch (e) {
    proxy.$modal.msgError("导入失败: " + (e.message || e));
  } finally {
    importing.value = false;
  }
}

function handleClosed() {
  resetWizard();
}
</script>

<style scoped>
.wizard-steps {
  margin-bottom: 24px;
}
.wizard-body {
  min-height: 320px;
}
.empty-tip {
  color: #f56c6c;
  font-size: 12px;
  margin-top: 4px;
}
.ml10 {
  margin-left: 10px;
}
.muted-tip {
  color: #909399;
  font-size: 12px;
}
.mb12 {
  margin-bottom: 12px;
}
.preview-toolbar {
  display: flex;
  align-items: center;
  margin-bottom: 8px;
}
.content-preview {
  color: #606266;
  font-size: 12px;
}
.parse-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 200px;
  color: #409eff;
  gap: 8px;
  font-size: 14px;
}
</style>
