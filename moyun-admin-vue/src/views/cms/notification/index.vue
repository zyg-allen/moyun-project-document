<template>
  <div class="app-container">
    <!-- 搜索表单 -->
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="通知标题" prop="title">
        <el-input
          v-model="queryParams.title"
          placeholder="请输入通知标题"
          clearable
          style="width: 200px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="通知类型" prop="type">
        <el-select v-model="queryParams.type" placeholder="通知类型" clearable style="width: 200px">
          <el-option label="系统通知" value="system" />
          <el-option label="评论通知" value="comment" />
          <el-option label="点赞通知" value="like" />
          <el-option label="关注通知" value="follow" />
          <el-option label="订单通知" value="order" />
        </el-select>
      </el-form-item>
      <el-form-item label="已读状态" prop="isRead">
        <el-select v-model="queryParams.isRead" placeholder="已读状态" clearable style="width: 200px">
          <el-option label="未读" :value="false" />
          <el-option label="已读" :value="true" />
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
          v-hasPermi="['cms:notification:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="Message"
          @click="handleSendAll"
          v-hasPermi="['cms:notification:sendAll']"
        >发送系统通知</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="Delete"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['cms:notification:remove']"
        >删除</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" :columns="columns"></right-toolbar>
    </el-row>

    <!-- 数据表格 -->
    <el-table v-loading="loading" :data="notificationList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="通知编号" align="center" prop="id" width="80" />
      <el-table-column label="通知标题" align="center" prop="title" width="200" :show-overflow-tooltip="true" />
      <el-table-column label="通知内容" align="center" prop="content" min-width="200" :show-overflow-tooltip="true" />
      <el-table-column label="用户昵称" align="center" prop="userNickname" width="120" />
      <el-table-column label="通知类型" align="center" prop="type" width="100">
        <template #default="scope">
          <el-tag :type="getTypeTagType(scope.row.type)">{{ getTypeText(scope.row.type) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="已读状态" align="center" prop="isRead" width="100">
        <template #default="scope">
          <el-tag :type="scope.row.isRead ? 'success' : 'danger'">
            {{ scope.row.isRead ? '已读' : '未读' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" align="center" prop="createTime" width="160">
        <template #default="scope">
          <span>{{ parseTime(scope.row.createTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="180">
        <template #default="scope">
          <el-button
            link
            type="primary"
            icon="View"
            @click="handleView(scope.row)"
            v-hasPermi="['cms:notification:query']"
          >查看</el-button>
          <el-button
            link
            type="primary"
            icon="Edit"
            @click="handleUpdate(scope.row)"
            v-hasPermi="['cms:notification:edit']"
          >修改</el-button>
          <el-button
            link
            type="primary"
            icon="Delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['cms:notification:remove']"
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

    <!-- 添加或修改通知对话框 -->
    <el-dialog :title="title" v-model="open" width="600px" append-to-body>
      <el-form ref="notificationRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="通知标题" prop="title">
          <el-input v-model="form.title" placeholder="请输入通知标题" />
        </el-form-item>
        <el-form-item label="通知类型" prop="type">
          <el-select v-model="form.type" placeholder="请选择通知类型">
            <el-option label="系统通知" value="system" />
            <el-option label="评论通知" value="comment" />
            <el-option label="点赞通知" value="like" />
            <el-option label="关注通知" value="follow" />
            <el-option label="订单通知" value="order" />
          </el-select>
        </el-form-item>
        <el-form-item label="通知内容" prop="content">
          <el-input v-model="form.content" type="textarea" :rows="5" placeholder="请输入通知内容" />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" placeholder="请输入备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">确 定</el-button>
          <el-button @click="cancel">取 消</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 发送系统通知对话框 -->
    <el-dialog title="发送系统通知" v-model="sendAllOpen" width="600px" append-to-body>
      <el-form ref="sendAllRef" :model="sendAllForm" :rules="sendAllRules" label-width="80px">
        <el-form-item label="通知标题" prop="title">
          <el-input v-model="sendAllForm.title" placeholder="请输入通知标题" />
        </el-form-item>
        <el-form-item label="通知内容" prop="content">
          <el-input v-model="sendAllForm.content" type="textarea" :rows="5" placeholder="请输入通知内容" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitSendAll">发 送</el-button>
          <el-button @click="sendAllOpen = false">取 消</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 查看通知对话框 -->
    <el-dialog title="通知详情" v-model="viewOpen" width="600px" append-to-body>
      <el-descriptions :column="1" border>
        <el-descriptions-item label="通知编号">{{ viewForm.id }}</el-descriptions-item>
        <el-descriptions-item label="用户昵称">{{ viewForm.userNickname || '-' }}</el-descriptions-item>
        <el-descriptions-item label="通知标题">{{ viewForm.title }}</el-descriptions-item>
        <el-descriptions-item label="通知类型">
          <el-tag :type="getTypeTagType(viewForm.type)">{{ getTypeText(viewForm.type) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="通知内容">
          <span>{{ viewForm.content }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="已读状态">
          <el-tag :type="viewForm.isRead ? 'success' : 'danger'">
            {{ viewForm.isRead ? '已读' : '未读' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ parseTime(viewForm.createTime) }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="viewOpen = false">关 闭</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="CmsNotification">
import { listNotification, getNotification, addNotification, updateNotification, delNotification, sendSystemNotification } from "@/api/cms/notification";

const { proxy } = getCurrentInstance();

// 表格数据
const notificationList = ref([]);
const open = ref(false);
const sendAllOpen = ref(false);
const viewOpen = ref(false);
const loading = ref(true);
const showSearch = ref(true);
const ids = ref([]);
const single = ref(true);
const multiple = ref(true);
const total = ref(0);
const title = ref("");

// 列显隐信息
const columns = ref([
  { key: 0, label: `通知编号`, visible: true },
  { key: 1, label: `通知标题`, visible: true },
  { key: 2, label: `通知内容`, visible: true },
  { key: 3, label: `用户昵称`, visible: true },
  { key: 4, label: `通知类型`, visible: true },
  { key: 5, label: `已读状态`, visible: true },
  { key: 6, label: `创建时间`, visible: true }
]);

// 查询参数
const data = reactive({
  form: {},
  viewForm: {},
  sendAllForm: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    title: undefined,
    type: undefined,
    isRead: undefined
  },
  rules: {
    title: [{ required: true, message: "通知标题不能为空", trigger: "blur" }],
    type: [{ required: true, message: "通知类型不能为空", trigger: "change" }],
    content: [{ required: true, message: "通知内容不能为空", trigger: "blur" }]
  },
  sendAllRules: {
    title: [{ required: true, message: "通知标题不能为空", trigger: "blur" }],
    content: [{ required: true, message: "通知内容不能为空", trigger: "blur" }]
  }
});

const { queryParams, form, viewForm, sendAllForm, rules, sendAllRules } = toRefs(data);

// 获取通知类型标签颜色
function getTypeTagType(type) {
  const typeMap = {
    'system': 'info',
    'comment': 'primary',
    'like': 'success',
    'follow': 'warning',
    'order': 'danger'
  };
  return typeMap[type] || 'info';
}

// 获取通知类型文本
function getTypeText(type) {
  const textMap = {
    'system': '系统通知',
    'comment': '评论通知',
    'like': '点赞通知',
    'follow': '关注通知',
    'order': '订单通知'
  };
  return textMap[type] || type;
}

// 查询通知列表
function getList() {
  loading.value = true;
  listNotification(queryParams.value).then(response => {
    notificationList.value = response.rows;
    total.value = response.total;
    loading.value = false;
  });
}

// 取消按钮
function cancel() {
  open.value = false;
  reset();
}

// 表单重置
function reset() {
  form.value = {
    id: null,
    title: null,
    type: null,
    content: null,
    remark: null
  };
  proxy.resetForm("notificationRef");
}

// 搜索按钮操作
function handleQuery() {
  queryParams.value.pageNum = 1;
  getList();
}

// 重置按钮操作
function resetQuery() {
  proxy.resetForm("queryRef");
  handleQuery();
}

// 表格多选
function handleSelectionChange(selection) {
  ids.value = selection.map(item => item.id);
  single.value = selection.length !== 1;
  multiple.value = !selection.length;
}

// 新增按钮操作
function handleAdd() {
  reset();
  open.value = true;
  title.value = "添加通知";
}

// 修改按钮操作
function handleUpdate(row) {
  reset();
  const id = row.id;
  getNotification(id).then(response => {
    form.value = response.data;
    open.value = true;
    title.value = "修改通知";
  });
}

// 查看按钮操作
function handleView(row) {
  const id = row.id;
  getNotification(id).then(response => {
    viewForm.value = response.data;
    viewOpen.value = true;
  });
}

// 发送系统通知按钮操作
function handleSendAll() {
  sendAllForm.value = {
    title: null,
    content: null,
    type: 'system'
  };
  sendAllOpen.value = true;
  proxy.resetForm("sendAllRef");
}

// 提交发送系统通知
function submitSendAll() {
  proxy.$refs["sendAllRef"].validate(valid => {
    if (valid) {
      sendSystemNotification(sendAllForm.value).then(response => {
        proxy.$modal.msgSuccess("发送成功");
        sendAllOpen.value = false;
        getList();
      });
    }
  });
}

// 提交按钮
function submitForm() {
  proxy.$refs["notificationRef"].validate(valid => {
    if (valid) {
      if (form.value.id != null) {
        updateNotification(form.value).then(response => {
          proxy.$modal.msgSuccess("修改成功");
          open.value = false;
          getList();
        });
      } else {
        addNotification(form.value).then(response => {
          proxy.$modal.msgSuccess("新增成功");
          open.value = false;
          getList();
        });
      }
    }
  });
}

// 删除按钮操作
function handleDelete(row) {
  const notificationIds = row.id || ids.value;
  proxy.$modal.confirm('是否确认删除通知编号为"' + notificationIds + '"的数据项？').then(function () {
    return delNotification(notificationIds);
  }).then(() => {
    getList();
    proxy.$modal.msgSuccess("删除成功");
  }).catch(() => {});
}

// 初始化查询
getList();
</script>
