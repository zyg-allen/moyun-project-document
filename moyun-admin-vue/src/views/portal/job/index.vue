<template>
  <div class="app-container">
    <!-- 搜索表单 -->
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="职位名称" prop="title">
        <el-input
          v-model="queryParams.title"
          placeholder="请输入职位名称"
          clearable
          style="width: 200px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="公司" prop="companyId">
        <el-select
          v-model="queryParams.companyId"
          placeholder="请选择公司"
          clearable
          filterable
          style="width: 180px"
          @change="handleQuery"
        >
          <el-option
            v-for="item in companyOptions"
            :key="item.id"
            :label="item.name"
            :value="item.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="城市" prop="city">
        <el-input
          v-model="queryParams.city"
          placeholder="请输入城市"
          clearable
          style="width: 140px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="状态" clearable style="width: 140px">
          <el-option label="招聘中" value="open" />
          <el-option label="已关闭" value="closed" />
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
          v-hasPermi="['portal:job:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="Edit"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['portal:job:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="Delete"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['portal:job:remove']"
        >删除</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <!-- 数据表格 -->
    <el-table v-loading="loading" :data="jobList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="编号" align="center" prop="id" width="70" />
      <el-table-column label="职位名称" align="center" prop="title" width="180" :show-overflow-tooltip="true" />
      <el-table-column label="公司" align="center" prop="companyId" width="160">
        <template #default="scope">
          <span>{{ getCompanyName(scope.row.companyId) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="城市" align="center" prop="city" width="100" />
      <el-table-column label="薪资范围" align="center" prop="salaryMin" width="140">
        <template #default="scope">
          <span>{{ formatSalary(scope.row) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="经验要求" align="center" prop="experience" width="100" />
      <el-table-column label="学历要求" align="center" prop="education" width="100" />
      <el-table-column label="状态" align="center" prop="status" width="90">
        <template #default="scope">
          <el-tag :type="scope.row.status === 'open' ? 'success' : 'info'" size="small">
            {{ scope.row.status === 'open' ? '招聘中' : '已关闭' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" align="center" prop="createdTime" width="160">
        <template #default="scope">
          <span>{{ parseTime(scope.row.createdTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="160">
        <template #default="scope">
          <el-button
            link
            type="primary"
            icon="Edit"
            @click="handleUpdate(scope.row)"
            v-hasPermi="['portal:job:edit']"
          >修改</el-button>
          <el-button
            link
            type="danger"
            icon="Delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['portal:job:remove']"
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

    <!-- 添加或修改职位对话框 -->
    <el-dialog :title="title" v-model="open" width="780px" append-to-body>
      <el-form ref="jobRef" :model="form" :rules="rules" label-width="100px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="职位名称" prop="title">
              <el-input v-model="form.title" placeholder="请输入职位名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="所属公司" prop="companyId">
              <el-select
                v-model="form.companyId"
                placeholder="请选择公司"
                clearable
                filterable
                style="width: 100%"
              >
                <el-option
                  v-for="item in companyOptions"
                  :key="item.id"
                  :label="item.name"
                  :value="item.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="工作城市" prop="city">
              <el-input v-model="form.city" placeholder="如：北京" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="经验要求" prop="experience">
              <el-select v-model="form.experience" placeholder="请选择经验要求" style="width: 100%">
                <el-option label="应届" value="应届" />
                <el-option label="1年以内" value="1年以内" />
                <el-option label="1-3年" value="1-3年" />
                <el-option label="3-5年" value="3-5年" />
                <el-option label="5-10年" value="5-10年" />
                <el-option label="10年以上" value="10年以上" />
                <el-option label="不限" value="不限" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="学历要求" prop="education">
              <el-select v-model="form.education" placeholder="请选择学历要求" style="width: 100%">
                <el-option label="大专" value="大专" />
                <el-option label="本科" value="本科" />
                <el-option label="硕士" value="硕士" />
                <el-option label="博士" value="博士" />
                <el-option label="不限" value="不限" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态" prop="status">
              <el-radio-group v-model="form.status">
                <el-radio label="open">招聘中</el-radio>
                <el-radio label="closed">已关闭</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="薪资下限" prop="salaryMin">
              <el-input-number v-model="form.salaryMin" :min="0" :step="1000" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="薪资上限" prop="salaryMax">
              <el-input-number v-model="form.salaryMax" :min="0" :step="1000" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="24">
            <el-form-item label="职位描述" prop="description">
              <el-input v-model="form.description" type="textarea" :rows="4" placeholder="请输入职位描述" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="24">
            <el-form-item label="任职要求" prop="requirement">
              <el-input v-model="form.requirement" type="textarea" :rows="4" placeholder="请输入任职要求" />
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
  </div>
</template>

<script setup name="Job">
import { ref, reactive, getCurrentInstance, onMounted } from "vue";
import { listJob, getJob, addJob, updateJob, delJob, listInterviewCompany } from "@/api/portal/job";

const { proxy } = getCurrentInstance();

// 公司下拉数据
const companyOptions = ref([]);
// 公司ID -> 名称映射，用于表格列显示
const companyMap = ref({});

// 搜索参数
const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  title: null,
  companyId: null,
  city: null,
  status: null
});

// 表格相关
const showSearch = ref(true);
const loading = ref(false);
const jobList = ref([]);
const total = ref(0);
const selectedRows = ref([]);
const single = ref(true);
const multiple = ref(true);

// 弹窗相关
const open = ref(false);
const title = ref("");
const jobRef = ref();
const form = ref({
  id: null,
  companyId: null,
  title: null,
  city: null,
  salaryMin: null,
  salaryMax: null,
  experience: null,
  education: null,
  description: null,
  requirement: null,
  status: "open"
});

// 校验规则
const rules = {
  title: [{ required: true, message: "职位名称不能为空", trigger: "blur" }],
  companyId: [{ required: true, message: "所属公司不能为空", trigger: "change" }],
  status: [{ required: true, message: "状态不能为空", trigger: "change" }]
};

// 查询列表
function getList() {
  loading.value = true;
  listJob(queryParams).then((response) => {
    const rows = response.data.records || [];
    const totalCount = response.data.total || 0;
    jobList.value = rows;
    total.value = totalCount;
    loading.value = false;
  }).catch((e) => {
    proxy.$modal.msgError("查询失败: " + e.message);
    loading.value = false;
  });
}

// 加载公司下拉数据
function loadCompanies() {
  listInterviewCompany({ status: "0" }).then((response) => {
    const list = response.data || [];
    companyOptions.value = list;
    const map = {};
    list.forEach((c) => {
      map[c.id] = c.name;
    });
    companyMap.value = map;
  }).catch(() => {
    companyOptions.value = [];
    companyMap.value = {};
  });
}

// 根据公司ID获取公司名称
function getCompanyName(companyId) {
  if (companyId == null) return "-";
  return companyMap.value[companyId] || ("#" + companyId);
}

// 格式化薪资显示
function formatSalary(row) {
  const min = row.salaryMin;
  const max = row.salaryMax;
  if (min == null && max == null) return "面议";
  if (min != null && max != null) return min + " - " + max;
  if (min != null) return min + " 以上";
  return max + " 以下";
}

// 取消
function cancel() {
  open.value = false;
  resetForm();
}

// 重置表单
function resetForm() {
  form.value = {
    id: null,
    companyId: null,
    title: null,
    city: null,
    salaryMin: null,
    salaryMax: null,
    experience: null,
    education: null,
    description: null,
    requirement: null,
    status: "open"
  };
  if (jobRef.value) jobRef.value.resetFields();
}

// 搜索重置
function resetQuery() {
  queryParams.pageNum = 1;
  queryParams.title = null;
  queryParams.companyId = null;
  queryParams.city = null;
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
  title.value = "新增职位";
  open.value = true;
}

// 修改
function handleUpdate(row) {
  resetForm();
  const id = row.id || selectedRows.value[0].id;
  getJob(id).then((response) => {
    const data = response.data || response;
    Object.assign(form.value, data);
    title.value = "修改职位";
    open.value = true;
  }).catch((e) => {
    proxy.$modal.msgError("查询详情失败: " + e.message);
  });
}

// 提交
function submitForm() {
  jobRef.value.validate((valid) => {
    if (valid) {
      const action = form.value.id ? updateJob(form.value) : addJob(form.value);
      action.then((response) => {
        const code = response.code;
        if (code === 200) {
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
  proxy.$modal.confirm('是否确认删除职位编号为"' + ids + '"的数据项？').then(() => {
    delJob(ids).then((response) => {
      const code = response.code;
      if (code === 200) {
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

onMounted(() => {
  loadCompanies();
  getList();
});
</script>
