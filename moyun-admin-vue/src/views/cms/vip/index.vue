<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch">
      <el-form-item label="套餐名称" prop="name">
        <el-input v-model="queryParams.name" placeholder="请输入套餐名称" clearable style="width: 200px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="套餐状态" clearable style="width: 200px">
          <el-option v-for="d in cms_vip_status" :key="d.value" :label="d.label" :value="d.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['cms:vip:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" plain icon="Edit" :disabled="single" @click="handleUpdate" v-hasPermi="['cms:vip:edit']">修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete" v-hasPermi="['cms:vip:remove']">删除</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="vipList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="编号" align="center" prop="id" width="80" />
      <el-table-column label="套餐名称" align="center" prop="name" :show-overflow-tooltip="true" />
      <el-table-column label="价格(元)" align="center" prop="price" width="100">
        <template #default="scope">
          <span style="color: #f56c6c; font-weight: bold;">{{ scope.row.price }}</span>
        </template>
      </el-table-column>
      <el-table-column label="原价(元)" align="center" prop="originalPrice" width="100">
        <template #default="scope">
          <span style="text-decoration: line-through; color: #999;">{{ scope.row.originalPrice || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="有效期(天)" align="center" prop="duration" width="100" />
      <el-table-column label="热门" align="center" prop="popular" width="80">
        <template #default="scope">
          <el-tag v-if="scope.row.popular" type="danger">热门</el-tag>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="排序" align="center" prop="sort" width="80" />
      <el-table-column label="状态" align="center" prop="status" width="80">
        <template #default="scope">
          <dict-tag :options="cms_vip_status" :value="scope.row.status" />
        </template>
      </el-table-column>
      <el-table-column label="描述" align="center" prop="description" :show-overflow-tooltip="true" min-width="150" />
      <el-table-column label="操作" align="right" width="160" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['cms:vip:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['cms:vip:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <el-dialog :title="title" v-model="open" width="800px" append-to-body>
      <el-form ref="vipRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="套餐名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入套餐名称" />
        </el-form-item>
        <el-form-item label="价格(元)" prop="price">
          <el-input-number v-model="form.price" :precision="2" :min="0" :step="1" style="width: 200px" />
        </el-form-item>
        <el-form-item label="原价(元)" prop="originalPrice">
          <el-input-number v-model="form.originalPrice" :precision="2" :min="0" :step="1" style="width: 200px" placeholder="可选" />
        </el-form-item>
        <el-form-item label="有效期(天)" prop="duration">
          <el-input-number v-model="form.duration" :min="1" :step="1" style="width: 200px" />
        </el-form-item>
        <el-form-item label="套餐描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="请输入套餐描述" />
        </el-form-item>
        <el-form-item label="权益说明" prop="features">
          <el-input v-model="form.features" type="textarea" :rows="4" placeholder="每行一条权益，如：无限AI模拟面试" />
        </el-form-item>
        <el-form-item label="是否热门" prop="popular">
          <el-switch v-model="form.popular" />
        </el-form-item>
        <el-form-item label="排序" prop="sort">
          <el-input-number v-model="form.sort" :min="0" :step="1" style="width: 200px" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio v-for="d in cms_vip_status" :key="d.value" :label="d.value">{{ d.label }}</el-radio>
          </el-radio-group>
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

<script setup name="Vip">
import { listVip, getVip, addVip, updateVip, delVip } from "@/api/cms/vip";

const { proxy } = getCurrentInstance();
const { cms_vip_status } = proxy.useDict("cms_vip_status");

const vipList = ref([]);
const open = ref(false);
const loading = ref(true);
const showSearch = ref(true);
const ids = ref([]);
const single = ref(true);
const multiple = ref(true);
const total = ref(0);
const title = ref("");

const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    name: undefined,
    status: undefined
  },
  rules: {
    name: [{ required: true, message: "套餐名称不能为空", trigger: "blur" }],
    price: [{ required: true, message: "价格不能为空", trigger: "blur" }],
    duration: [{ required: true, message: "有效期不能为空", trigger: "blur" }]
  }
});

const { queryParams, form, rules } = toRefs(data);

function getList() {
  loading.value = true;
  listVip(queryParams.value).then(response => {
    vipList.value = response.data.records;
    total.value = response.data.total;
    loading.value = false;
  });
}

function cancel() {
  open.value = false;
  reset();
}

function reset() {
  form.value = {
    id: undefined,
    name: undefined,
    price: 0,
    originalPrice: undefined,
    duration: 30,
    description: undefined,
    features: undefined,
    popular: false,
    sort: 0,
    status: "0"
  };
  proxy.resetForm("vipRef");
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
  title.value = "新增VIP套餐";
}

function handleUpdate(row) {
  reset();
  const id = row.id || ids.value[0];
  getVip(id).then(response => {
    form.value = response.data;
    open.value = true;
    title.value = "修改VIP套餐";
  });
}

function submitForm() {
  proxy.$refs["vipRef"].validate(valid => {
    if (valid) {
      if (form.value.id != undefined) {
        updateVip(form.value).then(() => {
          proxy.$modal.msgSuccess("修改成功");
          open.value = false;
          getList();
        });
      } else {
        addVip(form.value).then(() => {
          proxy.$modal.msgSuccess("新增成功");
          open.value = false;
          getList();
        });
      }
    }
  });
}

function handleDelete(row) {
  const vipIds = row.id || ids.value;
  proxy.$modal.confirm('是否确认删除VIP套餐编号为"' + vipIds + '"的数据项？').then(function () {
    return delVip(vipIds);
  }).then(() => {
    getList();
    proxy.$modal.msgSuccess("删除成功");
  }).catch(() => {});
}

getList();
</script>
