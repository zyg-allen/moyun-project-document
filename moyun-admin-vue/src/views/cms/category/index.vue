<template>
  <div class="app-container">
    <!-- 搜索表单 -->
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="栏目名称" prop="name">
        <el-input
          v-model="queryParams.name"
          placeholder="请输入栏目名称"
          clearable
          style="width: 200px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="栏目状态" clearable style="width: 200px">
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
          @click="handleAddRoot"
          v-hasPermi="['cms:category:add']"
        >新增一级栏目</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="info"
          plain
          icon="Sort"
          @click="toggleExpandAll"
        >展开/折叠</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <!-- 栏目表格 -->
    <el-table
      v-if="refreshTable"
      v-loading="loading"
      :data="categoryList"
      row-key="id"
      :default-expand-all="isExpandAll"
      :tree-props="{ children: 'children', hasChildren: 'hasChildren' }"
    >
      <el-table-column prop="name" label="栏目名称" width="250" :show-overflow-tooltip="true">
        <template #default="{ row }">
          <span>{{ row.name }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="slug" label="别名" width="150" :show-overflow-tooltip="true" />
      <el-table-column prop="sort" label="排序" width="80" align="center" />
      <el-table-column prop="articleCount" label="文章数" width="80" align="center">
        <template #default="{ row }">
          <span>{{ row.articleCount || 0 }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="80" align="center">
        <template #default="{ row }">
          <el-tag :type="row.status === '0' ? 'success' : 'info'">
            {{ row.status === '0' ? '正常' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="description" label="描述" :show-overflow-tooltip="true" />
      <el-table-column label="创建时间" align="center" width="160" prop="createTime">
        <template #default="scope">
          <span>{{ parseTime(scope.row.createTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="280" class-name="small-padding fixed-width">
        <template #default="scope">
          <!-- 一级栏目操作 -->
          <template v-if="scope.row.parentId === 0 || scope.row.parentId === null">
            <el-button
              link
              type="primary"
              icon="Edit"
              @click="handleUpdate(scope.row)"
              v-hasPermi="['cms:category:edit']"
            >修改</el-button>
            <el-button
              link
              type="primary"
              icon="Plus"
              @click="handleAddChild(scope.row)"
              v-hasPermi="['cms:category:add']"
            >新增子栏目</el-button>
            <el-button
              link
              type="danger"
              icon="Delete"
              @click="handleDelete(scope.row)"
              v-hasPermi="['cms:category:remove']"
            >删除</el-button>
          </template>
          <!-- 二级栏目操作（只能修改和删除，不能新增子栏目） -->
          <template v-else>
            <el-button
              link
              type="primary"
              icon="Edit"
              @click="handleUpdate(scope.row)"
              v-hasPermi="['cms:category:edit']"
            >修改</el-button>
            <el-button
              link
              type="danger"
              icon="Delete"
              @click="handleDelete(scope.row)"
              v-hasPermi="['cms:category:remove']"
            >删除</el-button>
          </template>
        </template>
      </el-table-column>
    </el-table>

    <!-- 添加或修改栏目对话框 -->
    <el-dialog :title="title" v-model="open" width="600px" append-to-body>
      <el-form ref="categoryRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="上级栏目">
          <el-tree-select
            v-model="form.parentId"
            :data="categoryOptions"
            :props="{ value: 'id', label: 'name', children: 'children' }"
            value-key="id"
            placeholder="选择上级栏目（不选则为一级栏目）"
            check-strictly
            clearable
            :disabled="isLevel2"
            style="width: 100%"
          />
          <div v-if="isLevel2" style="color: #909399; font-size: 12px; margin-top: 4px;">
            二级栏目不能添加子栏目
          </div>
        </el-form-item>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="栏目名称" prop="name">
              <el-input v-model="form.name" placeholder="请输入栏目名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="栏目别名" prop="slug">
              <el-input v-model="form.slug" placeholder="请输入栏目别名(英文)" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="显示排序" prop="sort">
              <el-input-number v-model="form.sort" controls-position="right" :min="0" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="栏目状态" prop="status">
              <el-radio-group v-model="form.status">
                <el-radio label="0">正常</el-radio>
                <el-radio label="1">停用</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="栏目描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="请输入栏目描述" />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="请输入备注" />
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
import { listCategory, getCategory, addCategory, updateCategory, delCategory } from "@/api/cms/category";

const { proxy } = getCurrentInstance();

const categoryList = ref([]);
const open = ref(false);
const loading = ref(true);
const showSearch = ref(true);
const title = ref("");
const categoryOptions = ref([]);
const isExpandAll = ref(true);
const refreshTable = ref(true);
const isLevel2 = ref(false); // 是否在添加二级栏目（用于禁用父级选择）

const data = reactive({
  form: {},
  queryParams: {
    name: undefined,
    status: undefined
  },
  rules: {
    name: [{ required: true, message: "栏目名称不能为空", trigger: "blur" }],
    slug: [
      { required: true, message: "栏目别名不能为空", trigger: "blur" },
      { pattern: /^[a-z0-9-]+$/, message: "栏目别名只能包含小写字母、数字和连字符", trigger: 'blur' }
    ],
    sort: [{ required: true, message: "显示排序不能为空", trigger: "blur" }]
  }
});

const { queryParams, form, rules } = toRefs(data);

/** 查询栏目列表 */
function getList() {
  loading.value = true;
  listCategory(queryParams.value).then(response => {
    let listData = [];
    if (response.data && Array.isArray(response.data)) {
      listData = response.data;
    } else if (response.rows && Array.isArray(response.rows)) {
      listData = response.rows;
    }
    categoryList.value = proxy.handleTree(listData, "id");
    loading.value = false;
  });
}

/** 查询栏目下拉树结构（一级+二级） */
function getTreeselect() {
  categoryOptions.value = [];
  listCategory().then(response => {
    // 构建"无"选项（一级栏目）
    const root = { id: 0, name: "无（作为一级栏目）", children: [] };
    
    const listData = response.data || response.rows || [];
    
    // 先用 handleTree 构建树结构
    const treeData = proxy.handleTree(listData, "id");
    
    // 过滤掉三级及以上的子节点（只保留一级和二级）
    const filterTree = (nodes, level) => {
      if (level >= 2) return [];
      return nodes.filter(node => {
        if (node.children && node.children.length > 0) {
          node.children = filterTree(node.children, level + 1);
        }
        return true;
      });
    };
    
    root.children = filterTree(treeData, 1);
    categoryOptions.value.push(root);
  });
}

/** 取消按钮 */
function cancel() {
  open.value = false;
  reset();
}

/** 表单重置 */
function reset() {
  form.value = {
    id: undefined,
    parentId: 0,
    name: undefined,
    slug: undefined,
    sort: 0,
    status: "0",
    description: undefined,
    remark: undefined
  };
  isLevel2.value = false;
  proxy.resetForm("categoryRef");
}

/** 搜索按钮操作 */
function handleQuery() {
  getList();
}

/** 重置按钮操作 */
function resetQuery() {
  proxy.resetForm("queryRef");
  handleQuery();
}

/** 新增一级栏目 */
function handleAddRoot() {
  reset();
  getTreeselect();
  form.value.parentId = 0;
  title.value = "新增一级栏目";
  open.value = true;
}

/** 新增子栏目 */
function handleAddChild(row) {
  reset();
  getTreeselect();
  form.value.parentId = row.id;
  isLevel2.value = true; // 标记为二级栏目，禁用父级选择
  title.value = "新增子栏目（" + row.name + "）";
  open.value = true;
}

/** 展开/折叠操作 */
function toggleExpandAll() {
  refreshTable.value = false;
  isExpandAll.value = !isExpandAll.value;
  nextTick(() => {
    refreshTable.value = true;
  });
}

/** 修改按钮操作 */
async function handleUpdate(row) {
  reset();
  await getTreeselect();
  getCategory(row.id).then(response => {
    form.value = response.data;
    // 如果是二级栏目，禁用父级选择
    if (row.parentId && row.parentId !== 0) {
      isLevel2.value = true;
    }
    open.value = true;
    title.value = "修改栏目";
  });
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["categoryRef"].validate(valid => {
    if (valid) {
      // 前端校验：二级栏目不能添加子栏目
      if (isLevel2.value && form.value.parentId !== 0) {
        // 如果当前是在添加二级栏目，且选择了父级
        // 检查父级是否已经是二级
        const parentCategory = findCategoryById(categoryOptions.value, form.value.parentId);
        if (parentCategory && parentCategory.parentId !== 0) {
          proxy.$modal.msgError("最多只支持两级栏目，不能添加三级栏目");
          return;
        }
      }
      
      if (form.value.id != undefined) {
        updateCategory(form.value).then(response => {
          proxy.$modal.msgSuccess("修改成功");
          open.value = false;
          getList();
        });
      } else {
        addCategory(form.value).then(response => {
          proxy.$modal.msgSuccess("新增成功");
          open.value = false;
          getList();
        });
      }
    }
  });
}

/** 根据ID查找栏目 */
function findCategoryById(list, id) {
  for (const item of list) {
    if (item.id === id) {
      return item;
    }
    if (item.children && item.children.length > 0) {
      const found = findCategoryById(item.children, id);
      if (found) return found;
    }
  }
  return null;
}

/** 删除按钮操作 */
function handleDelete(row) {
  // 检查是否有子栏目
  if (row.children && row.children.length > 0) {
    proxy.$modal.msgError("该栏目下存在子栏目，无法删除");
    return;
  }
  
  proxy.$modal.confirm('是否确认删除栏目"' + row.name + '"？').then(function () {
    return delCategory(row.id);
  }).then(() => {
    getList();
    proxy.$modal.msgSuccess("删除成功");
  }).catch(() => {});
}

/** 初始化 */
onMounted(() => {
  getList();
});
</script>

<style scoped>
.mb8 {
  margin-bottom: 8px;
}
</style>
