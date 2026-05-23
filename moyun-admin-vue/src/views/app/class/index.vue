<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="班级" prop="className">
        <el-input
          v-model="queryParams.className"
          placeholder="请输入班级"
          clearable
          @keyup.enter="handleQuery"
        />
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
          v-hasPermi="['app:class:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="Edit"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['app:class:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="Delete"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['app:class:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="Download"
          @click="handleExport"
          v-hasPermi="['app:class:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="classList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="主键" align="center" prop="id" />
      <el-table-column label="班级" align="center" prop="className" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['app:class:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['app:class:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    
    <pagination
      v-show="total>0"
      :total="total"
      v-model:page="queryParams.pageNum"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />

    <!-- 添加或修改班级对话框 -->
    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form ref="classRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="班级" prop="className">
          <el-input v-model="form.className" placeholder="请输入班级" />
        </el-form-item>
        <el-divider content-position="center">学生信息</el-divider>
        <el-row :gutter="10" class="mb8">
          <el-col :span="1.5">
            <el-button type="primary" icon="Plus" @click="handleAddMyStudent">添加</el-button>
          </el-col>
          <el-col :span="1.5">
            <el-button type="danger" icon="Delete" @click="handleDeleteMyStudent">删除</el-button>
          </el-col>
        </el-row>
        <el-table :data="myStudentList" :row-class-name="rowMyStudentIndex" @selection-change="handleMyStudentSelectionChange" ref="myStudent">
          <el-table-column type="selection" width="50" align="center" />
          <el-table-column label="序号" align="center" prop="index" width="50"/>
          <el-table-column label="姓名" prop="name" width="150">
            <template #default="scope">
              <el-input v-model="scope.row.name" placeholder="请输入姓名" />
            </template>
          </el-table-column>
          <el-table-column label="性别" prop="sex" width="150">
            <template #default="scope">
              <el-select v-model="scope.row.sex" placeholder="请选择性别">
                <el-option
                  v-for="dict in sys_user_sex"
                  :key="dict.value"
                  :label="dict.label"
                  :value="dict.value"
                ></el-option>
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="年龄" prop="age" width="150">
            <template #default="scope">
              <el-input v-model="scope.row.age" placeholder="请输入年龄" />
            </template>
          </el-table-column>
        </el-table>
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

<script setup name="Class">
import { listClass, getClass, delClass, addClass, updateClass } from "@/api/app/class";

const { proxy } = getCurrentInstance();
const { sys_user_sex } = proxy.useDict('sys_user_sex');

const classList = ref([]);
const myStudentList = ref([]);
const open = ref(false);
const loading = ref(true);
const showSearch = ref(true);
const ids = ref([]);
const checkedMyStudent = ref([]);
const single = ref(true);
const multiple = ref(true);
const total = ref(0);
const title = ref("");

const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    className: null
  },
  rules: {
    className: [
      { required: true, message: "班级不能为空", trigger: "blur" }
    ]
  }
});

const { queryParams, form, rules } = toRefs(data);

/** 查询班级列表 */
function getList() {
  loading.value = true;
  listClass(queryParams.value).then(response => {
    classList.value = response.rows;
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
    className: null
  };
  myStudentList.value = [];
  proxy.resetForm("classRef");
}

/** 搜索按钮操作 */
function handleQuery() {
  queryParams.value.pageNum = 1;
  getList();
}

/** 重置按钮操作 */
function resetQuery() {
  proxy.resetForm("queryRef");
  handleQuery();
}

// 多选框选中数据
function handleSelectionChange(selection) {
  ids.value = selection.map(item => item.id);
  single.value = selection.length != 1;
  multiple.value = !selection.length;
}

/** 新增按钮操作 */
function handleAdd() {
  reset();
  open.value = true;
  title.value = "添加班级";
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset();
  const _id = row.id || ids.value
  getClass(_id).then(response => {
    form.value = response.data;
    myStudentList.value = response.data.myStudentList;
    open.value = true;
    title.value = "修改班级";
  });
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["classRef"].validate(valid => {
    if (valid) {
      form.value.myStudentList = myStudentList.value;
      if (form.value.id != null) {
        updateClass(form.value).then(response => {
          proxy.$modal.msgSuccess("修改成功");
          open.value = false;
          getList();
        });
      } else {
        addClass(form.value).then(response => {
          proxy.$modal.msgSuccess("新增成功");
          open.value = false;
          getList();
        });
      }
    }
  });
}

/** 删除按钮操作 */
function handleDelete(row) {
  const _ids = row.id || ids.value;
  proxy.$modal.confirm('是否确认删除班级编号为"' + _ids + '"的数据项？').then(function() {
    return delClass(_ids);
  }).then(() => {
    getList();
    proxy.$modal.msgSuccess("删除成功");
  }).catch(() => {});
}

/** 学生序号 */
function rowMyStudentIndex({ row, rowIndex }) {
  row.index = rowIndex + 1;
}

/** 学生添加按钮操作 */
function handleAddMyStudent() {
  let obj = {};
  obj.name = "";
  obj.sex = "";
  obj.age = "";
  myStudentList.value.push(obj);
}

/** 学生删除按钮操作 */
function handleDeleteMyStudent() {
  if (checkedMyStudent.value.length == 0) {
    proxy.$modal.msgError("请先选择要删除的学生数据");
  } else {
    const myStudents = myStudentList.value;
    const checkedMyStudents = checkedMyStudent.value;
    myStudentList.value = myStudents.filter(function(item) {
      return checkedMyStudents.indexOf(item.index) == -1
    });
  }
}

/** 复选框选中数据 */
function handleMyStudentSelectionChange(selection) {
  checkedMyStudent.value = selection.map(item => item.index)
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download('app/class/export', {
    ...queryParams.value
  }, `class_${new Date().getTime()}.xlsx`)
}

getList();
</script>
