<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="网站名称" prop="name">
        <el-input
          v-model="queryParams.name"
          placeholder="请输入网站名称"
          clearable
          style="width: 200px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择状态" clearable style="width: 200px">
          <el-option label="正常" value="0" />
          <el-option label="停用" value="1" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          type="primary"
          plain
          icon="Plus"
          @click="handleAdd"
          v-hasPermi="['cms:friend-link:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="Delete"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['cms:friend-link:remove']"
        >删除</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" :columns="columns"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="friendLinkList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="编号" align="center" prop="id" width="80" />
      <el-table-column label="网站名称" align="center" prop="name" width="150" :show-overflow-tooltip="true" />
      <el-table-column label="网站地址" align="center" prop="url" min-width="200" :show-overflow-tooltip="true">
        <template #default="scope">
          <el-link :href="scope.row.url" target="_blank" type="primary">{{ scope.row.url }}</el-link>
        </template>
      </el-table-column>
      <el-table-column label="网站描述" align="center" prop="description" min-width="150" :show-overflow-tooltip="true" />
      <el-table-column label="网站logo" align="center" prop="logo" width="100">
        <template #default="scope">
          <el-image v-if="scope.row.logo" :src="scope.row.logo" style="width: 50px; height: 50px;" fit="cover" />
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="排序" align="center" prop="sort" width="80" />
      <el-table-column label="状态" align="center" prop="status" width="100">
        <template #default="scope">
          <el-tag :type="scope.row.status === '0' ? 'success' : 'danger'">
            {{ scope.row.status === '0' ? '正常' : '停用' }}
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
            icon="Edit"
            @click="handleUpdate(scope.row)"
            v-hasPermi="['cms:friend-link:edit']"
          >修改</el-button>
          <el-button
            link
            type="primary"
            icon="Delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['cms:friend-link:remove']"
          >删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination
      v-show="total > 0"
      :total="total"
      v-model:page="queryParams.pageNum"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />

    <el-dialog :title="title" v-model="open" width="600px" append-to-body>
      <el-form ref="friendLinkRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="网站名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入网站名称" />
        </el-form-item>
        <el-form-item label="网站地址" prop="url">
          <el-input v-model="form.url" placeholder="请输入网站地址" />
        </el-form-item>
        <el-form-item label="网站描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="请输入网站描述" />
        </el-form-item>
        <el-form-item label="网站logo" prop="logo">
          <el-input v-model="form.logo" placeholder="请输入网站logo地址" />
        </el-form-item>
        <el-form-item label="排序" prop="sort">
          <el-input-number v-model="form.sort" :min="0" />
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

<script setup name="CmsFriendLink">
import { listFriendLink, getFriendLink, addFriendLink, updateFriendLink, delFriendLink } from "@/api/cms/friend-link";

const { proxy } = getCurrentInstance();

const friendLinkList = ref([]);
const open = ref(false);
const loading = ref(true);
const showSearch = ref(true);
const ids = ref([]);
const single = ref(true);
const multiple = ref(true);
const total = ref(0);
const title = ref("");

const columns = ref([
  { key: 0, label: `编号`, visible: true },
  { key: 1, label: `网站名称`, visible: true },
  { key: 2, label: `网站地址`, visible: true },
  { key: 3, label: `网站描述`, visible: true },
  { key: 4, label: `网站logo`, visible: true },
  { key: 5, label: `排序`, visible: true },
  { key: 6, label: `状态`, visible: true },
  { key: 7, label: `创建时间`, visible: true }
]);

const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    name: undefined,
    status: undefined
  },
  rules: {
    name: [{ required: true, message: "网站名称不能为空", trigger: "blur" }],
    url: [{ required: true, message: "网站地址不能为空", trigger: "blur" }]
  }
});

const { queryParams, form, rules } = toRefs(data);

function getList() {
  loading.value = true;
  listFriendLink(queryParams.value).then(response => {
    friendLinkList.value = response.rows;
    total.value = response.total;
    loading.value = false;
  });
}

function cancel() {
  open.value = false;
  reset();
}

function reset() {
  form.value = {
    id: null,
    name: null,
    url: null,
    description: null,
    logo: null,
    sort: 0,
    status: "0",
    remark: null
  };
  proxy.resetForm("friendLinkRef");
}

function handleQuery() {
  queryParams.value.pageNum = 1;
  getList();
}

function resetQuery() {
  proxy.resetForm("queryRef");
  handleQuery();
}

function handleSelectionChange(selection) {
  ids.value = selection.map(item => item.id);
  single.value = selection.length !== 1;
  multiple.value = !selection.length;
}

function handleAdd() {
  reset();
  open.value = true;
  title.value = "添加友情链接";
}

function handleUpdate(row) {
  reset();
  const id = row.id;
  getFriendLink(id).then(response => {
    form.value = response.data;
    open.value = true;
    title.value = "修改友情链接";
  });
}

function submitForm() {
  proxy.$refs["friendLinkRef"].validate(valid => {
    if (valid) {
      if (form.value.id != null) {
        updateFriendLink(form.value).then(response => {
          proxy.$modal.msgSuccess("修改成功");
          open.value = false;
          getList();
        });
      } else {
        addFriendLink(form.value).then(response => {
          proxy.$modal.msgSuccess("新增成功");
          open.value = false;
          getList();
        });
      }
    }
  });
}

function handleDelete(row) {
  const friendLinkIds = row.id || ids.value;
  proxy.$modal.confirm('是否确认删除友情链接编号为"' + friendLinkIds + '"的数据项？').then(function () {
    return delFriendLink(friendLinkIds);
  }).then(() => {
    getList();
    proxy.$modal.msgSuccess("删除成功");
  }).catch(() => {});
}

getList();
</script>
