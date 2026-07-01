<template>
  <div class="app-container">
    <!-- 搜索表单 -->
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="推荐位置" prop="position">
        <el-select v-model="queryParams.position" placeholder="请选择位置" clearable style="width: 180px">
          <el-option label="首页轮播" value="home_banner" />
          <el-option label="首页热门" value="home_hot" />
          <el-option label="分类顶推" value="category_top" />
          <el-option label="限免专区" value="limit_free" />
          <el-option label="发现页轮播" value="discover_banner" />
        </el-select>
      </el-form-item>
      <el-form-item label="书名" prop="bookTitle">
        <el-input v-model="queryParams.bookTitle" placeholder="请输入书名" clearable style="width: 200px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="状态" prop="isActive">
        <el-select v-model="queryParams.isActive" placeholder="生效状态" clearable style="width: 120px">
          <el-option label="生效中" :value="1" />
          <el-option label="已下架" :value="0" />
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
        <el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['portal:bookRecommend:add']">新增推荐</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete" v-hasPermi="['portal:bookRecommend:remove']">删除</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <!-- 表格 -->
    <el-table v-loading="loading" :data="recommendList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="ID" prop="id" width="80" align="center" />
      <el-table-column label="书籍ID" prop="bookId" width="100" align="center" />
      <el-table-column label="书名" prop="bookTitle" min-width="180" show-overflow-tooltip>
        <template #default="scope">
          <span v-if="scope.row.bookTitle">{{ scope.row.bookTitle }}</span>
          <span v-else class="text-gray">书籍ID: {{ scope.row.bookId }}</span>
        </template>
      </el-table-column>
      <el-table-column label="推荐位置" prop="position" width="130" align="center">
        <template #default="scope">
          <el-tag :type="positionTagType(scope.row.position)">{{ positionLabel(scope.row.position) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="排序" prop="sort" width="80" align="center" />
      <el-table-column label="生效时间" width="320" align="center">
        <template #default="scope">
          <span v-if="scope.row.startTime || scope.row.endTime">
            {{ scope.row.startTime || '不限' }} ~ {{ scope.row.endTime || '长期' }}
          </span>
          <span v-else class="text-gray">长期有效</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" prop="isActive" width="100" align="center">
        <template #default="scope">
          <el-switch :model-value="scope.row.isActive === 1 || scope.row.isActive === true"
                     @change="handleToggleActive(scope.row)"
                     v-hasPermi="['portal:bookRecommend:edit']" />
        </template>
      </el-table-column>
      <el-table-column label="创建时间" prop="createTime" width="180" align="center">
        <template #default="scope">
          <span>{{ parseTime(scope.row.createTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="160" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleEdit(scope.row)" v-hasPermi="['portal:bookRecommend:edit']">编辑</el-button>
          <el-button link type="danger" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['portal:bookRecommend:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <!-- 新增/编辑弹窗 -->
    <el-dialog :title="dialogTitle" v-model="dialogVisible" width="600px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="书籍ID" prop="bookId">
          <el-input v-model.number="form.bookId" placeholder="请输入书籍ID" style="width: 200px" />
        </el-form-item>
        <el-form-item label="推荐位置" prop="position">
          <el-select v-model="form.position" placeholder="请选择推荐位置" style="width: 240px">
            <el-option label="首页轮播" value="home_banner" />
            <el-option label="首页热门" value="home_hot" />
            <el-option label="分类顶推" value="category_top" />
            <el-option label="限免专区" value="limit_free" />
            <el-option label="发现页轮播" value="discover_banner" />
          </el-select>
        </el-form-item>
        <el-form-item label="排序值" prop="sort">
          <el-input-number v-model="form.sort" :min="0" :max="9999" controls-position="right" />
          <span class="ml10 text-gray">（越小越靠前）</span>
        </el-form-item>
        <el-form-item label="生效开始" prop="startTime">
          <el-date-picker v-model="form.startTime" type="datetime" placeholder="不选则立即生效" value-format="YYYY-MM-DD HH:mm:ss" style="width: 240px" />
        </el-form-item>
        <el-form-item label="生效结束" prop="endTime">
          <el-date-picker v-model="form.endTime" type="datetime" placeholder="不选则长期有效" value-format="YYYY-MM-DD HH:mm:ss" style="width: 240px" />
        </el-form-item>
        <el-form-item label="是否生效" prop="isActive">
          <el-switch v-model="form.isActive" :active-value="1" :inactive-value="0" />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="运营说明（可选）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="BookRecommend">
import { ref, reactive, toRefs, getCurrentInstance, onMounted } from "vue";
import { listBookRecommend, getBookRecommend, addBookRecommend, updateBookRecommend, delBookRecommend, toggleBookRecommendActive } from "@/api/portal/bookRecommend";

const { proxy } = getCurrentInstance();

const recommendList = ref([]);
const loading = ref(true);
const showSearch = ref(true);
const multiple = ref(true);
const total = ref(0);
const ids = ref([]);
const dialogVisible = ref(false);
const dialogTitle = ref("");

const POSITION_LABEL = {
  home_banner: "首页轮播",
  home_hot: "首页热门",
  category_top: "分类顶推",
  limit_free: "限免专区",
  discover_banner: "发现页轮播"
};

const data = reactive({
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    position: undefined,
    bookTitle: undefined,
    isActive: undefined
  },
  form: {}
});

const { queryParams, form } = toRefs(data);

const rules = {
  bookId: [{ required: true, message: "书籍ID不能为空", trigger: "blur" }],
  position: [{ required: true, message: "推荐位置不能为空", trigger: "change" }]
};

/** 位置标签文案 */
function positionLabel(pos) {
  return POSITION_LABEL[pos] || pos;
}

/** 位置标签颜色 */
function positionTagType(pos) {
  const map = {
    home_banner: "danger",
    home_hot: "warning",
    category_top: "success",
    limit_free: "primary",
    discover_banner: "info"
  };
  return map[pos] || "";
}

/** 查询列表 */
function getList() {
  loading.value = true;
  listBookRecommend(queryParams.value).then(response => {
    recommendList.value = response.data?.records || [];
    total.value = response.data?.total || 0;
    loading.value = false;
  }).catch(() => {
    loading.value = false;
  });
}

/** 搜索按钮 */
function handleQuery() {
  queryParams.value.pageNum = 1;
  getList();
}

/** 重置按钮 */
function resetQuery() {
  proxy.resetForm("queryRef");
  handleQuery();
}

/** 多选 */
function handleSelectionChange(selection) {
  ids.value = selection.map(item => item.id);
  multiple.value = !selection.length;
}

/** 新增 */
function handleAdd() {
  resetForm();
  dialogTitle.value = "新增推荐位";
  dialogVisible.value = true;
}

/** 编辑 */
function handleEdit(row) {
  resetForm();
  getBookRecommend(row.id).then(response => {
    const d = response.data;
    form.value = {
      id: d.id,
      bookId: d.bookId,
      position: d.position,
      sort: d.sort ?? 0,
      startTime: d.startTime,
      endTime: d.endTime,
      isActive: d.isActive === true ? 1 : (d.isActive === false ? 0 : d.isActive),
      remark: d.remark
    };
    dialogTitle.value = "编辑推荐位";
    dialogVisible.value = true;
  });
}

/** 上下架切换 */
function handleToggleActive(row) {
  toggleBookRecommendActive(row.id).then(() => {
    getList();
    proxy.$modal.msgSuccess("状态已更新");
  }).catch(() => {});
}

/** 提交表单 */
function submitForm() {
  proxy.$refs["formRef"].validate(valid => {
    if (!valid) return;
    const payload = { ...form.value };
    if (form.value.id != null) {
      updateBookRecommend(payload).then(() => {
        proxy.$modal.msgSuccess("修改成功");
        dialogVisible.value = false;
        getList();
      });
    } else {
      addBookRecommend(payload).then(() => {
        proxy.$modal.msgSuccess("新增成功");
        dialogVisible.value = false;
        getList();
      });
    }
  });
}

/** 删除 */
function handleDelete(row) {
  const targetIds = row.id ? [row.id] : ids.value;
  if (!targetIds.length) return;
  proxy.$modal.confirm('确认删除选中的 ' + targetIds.length + ' 条推荐位记录？').then(function () {
    return delBookRecommend(targetIds.join(','));
  }).then(() => {
    getList();
    proxy.$modal.msgSuccess("删除成功");
  }).catch(() => {});
}

/** 重置表单 */
function resetForm() {
  form.value = {
    id: undefined,
    bookId: undefined,
    position: undefined,
    sort: 0,
    startTime: undefined,
    endTime: undefined,
    isActive: 1,
    remark: undefined
  };
  proxy.resetForm("formRef");
}

/** 取消 */
function cancel() {
  dialogVisible.value = false;
  resetForm();
}

onMounted(() => {
  getList();
});
</script>
