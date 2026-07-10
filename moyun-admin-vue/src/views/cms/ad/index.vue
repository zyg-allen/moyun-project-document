<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="广告位" prop="slotKey">
        <el-select
          v-model="queryParams.slotKey"
          placeholder="请选择广告位"
          clearable
          filterable
          allow-create
          style="width: 200px"
        >
          <el-option label="文章详情底部" value="article_detail_bottom" />
          <el-option label="首页侧栏" value="home_sidebar" />
        </el-select>
      </el-form-item>
      <el-form-item label="标题" prop="title">
        <el-input
          v-model="queryParams.title"
          placeholder="请输入标题"
          clearable
          style="width: 200px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择状态" clearable style="width: 200px">
          <el-option label="启用" value="0" />
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
          v-hasPermi="['portal:ad:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="Delete"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['portal:ad:remove']"
        >删除</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" :columns="columns"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="adList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="编号" align="center" prop="id" width="80" />
      <el-table-column label="广告位" align="center" prop="slotKey" width="180" :show-overflow-tooltip="true">
        <template #default="scope">
          {{ slotKeyLabel(scope.row.slotKey) }}
        </template>
      </el-table-column>
      <el-table-column label="标题" align="center" prop="title" min-width="160" :show-overflow-tooltip="true" />
      <el-table-column label="广告图" align="center" prop="image" width="100">
        <template #default="scope">
          <el-image v-if="scope.row.image" :src="scope.row.image" style="width: 50px; height: 50px;" fit="cover" />
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="跳转链接" align="center" prop="link" min-width="180" :show-overflow-tooltip="true">
        <template #default="scope">
          <el-link v-if="scope.row.link" :href="scope.row.link" target="_blank" type="primary">{{ scope.row.link }}</el-link>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="排序" align="center" prop="sort" width="80" />
      <el-table-column label="状态" align="center" prop="status" width="100">
        <template #default="scope">
          <el-tag :type="scope.row.status === '0' ? 'success' : 'danger'">
            {{ scope.row.status === '0' ? '启用' : '停用' }}
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
            v-hasPermi="['portal:ad:edit']"
          >修改</el-button>
          <el-button
            link
            type="primary"
            icon="Delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['portal:ad:remove']"
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
      <el-form ref="adRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="广告位" prop="slotKey">
          <el-select
            v-model="form.slotKey"
            placeholder="请选择或输入广告位标识"
            filterable
            allow-create
            default-first-option
            style="width: 100%"
          >
            <el-option label="文章详情底部" value="article_detail_bottom" />
            <el-option label="首页侧栏" value="home_sidebar" />
          </el-select>
        </el-form-item>
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" placeholder="请输入标题" />
        </el-form-item>
        <el-form-item label="广告图" prop="image">
          <el-input v-model="form.image" placeholder="请输入广告图地址" />
        </el-form-item>
        <el-form-item label="跳转链接" prop="link">
          <el-input v-model="form.link" placeholder="请输入跳转链接" />
        </el-form-item>
        <el-form-item label="文案" prop="content">
          <el-input v-model="form.content" type="textarea" :rows="3" placeholder="请输入文案" />
        </el-form-item>
        <el-form-item label="排序" prop="sort">
          <el-input-number v-model="form.sort" :min="0" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio label="0">启用</el-radio>
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

<script setup name="CmsAd">
import { listAdSlot, getAdSlot, addAdSlot, updateAdSlot, delAdSlot } from "@/api/cms/ad";

const { proxy } = getCurrentInstance();

const adList = ref([]);
const open = ref(false);
const loading = ref(true);
const showSearch = ref(true);
const ids = ref([]);
const single = ref(true);
const multiple = ref(true);
const total = ref(0);
const title = ref("");

const slotKeyOptions = [
  { value: "article_detail_bottom", label: "文章详情底部" },
  { value: "home_sidebar", label: "首页侧栏" }
];

function slotKeyLabel(key) {
  const opt = slotKeyOptions.find(o => o.value === key);
  return opt ? opt.label : (key || "-");
}

const columns = ref([
  { key: 0, label: `编号`, visible: true },
  { key: 1, label: `广告位`, visible: true },
  { key: 2, label: `标题`, visible: true },
  { key: 3, label: `广告图`, visible: true },
  { key: 4, label: `跳转链接`, visible: true },
  { key: 5, label: `排序`, visible: true },
  { key: 6, label: `状态`, visible: true },
  { key: 7, label: `创建时间`, visible: true }
]);

const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    slotKey: undefined,
    title: undefined,
    status: undefined
  },
  rules: {
    slotKey: [{ required: true, message: "广告位标识不能为空", trigger: "change" }],
    title: [{ required: true, message: "标题不能为空", trigger: "blur" }]
  }
});

const { queryParams, form, rules } = toRefs(data);

function getList() {
  loading.value = true;
  listAdSlot(queryParams.value).then(response => {
    adList.value = response.data.records || [];
    total.value = response.data.total || 0;
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
    slotKey: null,
    title: null,
    image: null,
    link: null,
    content: null,
    sort: 0,
    status: "0",
    remark: null
  };
  proxy.resetForm("adRef");
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
  title.value = "添加广告位";
}

function handleUpdate(row) {
  reset();
  const id = row.id;
  getAdSlot(id).then(response => {
    form.value = response.data;
    open.value = true;
    title.value = "修改广告位";
  });
}

function submitForm() {
  proxy.$refs["adRef"].validate(valid => {
    if (valid) {
      if (form.value.id != null) {
        updateAdSlot(form.value).then(response => {
          proxy.$modal.msgSuccess("修改成功");
          open.value = false;
          getList();
        });
      } else {
        addAdSlot(form.value).then(response => {
          proxy.$modal.msgSuccess("新增成功");
          open.value = false;
          getList();
        });
      }
    }
  });
}

function handleDelete(row) {
  const adIds = row.id || ids.value;
  proxy.$modal.confirm('是否确认删除广告位编号为"' + adIds + '"的数据项？').then(function () {
    return delAdSlot(adIds);
  }).then(() => {
    getList();
    proxy.$modal.msgSuccess("删除成功");
  }).catch(() => {});
}

getList();
</script>
