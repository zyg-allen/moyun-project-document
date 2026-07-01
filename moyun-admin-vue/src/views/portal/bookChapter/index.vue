<template>
  <div class="app-container">
    <!-- 书籍选择器（章节必须按书籍组织） -->
    <el-card class="book-bar" shadow="never">
      <el-form :inline="true" :model="bookSelect" label-width="80px">
        <el-form-item label="书籍">
          <el-select
            v-model="bookSelect.bookId"
            placeholder="请选择书籍后查看章节"
            filterable
            clearable
            style="width: 360px"
            @change="handleBookChange"
          >
            <el-option
              v-for="b in bookOptions"
              :key="b.id"
              :label="`${b.title}（${b.author || '未知作者'}）`"
              :value="b.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item v-if="currentBook">
          <el-tag type="info">类型：{{ getBookTypeText(currentBook.type) }}</el-tag>
          <el-tag class="ml8" :type="getSerialStatusType(currentBook.serialStatus)">
            {{ getSerialStatusText(currentBook.serialStatus) }}
          </el-tag>
          <el-tag v-if="currentBook.chapterCount != null" class="ml8" type="success">
            已发布 {{ currentBook.chapterCount }} 章
          </el-tag>
          <el-tag v-if="currentBook.wordCount != null" class="ml8">
            {{ currentBook.wordCount }} 字
          </el-tag>
        </el-form-item>
      </el-form>
    </el-card>

    <div v-if="!bookSelect.bookId" class="empty-state">
      <el-empty description="请先选择书籍后查看章节列表" />
    </div>

    <template v-else>
      <!-- 搜索表单 -->
      <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
        <el-form-item label="标题" prop="title">
          <el-input
            v-model="queryParams.title"
            placeholder="请输入章节标题"
            clearable
            style="width: 200px"
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item label="状态" prop="isPublished">
          <el-select v-model="queryParams.isPublished" placeholder="发布状态" clearable style="width: 140px">
            <el-option label="已发布" :value="true" />
            <el-option label="草稿" :value="false" />
          </el-select>
        </el-form-item>
        <el-form-item label="是否免费" prop="isFree">
          <el-select v-model="queryParams.isFree" placeholder="免费/VIP" clearable style="width: 120px">
            <el-option label="免费" :value="true" />
            <el-option label="VIP" :value="false" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
          <el-button icon="Refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 操作按钮 -->
      <el-row :gutter="10" class="mb8">
        <el-col :span="1.5">
          <el-button
            type="primary"
            plain
            icon="Plus"
            @click="handleAdd"
            v-hasPermi="['portal:bookChapter:add']"
          >新增章节</el-button>
        </el-col>
        <el-col :span="1.5">
          <el-button
            type="warning"
            plain
            icon="Upload"
            @click="handleImport"
            v-hasPermi="['portal:bookChapter:add']"
          >文档导入</el-button>
        </el-col>
        <el-col :span="1.5">
          <el-button
            type="success"
            plain
            icon="Sort"
            @click="handleRecount"
            v-hasPermi="['portal:bookChapter:edit']"
          >重新统计</el-button>
        </el-col>
        <el-col :span="1.5">
          <el-button
            type="danger"
            plain
            icon="Delete"
            :disabled="multiple"
            @click="handleDelete"
            v-hasPermi="['portal:bookChapter:remove']"
          >删除</el-button>
        </el-col>
        <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
      </el-row>

      <!-- 数据表格 -->
      <el-table v-loading="loading" :data="chapterList" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="55" align="center" />
        <el-table-column label="序号" align="center" prop="chapterNo" width="70" />
        <el-table-column label="章节标题" align="left" prop="title" min-width="220" :show-overflow-tooltip="true" />
        <el-table-column label="字数" align="center" prop="wordCount" width="90" />
        <el-table-column label="浏览量" align="center" prop="viewCount" width="90" />
        <el-table-column label="免费" align="center" prop="isFree" width="80">
          <template #default="scope">
            <el-tag v-if="scope.row.isFree" type="success" size="small">免费</el-tag>
            <el-tag v-else type="warning" size="small">VIP</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" align="center" prop="isPublished" width="80">
          <template #default="scope">
            <el-tag v-if="scope.row.isPublished" type="success" size="small">已发布</el-tag>
            <el-tag v-else type="info" size="small">草稿</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="发布时间" align="center" prop="publishTime" width="160">
          <template #default="scope">
            <span>{{ scope.row.publishTime ? parseTime(scope.row.publishTime) : '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" align="center" prop="createTime" width="160">
          <template #default="scope">
            <span>{{ parseTime(scope.row.createTime) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="240" fixed="right">
          <template #default="scope">
            <el-button
              link
              type="primary"
              icon="Edit"
              @click="handleUpdate(scope.row)"
              v-hasPermi="['portal:bookChapter:edit']"
            >编辑</el-button>
            <el-button
              v-if="!scope.row.isPublished"
              link
              type="success"
              icon="Promotion"
              @click="handlePublish(scope.row)"
              v-hasPermi="['portal:bookChapter:publish']"
            >发布</el-button>
            <el-button
              v-else
              link
              type="warning"
              icon="RefreshLeft"
              @click="handleUnpublish(scope.row)"
              v-hasPermi="['portal:bookChapter:publish']"
            >撤回</el-button>
            <el-button
              link
              type="danger"
              icon="Delete"
              @click="handleDelete(scope.row)"
              v-hasPermi="['portal:bookChapter:remove']"
            >删除</el-button>
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
    </template>

    <!-- 新增/编辑章节对话框 -->
    <el-dialog :title="title" v-model="open" width="900px" append-to-body :close-on-click-modal="false">
      <el-form ref="chapterRef" :model="form" :rules="rules" label-width="100px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="章节标题" prop="title">
              <el-input v-model="form.title" placeholder="请输入章节标题" />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="序号" prop="chapterNo">
              <el-input-number v-model="form.chapterNo" :min="1" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="发布状态">
              <el-switch
                v-model="form.isPublished"
                active-text="已发布"
                inactive-text="草稿"
              />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="6">
            <el-form-item label="是否免费">
              <el-switch v-model="form.isFree" />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="价格" v-if="!form.isFree">
              <el-input-number v-model="form.price" :min="0" :step="0.01" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="编辑器模式">
              <el-radio-group v-model="form.editorMode">
                <el-radio label="richtext">富文本</el-radio>
                <el-radio label="markdown">Markdown</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="章节正文" prop="content">
          <editor
            v-if="form.editorMode === 'richtext'"
            v-model="form.content"
            :min-height="300"
            :height="380"
          />
          <el-input
            v-else
            v-model="form.contentMarkdown"
            type="textarea"
            :rows="18"
            placeholder="请输入 Markdown 正文"
          />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" placeholder="备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">确 定</el-button>
          <el-button @click="cancel">取 消</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 文档导入向导 -->
    <ImportWizard
      v-model="importVisible"
      :book-id="bookSelect.bookId"
      :book-options="bookOptions"
      @success="handleImportSuccess"
    />
  </div>
</template>

<script setup name="BookChapter">
import { ref, reactive, getCurrentInstance, onMounted, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { listBook } from "@/api/portal/book";
import {
  listBookChapter,
  getBookChapter,
  addBookChapter,
  updateBookChapter,
  delBookChapter,
  delBookChapterBatch,
  publishBookChapter,
  unpublishBookChapter,
  recountBookStats
} from "@/api/portal/bookChapter";
import ImportWizard from "@/components/ImportWizard/index.vue";

const { proxy } = getCurrentInstance();
const route = useRoute();
const router = useRouter();

// 书籍选择
const bookSelect = reactive({ bookId: null });
const bookOptions = ref([]);
const currentBook = ref(null);

// 搜索参数
const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  bookId: null,
  title: null,
  isPublished: null,
  isFree: null
});

// 表格
const showSearch = ref(true);
const loading = ref(false);
const chapterList = ref([]);
const total = ref(0);
const selectedRows = ref([]);
const single = ref(true);
const multiple = ref(true);

// 弹窗
const open = ref(false);
const title = ref("");
const chapterRef = ref();
const form = ref({
  id: null,
  bookId: null,
  title: null,
  content: null,
  contentMarkdown: null,
  editorMode: "richtext",
  wordCount: 0,
  chapterNo: 1,
  isFree: true,
  price: 0,
  isPublished: false,
  publishTime: null,
  remark: null
});

const rules = {
  title: [{ required: true, message: "章节标题不能为空", trigger: "blur" }],
  chapterNo: [{ required: true, message: "序号不能为空", trigger: "blur" }]
};

// 导入向导
const importVisible = ref(false);

// 加载书籍选项（一次性，限制 200 条）
function loadBookOptions() {
  return listBook({ pageNum: 1, pageSize: 200, status: "active" })
    .then((resp) => {
      bookOptions.value = resp.data?.records || [];
    })
    .catch(() => {
      bookOptions.value = [];
    });
}

function handleBookChange(bookId) {
  queryParams.bookId = bookId;
  currentBook.value = bookOptions.value.find((b) => b.id === bookId) || null;
  if (bookId) {
    handleQuery();
  } else {
    chapterList.value = [];
    total.value = 0;
  }
}

function getList() {
  if (!queryParams.bookId) {
    proxy.$modal.msgWarning("请先选择书籍");
    return;
  }
  loading.value = true;
  listBookChapter(queryParams)
    .then((resp) => {
      chapterList.value = resp.data?.records || [];
      total.value = resp.data?.total || 0;
    })
    .catch((e) => {
      proxy.$modal.msgError("查询失败: " + e.message);
    })
    .finally(() => {
      loading.value = false;
    });
}

function handleQuery() {
  queryParams.pageNum = 1;
  getList();
}

function resetQuery() {
  queryParams.title = null;
  queryParams.isPublished = null;
  queryParams.isFree = null;
  handleQuery();
}

function handleSelectionChange(selection) {
  selectedRows.value = selection;
  single.value = selection.length !== 1;
  multiple.value = !selection.length;
}

function resetForm() {
  form.value = {
    id: null,
    bookId: bookSelect.bookId,
    title: null,
    content: null,
    contentMarkdown: null,
    editorMode: "richtext",
    wordCount: 0,
    chapterNo: 1,
    isFree: true,
    price: 0,
    isPublished: false,
    publishTime: null,
    remark: null
  };
  if (chapterRef.value) chapterRef.value.resetFields();
}

function cancel() {
  open.value = false;
  resetForm();
}

function handleAdd() {
  resetForm();
  title.value = "新增章节";
  open.value = true;
}

function handleUpdate(row) {
  resetForm();
  const id = row.id || selectedRows.value[0]?.id;
  getBookChapter(id)
    .then((resp) => {
      const data = resp.data || resp;
      Object.assign(form.value, data);
      if (!form.value.editorMode) form.value.editorMode = "richtext";
      if (form.value.isFree == null) form.value.isFree = true;
      title.value = "修改章节";
      open.value = true;
    })
    .catch((e) => {
      proxy.$modal.msgError("查询详情失败: " + e.message);
    });
}

function submitForm() {
  chapterRef.value.validate((valid) => {
    if (!valid) return;
    form.value.bookId = bookSelect.bookId;
    // 富文本模式同步正文，markdown 模式只用 markdown
    if (form.value.editorMode === "markdown" && form.value.contentMarkdown) {
      form.value.content = null;
    }
    const action = form.value.id ? updateBookChapter(form.value) : addBookChapter(form.value);
    action
      .then((resp) => {
        if (resp.code === 200) {
          proxy.$modal.msgSuccess(resp.msg || "操作成功");
          open.value = false;
          getList();
        } else {
          proxy.$modal.msgError(resp.msg || "操作失败");
        }
      })
      .catch((e) => {
        proxy.$modal.msgError("操作失败: " + e.message);
      });
  });
}

function handleDelete(row) {
  const ids = row.id ? row.id : selectedRows.value.map((r) => r.id).join(",");
  proxy.$modal
    .confirm('是否确认删除章节编号为"' + ids + '"的数据项？')
    .then(() => {
      const action = row.id ? delBookChapter(ids) : delBookChapterBatch(ids);
      return action;
    })
    .then((resp) => {
      if (resp.code === 200) {
        proxy.$modal.msgSuccess("删除成功");
        getList();
      } else {
        proxy.$modal.msgError(resp.msg || "删除失败");
      }
    })
    .catch(() => {});
}

function handlePublish(row) {
  publishBookChapter(row.id)
    .then((resp) => {
      if (resp.code === 200) {
        proxy.$modal.msgSuccess("发布成功");
        getList();
      } else {
        proxy.$modal.msgError(resp.msg || "发布失败");
      }
    })
    .catch((e) => proxy.$modal.msgError("发布失败: " + e.message));
}

function handleUnpublish(row) {
  proxy.$modal
    .confirm(`确认撤回章节「${row.title}」的发布？撤回后前台将不可见`)
    .then(() => unpublishBookChapter(row.id))
    .then((resp) => {
      if (resp.code === 200) {
        proxy.$modal.msgSuccess("已撤回");
        getList();
      } else {
        proxy.$modal.msgError(resp.msg || "撤回失败");
      }
    })
    .catch(() => {});
}

function handleRecount() {
  if (!bookSelect.bookId) return;
  proxy.$modal
    .confirm("将根据已发布章节重新统计该书籍的字数和章节数，是否继续？")
    .then(() => recountBookStats(bookSelect.bookId))
    .then((resp) => {
      if (resp.code === 200) {
        proxy.$modal.msgSuccess("统计完成");
        loadBookOptions();
        handleBookChange(bookSelect.bookId);
      } else {
        proxy.$modal.msgError(resp.msg || "统计失败");
      }
    })
    .catch(() => {});
}

function handleImport() {
  if (!bookSelect.bookId) {
    proxy.$modal.msgWarning("请先选择目标书籍");
    return;
  }
  importVisible.value = true;
}

function handleImportSuccess() {
  getList();
  loadBookOptions();
}

// 显示辅助
function getBookTypeText(type) {
  if (type === "novel") return "网络小说";
  if (type === "published") return "出版书籍";
  if (type === "longform") return "长文文章";
  return type || "未分类";
}
function getSerialStatusText(status) {
  if (status === "ongoing") return "连载中";
  if (status === "completed") return "已完结";
  if (status === "hiatus") return "暂停";
  return status || "-";
}
function getSerialStatusType(status) {
  if (status === "ongoing") return "success";
  if (status === "completed") return "info";
  if (status === "hiatus") return "warning";
  return "";
}

onMounted(() => {
  loadBookOptions().then(() => {
    // 从路由参数自动加载书籍
    const bid = route.params.bookId;
    if (bid) {
      const id = Number(bid);
      if (bookOptions.value.find((b) => b.id === id) || id) {
        bookSelect.bookId = id;
        handleBookChange(id);
      }
    }
  });
});

// 监听路由参数变化（从书籍管理跳转过来时）
watch(
  () => route.params.bookId,
  (bid) => {
    if (bid && Number(bid) !== bookSelect.bookId) {
      const id = Number(bid);
      bookSelect.bookId = id;
      handleBookChange(id);
    }
  }
);
</script>

<style scoped>
.book-bar {
  margin-bottom: 12px;
}
.empty-state {
  padding: 60px 0;
  background: #fff;
}
.ml8 {
  margin-left: 8px;
}
</style>
