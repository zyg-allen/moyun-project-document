<template>
   <div class="app-container">
      <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch">
         <el-form-item label="字典名称" prop="dictType">
            <el-select v-model="queryParams.dictType" style="width: 200px">
               <el-option
                  v-for="item in typeOptions"
                  :key="item.dictId"
                  :label="item.dictName"
                  :value="item.dictType"
               />
            </el-select>
         </el-form-item>
         <el-form-item label="字典标签" prop="dictLabel">
            <el-input
               v-model="queryParams.dictLabel"
               placeholder="请输入字典标签"
               clearable
               style="width: 200px"
               @keyup.enter="handleQuery"
            />
         </el-form-item>
         <el-form-item label="状态" prop="status">
            <el-select v-model="queryParams.status" placeholder="数据状态" clearable style="width: 200px">
               <el-option
                  v-for="dict in sys_normal_disable"
                  :key="dict.value"
                  :label="dict.label"
                  :value="dict.value"
               />
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
               v-hasPermi="['system:dict:add']"
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
         <right-toolbar v-model:showSearch="showSearch" @queryTable="getTreeList"></right-toolbar>
      </el-row>

      <el-table
         v-if="refreshTable"
         v-loading="loading"
         :data="treeList"
         row-key="dictCode"
         :default-expand-all="isExpandAll"
         :tree-props="{ children: 'children', hasChildren: 'hasChildren' }"
      >
         <el-table-column label="字典标签" prop="dictLabel" :show-overflow-tooltip="true" />
         <el-table-column label="字典键值" prop="dictValue" />
         <el-table-column label="字典排序" prop="dictSort" width="90" />
         <el-table-column label="状态" align="center" prop="status" width="80">
            <template #default="scope">
               <dict-tag :options="sys_normal_disable" :value="scope.row.status" />
            </template>
         </el-table-column>
         <el-table-column label="备注" prop="remark" :show-overflow-tooltip="true" />
         <el-table-column label="操作" align="right" width="210" class-name="small-padding fixed-width">
            <template #default="scope">
               <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['system:dict:edit']">修改</el-button>
               <el-button link type="primary" icon="Plus" @click="handleAdd(scope.row)" v-hasPermi="['system:dict:add']">新增</el-button>
               <el-button v-if="scope.row.status === '0'" link type="danger" icon="CircleClose" @click="handleChangeStatus(scope.row)" v-hasPermi="['system:dict:edit']">停用</el-button>
               <el-button v-else link type="success" icon="CircleCheck" @click="handleChangeStatus(scope.row)" v-hasPermi="['system:dict:edit']">启用</el-button>
            </template>
         </el-table-column>
      </el-table>

      <el-dialog :title="title" v-model="open" width="620px" append-to-body>
         <el-form ref="treeRef" :model="form" :rules="rules" label-width="90px">
            <el-form-item label="字典类型">
               <el-input v-model="form.dictType" :disabled="true" />
            </el-form-item>
            <el-form-item label="上级节点" prop="parentDictId">
               <el-tree-select
                  v-model="form.parentDictId"
                  :data="treeSelectOptions"
                  :props="{ value: 'dictCode', label: 'dictLabel', children: 'children' }"
                  value-key="dictCode"
                  placeholder="选择上级节点"
                  check-strictly
               />
            </el-form-item>
            <el-form-item label="数据标签" prop="dictLabel">
               <el-input v-model="form.dictLabel" placeholder="请输入数据标签" />
            </el-form-item>
            <el-form-item label="数据键值" prop="dictValue">
               <el-input v-model="form.dictValue" placeholder="请输入数据键值" />
            </el-form-item>
            <el-form-item label="端代码" prop="platformCode">
               <el-select v-model="form.platformCode" placeholder="所属端（空=全局）" clearable style="width: 100%">
                  <el-option
                     v-for="item in platformOptions"
                     :key="item.platformCode"
                     :label="item.platformName"
                     :value="item.platformCode"
                  />
               </el-select>
            </el-form-item>
            <el-form-item label="显示排序" prop="dictSort">
               <el-input-number v-model="form.dictSort" controls-position="right" :min="0" />
            </el-form-item>
            <el-form-item label="状态" prop="status">
               <el-radio-group v-model="form.status">
                  <el-radio
                     v-for="dict in sys_normal_disable"
                     :key="dict.value"
                     :label="dict.value"
                  >{{ dict.label }}</el-radio>
               </el-radio-group>
            </el-form-item>
            <el-form-item label="备注" prop="remark">
               <el-input v-model="form.remark" type="textarea" placeholder="请输入内容"></el-input>
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

<script setup name="DictTree">
import useDictStore from '@/store/modules/dict'
import { optionselect as getDictOptionselect, getType } from "@/api/system/dict/type";
import { getDictTreeAll, getData, addData, updateData } from "@/api/system/dict/data";
import { platformOptionselect } from "@/api/system/platform";

const { proxy } = getCurrentInstance();
const { sys_normal_disable } = proxy.useDict("sys_normal_disable");

const treeList = ref([]);
const allData = ref([]);
const open = ref(false);
const loading = ref(true);
const showSearch = ref(true);
const title = ref("");
const defaultDictType = ref("");
const typeOptions = ref([]);
const platformOptions = ref([]);
const treeSelectOptions = ref([]);
const isExpandAll = ref(false);
const refreshTable = ref(true);
const route = useRoute();

const data = reactive({
  form: {},
  queryParams: {
    dictType: undefined,
    dictLabel: undefined,
    status: undefined
  },
  rules: {
    dictLabel: [{ required: true, message: "数据标签不能为空", trigger: "blur" }],
    dictValue: [{ required: true, message: "数据键值不能为空", trigger: "blur" }],
    dictSort: [{ required: true, message: "数据顺序不能为空", trigger: "blur" }]
  }
});

const { queryParams, form, rules } = toRefs(data);

/** 平铺数据浅拷贝（去除 children，避免 handleTree 反复调用污染） */
function cloneFlat(list) {
  return (list || []).map(item => {
    const { children, ...rest } = item;
    return { ...rest };
  });
}

/** 收集某节点及其所有子孙节点 id（防止父节点选成自己/子孙成环） */
function descendantIds(list, id) {
  const result = new Set([id]);
  let changed = true;
  while (changed) {
    changed = false;
    for (const item of list) {
      if (result.has(item.parentDictId) && !result.has(item.dictCode)) {
        result.add(item.dictCode);
        changed = true;
      }
    }
  }
  return result;
}

/** 查询字典类型详细 */
function getTypes(dictId) {
  getType(dictId).then(response => {
    queryParams.value.dictType = response.data.dictType;
    defaultDictType.value = response.data.dictType;
    getTreeList();
  });
}

/** 查询字典类型列表（下拉切换用，仅树形字典） */
function getTypeList() {
  getDictOptionselect().then(response => {
    typeOptions.value = (response.data || []).filter(item => item.isTree === "1");
  });
}

/** 查询树形字典全量数据 */
function getTreeList() {
  loading.value = true;
  getDictTreeAll(queryParams.value.dictType).then(response => {
    allData.value = response.data || [];
    refreshTableData();
    loading.value = false;
  });
}

/** 按查询条件前端过滤并组装树 */
function refreshTableData() {
  const label = queryParams.value.dictLabel;
  const status = queryParams.value.status;
  const filtered = cloneFlat(allData.value).filter(item => {
    if (label && !(item.dictLabel || "").includes(label)) return false;
    if (status && item.status !== status) return false;
    return true;
  });
  treeList.value = proxy.handleTree(filtered, "dictCode", "parentDictId");
}

/** 查询端下拉选项 */
function getPlatformOptions() {
  platformOptionselect().then(response => {
    platformOptions.value = response.data;
  });
}

/** 上级节点下拉树（编辑时排除自身及子孙，避免成环） */
function getTreeSelect(excludeDictCode) {
  let flat = cloneFlat(allData.value);
  if (excludeDictCode != null) {
    const exclude = descendantIds(allData.value, excludeDictCode);
    flat = flat.filter(d => !exclude.has(d.dictCode));
  }
  const tree = proxy.handleTree(flat, "dictCode", "parentDictId");
  treeSelectOptions.value = [{ dictCode: 0, dictLabel: "一级节点", children: tree }];
}

/** 取消按钮 */
function cancel() {
  open.value = false;
  reset();
}
/** 表单重置 */
function reset() {
  form.value = {
    dictCode: undefined,
    dictLabel: undefined,
    dictValue: undefined,
    parentDictId: 0,
    platformCode: undefined,
    dictSort: 0,
    status: "0",
    remark: undefined,
    dictType: defaultDictType.value
  };
  proxy.resetForm("treeRef");
}
/** 搜索按钮操作 */
function handleQuery() {
  if (queryParams.value.dictType !== defaultDictType.value) {
    defaultDictType.value = queryParams.value.dictType;
    getTreeList();
  } else {
    refreshTableData();
  }
}
/** 重置按钮操作 */
function resetQuery() {
  proxy.resetForm("queryRef");
  queryParams.value.dictType = defaultDictType.value;
  refreshTableData();
}
/** 新增按钮操作 */
function handleAdd(row) {
  reset();
  if (row != null && row.dictCode) {
    form.value.parentDictId = row.dictCode;
  } else {
    form.value.parentDictId = 0;
  }
  getTreeSelect(undefined);
  open.value = true;
  title.value = "添加字典数据";
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
function handleUpdate(row) {
  reset();
  getData(row.dictCode).then(response => {
    form.value = response.data;
    getTreeSelect(row.dictCode);
    open.value = true;
    title.value = "修改字典数据";
  });
}
/** 提交按钮 */
function submitForm() {
  proxy.$refs["treeRef"].validate(valid => {
    if (valid) {
      form.value.isFirstLevel = (form.value.parentDictId == 0 || form.value.parentDictId == null) ? "1" : "0";
      if (form.value.dictCode != undefined) {
        updateData(form.value).then(response => {
          useDictStore().removeDict(queryParams.value.dictType);
          proxy.$modal.msgSuccess("修改成功");
          open.value = false;
          getTreeList();
        });
      } else {
        addData(form.value).then(response => {
          useDictStore().removeDict(queryParams.value.dictType);
          proxy.$modal.msgSuccess("新增成功");
          open.value = false;
          getTreeList();
        });
      }
    }
  });
}
/** 停用/启用按钮操作（逻辑停用，不物理删除；停用后后台仍可见，业务端不再返回）
 *  注意：此方案仅适用于后台同级别（内部）维护的字典；若业务允许用户自行增改字典，则不宜开放此入口。 */
function handleChangeStatus(row) {
  const nextStatus = row.status === "0" ? "1" : "0";
  const isDisable = nextStatus === "1";
  const actionText = isDisable ? "停用" : "启用";
  const message = isDisable
    ? '停用「' + row.dictLabel + '」后，后台仍可见，但业务端将不再返回该节点；请务必先核对业务是否仍引用该字典值，否则会出现数据找不到或错乱。确认停用？'
    : '确认启用字典「' + row.dictLabel + '」？';
  proxy.$modal.confirm(message).then(function() {
    const { children, ...payload } = row;
    payload.status = nextStatus;
    return updateData(payload);
  }).then(() => {
    useDictStore().removeDict(queryParams.value.dictType);
    proxy.$modal.msgSuccess(actionText + "成功");
    getTreeList();
  }).catch(() => {});
}

getTypes(route.params && route.params.dictId);
getTypeList();
getPlatformOptions();
</script>
