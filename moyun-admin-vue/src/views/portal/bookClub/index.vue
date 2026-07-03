<template>
  <div class="app-container">
    <!-- 搜索表单 -->
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="活动标题" prop="title">
        <el-input
          v-model="queryParams.title"
          placeholder="请输入活动标题"
          clearable
          style="width: 200px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="状态" clearable style="width: 160px">
          <el-option label="未开始" value="upcoming" />
          <el-option label="进行中" value="ongoing" />
          <el-option label="已结束" value="ended" />
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
          v-hasPermi="['portal:bookClub:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="Edit"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['portal:bookClub:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="Delete"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['portal:bookClub:remove']"
        >删除</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <!-- 数据表格 -->
    <el-table v-loading="loading" :data="dataList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="编号" align="center" prop="id" width="70" />
      <el-table-column label="活动标题" align="center" prop="title" min-width="180" :show-overflow-tooltip="true" />
      <el-table-column label="书籍ID" align="center" prop="bookId" width="90" />
      <el-table-column label="活动周期" align="center" width="200">
        <template #default="scope">
          <span>{{ scope.row.startDate }} ~ {{ scope.row.endDate }}</span>
        </template>
      </el-table-column>
      <el-table-column label="参与人数" align="center" width="110">
        <template #default="scope">
          <span>{{ scope.row.currentParticipants || 0 }} / {{ scope.row.maxParticipants || 0 }}</span>
        </template>
      </el-table-column>
      <el-table-column label="创建者ID" align="center" prop="createdBy" width="90" />
      <el-table-column label="状态" align="center" prop="status" width="100">
        <template #default="scope">
          <el-tag :type="getStatusTagType(scope.row.status)" size="small">
            {{ getStatusText(scope.row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" align="center" prop="createTime" width="160">
        <template #default="scope">
          <span>{{ parseTime(scope.row.createTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="220" fixed="right">
        <template #default="scope">
          <el-button
            link
            type="primary"
            icon="Edit"
            @click="handleUpdate(scope.row)"
            v-hasPermi="['portal:bookClub:edit']"
          >修改</el-button>
          <el-button
            link
            type="warning"
            icon="Switch"
            @click="handleStatus(scope.row)"
            v-hasPermi="['portal:bookClub:edit']"
          >状态</el-button>
          <el-button
            link
            type="danger"
            icon="Delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['portal:bookClub:remove']"
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

    <!-- 添加或修改活动对话框 -->
    <el-dialog :title="title" v-model="open" width="720px" append-to-body>
      <el-form ref="bookClubRef" :model="form" :rules="rules" label-width="100px">
        <el-row :gutter="20">
          <el-col :span="24">
            <el-form-item label="活动标题" prop="title">
              <el-input v-model="form.title" placeholder="请输入活动标题" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="书籍ID" prop="bookId">
              <el-input-number v-model="form.bookId" :min="1" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="活动封面" prop="cover">
              <el-input v-model="form.cover" placeholder="封面URL（选填）" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="开始日期" prop="startDate">
              <el-date-picker
                v-model="form.startDate"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="选择开始日期"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="结束日期" prop="endDate">
              <el-date-picker
                v-model="form.endDate"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="选择结束日期"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="最大人数" prop="maxParticipants">
              <el-input-number v-model="form.maxParticipants" :min="0" :step="10" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态" prop="status">
              <el-radio-group v-model="form.status">
                <el-radio label="upcoming">未开始</el-radio>
                <el-radio label="ongoing">进行中</el-radio>
                <el-radio label="ended">已结束</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="24">
            <el-form-item label="活动描述" prop="description">
              <el-input v-model="form.description" type="textarea" :rows="4" placeholder="请输入活动描述" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">确 定</el-button>
          <el-button @click="cancel">取 消</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 状态切换对话框 -->
    <el-dialog title="活动状态变更" v-model="statusOpen" width="420px" append-to-body>
      <el-form label-width="100px">
        <el-form-item label="活动标题">
          <span>{{ currentRow.title }}</span>
        </el-form-item>
        <el-form-item label="当前状态">
          <el-tag :type="getStatusTagType(currentRow.status)">{{ getStatusText(currentRow.status) }}</el-tag>
        </el-form-item>
        <el-form-item label="新状态">
          <el-select v-model="statusForm.status" placeholder="请选择状态" style="width: 100%">
            <el-option label="未开始" value="upcoming" />
            <el-option label="进行中" value="ongoing" />
            <el-option label="已结束" value="ended" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitStatus">确 定</el-button>
          <el-button @click="statusOpen = false">取 消</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="BookClub">
import { getCurrentInstance, ref, reactive, onMounted } from "vue";
import { listBookClub, getBookClub, addBookClub, updateBookClub, delBookClub, changeBookClubStatus } from "@/api/portal/bookClub";

const { proxy } = getCurrentInstance();

// 搜索参数
const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  title: null,
  status: null
});

// 表格相关
const showSearch = ref(true);
const loading = ref(false);
const dataList = ref([]);
const total = ref(0);
const selectedRows = ref([]);
const single = ref(true);
const multiple = ref(true);

// 弹窗相关
const open = ref(false);
const title = ref("");
const bookClubRef = ref();
const statusOpen = ref(false);
const currentRow = ref({});
const statusForm = reactive({ id: null, status: null });

const defaultForm = () => ({
  id: null,
  title: null,
  bookId: null,
  description: null,
  cover: null,
  startDate: null,
  endDate: null,
  maxParticipants: 100,
  status: "upcoming"
});
const form = ref(defaultForm());

// 校验规则
const rules = {
  title: [{ required: true, message: "活动标题不能为空", trigger: "blur" }],
  bookId: [{ required: true, message: "书籍ID不能为空", trigger: "blur" }],
  startDate: [{ required: true, message: "开始日期不能为空", trigger: "change" }],
  endDate: [{ required: true, message: "结束日期不能为空", trigger: "change" }],
  status: [{ required: true, message: "状态不能为空", trigger: "change" }]
};

// 状态文本
function getStatusText(status) {
  const map = { upcoming: "未开始", ongoing: "进行中", ended: "已结束" };
  return map[status] || status || "-";
}

// 状态标签类型
function getStatusTagType(status) {
  const map = { upcoming: "info", ongoing: "success", ended: "warning" };
  return map[status] || "info";
}

// 查询列表
function getList() {
  loading.value = true;
  listBookClub(queryParams).then((response) => {
    dataList.value = response.data.records || [];
    total.value = response.data.total || 0;
    loading.value = false;
  }).catch((e) => {
    proxy.$modal.msgError("查询失败: " + e.message);
    loading.value = false;
  });
}

// 取消
function cancel() {
  open.value = false;
  resetForm();
}

// 重置表单
function resetForm() {
  form.value = defaultForm();
  if (bookClubRef.value) bookClubRef.value.resetFields();
}

// 搜索重置
function resetQuery() {
  queryParams.pageNum = 1;
  queryParams.title = null;
  queryParams.status = null;
  handleQuery();
}

// 搜索
function handleQuery() {
  queryParams.pageNum = 1;
  getList();
}

// 多选变化
function handleSelectionChange(selection) {
  selectedRows.value = selection;
  single.value = selection.length != 1;
  multiple.value = !selection.length;
}

// 新增
function handleAdd() {
  resetForm();
  title.value = "新增共读活动";
  open.value = true;
}

// 修改
function handleUpdate(row) {
  resetForm();
  const id = row.id || selectedRows.value[0].id;
  getBookClub(id).then((response) => {
    const data = response.data || response;
    Object.assign(form.value, data);
    title.value = "修改共读活动";
    open.value = true;
  }).catch((e) => {
    proxy.$modal.msgError("查询详情失败: " + e.message);
  });
}

// 提交
function submitForm() {
  bookClubRef.value.validate((valid) => {
    if (valid) {
      const action = form.value.id ? updateBookClub(form.value) : addBookClub(form.value);
      action.then((response) => {
        if (response.code === 200) {
          proxy.$modal.msgSuccess("操作成功");
          open.value = false;
          getList();
        } else {
          proxy.$modal.msgError(response.msg || "操作失败");
        }
      }).catch((e) => {
        proxy.$modal.msgError("操作失败: " + e.message);
      });
    }
  });
}

// 删除
function handleDelete(row) {
  const ids = row.id ? row.id : selectedRows.value.map((r) => r.id).join(",");
  proxy.$modal.confirm('是否确认删除共读活动编号为"' + ids + '"的数据项？').then(() => {
    delBookClub(ids).then((response) => {
      if (response.code === 200) {
        proxy.$modal.msgSuccess("删除成功");
        getList();
      } else {
        proxy.$modal.msgError(response.msg || "删除失败");
      }
    }).catch((e) => {
      proxy.$modal.msgError("删除失败: " + e.message);
    });
  }).catch(() => {});
}

// 打开状态变更
function handleStatus(row) {
  currentRow.value = row;
  statusForm.id = row.id;
  statusForm.status = row.status;
  statusOpen.value = true;
}

// 提交状态变更
function submitStatus() {
  changeBookClubStatus(statusForm.id, statusForm.status).then((response) => {
    if (response.code === 200) {
      proxy.$modal.msgSuccess("状态变更成功");
      statusOpen.value = false;
      getList();
    } else {
      proxy.$modal.msgError(response.msg || "状态变更失败");
    }
  }).catch((e) => {
    proxy.$modal.msgError("状态变更失败: " + e.message);
  });
}

onMounted(() => {
  getList();
});
</script>
