<template>
  <div class="app-container">
    <!-- 搜索表单 -->
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="用户ID" prop="userId">
        <el-input v-model="queryParams.userId" placeholder="请输入用户ID" clearable style="width: 160px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="书籍ID" prop="bookId">
        <el-input v-model="queryParams.bookId" placeholder="请输入书籍ID" clearable style="width: 160px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="书名" prop="bookTitle">
        <el-input v-model="queryParams.bookTitle" placeholder="请输入书名" clearable style="width: 200px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="排序" prop="orderBy">
        <el-select v-model="queryParams.orderBy" placeholder="排序方式" clearable style="width: 140px">
          <el-option label="按收藏时间" value="latest" />
          <el-option label="按最近阅读" value="recent" />
          <el-option label="按自定义排序" value="sort" />
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
        <el-button type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete" v-hasPermi="['portal:bookshelf:remove']">移出书架</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <!-- 表格 -->
    <el-table v-loading="loading" :data="bookshelfList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="ID" prop="id" width="80" align="center" />
      <el-table-column label="用户ID" prop="userId" width="100" align="center" />
      <el-table-column label="书籍ID" prop="bookId" width="100" align="center" />
      <el-table-column label="最后阅读章节ID" prop="lastChapterId" width="140" align="center">
        <template #default="scope">
          <span v-if="scope.row.lastChapterId">{{ scope.row.lastChapterId }}</span>
          <span v-else class="text-gray">-</span>
        </template>
      </el-table-column>
      <el-table-column label="最后章节序号" prop="lastChapterNo" width="120" align="center" />
      <el-table-column label="排序值" prop="sort" width="100" align="center" />
      <el-table-column label="收藏时间" prop="createTime" width="180" align="center">
        <template #default="scope">
          <span>{{ parseTime(scope.row.createTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="更新时间" prop="updateTime" width="180" align="center">
        <template #default="scope">
          <span>{{ parseTime(scope.row.updateTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="120" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['portal:bookshelf:remove']">移出</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />
  </div>
</template>

<script setup name="Bookshelf">
import { ref, reactive, toRefs, getCurrentInstance, onMounted } from "vue";
import { listBookshelf, delBookshelf } from "@/api/portal/bookshelf";

const { proxy } = getCurrentInstance();

const bookshelfList = ref([]);
const loading = ref(true);
const showSearch = ref(true);
const multiple = ref(true);
const total = ref(0);
const ids = ref([]);

const data = reactive({
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    userId: undefined,
    bookId: undefined,
    bookTitle: undefined,
    orderBy: "latest"
  }
});

const { queryParams } = toRefs(data);

/** 查询列表 */
function getList() {
  loading.value = true;
  listBookshelf(queryParams.value).then(response => {
    bookshelfList.value = response.data?.records || [];
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
  queryParams.value.orderBy = "latest";
  handleQuery();
}

/** 多选 */
function handleSelectionChange(selection) {
  ids.value = selection.map(item => item.id);
  multiple.value = !selection.length;
}

/** 移出书架 */
function handleDelete(row) {
  const targetIds = row.id ? [row.id] : ids.value;
  if (!targetIds.length) return;
  proxy.$modal.confirm('确认将选中的 ' + targetIds.length + ' 条记录从书架中移除？').then(function () {
    return delBookshelf(targetIds.join(','));
  }).then(() => {
    getList();
    proxy.$modal.msgSuccess("移除成功");
  }).catch(() => {});
}

onMounted(() => {
  getList();
});
</script>
