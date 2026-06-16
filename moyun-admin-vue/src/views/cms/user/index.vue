<template>
  <div class="app-container">
    <!-- 搜索表单 -->
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="用户名" prop="username">
        <el-input
            v-model="queryParams.username"
            placeholder="请输入用户名"
            clearable
            style="width: 200px"
            @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="用户昵称" prop="nickname">
        <el-input
            v-model="queryParams.nickname"
            placeholder="请输入用户昵称"
            clearable
            style="width: 200px"
            @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="手机号" prop="phone">
        <el-input
            v-model="queryParams.phone"
            placeholder="请输入手机号"
            clearable
            style="width: 200px"
            @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="用户状态" clearable style="width: 200px">
          <el-option label="正常" value="0" />
          <el-option label="停用" value="1" />
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
            v-hasPermi="['cms:user:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
            type="success"
            plain
            icon="Edit"
            :disabled="single"
            @click="handleUpdate"
            v-hasPermi="['cms:user:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
            type="danger"
            plain
            icon="Delete"
            :disabled="multiple"
            @click="handleDelete"
            v-hasPermi="['cms:user:remove']"
        >删除</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" :columns="columns"></right-toolbar>
    </el-row>

    <!-- 数据表格 -->
    <el-table v-loading="loading" :data="userList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="用户编号" align="center" prop="id" width="80" />
      <el-table-column label="用户名" align="center" prop="username" width="120" />
      <el-table-column label="用户昵称" align="center" prop="nickname" :show-overflow-tooltip="true" />
      <el-table-column label="头像" align="center" prop="avatar" width="100">
        <template #default="scope">
          <el-image
              :src="scope.row.avatar"
              :preview-src-list="[scope.row.avatar]"
              fit="cover"
              style="width: 40px; height: 40px; border-radius: 50%"
          />
        </template>
      </el-table-column>
      <el-table-column label="手机号" align="center" prop="phone" width="120" />
      <el-table-column label="邮箱" align="center" prop="email" width="180" :show-overflow-tooltip="true" />
      <el-table-column label="状态" align="center" prop="status" width="80">
        <template #default="scope">
          <el-switch
              v-model="scope.row.status"
              active-value="0"
              inactive-value="1"
              @change="handleStatusChange(scope.row)"
          />
        </template>
      </el-table-column>
      <el-table-column label="注册时间" align="center" prop="createTime" width="160">
        <template #default="scope">
          <span>{{ parseTime(scope.row.createTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="180">
        <template #default="scope">
          <el-button
              link
              type="primary"
              icon="Edit"
              @click="handleUpdate(scope.row)"
              v-hasPermi="['cms:user:edit']"
          >修改</el-button>
          <el-button
              link
              type="primary"
              icon="Key"
              @click="handleResetPwd(scope.row)"
              v-hasPermi="['cms:user:resetPwd']"
          >重置密码</el-button>
          <el-button
              link
              type="primary"
              icon="Delete"
              @click="handleDelete(scope.row)"
              v-hasPermi="['cms:user:remove']"
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

    <!-- 添加或修改用户对话框 -->
    <el-dialog :title="title" v-model="open" width="600px" append-to-body>
      <el-form ref="userRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="用户名" prop="username" v-if="!form.id">
          <el-input v-model="form.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="用户昵称" prop="nickname">
          <el-input v-model="form.nickname" placeholder="请输入用户昵称" />
        </el-form-item>
        <el-form-item label="头像" prop="avatar">
          <ImageUpload v-model="form.avatar" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="个人简介" prop="bio">
          <el-input v-model="form.bio" type="textarea" :rows="3" placeholder="请输入个人简介" />
        </el-form-item>
        <el-form-item label="职位" prop="position">
          <el-input v-model="form.position" placeholder="请输入职位" />
        </el-form-item>
        <el-form-item label="密码" prop="password" v-if="!form.id">
          <el-input v-model="form.password" type="password" placeholder="请输入密码" show-password />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio label="0">正常</el-radio>
            <el-radio label="1">停用</el-radio>
          </el-radio-group>
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
  </div>
</template>

<script setup name="CmsUser">
import { listUser, getUser, addUser, updateUser, delUser, changeUserStatus, resetUserPwd } from "@/api/cms/user";
import ImageUpload from "@/components/ImageUpload/index.vue";

const { proxy } = getCurrentInstance();

// 表格数据
const userList = ref([]);
const open = ref(false);
const loading = ref(true);
const showSearch = ref(true);
const ids = ref([]);
const single = ref(true);
const multiple = ref(true);
const total = ref(0);
const title = ref("");

// 列显隐信息
const columns = ref([
  { key: 0, label: `用户编号`, visible: true },
  { key: 1, label: `用户名`, visible: true },
  { key: 2, label: `用户昵称`, visible: true },
  { key: 3, label: `头像`, visible: true },
  { key: 4, label: `手机号`, visible: true },
  { key: 5, label: `邮箱`, visible: true },
  { key: 6, label: `状态`, visible: true },
  { key: 7, label: `注册时间`, visible: true }
]);

// 查询参数
const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    username: undefined,
    nickname: undefined,
    phone: undefined,
    status: undefined
  },
  rules: {
    username: [{ required: true, message: "用户名不能为空", trigger: "blur" }],
    phone: [
      { required: true, message: "手机号不能为空", trigger: "blur" },
      { pattern: /^1[3|4|5|6|7|8|9][0-9]\d{8}$/, message: "请输入正确的手机号", trigger: "blur" }
    ],
    email: [
      { type: "email", message: "请输入正确的邮箱地址", trigger: ["blur", "change"] }
    ]
  }
});

const { queryParams, form, rules } = toRefs(data);

// 查询用户列表
function getList() {
  loading.value = true;
  listUser(queryParams.value).then(response => {
    userList.value = response.data.records;
    total.value = response.data.total;
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
    id: undefined,
    username: undefined,
    nickname: undefined,
    avatar: undefined,
    phone: undefined,
    email: undefined,
    bio: undefined,
    position: undefined,
    password: undefined,
    status: "0",
    remark: undefined
  };
  proxy.resetForm("userRef");
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
  title.value = "添加用户";
}

// 修改按钮操作
function handleUpdate(row) {
  reset();
  const id = row.id || ids.value[0];
  getUser(id).then(response => {
    form.value = response.data;
    open.value = true;
    title.value = "修改用户";
  });
}

// 提交按钮
function submitForm() {
  proxy.$refs["userRef"].validate(valid => {
    if (valid) {
      if (form.value.id !== undefined) {
        updateUser(form.value).then(response => {
          proxy.$modal.msgSuccess("修改成功");
          open.value = false;
          getList();
        });
      } else {
        addUser(form.value).then(response => {
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
  const userIds = row.id || ids.value;
  proxy.$modal.confirm('是否确认删除用户编号为"' + userIds + '"的数据项？').then(function () {
    return delUser(userIds);
  }).then(() => {
    getList();
    proxy.$modal.msgSuccess("删除成功");
  }).catch(() => {});
}

// 用户状态修改
function handleStatusChange(row) {
  let text = row.status === "0" ? "启用" : "停用";
  proxy.$modal.confirm('确认要"' + text + '""' + (row.nickname || row.username) + '"用户吗？').then(function () {
    return changeUserStatus(row.id, row.status);
  }).then(() => {
    proxy.$modal.msgSuccess(text + "成功");
  }).catch(function () {
    row.status = row.status === "0" ? "1" : "0";
  });
}

// 重置密码
function handleResetPwd(row) {
  proxy.$prompt('请输入"' + (row.nickname || row.username) + '"的新密码', "提示", {
    confirmButtonText: "确定",
    cancelButtonText: "取消",
    closeOnClickModal: false,
    inputPattern: /^.{6,20}$/,
    inputErrorMessage: "密码长度必须在6-20位之间"
  }).then(({ value }) => {
    resetUserPwd(row.id, value).then(response => {
      proxy.$modal.msgSuccess("修改成功，新密码是：" + value);
    });
  }).catch(() => {});
}

// 初始化查询
getList();
</script>
