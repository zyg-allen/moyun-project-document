<template>
  <div class="app-container">
    <el-form :model="queryParams" :inline="true" class="search-form">
      <el-form-item label="题库类型">
        <el-select v-model="queryParams.bankType" placeholder="全部类型" clearable style="width: 160px">
          <el-option v-for="t in BANK_TYPES" :key="t.value" :label="t.label" :value="t.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="分类名称">
        <el-input v-model="queryParams.name" placeholder="请输入名称" clearable @keyup.enter="getList" />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="queryParams.status" placeholder="请选择状态" clearable style="width: 120px">
          <el-option label="启用" value="active" />
          <el-option label="停用" value="inactive" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="getList">搜索</el-button>
        <el-button @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-alert
      type="info"
      :closable="false"
      show-icon
      class="tip-alert"
      title="题库分类支持按「考试类型 + 两级分类」组织：顶级分类可将 parent_id 留空，细分方向选择对应上级分类。"
    />

    <div class="button-group">
      <el-button type="primary" @click="handleAdd">新增</el-button>
    </div>

    <el-table v-loading="loading" :data="filteredList" row-key="id" :tree-props="{ children: 'children' }">
      <el-table-column label="名称" prop="name" min-width="200">
        <template #default="{ row }">
          <span :style="{ paddingLeft: (row.parentId && row.parentId !== 0 ? 16 : 0) + 'px' }">
            <span v-if="row.parentId && row.parentId !== 0" class="child-mark">└</span>
            {{ row.name }}
          </span>
        </template>
      </el-table-column>
      <el-table-column label="题库类型" width="120">
        <template #default="{ row }">
          <el-tag size="small" :type="bankTypeTag(row.bankType)">{{ bankTypeLabel(row.bankType) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="上级分类" width="130">
        <template #default="{ row }">{{ parentName(row.parentId) }}</template>
      </el-table-column>
      <el-table-column label="职业族群" prop="jobFamily" width="110">
        <template #default="{ row }">{{ row.jobFamily || '-' }}</template>
      </el-table-column>
      <el-table-column label="Slug" prop="slug" width="130" />
      <el-table-column label="题目数" prop="questionCount" width="90" />
      <el-table-column label="排序" prop="sort" width="70" />
      <el-table-column label="前台展示" width="100">
        <template #default="{ row }">
          <el-tag size="small" :type="row.visible === '1' ? 'info' : 'success'">
            {{ row.visible === '1' ? '隐藏' : '展示' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 'active' ? 'success' : 'info'">
            {{ row.status === 'active' ? '启用' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="150" fixed="right" align="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="handleEdit(row)">编辑</el-button>
          <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="640px">
      <el-form :model="form" label-width="110px">
        <el-form-item label="题库类型">
          <el-select v-model="form.bankType" placeholder="请选择题库类型" style="width: 100%">
            <el-option v-for="t in BANK_TYPES" :key="t.value" :label="t.label" :value="t.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="上级分类">
          <el-select v-model="form.parentId" placeholder="不选则为顶级分类" clearable style="width: 100%">
            <el-option
              v-for="c in topLevelOptions"
              :key="c.id"
              :label="`${c.name}（${bankTypeLabel(c.bankType)}）`"
              :value="c.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="名称"><el-input v-model="form.name" placeholder="请输入名称" /></el-form-item>
        <el-form-item label="Slug"><el-input v-model="form.slug" placeholder="英文标识，如 algorithm" /></el-form-item>
        <el-form-item label="职业族群">
          <el-input v-model="form.jobFamily" placeholder="如 后端 / 前端 / 测试 / 产品（可留空）" />
        </el-form-item>
        <el-form-item label="图标"><el-input v-model="form.icon" placeholder="图标名称，如 fa-code" /></el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="2" placeholder="请输入描述" />
        </el-form-item>
        <el-form-item label="排序"><el-input-number v-model="form.sort" :min="0" /></el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio value="active">启用</el-radio>
            <el-radio value="inactive">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="前台展示">
          <el-radio-group v-model="form.visible">
            <el-radio value="0">展示</el-radio>
            <el-radio value="1">隐藏</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import {
  listInterviewCategory, getInterviewCategory, addInterviewCategory,
  updateInterviewCategory, delInterviewCategory
} from '@/api/cms/interview';

/** 题库类型字典（与 portal_interview_category.bank_type 注释保持一致） */
const BANK_TYPES = [
  { value: 'interview', label: '面试题库', tag: 'primary' },
  { value: 'certification', label: '职业资格', tag: 'success' },
  { value: 'civil-service', label: '公务员', tag: 'warning' },
  { value: 'postgraduate', label: '考研', tag: 'danger' },
  { value: 'other', label: '其他', tag: 'info' }
];

const loading = ref(true);
const allList = ref([]);
const queryParams = reactive({ name: '', status: '', bankType: '' });

/** 顶级分类（供「上级分类」下拉使用；避免把自己选成自己的父级） */
const topLevelOptions = computed(() =>
  allList.value.filter(c => (!c.parentId || c.parentId === 0) && c.id !== form.value.id)
);

const filteredList = computed(() => {
  const list = allList.value.filter(item => {
    const matchName = !queryParams.name || (item.name && item.name.includes(queryParams.name));
    const matchStatus = !queryParams.status || item.status === queryParams.status;
    const matchBank = !queryParams.bankType || item.bankType === queryParams.bankType;
    return matchName && matchStatus && matchBank;
  });
  // 组装两级树：顶级在前，子级挂到父节点下（父级缺失时降级为顶级，避免数据丢失）
  const byId = new Map(list.map(i => [i.id, { ...i, children: [] }]));
  const roots = [];
  byId.forEach(node => {
    const pid = node.parentId;
    if (pid && pid !== 0 && byId.has(pid)) {
      byId.get(pid).children.push(node);
    } else {
      roots.push(node);
    }
  });
  roots.forEach(r => { if (!r.children.length) delete r.children; });
  return roots;
});

function bankTypeLabel(v) {
  return (BANK_TYPES.find(t => t.value === v) || { label: v || '面试题库' }).label;
}
function bankTypeTag(v) {
  return (BANK_TYPES.find(t => t.value === v) || { tag: 'primary' }).tag;
}
function parentName(pid) {
  if (!pid || pid === 0) return '顶级';
  const p = allList.value.find(c => c.id === pid);
  return p ? p.name : `#${pid}`;
}

const dialogVisible = ref(false);
const dialogTitle = computed(() => (form.value.id ? '编辑分类' : '新增分类'));
const emptyForm = () => ({
  id: null, name: '', slug: '', bankType: 'interview', parentId: null, jobFamily: '',
  icon: '', description: '', sort: 0, status: 'active', visible: '0'
});
const form = ref(emptyForm());

async function getList() {
  loading.value = true;
  try {
    const res = await listInterviewCategory();
    allList.value = res.data || [];
  } catch (e) { /* ignore */ } finally {
    loading.value = false;
  }
}

function resetQuery() { queryParams.name = ''; queryParams.status = ''; queryParams.bankType = ''; }

function handleAdd() {
  form.value = emptyForm();
  dialogVisible.value = true;
}

async function handleEdit(row) {
  try {
    const res = await getInterviewCategory(row.id);
    const data = res.data || {};
    form.value = {
      id: data.id,
      name: data.name || '',
      slug: data.slug || '',
      bankType: data.bankType || 'interview',
      parentId: data.parentId && data.parentId !== 0 ? data.parentId : null,
      jobFamily: data.jobFamily || '',
      icon: data.icon || '',
      description: data.description || '',
      sort: data.sort || 0,
      status: data.status || 'active',
      visible: data.visible || '0'
    };
    dialogVisible.value = true;
  } catch (e) { /* ignore */ }
}

async function submitForm() {
  if (!form.value.name) { ElMessage.warning('请输入名称'); return; }
  const payload = { ...form.value, parentId: form.value.parentId || 0 };
  try {
    if (payload.id) {
      await updateInterviewCategory(payload);
      ElMessage.success('修改成功');
    } else {
      await addInterviewCategory(payload);
      ElMessage.success('新增成功');
    }
    dialogVisible.value = false;
    getList();
  } catch (e) { /* ignore */ }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm('确认删除该分类？若存在子分类请先处理子分类。', '提示', { type: 'warning' });
    await delInterviewCategory(row.id);
    ElMessage.success('删除成功');
    getList();
  } catch (e) { /* ignore */ }
}

onMounted(() => getList());
</script>

<style scoped>
.app-container { padding: 20px; }
.search-form, .button-group { margin-bottom: 16px; }
.tip-alert { margin-bottom: 12px; }
.child-mark { vertical-align: -2px; margin-right: 2px; color: #909399; }
</style>
