<template>
  <div class="app-container">
    <!-- 搜索表单 -->
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="分类名称" prop="name">
        <el-input
            v-model="queryParams.name"
            placeholder="请输入分类名称"
            clearable
            style="width: 200px"
            @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="状态" clearable style="width: 200px">
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
            v-hasPermi="['cms:category:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
            type="info"
            plain
            icon="Sort"
            @click="toggleExpandAll"
        >展开/折叠</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" :columns="columns"></right-toolbar>
    </el-row>

    <!-- 数据表格 -->
    <el-table
        v-if="refreshTable"
        v-loading="loading"
        :data="categoryList"
        row-key="categoryId"
        :default-expand-all="isExpandAll"
        :tree-props="{ children: 'children', hasChildren: 'hasChildren' }"
    >
      <el-table-column label="分类名称" :show-overflow-tooltip="true" width="160">
        <template #default="scope">
          <el-icon v-if="scope.row.icon" :size="18" class="mr-2"><component :is="scope.row.icon" /></el-icon>
          <span>{{ scope.row.name }}</span>
        </template>
      </el-table-column>
      <el-table-column label="排序" width="60">{{ scope.row.sort }}</el-table-column>
      <el-table-column label="状态" width="80">
        <template #default="scope">
          <el-switch
              v-model="scope.row.status"
              active-value="0"
              inactive-value="1"
              @change="handleStatusChange(scope.row)"
          />
        </template>
      </el-table-column>
      <el-table-column label="创建时间" align="center" width="160" prop="createTime">
        <template #default="scope">
          <span>{{ parseTime(scope.row.createTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="210" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['cms:category:edit']">修改</el-button>
          <el-button link type="primary" icon="Plus" @click="handleAdd(scope.row)" v-hasPermi="['cms:category:add']">新增子分类</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['cms:category:remove']">删除</el-button>
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

    <!-- 添加或修改分类对话框 -->
    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form ref="categoryRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="父分类" prop="parentId">
          <el-tree-select
              v-model="form.parentId"
              :data="categoryTree"
              :props="{ value: 'categoryId', label: 'name', children: 'children' }"
              value-key="categoryId"
              placeholder="请选择父分类"
              check-strictly
              clearable
              style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="分类名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入分类名称" />
        </el-form-item>
        <el-form-item label="分类图标" prop="icon">
          <el-input v-model="form.icon" placeholder="请输入分类图标" />
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

<script setup name="CmsCategory">
import { listCategory, getCategory, addCategory, updateCategory, delCategory, changeCategoryStatus } from "@/api/cms/category";

const { proxy } = getCurrentInstance();

// 表格数据
const categoryList = ref([]);
const categoryTree = ref([]);
const open = ref(false);
const loading = ref(true);
const showSearch = ref(true);
const total = ref(0);
const title = ref("");
const refreshTable = ref(true);
const isExpandAll = ref(true);

// 列显隐信息
const columns = ref([
  { key: 0, label: `分类名称`, visible: true },
  { key: 1, label: `排序`, visible: true },
  { key: 2, label: `状态`, visible: true },
  { key: 3, label: `创建时间`, visible: true }
]);

// 查询参数
const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    name: undefined,
    status: undefined
  },
  rules: {
    name: [{ required: true, message: "分类名称不能为空", trigger: "blur" }]
  }
});

const { queryParams, form, rules } = toRefs(data);

// 查询分类列表
function getList() {
  loading.value = true;
  listCategory(queryParams.value).then(response => {
    // 处理两种返回格式
    let dataList = [];
    if (Array.isArray(response.data)) {
      dataList = response.data;
    } else if (response.data && Array.isArray(response.data.rows)) {
      dataList = response.data.rows;
      total.value = response.data.total || 0;
    } else if (Array.isArray(response.rows)) {
      dataList = response.rows;
      total.value = response.total || 0;
    } else {
      dataList = [];
    }

    // 字段名映射：将后端字段转换为前端期望的字段
    const mappedList = dataList.map(item => ({
      categoryId: item.id,
      name: item.name,
      slug: item.slug,
      description: item.description,
      icon: item.icon,
      sort: item.sort,
      parentId: item.parentId || 0,
      status: item.status,
      createTime: item.createTime,
      remark: item.remark,
      children: []
    }));

    // 构建树形结构
    categoryList.value = buildTree(mappedList);
    categoryTree.value = categoryList.value;
    
    if (!total.value || total.value === 0) {
      total.value = categoryList.value.length;
    }

    loading.value = false;
  }).catch(error => {
    console.error('获取分类列表失败:', error);
    loading.value = false;
  });
}

// 构建树形结构
function buildTree(list) {
  const map = new Map();
  const roots = [];
  
  // 将 parentId 为 null 或 undefined 的转换为 0
  list.forEach(item => {
    if (item.parentId === null || item.parentId === undefined) {
      item.parentId = 0;
    }
    map.set(item.categoryId, item);
  });
  
  list.forEach(item => {
    const parent = map.get(item.parentId);
    if (parent && item.categoryId !== item.parentId) {
      if (!parent.children) {
        parent.children = [];
      }
      parent.children.push(item);
      parent.hasChildren = true;
    } else if (item.parentId === 0) {
      roots.push(item);
    }
  });
  
  // 按排序字段排序
  const sortChildren = (nodes) => {
    nodes.forEach(node => {
      if (node.children && node.children.length > 0) {
        node.children.sort((a, b) => (a.sort || 0) - (b.sort || 0));
        sortChildren(node.children);
      }
    });
    return nodes.sort((a, b) => (a.sort || 0) - (b.sort || 0));
  };
  
  return sortChildren(roots);
}

// 展开/折叠
function toggleExpandAll() {
  isExpandAll.value = !isExpandAll.value;
}

// 取消按钮
function cancel() {
  open.value = false;
  reset();
}

// 表单重置
function reset() {
  form.value = {
    categoryId: undefined,
    parentId: undefined,
    name: undefined,
    icon: undefined,
    sort: 0,
    status: "0",
    remark: undefined
  };
  proxy.resetForm("categoryRef");
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

// 新增按钮操作
function handleAdd(row) {
  reset();
  if (row && row.categoryId) {
    form.value.parentId = row.categoryId;
    title.value = "添加子分类";
  } else {
    title.value = "添加分类";
  }
  open.value = true;
}

// 修改按钮操作
function handleUpdate(row) {
  reset();
  const categoryId = row.categoryId;
  getCategory(categoryId).then(response => {
    // 🔥 字段映射：后端字段 -> 前端字段
    const data = response.data;
    form.value = {
      categoryId: data.id,
      name: data.name,
      icon: data.icon,
      sort: data.sort,
      parentId: data.parentId,
      status: data.status,
      remark: data.remark
    };
    open.value = true;
    title.value = "修改分类";
  });
}

// 提交按钮
function submitForm() {
  proxy.$refs["categoryRef"].validate(valid => {
    if (valid) {
      // 🔥 字段映射：前端字段 -> 后端字段
      const submitData = {
        id: form.value.categoryId,
        name: form.value.name,
        icon: form.value.icon,
        sort: form.value.sort,
        parentId: form.value.parentId,
        status: form.value.status,
        remark: form.value.remark
      };

      if (form.value.categoryId !== undefined) {
        updateCategory(submitData).then(response => {
          proxy.$modal.msgSuccess("修改成功");
          open.value = false;
          getList();
        });
      } else {
        addCategory(submitData).then(response => {
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
  const categoryId = row.categoryId;
  proxy.$modal.confirm('是否确认删除分类"' + row.name + '"？').then(function () {
    return delCategory(categoryId);
  }).then(() => {
    getList();
    proxy.$modal.msgSuccess("删除成功");
  }).catch(() => {});
}

// 分类状态修改
function handleStatusChange(row) {
  let text = row.status === "0" ? "启用" : "停用";
  proxy.$modal.confirm('确认要"' + text + '""' + row.name + '"分类吗？').then(function () {
    return changeCategoryStatus(row.categoryId, row.status);
  }).then(() => {
    proxy.$modal.msgSuccess(text + "成功");
  }).catch(function () {
    row.status = row.status === "0" ? "1" : "0";
  });
}

// 初始化查询
getList();
</script>
